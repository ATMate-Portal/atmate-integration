package com.atmate.portal.integration.atmateintegration.utils.interfaces;

public interface EmailSendingInterface {
    /**
     * Envia um email.
     * @param to O destinatário do email.
     * @param subject O assunto do email.
     * @param body O corpo do email. Pode ser texto simples ou HTML.
     * @param isHtml Indica se o corpo do email é HTML.
     * @return true se o email foi enviado com sucesso (ou colocado na fila de envio), false caso contrário.
     */
    boolean sendEmail(String to, String subject, String body, boolean isHtml);
}
