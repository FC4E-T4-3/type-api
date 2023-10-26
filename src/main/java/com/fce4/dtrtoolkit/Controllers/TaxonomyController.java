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

@RestController
public class TaxonomyController {

    @Autowired
    TypeService typeService;
    
    Logger logger = Logger.getLogger(UnitController.class.getName());
    ObjectMapper mapper = new ObjectMapper();

    @CrossOrigin
    @RequestMapping(value = "/v1/taxonomy", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> getTaxonomy(@RequestParam(defaultValue = "false") Boolean onlyIDs) throws Exception{
        logger.info(String.format("Retrieving entire taxonomy tree..."));
        final HttpHeaders responseHeaders = new HttpHeaders();
        return new ResponseEntity<Object>("Taxonomy", responseHeaders, HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/v1/taxonomy/search", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> search(@RequestParam String query, @RequestParam(defaultValue = "name,authors,description") String[] queryBy, @RequestParam(defaultValue="{\"\":\"\"}") Map<String,String> filterBy, @RequestParam(defaultValue = "true", required = true) Boolean infix) throws Exception {
        logger.info("Searching for...");
        final HttpHeaders responseHeaders = new HttpHeaders();
        return new ResponseEntity<Object>(mapper.readTree("Taxonomy Subtree."), responseHeaders, HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/v1/taxonomy/{prefix}/{suffix}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE}) 
    @ResponseBody
    public ResponseEntity<String> getTaxonomyNode(@PathVariable String prefix, @PathVariable String suffix, @RequestParam Optional<Boolean> refresh, @RequestHeader HttpHeaders header) throws Exception{
        logger.info(String.format("Retrieving taxonomy node ", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();
        return new ResponseEntity<String>("Test", responseHeaders, HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/v1/taxonomy/{prefix}/{suffix}/types", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> getTaxonomyTypes(@PathVariable String prefix, @PathVariable String suffix, @RequestParam Optional <Boolean> subtree) throws Exception{
        logger.info(String.format("Getting Type Description for %s.", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();
        return new ResponseEntity<Object>(mapper.readTree("Taxonomy Subtree."), responseHeaders, HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/v1/taxonomy/{prefix}/{suffix}/subtree", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> getTaxonomySubtree(@PathVariable String prefix, @PathVariable String suffix) throws Exception{
        logger.info(String.format("Getting subtree from node %s.", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();
        return new ResponseEntity<Object>(mapper.readTree("Taxonomy Subtree."), responseHeaders, HttpStatus.OK);
    }
}