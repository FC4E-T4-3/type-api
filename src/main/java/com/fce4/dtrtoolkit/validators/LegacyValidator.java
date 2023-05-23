package com.fce4.dtrtoolkit.validators;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.fce4.dtrtoolkit.TypeEntity;

@Component
public class LegacyValidator extends BaseValidator {

    Logger logger = Logger.getLogger(LegacyValidator.class.getName());

    /**
     * Generates the validation schema for a single basic info type.
     * @param type the TypeEntity of which the schema is to be created
     */
    public ObjectNode handleBasicType(TypeEntity type){
        Boolean isNumeric = false;
        ObjectNode target = mapper.createObjectNode();
        //The properties field is an array, but never meaningfully used beyond the first element. 
        JsonNode properties = type.getContent().get("properties").get(0);
        String datatype = properties.get("dataType").textValue();
        if(datatype.length()>0){
            if(datatype.equals("number")){
                isNumeric = true;
            }
            target.put("type", datatype);
        }

        if(properties.has("restrict")){
            String cleaned = properties.get("restrict").textValue().replaceAll("\\s+","").replaceAll("\"","");
            String[] restrictions = cleaned.split(",");
            for(String i : restrictions){
                String key = i.split(":")[0];
                String value = i.split(":")[1];
                if(isNumeric){
                    Double valueNumeric = Double.parseDouble(value);
                    if(valueNumeric % 1 == 0){
                        target.put(key, Integer.parseInt(value));
                    }
                    target.put(key, valueNumeric);
                }
                else{
                    target.put(key, value);
                }
            }
        }

        if(properties.has("enum")){
            ArrayNode arrayNode = target.putArray("enum");
            String cleaned = properties.get("enum").textValue()
                .replaceAll("\\s+","").replaceAll("\"","")
                .replaceAll("\\[", "").replaceAll("\\]", "");
            String[] enumValues = cleaned.split(",");
            for(String i : enumValues){
                if(isNumeric){
                    Double valueNumeric = Double.parseDouble(i);
                    if(valueNumeric % 1 == 0){
                        arrayNode.add(Integer.parseInt(i));
                    }
                    arrayNode.add(valueNumeric);
                }
                else{
                    arrayNode.add(i);
                }
            }
        }

        if(properties.has("regexp")){
            String regex = properties.get("regexp").textValue();
            target.put("pattern", regex);
        }

        if(properties.has("default")){
            String defaultValue = properties.get("default").textValue();
            if(isNumeric){
                Double valueNumeric = Double.parseDouble(defaultValue);
                if(valueNumeric % 1 == 0){
                    target.put("const", Integer.parseInt(defaultValue));
                }
                target.put("const", valueNumeric);
            }
            else{
                target.put("const", defaultValue);
            }            
        }

        return target;
    }

    public ObjectNode handleInfoType(){
        ObjectNode target = mapper.createObjectNode();

        return target;
    }

    public ObjectNode validation(String pid) {
        TypeEntity type = typeRepository.get(pid);
        ObjectNode root = mapper.createObjectNode();
        if(type.getSchema().equals("PID-BasicInfoType")){
            root = handleBasicType(type);
        }
        else{
            for(JsonNode i : type.getContent().get("properties")){
                String tmp = i.get("identifier").textValue();
                logger.info(tmp);
                validation(tmp);
            }
        }
        return root;
    }
}
