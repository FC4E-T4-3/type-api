package com.fce4.dtrtoolkit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TypeEntity {
    
    private String pid;
    private String prefix;
    private String type;
    private JsonNode content;
    private boolean validate;

    public TypeEntity(String pid, String prefix, String type, JsonNode content, boolean validate)
    {
        this.pid = pid;
        this.prefix = prefix;
        this.type = type;
        this.content = content;
        this.validate = validate;
    }

    public TypeEntity(JsonNode node)
    {
        this.pid = node.get("id").textValue();
        this.prefix = pid.split("/")[0];
        this.type = node.get("type").textValue();
        this.content = node.get("content");
        this.validate = true;
    }

    public TypeEntity(JsonNode node, boolean validate)
    {
        this.pid = node.get("id").textValue();
        this.prefix = pid.split("/")[0];
        this.type = node.get("type").textValue();
        this.content = node.get("content");
        this.validate = validate;
    }

    public String getPid(){
        return this.pid;
    }

    public String getPrefix(){
        return this.prefix;
    }

    public String getSchema(){
        return this.type;
    }

    public JsonNode getContent(){
        return this.content;
    }

    public boolean getValidate(){
        return this.validate;
    }

    /**
     * Serializes a TypeObject to a JsonNode object. The setters and getters are required for the mapper.
     * @return a JsonNode representing the TypeObject.
     */
    public JsonNode serialize() {
        ObjectMapper mapper = new ObjectMapper(); 
        JsonNode node = mapper.convertValue(this, JsonNode.class);
        return node;
    }
}
