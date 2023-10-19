package com.fce4.dtrtoolkit.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fce4.dtrtoolkit.TypeSearch;

@Component
public class BaseValidator {
    @Autowired
    TypeSearch typeSearch;
    
    ObjectMapper mapper = new ObjectMapper();
}
