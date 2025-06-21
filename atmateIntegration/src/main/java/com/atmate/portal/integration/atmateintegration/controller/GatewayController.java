package com.atmate.portal.integration.atmateintegration.controller;

import com.atmate.portal.integration.atmateintegration.database.entitites.Client;
import com.atmate.portal.integration.atmateintegration.database.services.AtCredentialService;
import com.atmate.portal.integration.atmateintegration.database.services.ClientService;
import com.atmate.portal.integration.atmateintegration.services.NotificationSendingService;
import com.atmate.portal.integration.atmateintegration.services.NotificationService;
import com.atmate.portal.integration.atmateintegration.services.ScrappingService;
import com.atmate.portal.integration.atmateintegration.utils.enums.ErrorEnum;
import com.atmate.portal.integration.atmateintegration.utils.exceptions.ATMateException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("gateway")
@Tag(name = "Gestão de clientes e notificações", description = "Endpoints para sincronização de dados AT e envio de notificações.")
public class GatewayController {
    @Autowired
    ClientService clientService;
    @Autowired
    AtCredentialService atCredentialService;

    @Autowired
    ScrappingService scrappingService;
    @Autowired
    NotificationService notificationService;

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    @GetMapping("/sync/{clientId}")
    @Operation(
            summary = "Atualizar dados AT de um cliente",
            description = "Endpoint que invoca o web scraping à Autoridade Tributária (AT) para atualização completa dos dados de um cliente específico. Retorna 200 OK em caso de sucesso."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Dados do cliente sincronizados com sucesso.",
                    content = @Content(schema = @Schema(implementation = Void.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Requisição inválida, por exemplo, cliente não encontrado.",
                    content = @Content(schema = @Schema(
                            type = "object",
                            example = "{\n  \"timestamp\": \"2025-06-18T10:12:42.172+00:00\",\n  \"status\": 400,\n  \"error\": \"Bad Request\",\n  \"message\": \"O cliente não existe.\",\n  \"errorCode\": \"CLIENT-003\",\n  \"path\": \"/atmate-integration/gateway/sync/59\"\n}"
                    ))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno do servidor ou falha durante o scraping.",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    public ResponseEntity<?> sync(
            @Parameter(
                    description = "ID único do cliente a ser sincronizado.",
                    required = true,
                    example = "22"
            )
            @PathVariable Integer clientId,
            @Parameter(
                    description = "Define se o tipo de utilizador deve ser obtido da AT. Se 'true', o scraping tentará identificar o tipo de utilizador. Padrão é 'false'.",
                    example = "true"
            )
            @RequestParam(value = "getTypeFromAT", required = false, defaultValue = "false") boolean getTypeFromAT) throws Exception {
        logger.info("Syncing client...");
        Client client = clientService.getClientById(clientId)
                .orElseThrow(() -> new ATMateException(ErrorEnum.CLIENT_NOT_FOUND));
        logger.info("Syncing client with NIF = " + client.getNif());

        scrappingService.syncClient(client, getTypeFromAT);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/sync")
    @Operation(
            summary = "Atualizar dados AT de todos os clientes",
            description = "Endpoint que invoca o web scraping à Autoridade Tributária (AT) para atualização completa dos dados de todos os clientes. Retorna 200 OK em caso de sucesso."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Dados do cliente sincronizados com sucesso.",
                    content = @Content(schema = @Schema(implementation = Void.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno do servidor ou falha durante o scraping.",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    public ResponseEntity<?> sync() throws Exception {
        logger.info("Syncing all clients...");
        scrappingService.executeScrape();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/sendNotification/{configId}")
    @Operation(
            summary = "Enviar notificações para uma configuração específica",
            description = "Inicia o processo de preparação e execução de notificações com base numa configuração pré-existente. Retorna o número de notificações enviadas."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Notificações lançadas com sucesso. Retorna o número de notificações enviadas.",
                    content = @Content(schema = @Schema(type = "integer", format = "int32", example = "5"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Configuração de notificação não encontrada.",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno do servidor durante o envio das notificações.",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    public ResponseEntity<?> sendNotification(
            @Parameter(
                    description = "ID único da configuração de notificação para a qual as notificações serão enviadas.",
                    required = true,
                    example = "408"
            )
            @PathVariable Integer configId) throws Exception {
        logger.info("Sending notification...");

        int numberOfSentNotification = notificationService.prepareAndTriggerSingleConfigNotifications(configId);

        return ResponseEntity.ok().body(numberOfSentNotification);
    }
}
