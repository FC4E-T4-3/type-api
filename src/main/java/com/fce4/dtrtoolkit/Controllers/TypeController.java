package com.fce4.dtrtoolkit.Controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Types", description = "Endpoints related to BasicInfoTypes, InfoTypes and Profiles, describing schema elements. " +
        "Creation of validation schemas and validating JSON objects against the schema for a type.")

public class TypeController {

    Logger logger = Logger.getLogger(TypeController.class.getName());
    ObjectMapper mapper = new ObjectMapper();
    
    @Autowired
    TypeService typeService;

    @Operation(summary = "Retrieve a single schema element data type.",
            description= "This supports as of now BasicInfoTypes, InfoTypes and Profiles registered in one of the supported DTR's.")
    @CrossOrigin
    @RequestMapping(value = "/v1/types/{prefix}/{suffix}", method = RequestMethod.GET,  produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @ResponseBody
    /**
     * Returns the description of a type. Per default, JSON is returned, but via the http header XML can be requested.
     * @param refresh if the requested PID should be refreshed in the cache.
     */
    public ResponseEntity<String> desc(
            @PathVariable
            @Parameter(description = "The prefix of the Type PID", example = "21.T11969", required = true)
            String prefix,

            @PathVariable
            @Parameter(description = "The suffix of the Type PID", example = "db605a11c81e79e1efc4", required = true)
            String suffix,

            @Parameter(description = "Whether to refresh the Type to include changes recently made in the DTR, recaching it.")
            @RequestParam Optional<Boolean> refresh,

            @Parameter(description = "Whether to refresh the Properties of the Type, if available, to include changes recently made in the DTR, recaching them.")
            @RequestParam Optional<Boolean> refreshChildren, @RequestHeader HttpHeaders header) throws Exception {
        logger.info(String.format("Getting Type Description for %s.", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        JsonNode type = typeService.getDescription(prefix+"/"+suffix, refresh.orElse(false), refreshChildren.orElse(false));

        if(header.get("Accept") != null)
        {
            String format = header.get("Accept").get(0);
            if(format.equalsIgnoreCase("application/xml")){
                responseHeaders.setContentType(MediaType.APPLICATION_XML);
                //Using the https://github.com/javadev/underscore-java/ library to convert JSON to XML
                return new ResponseEntity<String>(U.jsonToXml(type.toString()), responseHeaders, HttpStatus.OK);
            }
        }
        return new ResponseEntity<String>(type.toString(), responseHeaders, HttpStatus.OK);
    }

    @Operation(summary = "Generate a JSON schema for a data type..",
            description= "This supports as of now BasicInfoTypes, InfoTypes and Profiles registered in one of the supported DTR's.")
    @CrossOrigin
    @RequestMapping(value = "/v1/types/schema/{prefix}/{suffix}", method = RequestMethod.GET,  produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    /**
     * Returns the JSON validation schema for a type. 
     * @param refresh if the requested PID should be refreshed in the cache.
     */
    public ResponseEntity<String> validation(
            @PathVariable
            @Parameter(description = "The prefix of the Type PID", example = "21.T11969", required = true)
            String prefix,

            @PathVariable
            @Parameter(description = "The suffix of the Type PID", example = "db605a11c81e79e1efc4", required = true)
            String suffix,

            @Parameter(description = "Whether to refresh the Type to include changes recently made in the DTR, recaching it.")
            @RequestParam Optional<Boolean> refresh,

            @Parameter(description = "Whether to refresh all the Properties of the Type down to its basic properties, " +
                    "if available, to include changes recently made in the DTR, recaching them.")
            @RequestParam Optional<Boolean> refreshChildren, @RequestHeader HttpHeaders header) throws Exception {
        logger.info(String.format("Getting Validation Schema for %s.", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();
        ObjectNode node = typeService.getValidation(prefix+"/"+suffix, refresh.orElse(false), refreshChildren.orElse(false));
        //Neccessary to clean the JSON string, since Java escapes already escaped characters.
        //String cleaned = node.toString().replace("\\\\n","\\n").replace("\\\\\\\\", "\\\\").replace("\\\\", "\\");
        //logger.info(cleaned);
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<String>(node.toString(), responseHeaders, HttpStatus.OK);
    }

    @Operation(summary = "Validate data against a data type.",
            description= "This endpoints takes some input in valid JSON format, generates the validation schema for the " +
                    "given type behind the PID and validates the data. This supports as of now BasicInfoTypes, InfoTypes" +
                    " and Profiles registered in one of the supported DTR's.")
    @CrossOrigin
    @RequestMapping(value = "/v1/types/validate/{prefix}/{suffix}", method = RequestMethod.POST,  produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    /**
     * Given a digital object and a registered type, check if the object can be validated using the schema of the type
     */
    public ResponseEntity<String> validate(
            @PathVariable
            @Parameter(description = "The prefix of the Type PID", example = "21.T11969", required = true)
            String prefix,

            @PathVariable
            @Parameter(description = "The suffix of the Type PID", example = "d4242fb8297d3ff4199b", required = true)
            String suffix,

            @Parameter(description = "The JSON object to be validated", required = true)
            @RequestBody Object payload,

            @Parameter(description = "Whether to refresh the Type to include changes recently made in the DTR, recaching it.")
            @RequestParam Optional<Boolean> refresh,

            @Parameter(description = "Whether to refresh all the Properties of the Type down to its basic properties, " +
                    "if available, to include changes recently made in the DTR, recaching them.")
            @RequestParam Optional<Boolean> refreshChildren, @RequestHeader HttpHeaders header) throws Exception {

        logger.info(String.format("Validating..."));
        final HttpHeaders responseHeaders = new HttpHeaders();
        String response = typeService.validate(prefix + "/" + suffix, payload, refresh.orElse(false), refreshChildren.orElse(false));
        return new ResponseEntity<String>(response, responseHeaders, HttpStatus.OK);
    }

    @Operation(summary = "Search only in types that represent schema elements.",
            description= "This supports as of now BasicInfoTypes, InfoTypes and Profiles registered in one of the supported DTR's.")
    @CrossOrigin
    @RequestMapping(value = "/v1/types/search/", method = RequestMethod.GET,  produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    /**
     * Search for types by name, author and desc by default. can be adjusted by using the queryBy parameters.
     */
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
