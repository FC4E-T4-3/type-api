package com.fce4.dtrtoolkit.validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fce4.dtrtoolkit.TypeRepository;


@Component
public class BaseValidator {

    @Autowired
    TypeRepository typeRepository;
    ObjectMapper mapper = new ObjectMapper();

}
