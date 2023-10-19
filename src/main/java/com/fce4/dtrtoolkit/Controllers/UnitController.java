package com.fce4.dtrtoolkit.Controllers;

import java.util.logging.Logger;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fce4.dtrtoolkit.TypeService;

@RestController
public class UnitController {
    
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
    public ResponseEntity<String> search(@RequestParam String query, @RequestParam(defaultValue = "name,authors,desc") String[] queryBy, @RequestParam(defaultValue = "false") Boolean infix) throws Exception {
        logger.info("Searching for...");
        final HttpHeaders responseHeaders = new HttpHeaders();

        return new ResponseEntity<String>("GetUnit", responseHeaders, HttpStatus.OK);
    }

    @CrossOrigin
    @RequestMapping(value = "/v1/units/{prefix}/{suffix}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> getUnit(@PathVariable String prefix, @PathVariable String suffix){
        logger.info(String.format("Getting Type Description for %s.", prefix+"/"+suffix));
        final HttpHeaders responseHeaders = new HttpHeaders();

        return new ResponseEntity<String>("GetUnit", responseHeaders, HttpStatus.OK);
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
