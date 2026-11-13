package com.payforge.transfer.web;

import com.payforge.commons.web.ApiError;
import com.payforge.commons.web.CorrelationIdFilter;
import com.payforge.transfer.service.DependencyUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TransferExceptionHandler {

    @ExceptionHandler(DependencyUnavailableException.class)
    public ResponseEntity<ApiError> unavailable(DependencyUnavailableException ex, HttpServletRequest request) {
        ApiError body = ApiError.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "dependency_unavailable",
                ex.getMessage(),
                request.getRequestURI(),
                MDC.get(CorrelationIdFilter.MDC_KEY));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "5")
                .body(body);
    }
}
