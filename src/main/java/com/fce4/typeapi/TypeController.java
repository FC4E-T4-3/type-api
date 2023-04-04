package com.fce4.typeapi;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import com.github.underscore.U;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.logging.Logger;

@RestController
public class TypeController {

    Logger logger = Logger.getLogger(TypeService.class.getName());
    
    @Autowired
    TypeService typeService;


    @RequestMapping(value = "/api/v1/desc/**", method = RequestMethod.GET,  produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    /**
     * Returns the description of a type. Per default, JSON is returned, but via the http header XML can be requested.
     * @param refresh if the requested PID should be refreshed in the cache.
     */
    public ResponseEntity<String> desc(@RequestParam Optional<Boolean> refresh, @RequestHeader HttpHeaders header, HttpServletRequest request) throws IOException, InterruptedException {
        final String url = request.getRequestURL().toString();
        final String pid = url.split("/api/v1/desc/")[1];
        final HttpHeaders responseHeaders = new HttpHeaders();
        JsonNode type = JsonNodeFactory.instance.objectNode();  
        type =  typeService.getDescription(pid, refresh.orElse(false));

        if(header.get("Content-Type") != null)
        {
            String format = header.get("Content-Type").get(0);
            if(format.equalsIgnoreCase("application/xml")){
                responseHeaders.setContentType(MediaType.APPLICATION_XML);
                //Using the https://github.com/javadev/underscore-java/ library to conver JSON to XML
                return new ResponseEntity<String>(U.jsonToXml(type.toString()), responseHeaders, HttpStatus.OK);
            }
        }
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<String>(type.toString(), responseHeaders, HttpStatus.OK);
    }
}
