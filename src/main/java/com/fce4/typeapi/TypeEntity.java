package com.fce4.typeapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TypeEntity {
    
    private String pid;
    private String prefix;
    private String type;
    private JsonNode content;

    public TypeEntity(String pid, String prefix, String type, JsonNode content)
    {
        this.pid = pid;
        this.prefix = prefix;
        this.type = type;
        this.content = content;
    }

    public TypeEntity(JsonNode node)
    {
        this.pid = node.get("id").textValue();
        this.prefix = pid.split("/")[0];
        this.type = node.get("type").textValue();
        this.content = node.get("content");
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
