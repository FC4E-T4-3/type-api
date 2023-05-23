package com.fce4.dtrtoolkit.validators;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
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
    public ObjectNode handleBasicType(TypeEntity typeEntity){
        Boolean isNumeric = false;
        ObjectNode node = mapper.createObjectNode();
        //The properties field is an array, but never meaningfully used beyond the first element. 
        JsonNode properties = typeEntity.getContent().get("properties").get(0);
        String datatype = properties.get("dataType").textValue();
        //If a datatype is provided, add it to the root node. Check if it is a numeric type.
        if(datatype.length()>0){
            if(datatype.equals("number") || datatype.equals("integer")){
                isNumeric = true;
            }
            node.put("type", datatype);
        }

        //If any restrictions are given, add them to the root node. If a number can be an integer, it will be.
        if(properties.has("restrict")){
            String cleaned = properties.get("restrict").textValue()
                .replaceAll("\\s+","").replaceAll("\"","");
            String[] restrictions = cleaned.split(",");
            for(String i : restrictions){
                String key = i.split(":")[0];
                String value = i.split(":")[1];
                if(isNumeric){
                    Double valueNumeric = Double.parseDouble(value);
                    if(valueNumeric % 1 == 0){
                        node.put(key, Integer.parseInt(value));
                    }
                    node.put(key, valueNumeric);
                }
                else{
                    node.put(key, value);
                }
            }
        }

        //Same thing as restrictions for a possible enum.
        if(properties.has("enum")){
            ArrayNode arrayNode = node.putArray("enum");
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

        //Add a regular expression if given
        if(properties.has("regexp")){
            String regex = properties.get("regexp").textValue();
            node.put("pattern", regex);
        }

        //Add a constant value if given.
        if(properties.has("default")){
            String defaultValue = properties.get("default").textValue();
            if(isNumeric){
                Double valueNumeric = Double.parseDouble(defaultValue);
                if(valueNumeric % 1 == 0){
                    node.put("const", Integer.parseInt(defaultValue));
                }
                node.put("const", valueNumeric);
            }
            else{
                node.put("const", defaultValue);
            }            
        }
        return node;
    }

    public ObjectNode handleInfoType(TypeEntity typeEntity){
        ObjectNode node = mapper.createObjectNode();
        ArrayNode mandatory = mapper.createArrayNode();
        JsonNode properties = typeEntity.getContent().get("properties");
        //System.out.println(properties);
        ObjectNode propertyNode = mapper.createObjectNode();

        for(JsonNode i : properties){
            ObjectNode tempNode = mapper.createObjectNode();
            TypeEntity tempEntity = typeRepository.get(i.get("identifier").textValue());

            if(tempEntity.getSchema().equals("PID-BasicInfoType")){
                tempNode = handleBasicType(tempEntity);
            }
            else{
                tempNode = handleInfoType(tempEntity);
            }
            
            if(tempEntity.getContent().has("description")){
                tempNode.put("description", tempEntity.getContent().get("description").textValue());
            }

            if(i.has("representationsAndSemantics")){
                JsonNode repSem = i.get("representationsAndSemantics").get(0);
                if(repSem.get("obligation").textValue().equals("Mandatory")){
                    mandatory.add(i.get("name").textValue());
                }
            }
            
            propertyNode.set(i.get("name").textValue(), tempNode);
        }
        node.set("properties", propertyNode);
        if(!mandatory.isEmpty()){
            node.set("required", mandatory);
        }
        return node;
    }

    public ObjectNode validation(String pid) {
        TypeEntity type = typeRepository.get(pid);
        ObjectNode root = mapper.createObjectNode();

        if(type.getSchema().equals("PID-BasicInfoType")){
            root = handleBasicType(type);
        }
        else{
            root = handleInfoType(type);
            root.put("type", "object");
        }
        
        //Inserting common fields 'title', 'description' and '$schema'. Description optional.
        root.put("title", String.format("Validation schema for type '%s' with the PID '%s'",
            type.getContent().get("name").textValue(), type.getPid()));
        if(type.getContent().has("description")){
            root.put("description", type.getContent().get("description").textValue());
        }
        root.put("$schema", "http://json-schema.org/draft-04/schema#");
        return root;
    }
}
