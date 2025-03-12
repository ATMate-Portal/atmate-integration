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
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class GetATDataThread implements Runnable {

    private Client client;
    private String password;
    private final String scriptAbsolutePath = "src/main/resources/scripts/";
    TaxService taxService;
    TaxTypeService taxTypeService;

    private static final Logger logger = LoggerFactory.getLogger(GetATDataThread.class);

    @Autowired
    public GetATDataThread(TaxService taxService, TaxTypeService taxTypeService) {
        this.taxService = taxService;
        this.taxTypeService = taxTypeService;
    }

    @Override
    public void run() {
        logger.info("A iniciar scraping do cliente " + client.getName() + " NIF: " + client.getNif());
        doLoginAT(client.getNif(), password);
        getIUC();
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private void doLoginAT(Integer nif, String password){
        try {
            String atLoginFileName = "at_login.py";
            String scriptPath = new File(scriptAbsolutePath+ atLoginFileName).getAbsolutePath();
            ProcessBuilder processBuilder = new ProcessBuilder("python", scriptPath, String.valueOf(nif), password);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                logger.info(line);
            }

            int exitCode = process.waitFor();
            logger.info("Código de saída do Python: " + exitCode);

            if (exitCode != 0) {
                logger.info("Erro ao executar o script Python.");
            }

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.info("A iniciar scraping do cliente " + client.getName() + " NIF: " + client.getNif());
            e.printStackTrace();
        }
    }

    private void getIUC(){
        try {
            String atGetIUCFileName = "at_get_iuc.py";
            String taxJSON = "";
            String scriptPath = new File(scriptAbsolutePath+atGetIUCFileName).getAbsolutePath();
            ProcessBuilder processBuilder = new ProcessBuilder("python", scriptPath);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.ISO_8859_1));


            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                taxJSON = line;
            }

            String formattedJSON = GSONFormatter.formatIUCJSON(taxJSON);

            //IUC é o tipo 1
            Optional<TaxType> taxType = taxTypeService.getTaxTypeById(1);

            Optional<Tax> tax = taxService.getTaxByClientAndType(client, taxType.orElse(null));

            if(tax.isPresent()){
                taxService.updateTax(tax.orElse(null).getId(), tax.orElse(null));

            }else{

                Tax newClientTax = new Tax();
                newClientTax.setTaxType(taxType.orElse(null));
                newClientTax.setTaxData(formattedJSON);
                newClientTax.setClient(client);

                taxService.createTax(newClientTax);
            }



            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
