package com.fce4.dtrtoolkit.Controllers;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import com.github.underscore.U;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.logging.Logger;

import com.fce4.dtrtoolkit.TypeService;

@RestController
public class TypeController {

    Logger logger = Logger.getLogger(TypeController.class.getName());
    ObjectMapper mapper = new ObjectMapper();
    
    @Autowired
    TypeService typeService;

    @CrossOrigin
    @RequestMapping(value = "/v1/types/desc/{prefix}/{suffix}", method = RequestMethod.GET,  produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    /**
     * Returns the description of a type. Per default, JSON is returned, but via the http header XML can be requested.
     * @param refresh if the requested PID should be refreshed in the cache.
     */
    public ResponseEntity<String> desc(@PathVariable String prefix, @PathVariable String suffix, @RequestParam Optional<Boolean> refresh, @RequestHeader HttpHeaders header) throws Exception {
        logger.info(String.format("Getting Type Description for %s.", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();
        JsonNode type = JsonNodeFactory.instance.objectNode(); 
        
        try{
            type = typeService.getDescription(prefix+"/"+suffix, refresh.orElse(false));
        }
        catch(Exception e){
            return new ResponseEntity<String>(e.getMessage().toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }

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

    @CrossOrigin
    @RequestMapping(value = "/v1/types/schema/{prefix}/{suffix}", method = RequestMethod.GET,  produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    /**
     * Returns the JSON validation schema for a type. 
     * @param refresh if the requested PID should be refreshed in the cache.
     */
    public ResponseEntity<String> validation(@PathVariable String prefix, @PathVariable String suffix, @RequestParam Optional<Boolean> refresh) throws Exception {
        logger.info(String.format("Getting Validation Schema for %s.", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();
        ObjectNode node = typeService.getValidation(prefix+"/"+suffix, refresh.orElse(false));
        //Neccessary to clean the JSON string, since Java escapes already escaped characters.
        String cleaned = node.toString().replace("\\\\n","\\n").replace("\\\\\\\\", "\\\\");
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<String>(cleaned, responseHeaders, HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/v1/types/validate/{prefix}/{suffix}", method = RequestMethod.POST,  produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    /**
     * Given a digital object and a registered type, check if the object can be validated using the schema of the type
     */
    public ResponseEntity<String> validate(@PathVariable String prefix, @PathVariable String suffix, @RequestBody Object payload) throws Exception {
        logger.info(String.format("Validating..."));
        final HttpHeaders responseHeaders = new HttpHeaders();
        System.out.println(payload);
        try{
            String response = typeService.validate(prefix + "/" + suffix, payload);
            return new ResponseEntity<String>(response, responseHeaders, HttpStatus.OK);
        }
        catch(Exception e){
            return new ResponseEntity<String>(e.getMessage().toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        }
    }
    
    @CrossOrigin
    @RequestMapping(value = "/v1/types/search/", method = RequestMethod.GET,  produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    /**
     * Search for types by name, author and desc by default. can be adjusted by using the queryBy parameters.
     */
    public ResponseEntity<Object> search(@RequestParam String query, @RequestParam(defaultValue = "name,authors,description") String[] queryBy, @RequestParam(defaultValue="{\"\":\"\"}") Map<String,String> filterBy, @RequestParam(defaultValue = "false") Boolean infix) throws Exception {
        logger.info(String.format("Searching %s in the fields %s.", query, queryBy));
        filterBy.remove("query");
        filterBy.remove("queryBy");
        filterBy.remove("infix");
        final HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        try{
            ArrayList<Object> result = typeService.search(query, queryBy, filterBy, "types", infix);
            return new ResponseEntity<Object>(mapper.readTree(result.toString()), responseHeaders, HttpStatus.OK);
        }
        catch(Exception e){
            return new ResponseEntity<Object>("FilterBy or QueryBy field does not exist or is not indexed.", responseHeaders, HttpStatus.NOT_FOUND);
        }
    }
}