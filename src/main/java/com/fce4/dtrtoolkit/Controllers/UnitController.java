package com.fce4.dtrtoolkit.Controllers;

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
import com.github.underscore.U;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Measurement Units", description = "Endpoints for the work with measurement units.")

public class UnitController {

    @Autowired
    TypeService typeService;
    
    Logger logger = Logger.getLogger(UnitController.class.getName());
    ObjectMapper mapper = new ObjectMapper();

    @Operation(summary = "Retrieve all registered measurement units.")
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

    @Operation(summary = "Search only in registered measurement units.")
    @CrossOrigin
    @RequestMapping(value = "/v1/units/search", method = RequestMethod.GET)
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
            ArrayList<Object> result = typeService.search(query, queryBy, filterBy, "units", infix);
            return new ResponseEntity<Object>(mapper.readTree(result.toString()), responseHeaders, HttpStatus.OK);
        }
        catch(Exception e){
            return new ResponseEntity<Object>("FilterBy or QueryBy field does not exist or is not indexed.", responseHeaders, HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Retrieve a registered measurement unit via it's PID.")
    @CrossOrigin
    @RequestMapping(value = "/v1/units/{prefix}/{suffix}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE}) 
    @ResponseBody
    public ResponseEntity<String> getUnit(
            @PathVariable
            @Parameter(description = "The prefix of the Measurement Unit PID", example = "21.T11969", required = true)
            String prefix,

            @PathVariable
            @Parameter(description = "The suffix of the Measurement Unit PID", example = "9be4ec7c88f130ac2598", required = true)
            String suffix,

            @Parameter(description = "Whether to refresh the Measurement Unit to include changes recently made in the DTR, recaching it.")
            @RequestParam Optional<Boolean> refresh,
            @RequestHeader HttpHeaders header) throws Exception{
        logger.info(String.format("Getting Unit Description for %s.", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();
        JsonNode unit = JsonNodeFactory.instance.objectNode(); 
        
        unit = typeService.getUnit(prefix+"/"+suffix, refresh.orElse(false));

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

    @Operation(summary = "Retrieve all other datatypes that are associated with the given measurement unit.",
            description = "Retrieve all types describing schema elements that are represented by the given measurement unit.")
    @CrossOrigin
    @RequestMapping(value = "/v1/units/{prefix}/{suffix}/types", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> getUnitTypes(
            @PathVariable
            @Parameter(description = "The prefix of the Measurement Unit PID", example = "21.T11969", required = true)
            String prefix,

            @PathVariable
            @Parameter(description = "The suffix of the Measurement Unit PID", example = "9be4ec7c88f130ac2598", required = true)
            String suffix) throws Exception{
        logger.info(String.format("Getting Type Description for %s.", prefix+"/"+suffix));
        
        JsonNode unit = JsonNodeFactory.instance.objectNode();         
        unit = typeService.getUnit(prefix+"/"+suffix, false);

        final HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        HashMap<String,String> filter = new HashMap<String,String>();
        filter.put("unit",prefix+"/"+suffix);
        String[] queryBy = {"name"};
        ArrayList<Object> result = typeService.search("*", queryBy, filter, "types", false);
        return new ResponseEntity<Object>(mapper.readTree(result.toString()), responseHeaders, HttpStatus.OK);
    }
}
