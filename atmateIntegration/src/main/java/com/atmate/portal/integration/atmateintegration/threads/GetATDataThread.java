package com.atmate.portal.integration.atmateintegration.threads;

import com.atmate.portal.integration.atmateintegration.beans.ClientDetailsBean;
import com.atmate.portal.integration.atmateintegration.database.entitites.*;
import com.atmate.portal.integration.atmateintegration.database.services.*;
import com.atmate.portal.integration.atmateintegration.utils.ClientDataUtils;
import com.atmate.portal.integration.atmateintegration.utils.GSONFormatter;
import com.atmate.portal.integration.atmateintegration.utils.enums.ErrorEnum;
import com.atmate.portal.integration.atmateintegration.utils.exceptions.ATMateException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Scope("prototype")
@Component
public class GetATDataThread implements Runnable {

    private Client client;
    private String password;
    @Value("${python.script.path}")
    private String scriptAbsolutePath;
    @Value("${python.path}")
    private String pythonPath;
    private boolean getTypeFromAT;
    TaxService taxService;
    TaxTypeService taxTypeService;
    @Autowired
    private GSONFormatter gsonFormatter;

    @Autowired
    ClientService clientService;
    @Autowired
    AddressService addressService;
    @Autowired
    ContactService contactService;
    @Autowired
    ClientTypeService clientTypeService;

    private static final Logger logger = LoggerFactory.getLogger(GetATDataThread.class);

    @Autowired
    public GetATDataThread(TaxService taxService, TaxTypeService taxTypeService) {
        this.taxService = taxService;
        this.taxTypeService = taxTypeService;
    }

    @Override
    public void run() {
        doLoginAT(client.getNif(), password);
        logger.info("--------------- Login Feito para o cliente: {} ---------------", client.getName());
        getClientPersonalData(client.getNif());
        logger.info("--------------- Obtidos dados do cliente: {} ---------------", client.getName());
        getIUC(client.getNif());
        logger.info("--------------- IUC Obtido para o cliente: {} ---------------", client.getName());
        getIMI(client.getNif());
        logger.info("--------------- IMI Obtido para o cliente: {} ---------------", client.getName());
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public void setGetTypeFromAT(boolean getTypeFromAT) {
        this.getTypeFromAT = getTypeFromAT;
    }

    private void doLoginAT(Integer nif, String password) {
        logger.info("Iniciando login no AT para o cliente com NIF: {}", nif);
        try {
            String atLoginFileName = "at_login.py";
            String scriptPath = new File(scriptAbsolutePath + atLoginFileName).getAbsolutePath();

            ProcessBuilder processBuilder = new ProcessBuilder(pythonPath, scriptPath);
            Map<String, String> environment = processBuilder.environment();
            environment.put("NIF", String.valueOf(nif));
            environment.put("PASSWORD", password);
            environment.put("SCRIPT_PATH", scriptAbsolutePath);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("Saída do script de login: {}", line);
            }

            int exitCode = process.waitFor();
            logger.info("Código de saída do script de login: {}", exitCode);

            if (exitCode != 0) {
                logger.error("Erro ao executar o script de login para o cliente com NIF: {}", nif);
            } else {
                logger.info("Login realizado com sucesso para o cliente com NIF: {}", nif);
            }

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.error("Erro durante o login no AT para o cliente com NIF: {}", nif, e);
        }
    }

    private void getIUC(Integer nif) {
        logger.info("Iniciando obtenção do IUC para o cliente: {}", client.getName());
        try {
            String atGetIUCFileName = "at_get_iuc.py";
            String taxJSONFromScript; // JSON bruto do script Python
            String scriptPath = new File(scriptAbsolutePath + atGetIUCFileName).getAbsolutePath();

            ProcessBuilder processBuilder = new ProcessBuilder(pythonPath, scriptPath);
            Map<String, String> environment = processBuilder.environment();
            environment.put("NIF", String.valueOf(nif));
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.ISO_8859_1));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                // logger.info("Saída do script de obtenção do IUC: {}", line); // Pode ser muito verboso
                output.append(line);
            }
            taxJSONFromScript = output.toString();

            logger.info("IUC (JSON bruto do script) do cliente {} obtido.", client.getName());
            // logger.debug("Conteúdo do taxJSONFromScript: {}", taxJSONFromScript); // Para depuração

            // ObjectMapper para todas as operações JSON
            ObjectMapper objectMapper = new ObjectMapper();

            // Parsear o JSON bruto do script uma vez
            JsonNode rootOfRawJson = objectMapper.readTree(taxJSONFromScript);
            JsonNode detalhesVeiculosArrayNode = rootOfRawJson.path("detalhes_veiculos"); // Plural, como no seu JSON bruto

            // Obter a lista de resumos formatada (como antes)
            // Esta linha assume que gsonFormatter.formatIUCTaxJSON processa a parte "resumo_iuc" do taxJSONFromScript
            List<Map<String, String>> formattedListResumos = gsonFormatter.formatIUCTaxJSON(taxJSONFromScript);

            Optional<TaxType> taxType = taxTypeService.getTaxTypeById(1); // Assumindo IUC é ID 1

            for (Map<String, String> resumoMap : formattedListResumos) { // resumoMap contém o resumo de um veículo
                String matricula = resumoMap.get("Matrícula"); // Obter matrícula diretamente do resumoMap

                if (matricula == null || matricula.isEmpty()) {
                    logger.warn("Matrícula não encontrada ou vazia no resumoMap para o cliente {}: {}. A processar próximo.", client.getName(), resumoMap);
                    continue;
                }

                // Construir o ObjectNode final que será guardado
                ObjectNode jsonFinalParaGuardar = objectMapper.createObjectNode();

                // 1. Adicionar todos os campos do resumoMap (resumo do IUC) ao JSON final
                for (Map.Entry<String, String> entry : resumoMap.entrySet()) {
                    jsonFinalParaGuardar.put(entry.getKey(), entry.getValue());
                }

                // 2. Encontrar e adicionar o objeto "detalhes_veiculo" (singular) correspondente
                JsonNode detalhesDoVeiculoEspecifico = null;
                if (detalhesVeiculosArrayNode != null && detalhesVeiculosArrayNode.isArray()) {
                    for (JsonNode veiculoDetalheNode : detalhesVeiculosArrayNode) {
                        // Compara a matrícula do resumo com a matrícula nos detalhes dos veículos
                        if (veiculoDetalheNode.path("matricula").asText("").equals(matricula)) { // Usar asText("") para evitar NullPointerException
                            detalhesDoVeiculoEspecifico = veiculoDetalheNode;
                            break; // Encontrou os detalhes, pode sair do loop interno
                        }
                    }
                }

                if (detalhesDoVeiculoEspecifico != null) {
                    // Adiciona o objeto de detalhes encontrado sob a chave "detalhes_veiculo" (singular)
                    jsonFinalParaGuardar.set("detalhes_veiculo", detalhesDoVeiculoEspecifico.deepCopy());
                } else {
                    logger.warn("Detalhes do veículo não encontrados para a matrícula: {} no JSON bruto para o cliente {}. A chave 'detalhes_veiculo' não será adicionada.", matricula, client.getName());
                    // Se o frontend SEMPRE espera a chave "detalhes_veiculo", mesmo que vazia:
                    // jsonFinalParaGuardar.set("detalhes_veiculo", objectMapper.createObjectNode());
                }

                // Converter o JSON combinado para uma string
                String jsonStringCompletaParaTaxData = objectMapper.writeValueAsString(jsonFinalParaGuardar);
                // logger.debug("JSON final para matrícula {} do cliente {}: {}", matricula, client.getName(), jsonStringCompletaParaTaxData);

                // Lógica original para encontrar ou criar a entidade Tax
                Tax clientTax = taxService.getTaxByClientAndType(client, taxType.orElse(null), matricula);

                if (clientTax != null) {
                    clientTax.setTaxData(jsonStringCompletaParaTaxData); // ATUALIZADO: Usar o JSON combinado
                    taxService.updateTax(clientTax.getId(), clientTax);
                    logger.info("Imposto IUC atualizado para o cliente: {} e matrícula: {}", client.getName(), matricula);
                } else {
                    Tax newClientTax = new Tax();
                    newClientTax.setTaxType(taxType.orElse(null));
                    newClientTax.setTaxData(jsonStringCompletaParaTaxData); // ATUALIZADO: Usar o JSON combinado
                    newClientTax.setClient(client);
                    // Se a sua entidade Tax tiver um campo para a matrícula (ou outro identificador do veículo),
                    // certifique-se de que o define aqui também.
                    // Ex: newClientTax.setVehicleIdentifier(matricula);
                    taxService.createTax(newClientTax);
                    logger.info("Novo imposto IUC criado para o cliente: {} e matrícula: {}", client.getName(), matricula);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.warn("O script Python de obtenção do IUC para o cliente {} terminou com código de saída: {}", client.getName(), exitCode);
                // Considere ler o errorStream do processo para obter mais detalhes do erro do script
            }

        } catch (Exception e) {
            logger.error("Erro fatal ao obter o IUC para o cliente: {}", client.getName(), e);
            // Dependendo da sua estratégia de tratamento de erros, pode querer propagar a exceção
            // ou tomar outras ações.
        }
    }

    private void getIMI(Integer nif) {
        logger.info("Iniciando obtenção do IMI para o cliente: {}", client.getName());
        try {
            String atGetIMIFileName = "at_get_imi.py";
            String taxJSON = "";
            String scriptPath = new File(scriptAbsolutePath + atGetIMIFileName).getAbsolutePath();

            ProcessBuilder processBuilder = new ProcessBuilder(pythonPath, scriptPath);
            Map<String, String> environment = processBuilder.environment();
            environment.put("NIF", String.valueOf(nif));
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.ISO_8859_1));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("Saída do script de obtenção do IMI: {}", line);
                output.append(line);
            }
            taxJSON = output.toString();

            logger.info("IMI do cliente {} obtido: {}", client.getName(), taxJSON);

            List<Map<String, String>> formattedList = gsonFormatter.formatTaxJSON(taxJSON);

            Optional<TaxType> taxType = taxTypeService.getTaxTypeById(5);

            for (Map<String, String> formattedMap : formattedList) {
                String formattedJSON = new ObjectMapper().writeValueAsString(formattedMap);

                //Extrair matricula
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(formattedJSON);
                String notaCobranca = rootNode.get("Nº Nota Cob.").asText();

                Tax clientTax = taxService.getTaxByClientAndType(client, taxType.orElse(null), notaCobranca);

                if (clientTax!=null) {
                    clientTax.setTaxData(formattedJSON); // Atualize apenas os campos necessários
                    taxService.updateTax(clientTax.getId(), clientTax);
                    logger.info("Imposto atualizado para o cliente: {}", client.getName());
                } else {
                    Tax newClientTax = new Tax();
                    newClientTax.setTaxType(taxType.orElse(null));
                    newClientTax.setTaxData(formattedJSON);
                    newClientTax.setClient(client);
                    taxService.createTax(newClientTax);
                    logger.info("Novo imposto IMI criado para o cliente: {}", client.getName());
                }
            }

            process.waitFor();
        } catch (Exception e) {
            logger.error("Erro ao obter o IMI para o cliente: {}", client.getName(), e);
        }
    }

    public void getClientPersonalData(Integer nif){
        logger.info("Iniciando obtenção de dados pessoais para o cliente: {}", client.getNif());
        try {
            String atGetClientData = "at_get_cliente_info.py";
            String taxJSON = "";
            String scriptPath = new File(scriptAbsolutePath + atGetClientData).getAbsolutePath();

            ProcessBuilder processBuilder = new ProcessBuilder(pythonPath, scriptPath);
            Map<String, String> environment = processBuilder.environment();
            environment.put("NIF", String.valueOf(nif));
            environment.put("getTypeFromAT", String.valueOf(this.getTypeFromAT));
            Process process = processBuilder.start();

            //Obter erros da execução python
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder errorOutput = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                logger.error("⚠️ ERRO do script: {}", errorLine);
                errorOutput.append(errorLine).append("\n");
            }
            if(!errorOutput.isEmpty())
                throw new ATMateException(ErrorEnum.SCRAPING_PYTHON_ERROR);

            //Se nao existir erros, ler output do python
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                //logger.info("Saída do script de obter dados cliente: {}", line);
                taxJSON = line;
            }

            if (process.waitFor() != 0) {
                logger.error("Erro ao executar o script de obter dados cliente para o cliente com NIF: {}", nif);
                throw new ATMateException(ErrorEnum.SCRAPING_PERSONAL_DATA_ERROR);
            }

            //Converter dados vindos do python para objeto
            ObjectMapper mapper = new ObjectMapper();
            ClientDetailsBean clientData = mapper.readValue(taxJSON, ClientDetailsBean.class);

            //Client Part
            this.client.setName(clientData.getNome());
            this.client.setGender(ClientDataUtils.formatGender(clientData.getSexo()));
            this.client.setBirthDate(ClientDataUtils.parseData(clientData.getData_nascimento()));
            this.client.setNationality(clientData.getNacionalidade());

            if(String.valueOf(this.client.getNif()).startsWith("1") || String.valueOf(this.client.getNif()).startsWith("2") || String.valueOf(this.client.getNif()).startsWith("3")){
                getTypeFromAT = true;
            }

            logger.info("Cliente " + client.getName() + " | atividade exercida: " + clientData.isAtividade_exercida_encontrada() + " | Data cessacao: " + clientData.getData_cessacao() + " | getTypeFromAT: " + getTypeFromAT);

            if(clientData.isAtividade_exercida_encontrada() && null==clientData.getData_cessacao() && getTypeFromAT){
                logger.info("A atualizar cliente" + clientData.getNome() + " para tipo de cliente ENI.");
                Optional<ClientType> clientType = clientTypeService.getClientTypeById(1);
                this.client.setClientType(clientType.orElse(null));
            }else{
                //Garantir que fica com o tipo certo
                Optional<ClientType> clientType = clientTypeService.getClientTypeById(4);
                this.client.setClientType(clientType.orElse(null));
            }

            clientService.updateClient(this.client.getId(), client);

            //Address Part
            Address address = ClientDataUtils.buildAddress(this.client, clientData);
            if (!addressService.existsAddressForClient(address)) {
                addressService.createAddress(address);
            } else {
                logger.info("Morada já existente para cliente {}", address.getClient().getNif());
            }

            //Contacts Part
            String phone_alt = clientData.getTelefone_alt();
            if (!phone_alt.isBlank() && !phone_alt.trim().equals("-")) {
                Contact phone_altf = ClientDataUtils.buildContactPhone(this.client, phone_alt);
                if (!contactService.existsContactForClient(phone_altf)) {
                    contactService.createContact(phone_altf);
                } else {
                    logger.info("Telefone já existe para cliente {}: {}", client.getNif(), phone_alt);
                }
            }

            String email_alt = clientData.getEmail_alt();
            if (!email_alt.isBlank() && !email_alt.trim().equals("-")) {
                Contact email_altf = ClientDataUtils.buildContactEmail(this.client, email_alt);
                if (!contactService.existsContactForClient(email_altf)) {
                    contactService.createContact(email_altf);
                } else {
                    logger.info("Email já existe para cliente {}: {}", client.getNif(), email_alt);
                }
            }

        } catch (Exception e) {
            logger.error("Erro ao obter os dados do cliente: {}", client.getNif(), e);
            throw new ATMateException(ErrorEnum.SCRAPING_PERSONAL_DATA_ERROR);
        }
    }
}