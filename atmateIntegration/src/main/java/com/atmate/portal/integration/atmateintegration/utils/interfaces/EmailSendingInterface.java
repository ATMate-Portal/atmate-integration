package com.atmate.portal.integration.atmateintegration.utils.interfaces;

public interface EmailSendingInterface {
    boolean sendEmail(String to, String subject, String body, boolean isHtml);
}
