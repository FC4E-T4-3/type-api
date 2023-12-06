package com.fce4.dtrtoolkit.Controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fce4.dtrtoolkit.TypeService;
import com.github.underscore.U;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Measurement Units", description = "Endpoints for the work with measurement units.")

public class UnitController {

    @Autowired
    TypeService typeService;
    
    Logger logger = Logger.getLogger(UnitController.class.getName());
    ObjectMapper mapper = new ObjectMapper();

    @CrossOrigin
    @RequestMapping(value = "/v1/units/", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> getAllUnits(@RequestParam(defaultValue = "false") Boolean onlyIDs) throws Exception{
        logger.info(String.format("Retrieving all units..."));
        final HttpHeaders responseHeaders = new HttpHeaders();

        ArrayList<Object> result = typeService.search("*", new String[]{"name"}, Collections.emptyMap(), "units", false);
        if(onlyIDs){
            ArrayList<Object> tmp = new ArrayList<Object>();
            for(Object o : result){
                JsonNode node = mapper.readTree(o.toString());
                tmp.add(node.get("id"));
                //System.out.println(node.get("id"));
            }
            result = tmp;
        }
        return new ResponseEntity<Object>(mapper.readTree(result.toString()), responseHeaders, HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/v1/units/search", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> search(@RequestParam String query, @RequestParam(defaultValue = "name,authors,description") String[] queryBy, @RequestParam(defaultValue="{\"\":\"\"}") Map<String,String> filterBy, @RequestParam(defaultValue = "true", required = true) Boolean infix) throws Exception {
        logger.info("Searching for...");
        final HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        filterBy.remove("query");
        filterBy.remove("queryBy");
        filterBy.remove("infix");
        try{
            ArrayList<Object> result = typeService.search(query, queryBy, filterBy, "units", infix);
            return new ResponseEntity<Object>(mapper.readTree(result.toString()), responseHeaders, HttpStatus.OK);
        }
        catch(Exception e){
            return new ResponseEntity<Object>("FilterBy or QueryBy field does not exist or is not indexed.", responseHeaders, HttpStatus.NOT_FOUND);
        }
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
    public ResponseEntity<Object> getUnitTypes(@PathVariable String prefix, @PathVariable String suffix) throws Exception{
        logger.info(String.format("Getting Type Description for %s.", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        HashMap<String,String> filter = new HashMap<String,String>();
        filter.put("unit",prefix+"/"+suffix);
        String[] queryBy = {"name"};
        ArrayList<Object> result = typeService.search("*", queryBy, filter, "types", false);
        return new ResponseEntity<Object>(mapper.readTree(result.toString()), responseHeaders, HttpStatus.OK);
    }
}
