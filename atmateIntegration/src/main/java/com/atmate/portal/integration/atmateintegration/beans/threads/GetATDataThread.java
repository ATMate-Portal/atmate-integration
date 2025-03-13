package com.atmate.portal.integration.atmateintegration.beans.threads;

import com.atmate.portal.integration.atmateintegration.database.entitites.Client;
import com.atmate.portal.integration.atmateintegration.database.entitites.Tax;
import com.atmate.portal.integration.atmateintegration.database.entitites.TaxType;
import com.atmate.portal.integration.atmateintegration.database.services.TaxService;
import com.atmate.portal.integration.atmateintegration.database.services.TaxTypeService;
import com.atmate.portal.integration.atmateintegration.utils.GSONFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Component
public class GetATDataThread implements Runnable {

    private Client client;
    private String password;
    @Value("${python.script.path}")
    private String scriptAbsolutePath;
    TaxService taxService;
    TaxTypeService taxTypeService;

    private static final Logger logger = LoggerFactory.getLogger(GetATDataThread.class);

    @Autowired
    public GetATDataThread(TaxService taxService, TaxTypeService taxTypeService) {
        this.taxService = taxService;
        this.taxTypeService = taxTypeService;
        logger.info("GetATDataThread inicializado com TaxService e TaxTypeService.");
    }

    @Override
    public void run() {
        logger.info("--------------- Thread iniciada para o cliente: {} ---------------", client.getName());
        doLoginAT(client.getNif(), password);
        logger.info("--------------- Login Feito para o cliente: {} ---------------", client.getName());
        getIUC();
        logger.info("--------------- IUC Obtido para o cliente: {} ---------------", client.getName());
        logger.info("--------------- Thread terminada para o cliente: {} ---------------", client.getName());
    }

    public void setClient(Client client) {
        this.client = client;
        logger.info("Cliente definido para a thread: {}", client.getName());
    }

    public void setPassword(String password) {
        this.password = password;
        logger.info("Password definida para a thread.");
    }

    private void doLoginAT(Integer nif, String password) {
        logger.info("Iniciando login no AT para o cliente com NIF: {}", nif);
        try {
            String atLoginFileName = "at_login.py";
            logger.info("Nome do script " + atLoginFileName);
            logger.info("Caminho absoluto " + scriptAbsolutePath);
            logger.info("Caminho do script de login: " + scriptAbsolutePath + atLoginFileName);
            String scriptPath = new File(scriptAbsolutePath + atLoginFileName).getAbsolutePath();

            String pythonPath = "C:\\Users\\Tiago Cardoso\\AppData\\Local\\Programs\\Python\\Python312\\python.exe";

            ProcessBuilder processBuilder = new ProcessBuilder(pythonPath, scriptPath);
            Map<String, String> environment = processBuilder.environment();
            environment.put("NIF", String.valueOf(nif));
            environment.put("PASSWORD", password);
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

    private void getIUC() {
        logger.info("Iniciando obtenção do IUC para o cliente: {}", client.getName());
        try {
            String atGetIUCFileName = "at_get_iuc.py";
            String taxJSON = "";
            String scriptPath = new File(scriptAbsolutePath + atGetIUCFileName).getAbsolutePath();
            logger.info("Caminho do script de obtenção do IUC: {}", scriptPath);

            System.out.println(System.getenv("PATH"));
            String pythonPath = "C:\\Users\\Tiago Cardoso\\AppData\\Local\\Programs\\Python\\Python312\\python.exe";

            ProcessBuilder processBuilder = new ProcessBuilder(pythonPath, scriptPath);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.ISO_8859_1));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("Saída do script de obtenção do IUC: {}", line);
                output.append(line);
            }
            taxJSON = output.toString();

            String formattedJSON = GSONFormatter.formatIUCJSON(taxJSON);
            logger.info("IUC do cliente {} obtido: {}", client.getName(), formattedJSON);

            Optional<TaxType> taxType = taxTypeService.getTaxTypeById(1);
            if (taxType.isPresent()) {
                logger.info("TaxType encontrado: {}", taxType.get().getId());
            } else {
                logger.warn("TaxType com ID 1 não encontrado.");
            }

            Optional<Tax> tax = taxService.getTaxByClientAndType(client, taxType.orElse(null));

            if (tax.isPresent()) {
                taxService.updateTax(tax.get().getId(), tax.get());
                logger.info("Tax atualizado para o cliente: {}", client.getName());
            } else {
                Tax newClientTax = new Tax();
                newClientTax.setTaxType(taxType.orElse(null));
                newClientTax.setTaxData(formattedJSON);
                newClientTax.setClient(client);
                taxService.createTax(newClientTax);
                logger.info("Novo Tax criado para o cliente: {}", client.getName());
            }

            process.waitFor();
        } catch (Exception e) {
            logger.error("Erro ao obter o IUC para o cliente: {}", client.getName(), e);
        }
    }
}