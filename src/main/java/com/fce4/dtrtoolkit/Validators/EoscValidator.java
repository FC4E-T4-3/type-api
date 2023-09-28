package com.fce4.dtrtoolkit.Validators;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fce4.dtrtoolkit.TypeEntity;

@Component
public class EoscValidator extends BaseValidator{
    
    Logger logger = Logger.getLogger(EoscValidator.class.getName());


    public ObjectNode handleBasicType(TypeEntity typeEntity){
        ObjectNode node = mapper.createObjectNode();
        JsonNode content = typeEntity.getContent();
        if(!content.has("TypeSchema")){
            return node;
        }
        JsonNode properties = content.get("TypeSchema").get("Properties");
        String datatype = properties.get("Type").textValue().toLowerCase();
        if(datatype.equals("none")){
            return node;
        }

        node.put("type", datatype);

        String propRelation = properties.get("PropRelations").textValue();
        if(!properties.has("Properties")){
            return node;
        }
        JsonNode typeProperties = properties.get("Properties");
        switch(datatype){
            case "enum":{
            String enumType = typeProperties.get(0).get("Property").textValue();
            JsonNode enumValues = typeProperties.get(0).get("Value");                
            switch(enumType){
                case "$ref":{
                    node.put("$ref", enumValues.textValue());
                    break;
                }
                default:{
                    ArrayNode arrayNode = node.putArray("enum");
                    for(JsonNode i : enumValues){
                        arrayNode.add(i);
                    }
                    break;
                }
            }
            break;
        }
            case "boolean": {
                node.put("type", datatype);
                if(properties.has("Properties")){
                    if(properties.get("Properties").textValue().equals("Always True")){
                        node.put("const", true);
                    }
                    else{
                        node.put("const",false);
                    }
                }
                break;
            }
            default: {
                node.put("type", datatype);
                if(propRelation.equals("AND")){
                    for(JsonNode i : typeProperties){
                        node.putPOJO(i.get("Property").textValue(), i.get("Value"));
                    }
                }
                else{
                    String relationName = (propRelation.equals("OR")) ? "anyOf" : "oneOf";
                    ArrayNode anyOf = node.putArray(relationName);
                    for(JsonNode i : typeProperties){
                        ObjectNode tmp = mapper.createObjectNode();
                        tmp.putPOJO(i.get("Property").textValue(), i.get("Value"));
                        anyOf.addPOJO(tmp);
                    }
                }
                break;
            }              
        }
        return node;
    }

    public ObjectNode handleInfoType(TypeEntity typeEntity) {
        ObjectNode node = mapper.createObjectNode();
            
        return node;
    } 
    
    public ObjectNode validation(String pid) {
        TypeEntity type = typeRepository.get(pid);
        ObjectNode root = mapper.createObjectNode();

        System.out.println(type.getContent());

        if(type.getSchema().equals("BasicInfoType")){
            root = handleBasicType(type);
        }
        else{
            root = handleInfoType(type);
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
