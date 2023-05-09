package com.fce4.dtrtoolkit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TypeEntity {
    
    private String pid;
    private String prefix;
    private String type;
    private String style;
    private String origin;
    private JsonNode content;

    public TypeEntity(String pid, String prefix, String type, JsonNode content, String style, String origin)
    {
        this.pid = pid;
        this.prefix = prefix;
        this.type = type;
        this.content = content;
        this.style = style;
        this.origin = origin;
    }

    public TypeEntity(JsonNode node, String style, String origin)
    {
        this.pid = node.get("id").textValue();
        this.prefix = pid.split("/")[0];
        this.type = node.get("type").textValue();
        this.content = node.get("content");
        this.style = style;
        this.origin = origin;
    }

    public TypeEntity(JsonNode node, String origin)
    {
        this.pid = node.get("id").textValue();
        this.prefix = pid.split("/")[0];
        this.type = node.get("type").textValue();
        this.content = node.get("content");
        this.style = "unknown";
        this.origin = origin;
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

    public String getStyle(){
        return this.style;
    }

    public String getOrigin(){
        return this.origin;
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
