package com.fce4.dtrtoolkit.Controllers;

import java.io.IOException;
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
import com.fce4.dtrtoolkit.Taxonomies.TaxonomyGraph;
import com.github.underscore.U;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Taxonomies", description = "Endpoints regarding taxonomies and filtering types depending on their assigned taxonomy node.")

public class TaxonomyController {

    @Autowired
    TypeService typeService;

    @Autowired TaxonomyGraph taxonomyGraph;
    
    Logger logger = Logger.getLogger(UnitController.class.getName());
    ObjectMapper mapper = new ObjectMapper();

    @CrossOrigin
    @RequestMapping(value = "/v1/taxonomy", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> getTaxonomy() throws Exception{
        logger.info(String.format("Retrieving entire taxonomy tree..."));
        final HttpHeaders responseHeaders = new HttpHeaders();

        ArrayList<Object> result = typeService.search("*", new String[]{"name"}, Collections.emptyMap(), "taxonomy", false);

        return new ResponseEntity<Object>(mapper.readTree(result.toString()), responseHeaders, HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/v1/taxonomy/search", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> search(@RequestParam String query, @RequestParam(defaultValue = "name,authors,description") String[] queryBy, @RequestParam(defaultValue="{\"\":\"\"}") Map<String,String> filterBy, @RequestParam(defaultValue = "true", required = true) Boolean infix) throws Exception {
        logger.info("Searching for...");
        final HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        filterBy.remove("query");
        filterBy.remove("queryBy");
        filterBy.remove("infix");
        try{
            ArrayList<Object> result = typeService.search(query, queryBy, filterBy, "taxonomy", infix);
            return new ResponseEntity<Object>(mapper.readTree(result.toString()), responseHeaders, HttpStatus.OK);
        }
        catch(Exception e){
            return new ResponseEntity<Object>("FilterBy or QueryBy field does not exist or is not indexed.", responseHeaders, HttpStatus.NOT_FOUND);
        }
    }

    @CrossOrigin
    @RequestMapping(value = "/v1/taxonomy/{prefix}/{suffix}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE}) 
    @ResponseBody
    public ResponseEntity<Object> getTaxonomyNode(@PathVariable String prefix, @PathVariable String suffix, @RequestParam Optional<Boolean> refresh, @RequestHeader HttpHeaders header) throws Exception{
        logger.info(String.format("Retrieving taxonomy node ", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        JsonNode taxonomyNode = JsonNodeFactory.instance.objectNode(); 
        taxonomyNode = typeService.getTaxonomyNode(prefix+"/"+suffix, refresh.orElse(false));

        if(header.get("accept") != null)
        {
            String format = header.get("accept").get(0);
            if(format.equalsIgnoreCase("application/xml")){
                responseHeaders.setContentType(MediaType.APPLICATION_XML);
                //Using the https://github.com/javadev/underscore-java/ library to conver JSON to XML
                return new ResponseEntity<Object>(U.jsonToXml(taxonomyNode.toString()), responseHeaders, HttpStatus.OK);
            }
        }
        return new ResponseEntity<Object>(taxonomyNode.toString(), responseHeaders, HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/v1/taxonomy/{prefix}/{suffix}/types", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> getTaxonomyTypes(@PathVariable String prefix, @PathVariable String suffix, @RequestParam(defaultValue="false") Boolean subtree) throws Exception{
        logger.info(String.format("Getting Type Description for %s.", prefix+"/"+suffix));
        JsonNode taxonomyNode = JsonNodeFactory.instance.objectNode(); 
        taxonomyNode = typeService.getTaxonomyNode(prefix+"/"+suffix, false);
        final HttpHeaders responseHeaders = new HttpHeaders();
        ArrayList<Object> result = typeService.getTypesTaxonomy(prefix+"/"+suffix, subtree);
        return new ResponseEntity<Object>(mapper.readTree(result.toString()), responseHeaders, HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/v1/taxonomy/{prefix}/{suffix}/subtree", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> getTaxonomySubtree(@PathVariable String prefix, @PathVariable String suffix) throws Exception{
        logger.info(String.format("Getting subtree from node %s.", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();
        JsonNode result = typeService.getTaxonomySubtree(prefix+"/"+suffix);
        return new ResponseEntity<Object>(result, responseHeaders, HttpStatus.OK);
    }
}