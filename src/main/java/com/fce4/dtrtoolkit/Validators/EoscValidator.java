package com.fce4.dtrtoolkit.Validators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fce4.dtrtoolkit.Entities.TypeEntity;

@Component
public class EoscValidator extends BaseValidator{

    Logger logger = Logger.getLogger(EoscValidator.class.getName());
    private static final int MAX_DEPTH = 100; //

    private ArrayList<Object> basicTypes;
    private ArrayList<Object> compositeTypes;

    public void setTypes(ArrayList<Object> basicTypes, ArrayList<Object> compositeTypes) {
        this.basicTypes = basicTypes;
        this.compositeTypes = compositeTypes;
    }

    public ObjectNode handleBasicType(TypeEntity typeEntity){
        ObjectNode node = mapper.createObjectNode();
        JsonNode content = typeEntity.getContent();
        node.put("@id", "hdl:" + typeEntity.getPid());
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
                    switch (enumType){
                        case "String Enum":
                            node.put("type","string");
                            break;
                        case "Integer Enum":
                            node.put("type","integer");
                            break;
                        case "Number Enum":
                            node.put("type","number");
                            break;
                    }
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

    public ObjectNode handleInfoType(TypeEntity typeEntity, int depth) throws Exception {
        if (depth > MAX_DEPTH) {
            logger.warning("Max depth reached for type: " + typeEntity.getPid());
            return mapper.createObjectNode().put("error", "Max depth reached");
        }

        ObjectNode node = mapper.createObjectNode();
        JsonNode content = typeEntity.getContent();
        node.put("@id", "hdl:" + typeEntity.getPid());

        if(!content.has("Schema")){
            return node;
        }

        JsonNode schema = content.get("Schema");
        String type = schema.get("Type").textValue();
        if(type.equals("Object")){

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
            if(properties.isEmpty()){
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
                if(compositeTypes.contains(propertyEntity.getType())){
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
                            ObjectNode tmp = handleInfoType(propertyEntity, depth + 1);
                            if(tmp.has("properties")){
                                propertyNodes.setAll(mapper.convertValue(tmp.get("properties"), ObjectNode.class));
                            } else if (tmp.has("allOf")) {
                                ArrayNode allOf = propertyNodes.putArray("allOf");
                                for(JsonNode jNode : tmp.get("allOf")){
                                    ObjectNode oNode = mapper.readValue(jNode.toString(), ObjectNode.class);
                                    allOf.add(oNode.get("properties"));
                                }
                               // propertyNodes.putPOJO("allOf", tmp.get("allOf"));
                            } else if (tmp.has("oneOf")) {
                                ArrayNode oneOf = propertyNodes.putArray("oneOf");
                                for(JsonNode jNode : tmp.get("oneOf")){
                                    ObjectNode oNode = mapper.readValue(jNode.toString(), ObjectNode.class);
                                    oneOf.add(oNode.get("properties"));
                                }
                                // propertyNodes.putPOJO("allOf", tmp.get("allOf"));
                            } else if (tmp.has("anyOf")) {
                                ArrayNode anyOf = propertyNodes.putArray("anyOf");
                                for(JsonNode jNode : tmp.get("anyOf")){
                                    ObjectNode oNode = mapper.readValue(jNode.toString(), ObjectNode.class);
                                    anyOf.add(oNode.get("properties"));
                                }
                            }
                        }
                        else{
                            propertyNode.put("type", "object");
                            propertyNode.setAll(handleInfoType(propertyEntity, depth));
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
                        if(extractSub){
                            propertyNodes.putPOJO("Info", "Extract Properties field is not compatible with arrays.");
                        }
                        else{
                            propertyNode.putPOJO("items", handleInfoType(propertyEntity, depth + 1 ));
                        }
                    }
                    if(cardinality.equals("1 - n")){
                        requiredArray.add(usedName);
                    }
                }
                if(typeProperties.has("Const Value")){
                    if(propertyNode.has("type")){
                        String typeString = propertyNode.get("type").textValue();
                        switch (typeString){
                            case "number":
                                propertyNode.put("const",typeProperties.get("Const Value").asDouble());
                                break;
                            case "integer":
                                propertyNode.put("const",typeProperties.get("Const Value").asInt());
                                break;
                            case "boolean":
                                propertyNode.put("const",typeProperties.get("Const Value").asBoolean());
                                break;
                            default:
                                propertyNode.putPOJO("const",typeProperties.get("Const Value"));
                                break;
                        }
                    }
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
                    oneOfNode.add(entry.getValue());
                }
            }
            else{
                node.put("type","object");
                node.putPOJO("properties", propertyNodes);
            }
        }
        else{
            node.put("type", "array");

            String subCond = "";

            if(schema.has("subCond")){
                subCond = schema.get("subCond").textValue();
            }
            if(schema.has("maxItems")){
                if(schema.get("maxItems").asInt()>0){
                node.put("maxItems", schema.get("maxItems").asInt());
                }
            }
            if(schema.has("minItems")){
                if(schema.get("minItems").asInt()>0){
                node.put("minItems", schema.get("minItems").asInt());
                }
            }
            if(schema.has("unique")){
                if(schema.get("unique").asBoolean()){
                    node.put("unique", true);
                }
            }

            if(subCond.equals("")){
                return node;
            }
            ObjectNode propertyNode = mapper.createObjectNode();
            TypeEntity propertyEntity = new TypeEntity(typeSearch.get(subCond, "types"));
            if(basicTypes.contains(propertyEntity.getType())){
                propertyNode = handleBasicType(propertyEntity);
            }
            else{
                propertyNode = handleInfoType(propertyEntity, depth + 1);
            }
            node.putPOJO("items",propertyNode);
        }
        return node;
    }

    public ObjectNode validation(String pid) throws Exception{
        Map<String, Object> typeFromSearch = typeSearch.get(pid, "types");
        String data = mapper.writeValueAsString(typeFromSearch);
        JsonNode obj = mapper.readTree(data);

        if(obj.has("schema")){
            return obj.get("schema").deepCopy();
        }

        TypeEntity type = new TypeEntity(typeFromSearch);
        ObjectNode root;

        if(basicTypes.contains(type.getType())){
            root = handleBasicType(type);
        }
        else{
            root = handleInfoType(type, 0);
        }

        //Inserting common fields 'title', 'description' and '$schema'. Description optional.
        if(type.getContent().has("description")){
            root.put("description", type.getContent().get("description").textValue());
        }
        else{
            root.put("description", String.format("Validation schema for type '%s' with the PID '%s'",
                    type.getContent().get("name").textValue(), type.getPid()));
        }
        root.put("title", type.getContent().get("name").textValue());
        root.put("$schema", "http://json-schema.org/draft-04/schema#");

        return root;
    }
}
