package com.fce4.dtrtoolkit.Validators;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Arrays;

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

        String propRelation = properties.get("PropRelations").textValue();
        if(!properties.has("Properties")){
            node.put("type", datatype);
            return node;
        }
        JsonNode typeProperties = properties.get("Properties");
        switch(datatype){
            case "enum":{
                String enumType = typeProperties.get(0).get("Property").textValue();
                JsonNode enumValues = typeProperties.get(0).get("Value");
                if(enumType.equals("$ref")){
                    node.put("$ref", enumValues.textValue());
                }
                else{
                    ArrayNode arrayNode = node.putArray("enum");
                    for(JsonNode i : enumValues){
                        arrayNode.add(i);
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
        JsonNode content = typeEntity.getContent();

        if(!content.has("Components")){
            return node;
        }
        JsonNode components = content.get("Components");
    
        JsonNode properties = components.get("Properties");
        boolean addProps = false;
        if(components.has("addProps")){
            addProps = content.get("Components").get("addProps").asBoolean();
        }
        
        if(properties.size()==0){
            return node;
        }

        node.put("type","object");
        ObjectNode propertyNodes = mapper.createObjectNode();
        ArrayNode required = mapper.createArrayNode();

        String relations = content.get("Components").get("Relations").textValue();

        switch(relations){
            case "AND":{
                for(JsonNode i : properties){
                    ObjectNode propertyNode = mapper.createObjectNode();
                    JsonNode typeProperties = i.get("Properties");
                    String cardinality = typeProperties.get("Cardinality").textValue();
                    boolean isBasic = true;
                    String usedName = i.get("Name").textValue();
                    TypeEntity propertyEntity = typeRepository.get(i.get("Type").textValue());
                    logger.info(i.get("Type").textValue());
                    System.out.println(propertyEntity.serialize());
                    if(propertyEntity.getSchema().equals("InfoType")){
                        isBasic = false;
                    }

                    if(cardinality.equals("0 - 1") || cardinality.equals("1")){
                        if(isBasic){
                            propertyNode = handleBasicType(propertyEntity);
                            System.out.println(propertyNode);
                        }
                        else{
                            propertyNode.put("type", "object");
                            propertyNode.put("additionalProperties", addProps);
                            propertyNode.putAll(handleInfoType(propertyEntity));
                        }
                        if(cardinality.equals("1")){
                            required.add(usedName);
                        }
                    }
                    else{
                        logger.info("ARRAY");
                        propertyNode.put("type", "array");
                        if(isBasic){
                            propertyNode.putPOJO("items", handleBasicType(propertyEntity));
                        }
                        else{
                            propertyNode.putPOJO("items", handleInfoType(propertyEntity));
                        }
                        if(cardinality.equals("1 - n")){
                            propertyNode.put("minItems", 1);
                            required.add(usedName);
                        }

                    }
                    propertyNodes.putPOJO(usedName, propertyNode);
                }
                if(required.size()>0){
                    node.putPOJO("required", required);
                }
                node.putPOJO("properties", propertyNodes);
            }
        }
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
        root.put("description", String.format("Validation schema for type '%s' with the PID '%s'",
            type.getContent().get("name").textValue(), type.getPid()));
        if(type.getContent().has("description")){
            root.put("description", type.getContent().get("description").textValue());
        }
        root.put("$schema", "http://json-schema.org/draft-04/schema#");

        return root;
    }
}
