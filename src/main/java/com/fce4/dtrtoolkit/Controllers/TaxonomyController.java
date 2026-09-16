package com.fce4.dtrtoolkit.Controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    
    Logger logger = Logger.getLogger(TaxonomyController.class.getName());
    ObjectMapper mapper = new ObjectMapper();

    @Operation(summary = "Retrieve all taxonomy nodes.")
    @CrossOrigin
    @RequestMapping(value = "/v1/taxonomy", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> getTaxonomy() throws Exception{
        logger.info(String.format("Retrieving entire taxonomy tree..."));
        final HttpHeaders responseHeaders = new HttpHeaders();

        ArrayList<Object> result = typeService.search("*", new String[]{"name"}, Collections.emptyMap(), "taxonomy", false);

        return new ResponseEntity<Object>(mapper.readTree(result.toString()), responseHeaders, HttpStatus.OK);
    }

    @Operation(summary = "Search only in registered taxonomy nodes.")
    @CrossOrigin
    @RequestMapping(value = "/v1/taxonomy/search", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> search(
            @RequestParam
            @Parameter(description = "The query to search for.")
            String query,

            @RequestParam(defaultValue = "name,authors,description")
            @Parameter(description = "The fields to search in.")
            String[] queryBy,

            @RequestParam
            @Parameter(description = "The filters to apply to the search.", example = "{\"\": \"\"}")
            Map<String,String> filterBy,

            @RequestParam(defaultValue = "true")
            @Parameter(description = "Whether to use infix search.")
            Boolean infix) throws Exception {
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

    @Operation(summary = "Retrieve a single taxonomy node.",
            description= "This includes only supported taxonomy nodes registered in any of the supported typeregistries. Includes parent and child nodes.")
    @CrossOrigin
    @RequestMapping(value = "/v1/taxonomy/{prefix}/{suffix}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE}) 
    @ResponseBody
    public ResponseEntity<Object> getTaxonomyNode(
            @PathVariable
            @Parameter(description = "The prefix of the Taxonomy Type PID", example = "21.T11969", required = true)
            String prefix,

            @PathVariable
            @Parameter(description = "The suffix of the Taxonomy type PID", example = "2ba636a1ce6806a8d22c", required = true)
            String suffix,

            @Parameter(description = "Whether to refresh the taxonomy node to include changes recently made in the DTR, recaching it.")
            @RequestParam Optional<Boolean> refresh,
            @RequestHeader HttpHeaders header) throws Exception{
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

    @Operation(summary = "Retrieve all types that are assigned to this taxonomy node.",
            description= "It is possible to include all types that are assigned to some taxonomy node that is hierarchically deeper in the subtree,")
    @CrossOrigin
    @RequestMapping(value = "/v1/taxonomy/{prefix}/{suffix}/types", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> getTaxonomyTypes(
            @PathVariable
            @Parameter(description = "The prefix of the Taxonomy Type PID", example = "21.T11969", required = true)
            String prefix,

            @PathVariable
            @Parameter(description = "The suffix of the Taxonomy type PID", example = "0e76292794888d4f1fa7", required = true)
            String suffix,

            @Parameter(description = "Whether to include all types that are assigned to some taxonomy node that is hierarchically deeper in the subtree.")
            @RequestParam(defaultValue="false") Boolean subtree) throws Exception{
        logger.info(String.format("Getting Type Description for %s.", prefix+"/"+suffix));
        JsonNode taxonomyNode = JsonNodeFactory.instance.objectNode(); 
        taxonomyNode = typeService.getTaxonomyNode(prefix+"/"+suffix, false);
        final HttpHeaders responseHeaders = new HttpHeaders();
        ArrayList<Object> result = typeService.getTypesTaxonomy(prefix+"/"+suffix, subtree);
        return new ResponseEntity<Object>(mapper.readTree(result.toString()), responseHeaders, HttpStatus.OK);
    }

    @Operation(summary = "Retrieve the subtree of a taxonomy node.")
    @CrossOrigin
    @RequestMapping(value = "/v1/taxonomy/{prefix}/{suffix}/subtree", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> getTaxonomySubtree(
        @PathVariable
        @Parameter(description = "The prefix of the Taxonomy Type PID", example = "21.T11969", required = true)
        String prefix,

        @PathVariable
        @Parameter(description = "The suffix of the Taxonomy type PID", example = "2ba636a1ce6806a8d22c", required = true)
        String suffix
    ) throws Exception{
        logger.info(String.format("Getting subtree from node %s.", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();
        JsonNode result = typeService.getTaxonomySubtree(prefix+"/"+suffix);
        return new ResponseEntity<Object>(result, responseHeaders, HttpStatus.OK);
    }
}