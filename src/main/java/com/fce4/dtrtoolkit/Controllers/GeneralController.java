package com.fce4.dtrtoolkit.Controllers;

import java.io.IOException;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.fce4.dtrtoolkit.TypeService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@Tag(name = "General", description = "General endpoints for all supported types.")public class GeneralController {

    Logger logger = Logger.getLogger(GeneralController.class.getName());

    @Autowired
    TypeService typeService;

    @Hidden
    @RequestMapping("/")
    public void index(HttpServletResponse response) throws IOException{
        response.sendRedirect("swagger-ui/index.html");
    }
    
    @CrossOrigin
    @RequestMapping(value = "/v1/retrieve/{prefix}/{suffix}", method = RequestMethod.GET,  produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    /**
     * Given a digital object where fields are described by PID's, resolve that type into human readable form by replacing PID's with 
     * explainable content
     * @param depth true if subfields of the types should be resolved as well and not just the first layer.
     */
    public ResponseEntity<String> retrieve(@PathVariable String prefix, @PathVariable String suffix, @RequestParam Optional<Boolean> depth) throws IOException, InterruptedException {
        logger.info(String.format("DECIPHER ", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();
        return new ResponseEntity<String>("Not implemented yet.", responseHeaders, HttpStatus.OK);
    }

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

    @RequestMapping(value = "/v1/refresh/", method = RequestMethod.GET,  produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> refresh() throws Exception { 

        typeService.refreshRepository();
        final HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<String>("success", responseHeaders, HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/v1/search", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Object> search(@RequestParam String query, @RequestParam(defaultValue = "name,authors,description") String[] queryBy, @RequestParam(defaultValue="{\"\":\"\"}") Map<String,String> filterBy, @RequestParam(defaultValue = "true", required = true) Boolean infix) throws Exception {
        logger.info("Searching for...");
        final HttpHeaders responseHeaders = new HttpHeaders();
        return new ResponseEntity<Object>("Not implemented.", responseHeaders, HttpStatus.OK);
    }
    
}
