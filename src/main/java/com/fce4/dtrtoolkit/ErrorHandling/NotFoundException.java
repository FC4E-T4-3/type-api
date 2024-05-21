package com.fce4.dtrtoolkit.ErrorHandling;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;
import java.util.Date;

@ControllerAdvice
public class NotFoundException {

@ExceptionHandler(value = {org.typesense.api.exceptions.ObjectNotFound.class})
    public ResponseEntity<Object> notFoundException(org.typesense.api.exceptions.ObjectNotFound ex, WebRequest request) {
        final HttpHeaders responseHeaders = new HttpHeaders();
        ErrorMessage message = new ErrorMessage(
        HttpStatus.NOT_FOUND.value(),
        new Date(),
        ex.message,
        request.getDescription(false));
        System.out.println(message.toXML());
        if(request.getHeader("Accept") != null){
            if(request.getHeader("Accept").equals("application/xml")){
                responseHeaders.setContentType(MediaType.APPLICATION_XML);
                return new ResponseEntity<Object>(message.toXML(), responseHeaders, HttpStatus.NOT_FOUND);
            }
        }
        return new ResponseEntity<Object>(message, HttpStatus.NOT_FOUND);
    }
}
