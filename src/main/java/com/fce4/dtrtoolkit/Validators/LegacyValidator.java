package com.fce4.dtrtoolkit.Validators;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.fce4.dtrtoolkit.TypeEntity;

/**
 * The class used to generate validation schemas for legacy types, which are types from the old GWDG DTR's
 */
@Component
public class LegacyValidator extends BaseValidator {

    Logger logger = Logger.getLogger(LegacyValidator.class.getName());

    /**
     * Describing possible relations between the properties of a type.
     */
    enum PropRelation {
        DENY_ADD,
        ARRAY,
        ALL_OF,
        ANY_OF,
        ONE_OF,
        // NOT,
        // SET,
        // TUPLE,
        NONE,
    }

    /**
     * Describing if a possible shortened version of the schema should be used
     */
    enum Abbreviation {
        YES,
        NO,
        BOTH,
    }


    /**
     * Generates the validation schema for a single basic info type.
     * @param type the TypeEntity of which the schema is to be created
     */
    public ObjectNode handleBasicType(TypeEntity typeEntity){
        Boolean isNumeric = false;
        ObjectNode node = mapper.createObjectNode();
        //The properties field is an array, but never meaningfully used beyond the first element. 
        if(!typeEntity.getContent().has("properties")){
            return node;
        }
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
                if(isNumeric || key.equals("minLength") || key.equals("maxLength")){
                    Double valueNumeric = Double.parseDouble(value);
                    if(valueNumeric % 1 == 0){
                        node.put(key, Integer.parseInt(value));
                    }
                    else{
                        node.put(key, valueNumeric);
                    }
                }
                else{
                    node.put(key, value);
                }
            }
        }

        //Same thing as restrictions for a possible enum.
        if(properties.has("enum")){
            ArrayNode arrayNode = node.putArray("enum");
            // String cleaned = properties.get("enum").textValue().replaceAll("\"","")
            //     .replaceAll("\\[", "").replaceAll("\\]", "");
            // String[] enumValues = cleaned.split(",");
            String cleaned = properties.get("enum").textValue().replaceAll("\\[", "")
                .replaceAll("\\]", "");
            ArrayList<String> enumValues = new ArrayList<String>(Arrays.asList(cleaned.split("\"")));
            enumValues.removeAll(Collections.singleton(","));
            enumValues.removeAll(Collections.singleton(", "));
            enumValues.removeAll(Collections.singleton(",  "));
            enumValues.removeAll(Collections.singleton(",   "));
            enumValues.removeAll(Collections.singleton(""));
            enumValues.removeAll(Collections.singleton(" "));
            enumValues.removeAll(Collections.singleton("  "));
            enumValues.removeAll(Collections.singleton("   "));


            for(String i : enumValues){
                if(isNumeric){
                    Double valueNumeric = Double.parseDouble(i);
                    if(valueNumeric % 1 == 0){
                        arrayNode.add(Integer.parseInt(i));
                    }
                    else{
                        arrayNode.add(valueNumeric);
                    }
                }
                else{
                    arrayNode.add(i);
                }
            }
        }

        //Add a regular expression if given.
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

    /**
     * Generates the validation schema for a single InfoType. KIP's are handled here as well, since only some conditions are different.
     * @param type the TypeEntity of which the schema is to be created
     */
    public ObjectNode handleInfoType(TypeEntity typeEntity, Boolean initial){
       
        ArrayNode mandatory = mapper.createArrayNode();
		Abbreviation abbreviation = Abbreviation.NO;
		if(!typeEntity.getSchema().equals("KernelInformationProfile")){
			abbreviation = getAbbreviation(typeEntity);
		}

        ObjectNode propertyNode = processProperties(typeEntity, mandatory);
		ObjectNode node = processPropRelations(typeEntity, propertyNode, mandatory, initial);

        //If no shortened version is desired for the root node, nest it as property in a new node.
        if(initial){
            if(typeEntity.getContent().has("description")){
                node.put("description", typeEntity.getContent().get("description").textValue());
            }
            if(abbreviation.equals(Abbreviation.NO)){
                ObjectNode tmpNode = mapper.createObjectNode();
                tmpNode.set(typeEntity.getName(), node);
                node = mapper.createObjectNode();
                ArrayNode tmpReq = mapper.createArrayNode();
                
                tmpReq.add(typeEntity.getName());
                node.set("properties", tmpNode);
                node.set("required",tmpReq);
                node.put("additionalProperties", false);
                node.put("type", "object");
            }
        }
        return node;
    }

    /**
     * Function that iterates all properties of a type. According to the schema of the type (Basic/Info), properties are directly or recursively resolved.
     * @param typeEntity the TypeEntity of which the schema is to be created
     * @param mandatory the ArrayNode to store the mandatory properties, to be passed to next function 
     */
	ObjectNode processProperties(TypeEntity typeEntity, ArrayNode mandatory) {
        ObjectNode propertyNode = mapper.createObjectNode();

        JsonNode properties = typeEntity.getContent().get("properties");

        if(properties != null)
        {
            for(JsonNode i : properties){
                ObjectNode tempNode = mapper.createObjectNode();
                TypeEntity tempEntity = typeRepository.get(i.get("identifier").textValue());
                
                //Function is recursively called, until only basic types remain.
                if(tempEntity.getSchema().equals("PID-BasicInfoType")){
                    tempNode = handleBasicType(tempEntity);
                }
                else{
                    tempNode = handleInfoType(tempEntity, false);
                }
                
                if(tempEntity.getContent().has("description")){
                    tempNode.put("description", tempEntity.getContent().get("description").textValue());
                }

                propertyNode.set(i.get("name").textValue(), tempNode);

                if(i.has("representationsAndSemantics")){
                    JsonNode repSem = i.get("representationsAndSemantics").get(0);	
                    if(repSem.get("obligation").textValue().equals("Mandatory")){
                        mandatory.add(i.get("name").textValue());
                    }
                }
            }
        }
		return propertyNode;
	}
    
    /**
     * Function that handles the relations that exist between properties of a type. I.e., if it is an array, tuple, oneOf etc.
     * The function handles to more complex assembling of the schema parts.
     * @param typeEntity the TypeEntity of which the schema is to be created
     * @param propertyNode the ObjectNode containing the properties
     * @param mandatory the ArrayNode to store the mandatory properties, to be passed to next function 
     */
	ObjectNode processPropRelations(TypeEntity typeEntity, ObjectNode propertyNode, ArrayNode mandatory, Boolean initial) {
        ObjectNode node = mapper.createObjectNode();
        PropRelation propRelation = getPropRelation(typeEntity);
        Abbreviation abbreviation = getAbbreviation(typeEntity);
        Boolean omitName = firstPropOmitName(typeEntity); //Check if the only property has "omit name as subsidiary" set to true
        JsonNode repSemObj = typeEntity.getContent().get("representationsAndSemantics");
        Boolean addPropSec = false;
        //Next part is hacky, but there are never more than two elements, and never used in a different way.
        if(repSemObj != null){
            if(repSemObj.size() == 2){
                if(repSemObj.get(1).get("subSchemaRelation").textValue().equals("denyAdditionalProperties")){ 
                    addPropSec = true;
                }
            }
        }

		switch(propRelation){
            case DENY_ADD:
                if(propertyNode.size() == 1 && omitName){
                    if(omitName){
                        String key = getJSONKeys(propertyNode).get(0);
                        node = propertyNode.get(key).deepCopy();
                    }
                }
                else{
                    if(abbreviation.equals(Abbreviation.YES) && !initial){
                        node.set("items", arrayFromObject(propertyNode));
                        //node.withObject("items").put("additionalProperties", false);
                        node.put("type", "array");
                    }
                    else{
                        node.put("type", "object");
                        node.set("properties", propertyNode);
                        if(!mandatory.isEmpty()){
                            node.set("required", mandatory);
                        }
                        node.put("additionalProperties", false);
                    }
                }
                break;

            case ARRAY:
                node.put("type", "array");
                ObjectNode propTmp = mapper.createObjectNode();
                //Arrays are special cases for legacy types, whereas a type is considered only an array iff it has one property, otherwise it's a tuple.
                //In addition to the amount of properties, multiple conditions must be considered: 
                // - Should the name of the property be omitted? (Only relevant for true arrays)
                // - Is 'Abbreviated Form' 
                if(propertyNode.size() == 1){
                    if(omitName){
                        if(abbreviation.equals(Abbreviation.YES)){
                            String key = getJSONKeys(propertyNode).get(0);
                            ArrayNode propArray = propertyNode.get(key).get("items").deepCopy();
                            propTmp.set("items", propArray);
                            node.set("items",propTmp);
                            if(addPropSec){
                                node.withObject("items").put("additionalProperties", false);
                            }
                        }
                        else{
                            String key = getJSONKeys(propertyNode).get(0);
                            if(propertyNode.has("properties")){
                                propTmp.set("properties", propertyNode.get(key).get("properties").deepCopy());
                                if(addPropSec){
                                    propTmp.put("additionalProperties", false);
                                }
                                if(!mandatory.isEmpty()){
                                    node.withObject("items").set("required", mandatory);
                                }
                            }
                            else{
                                propTmp = propertyNode.get(key).deepCopy();
                                if(addPropSec){
                                    propTmp.put("additionalProperties", false);
                                }
                            }
                            node.set("items",propTmp);
                        }
                    }
                    else{
                        propTmp.set("properties", propertyNode);
                        node.set("items", propTmp);
                        if(addPropSec){
                            node.withObject("items").put("additionalProperties", false);
                        }
                        node.put("minItems", 1);
                    }
                }
                //Means we are handling a tuple
                else{
                    if(omitName){
                        node.set("items", arrayFromObject(propertyNode));
                    }
                    else{
                        node.put("additionalItems", false);
                        propTmp.set("properties", arrayFromObject(propertyNode));
                        node.set("items", propTmp);
                        if(!mandatory.isEmpty()){
                            node.withObject("items").set("required", mandatory);
                        }
                    }
                }
                break;

			case ONE_OF:
				node.put("type", "object");
				node.set("oneOf", arrayFromObject(propertyNode));
                break;

			case ALL_OF:
				node.put("type", "object");
				node.set("allOf", arrayFromObject(propertyNode));
                break;

			case ANY_OF:
				node.put("type", "object");
				node.set("anyOf", arrayFromObject(propertyNode));
                break;

			case NONE:
                if(propertyNode.size() == 1 && omitName){
                    if(omitName){
                        String key = getJSONKeys(propertyNode).get(0);
                        node = propertyNode.get(key).deepCopy();
                    }
                }
                else{
                    if(abbreviation.equals(Abbreviation.YES) && !initial){
                        node.set("items", arrayFromObject(propertyNode));
                        node.put("type", "array");
                    }
                    else{
                        node.put("type", "object");
                        node.set("properties", propertyNode);
                        if(!mandatory.isEmpty()){
                            node.set("required", mandatory);
                        }
                    }
                }
                break;
        }

        //Same here, hacky but covers all cases.
        if(repSemObj != null){
            if(repSemObj.get(0).has("restrict")){
                String cleaned = repSemObj.get(0).get("restrict").textValue()
                    .replaceAll("\\s+","").replaceAll("\"","");
                String[] restrictions = cleaned.split(",");
                for(String i : restrictions){
                    String key = i.split(":")[0];
                    String value = i.split(":")[1];
                    if(NumberUtils.isParsable(value)){
                        Double valueNumeric = Double.parseDouble(value);
                        if(valueNumeric % 1 == 0){
                            node.put(key, Integer.parseInt(value));
                        }
                        else{
                            node.put(key, valueNumeric);
                        }
                    }
                    else{
                        node.put(key, value);
                    }
                }
            }
        }

		return  node;
	}

    /**
     * Takes the elements of an object and converts them into a Json ArrayNode
     * @param objectNode the ObjectNode containing the properties
     */
	ArrayNode arrayFromObject(ObjectNode objectNode){
		ArrayNode propArray = mapper.createArrayNode();
		for(JsonNode i : objectNode){
			propArray.add(i);
		}
		return propArray;
	}

     /**
     * Helper function, returns the keys on the first level of a JSON object
     * @param objectNode the ObjectNode in question
     */
    public List<String> getJSONKeys(ObjectNode objectNode) {
        List<String> keys = new ArrayList<>();
        Iterator<String> iterator = objectNode.fieldNames();
        iterator.forEachRemaining(e -> keys.add(e));
        return keys;
    }

    /**
     * Extracts the Propety Relation from a TypeEntity
     * @param typeEntity the TypeEntity in question
     */
    PropRelation getPropRelation(TypeEntity typeEntity) {
        PropRelation propRelation = PropRelation.NONE;
        if(!typeEntity.getContent().has("representationsAndSemantics")){
            return propRelation;
        }
        JsonNode repSemObj = typeEntity.getContent().get("representationsAndSemantics").get(0);
        switch(repSemObj.get("subSchemaRelation").textValue()){ 
            case "denyAdditionalProperties":
                propRelation = PropRelation.DENY_ADD;
                break;
            case "isArrayWithGivenProperties":
                propRelation = PropRelation.ARRAY;
                break;
            case "requestAllOfProperties":
                propRelation = PropRelation.ALL_OF;
                break;
            case "requestAnyOfProperties":
                propRelation = PropRelation.ANY_OF;
                break;
            case "requestOneOfProperties":
                propRelation = PropRelation.ONE_OF;
                break;
        }
        return propRelation;
    } 
    
    /**
     * Extracts if a TypeEntity allows an abbreviated form
     * @param typeEntity the TypeEntity in question
     */
    Abbreviation getAbbreviation(TypeEntity typeEntity) {
        Abbreviation abbreviation = Abbreviation.NO;
        if(typeEntity.getContent().has("representationsAndSemantics")){
            if(typeEntity.getContent().get("representationsAndSemantics").get(0).has("allowAbbreviatedForm")){
                JsonNode repSemObj = typeEntity.getContent().get("representationsAndSemantics").get(0);
                switch(repSemObj.get("allowAbbreviatedForm").textValue()){ 
                    case "No":
                        abbreviation = Abbreviation.NO;
                        break;
                    case "Yes":
                        abbreviation = Abbreviation.YES;
                        break;
                    case "Both":
                        abbreviation = Abbreviation.BOTH;
                        break;
                }
            }
        }
        return abbreviation;
    }

    /**
     * Since the omitName property is seemingly only relevant for single element types, i.e. arrays, this function extracts that property
     * only for the first, and only, property
     * @param typeEntity the TypeEntity in question
     */
    Boolean firstPropOmitName(TypeEntity typeEntity){
        if(typeEntity.getContent().has("properties")){
            JsonNode properties = typeEntity.getContent().get("properties").get(0);
            if(properties.has("representationsAndSemantics")){
                JsonNode repSem = properties.get("representationsAndSemantics").get(0);	
                if(repSem.has("allowOmitSubsidiaries")){
                    if(repSem.get("allowOmitSubsidiaries").textValue().equals("No")){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Entry function for validation. Creates the root node and depending on the schema of the type
     * moves foward with the validation. BasicInfoTypes get generated directly, InfoTypes recursively.
     * @param type the TypeEntity of which the schema is to be created
     */
    public ObjectNode validation(String pid) {
        TypeEntity type = typeRepository.get(pid);
        ObjectNode root = mapper.createObjectNode();

        if(type.getSchema().equals("PID-BasicInfoType")){
            root = handleBasicType(type);
        }
        else{
            root = handleInfoType(type, true);
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
