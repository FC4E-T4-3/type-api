package com.fce4.dtrtoolkit.Controllers;

import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fce4.dtrtoolkit.TypeService;
import com.github.underscore.U;

@RestController
public class UnitController {

    @Autowired
    TypeService typeService;
    
    Logger logger = Logger.getLogger(UnitController.class.getName());

    @CrossOrigin
    @RequestMapping(value = "/v1/units/", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getAllUnits(@PathVariable String prefix, @PathVariable String suffix){
        logger.info(String.format("Getting Type Description for %s.", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();

        return new ResponseEntity<String>("GetUnit", responseHeaders, HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/v1/units/search", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> search(@RequestParam String query, @RequestParam(defaultValue = "name,authors,description") String[] queryBy, @RequestParam(defaultValue = "false") Boolean infix) throws Exception {
        logger.info("Searching for...");
        final HttpHeaders responseHeaders = new HttpHeaders();
        ArrayList<Object> result = typeService.search(query, queryBy, "units", infix);
        return new ResponseEntity<String>(result.toString(), responseHeaders, HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/v1/units/{prefix}/{suffix}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE}) 
    @ResponseBody
    public ResponseEntity<String> getUnit(@PathVariable String prefix, @PathVariable String suffix, @RequestParam Optional<Boolean> refresh, @RequestHeader HttpHeaders header) throws Exception{
        logger.info(String.format("Getting Unit Description for %s.", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();
        JsonNode unit = JsonNodeFactory.instance.objectNode(); 
        
     //   try{
            unit = typeService.getUnit(prefix+"/"+suffix, refresh.orElse(false));
        // }
        // catch(Exception e){
        //     return new ResponseEntity<String>(e.getMessage().toString(), responseHeaders, HttpStatus.BAD_REQUEST);
        // }

        if(header.get("Content-Type") != null)
        {
            String format = header.get("Content-Type").get(0);
            if(format.equalsIgnoreCase("application/xml")){
                responseHeaders.setContentType(MediaType.APPLICATION_XML);
                //Using the https://github.com/javadev/underscore-java/ library to conver JSON to XML
                return new ResponseEntity<String>(U.jsonToXml(unit.toString()), responseHeaders, HttpStatus.OK);
            }
        }
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<String>(unit.toString(), responseHeaders, HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/v1/units/{prefix}/{suffix}/types", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getUnitTypes(@PathVariable String prefix, @PathVariable String suffix){
        logger.info(String.format("Getting Type Description for %s.", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();

        return new ResponseEntity<String>("GetUnit", responseHeaders, HttpStatus.OK);
    }
}
