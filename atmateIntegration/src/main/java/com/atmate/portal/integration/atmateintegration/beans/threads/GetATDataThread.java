package com.atmate.portal.integration.atmateintegration.beans.threads;

import com.atmate.portal.integration.atmateintegration.database.entitites.Client;
import com.atmate.portal.integration.atmateintegration.database.entitites.Tax;
import com.atmate.portal.integration.atmateintegration.database.entitites.TaxType;
import com.atmate.portal.integration.atmateintegration.database.services.TaxService;
import com.atmate.portal.integration.atmateintegration.database.services.TaxTypeService;
import com.atmate.portal.integration.atmateintegration.utils.GSONFormatter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    TaxService taxService;
    TaxTypeService taxTypeService;
    @Autowired
    private GSONFormatter gsonFormatter;

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
            String taxJSON = "";
            String scriptPath = new File(scriptAbsolutePath + atGetIUCFileName).getAbsolutePath();

            ProcessBuilder processBuilder = new ProcessBuilder(pythonPath, scriptPath);
            Map<String, String> environment = processBuilder.environment();
            environment.put("NIF", String.valueOf(nif));
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.ISO_8859_1));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("Saída do script de obtenção do IUC: {}", line);
                output.append(line);
            }
            taxJSON = output.toString();

            logger.info("IUC do cliente {} obtido: {}", client.getName(), taxJSON);

            List<Map<String, String>> formattedList = gsonFormatter.formatTaxJSON(taxJSON);

            Optional<TaxType> taxType = taxTypeService.getTaxTypeById(1);

            for (Map<String, String> formattedMap : formattedList) {
                String formattedJSON = new ObjectMapper().writeValueAsString(formattedMap);

                //Extrair matricula
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(formattedJSON);
                String matricula = rootNode.get("Matrícula").asText();

                Tax clientTax = taxService.getTaxByClientAndType(client, taxType.orElse(null), matricula);

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
                    logger.info("Novo imposto IUC criado para o cliente: {}", client.getName());
                }
            }

            process.waitFor();
        } catch (Exception e) {
            logger.error("Erro ao obter o IUC para o cliente: {}", client.getName(), e);
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
}