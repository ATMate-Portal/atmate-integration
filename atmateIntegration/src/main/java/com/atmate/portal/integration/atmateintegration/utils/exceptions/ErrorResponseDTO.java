package com.atmate.portal.integration.atmateintegration.utils.exceptions;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.Date;

@Data
@Getter
@Builder
public class ErrorResponseDTO {
    private Date timestamp;
    private int status;
    private String error;
    private String message;
    private String errorCode;
    private String path;
}

