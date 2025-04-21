package com.atmate.portal.integration.atmateintegration.controller;

import com.atmate.portal.integration.atmateintegration.database.entitites.Client;
import com.atmate.portal.integration.atmateintegration.database.services.AtCredentialService;
import com.atmate.portal.integration.atmateintegration.database.services.ClientService;
import com.atmate.portal.integration.atmateintegration.services.ScrappingService;
import com.atmate.portal.integration.atmateintegration.utils.enums.ErrorEnum;
import com.atmate.portal.integration.atmateintegration.utils.exceptions.ATMateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("clients")
public class ClientController {
    @Autowired
    ClientService clientService;
    @Autowired
    AtCredentialService atCredentialService;

    @Autowired
    ScrappingService scrappingService;

    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

    @GetMapping("/sync/{clientId}")
    public ResponseEntity<?> sync(@PathVariable Integer clientId, @RequestParam(value = "getTypeFromAT", required = false, defaultValue = "false") boolean getTypeFromAT) throws Exception {
        logger.info("Syncing client...");
        Client client = clientService.getClientById(clientId)
                .orElseThrow(() -> new ATMateException(ErrorEnum.CLIENT_NOT_FOUND));
        logger.info("Syncing client with NIF = " + client.getNif());

        scrappingService.syncClient(client, getTypeFromAT);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
