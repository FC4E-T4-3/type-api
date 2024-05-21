package com.fce4.dtrtoolkit.Controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.underscore.U;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.fce4.dtrtoolkit.TypeService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@Tag(name = "General", description = "General endpoints for all supported types.")
public class GeneralController {

    Logger logger = Logger.getLogger(GeneralController.class.getName());
    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    TypeService typeService;

    @Hidden
    @RequestMapping("/")
    public void index(HttpServletResponse response) throws IOException{
        response.sendRedirect("swagger-ui/index.html");
    }

    @Operation(summary = "Retrieve any registered type.",
            description= "This includes only supported types registered in any of the supported typeregistries.")
    @CrossOrigin
    @RequestMapping(value = "/v1/retrieve/{prefix}/{suffix}", method = RequestMethod.GET,  produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    /**
     * Given a digital object where fields are described by PID's, resolve that type into human readable form by replacing PID's with 
     * explainable content
     * @param depth true if subfields of the types should be resolved as well and not just the first layer.
     */
    public ResponseEntity<String> retrieve(@PathVariable String prefix, @PathVariable String suffix, @RequestHeader HttpHeaders header) throws Exception {
        final HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        JsonNode type = typeService.retrieve(prefix+"/"+suffix, "general");

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

    @Operation(summary = "Make an object using types as parameters human readable.",
                description= "This supports only JSON as input as of now..")
    @CrossOrigin
    @RequestMapping(value = "/v1/decipher/{prefix}/{suffix}", method = RequestMethod.GET,  produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    /**
     * Given a digital object where fields are described by PID's, resolve that type into human readable form by replacing PID's with 
     * explainable content
     * @param depth true if subfields of the types should be resolved as well and not just the first layer.
     */
    public ResponseEntity<String> resolve(@PathVariable String prefix, @PathVariable String suffix, @RequestParam Optional<Boolean> depth) throws IOException, InterruptedException {
        logger.info(String.format("DECIPHER ", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();
        return new ResponseEntity<String>("Not implemented yet.", responseHeaders, HttpStatus.OK);
    }

    @Hidden
    @RequestMapping(value = "/v1/refresh/", method = RequestMethod.GET,  produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> refresh() throws Exception {

        typeService.refreshRepository();
        final HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<String>("success", responseHeaders, HttpStatus.OK);
    }

    @RequestMapping(value = "/v1/mime/string/{prefix}/{suffix}", method = RequestMethod.GET)
    public ResponseEntity<String> mimeString(@PathVariable String prefix, @PathVariable String suffix) throws Exception {

        String mimeString = typeService.getMimeString(prefix + "/" + suffix);
        final HttpHeaders responseHeaders = new HttpHeaders();
        return new ResponseEntity<String>(mimeString, responseHeaders, HttpStatus.OK);
    }

    @Operation(summary = "Search over all kinds of registered types..")
    @CrossOrigin
    @RequestMapping(value = "/v1/search", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> search(@RequestParam String query, @RequestParam(defaultValue = "name,authors,description") String[] queryBy, @RequestParam(defaultValue="{\"\":\"\"}") Map<String,String> filterBy, @RequestParam(defaultValue = "false") Boolean infix) throws Exception {
        logger.info(String.format("Searching %s in the fields %s.", query, queryBy.toString()));
        filterBy.remove("query");
        filterBy.remove("queryBy");
        filterBy.remove("infix");
        final HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        try {
            ArrayList<Object> result = typeService.search(query, queryBy, filterBy, "general", infix);
            return new ResponseEntity<Object>(mapper.readTree(result.toString()), responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<Object>("FilterBy or QueryBy field does not exist or is not indexed.", responseHeaders, HttpStatus.NOT_FOUND);
        }
    }
}
