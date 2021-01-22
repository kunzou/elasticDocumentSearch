package me.kunzou.elasticDocumentSearch.handler;

import me.kunzou.elasticDocumentSearch.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

@RestControllerAdvice(annotations = RestController.class)
public class RestTemplateResponseErrorHandler extends ResponseEntityExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(RestTemplateResponseErrorHandler.class);

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleInternalError(Exception ex) {
    logger.error(ex.getMessage(), ex);
    ErrorResponse error = new ErrorResponse();
    error.setTimestamp(LocalDateTime.now());
    error.setMessage(ex.getCause().getMessage());
    error.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}

