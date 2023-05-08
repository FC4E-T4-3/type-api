package com.fce4.dtrtoolkit.ErrorHandling;

import java.io.IOException;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.HttpStatus;
import java.util.Date;

@ControllerAdvice
public class ControllerExceptionHandler {

  @ExceptionHandler(value = {IOException.class})
  public ResponseEntity<ErrorMessage> badRequestException(IOException ex, WebRequest request) {    
    ErrorMessage message = new ErrorMessage(
        HttpStatus.BAD_REQUEST.value(),
        new Date(),
        ex.getMessage(),
        request.getDescription(false));
    ResponseEntity<ErrorMessage> response = new ResponseEntity<ErrorMessage>(message, HttpStatus.BAD_REQUEST);
    return response;
  }
}
