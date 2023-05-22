package com.fce4.dtrtoolkit.validators;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.fce4.dtrtoolkit.TypeEntity;

@Component
public class LegacyValidator extends BaseValidator {

    Logger logger = Logger.getLogger(BaseValidator.class.getName());

    /**
     * Generates the validation schema for a single basic info type.
     * @param type the TypeEntity of which the schema is to be created
     */
    public ObjectNode handleBasicType(TypeEntity type){
        ObjectNode target = mapper.createObjectNode();
        //The properties field is an array, but never meaningfully used beyond the first element. 
        JsonNode properties = type.getContent().get("properties").get(0);
        String datatype = properties.get("dataType").textValue();
        logger.info(datatype);
        return target;
    }

    public void validation(String pid) {
        TypeEntity type = typeRepository.get(pid);
        if(type.getSchema().equals("PID-BasicInfoType")){
           ObjectNode tmpBasic = handleBasicType(type);
        }
        else{
            for(JsonNode i : type.getContent().get("properties")){
                String tmp = i.get("identifier").textValue();
                logger.info(tmp);
                validation(tmp);
            }
        }
    }
}
