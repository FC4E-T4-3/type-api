package com.fce4.dtrtoolkit.ErrorHandling;

import java.io.IOException;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import java.util.Date;

@ControllerAdvice
public class ControllerExceptionHandler {

@ExceptionHandler(value = {IOException.class})
    public ResponseEntity<Object> badRequestException(IOException ex, WebRequest request) {
        final HttpHeaders responseHeaders = new HttpHeaders();
        ErrorMessage message = new ErrorMessage(
        HttpStatus.BAD_REQUEST.value(),
        new Date(),
        ex.getMessage(),
        request.getDescription(false));
        System.out.println(message.toXML());
        if(request.getHeader("Accept") != null){
            if(request.getHeader("Accept").equals("application/xml")){
                responseHeaders.setContentType(MediaType.APPLICATION_XML);
                return new ResponseEntity<Object>(message.toXML(), responseHeaders, HttpStatus.BAD_REQUEST);
            }
        }
        return new ResponseEntity<Object>(message, HttpStatus.BAD_REQUEST); 
    }
}
