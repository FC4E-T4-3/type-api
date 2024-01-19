package com.fce4.dtrtoolkit.Validators;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.ObjectUtils.Null;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fce4.dtrtoolkit.TypeEntity;
import com.github.underscore.Json;

@Component
public class EoscValidator extends BaseValidator{
    
    Logger logger = Logger.getLogger(EoscValidator.class.getName());

    public ObjectNode handleBasicType(TypeEntity typeEntity){
        ObjectNode node = mapper.createObjectNode();
        JsonNode content = typeEntity.getContent();

        if(!content.has("Schema")){
            return node;
        }
        JsonNode properties = content.get("Schema");
        String datatype = properties.get("Type").textValue().toLowerCase();
        if(datatype.equals("none")){
            return node;
        }
        
        String propRelation = "AND";
        if(properties.has("PropRelations")){
            propRelation = properties.get("PropRelations").textValue();
        }
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
                logger.info(propRelation);
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

    public ObjectNode handleInfoType(TypeEntity typeEntity) throws Exception {
        ObjectNode node = mapper.createObjectNode();
        JsonNode content = typeEntity.getContent();

        if(!content.has("Schema")){
            return node;
        }

        JsonNode schema = content.get("Schema");
        String type = schema.get("Type").textValue();
        if(type.equals("Object")){
            node.put("type","object");

            if(!schema.has("Properties")){
                return node;
            }
            JsonNode properties = schema.get("Properties");
            boolean addProps = false;
            String subCond = "";
 
            if(schema.has("addProps")){
                addProps = schema.get("addProps").asBoolean();
            }
            if(schema.has("subCond")){
                subCond = schema.get("subCond").textValue();
            }
            if(properties.size()==0){
                return node;
            }

            node.put("additionalProperties", addProps);

            ObjectNode propertyNodes = mapper.createObjectNode();
            ArrayNode requiredArray = mapper.createArrayNode();
            for(JsonNode i : properties){
                ObjectNode propertyNode = mapper.createObjectNode();
                JsonNode typeProperties = i.get("Properties");
                String cardinality = typeProperties.get("Cardinality").textValue();
                boolean isBasic = true;
                boolean extractSub = false;
                String usedName = i.get("Name").textValue();
                TypeEntity propertyEntity = new TypeEntity(typeSearch.get(i.get("Type").textValue(), "types"));
                if(propertyEntity.getType().equals("InfoType")){
                    isBasic = false;
                    if(propertyEntity.getFundamentalType().equals("Object")){
                        if(typeProperties.has("extractProperties")){
                            if(typeProperties.get("extractProperties").asBoolean()){
                            extractSub = true;
                            }
                        }
                    }
                }
                if(cardinality.equals("0 - 1") || cardinality.equals("1")){
                    if(isBasic){
                        propertyNode = handleBasicType(propertyEntity);
                    }
                    else{
                        if(extractSub){
                            ObjectNode tmp = mapper.convertValue(handleInfoType(propertyEntity).get("properties"), ObjectNode.class);
                            propertyNodes.setAll(tmp);
                        }
                        else{
                            propertyNode.put("type", "object");
                            propertyNode.setAll(handleInfoType(propertyEntity));
                        }
                    }
                    if(cardinality.equals("1")){
                        requiredArray.add(usedName);
                    }
                }
                else{
                    propertyNode.put("type", "array");
                    if(isBasic){
                        propertyNode.putPOJO("items", handleBasicType(propertyEntity));
                    }
                    else{
                        propertyNode.putPOJO("items", handleInfoType(propertyEntity));
                    }
                    if(cardinality.equals("1 - n")){
                        requiredArray.add(usedName);
                    }
                }
                if(typeProperties.has("Value")){
                    propertyNode.putPOJO("const",typeProperties.get("Value"));
                }

                if(i.has("Title")){
                    propertyNode.put("title",i.get("Title").textValue());
                }

                if(i.has("Description")){
                    propertyNode.put("description",i.get("Description").textValue());
                }

                if(!extractSub){
                    propertyNodes.putPOJO(usedName, propertyNode);
                }
            }

            if(requiredArray.size()>0){
                node.putPOJO("required", requiredArray);
            }

            if(!subCond.equals("")){
                ArrayNode oneOfNode = node.putArray(subCond);
                Iterator<Map.Entry<String, JsonNode>> fieldsIterator = propertyNodes.fields();
                while (fieldsIterator.hasNext()) {
                    ObjectNode tmp = mapper.createObjectNode();
                    ObjectNode tmpProps = mapper.createObjectNode();
                    Map.Entry<String, JsonNode> entry = fieldsIterator.next();
                    tmp.putPOJO(entry.getKey(), entry.getValue());
                    tmpProps.putPOJO("properties", tmp);
                    oneOfNode.addPOJO(tmpProps);
                }
            }
            else{
                node.putPOJO("properties", propertyNodes);
            }
        }
        else{
            node.put("type", "array");
            if(!schema.has("Properties")){
                return node;
            }
            JsonNode arrayProps = schema.get("Properties");
            if(arrayProps.has("maxItems")){
                if(arrayProps.get("maxItems").asInt()>0){
                node.put("maxItems", arrayProps.get("maxItems").asInt());
                }
            }
            if(arrayProps.has("minItems")){
                if(arrayProps.get("minItems").asInt()>0){
                node.put("minItems", arrayProps.get("minItems").asInt());
                }
            }
            if(arrayProps.has("unique")){
                if(schema.get("unique").asBoolean()){
                    node.put("unique", true);
                }
            }

            ObjectNode propertyNode = mapper.createObjectNode();
            TypeEntity propertyEntity = new TypeEntity(typeSearch.get((schema.get("subCond").textValue()), "types"));
            if(propertyEntity.getType().equals("BasicInfoType")){
                propertyNode = handleBasicType(propertyEntity);
            }
            else{
                propertyNode = handleInfoType(propertyEntity);
            }
            node.putPOJO("items",propertyNode);
        }
        
        return node;
    } 
    
    public ObjectNode validation(String pid) throws Exception{
        TypeEntity type = new TypeEntity(typeSearch.get(pid, "types"));
        ObjectNode root = mapper.createObjectNode();

        if(type.getType().equals("BasicInfoType")){
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
