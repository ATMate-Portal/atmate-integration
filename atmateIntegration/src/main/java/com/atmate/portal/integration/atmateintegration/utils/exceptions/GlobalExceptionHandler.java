package com.atmate.portal.integration.atmateintegration.utils.exceptions;

import com.atmate.portal.integration.atmateintegration.utils.enums.ErrorEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Date;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ATMateException.class)
    public ResponseEntity<ErrorResponseDTO> handleATMateException(ATMateException ex, HttpServletRequest request) {
        ErrorEnum error = ex.getErrorEnum();
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(error.getMessage())
                .errorCode(error.getErrorCode())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(new Date())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .errorCode(ErrorEnum.GENERIC_ERROR.getErrorCode())
                .path(ex.getLocalizedMessage())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
