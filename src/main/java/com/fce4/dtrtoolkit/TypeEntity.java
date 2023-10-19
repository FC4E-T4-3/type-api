package com.fce4.dtrtoolkit;

import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class TypeEntity {
    
    private String pid;
    private String type;
    private String style;
    private String origin;
    private String name = "";
    private long date = 0;
    private String desc = "";
    private ArrayList<String> authors;
    private ArrayList<String> aliases;
    private JsonNode content;

    public TypeEntity(String pid, String prefix, String type, JsonNode content, String style, String origin, String name)
    {
        this.pid = pid;
        this.type = type;
        this.content = content;
        this.style = style;
        this.origin = origin;
        this.name = name;
    }

    public TypeEntity(JsonNode node, String style, String origin)
    {
        this.pid = node.get("id").textValue();
        this.type = node.get("type").textValue();
        this.content = node.get("content");
        this.style = style;
        this.name = node.get("content").get("name").textValue();
        this.origin = origin;
    }

    public TypeEntity(JsonNode node, String origin)
    {
        this.pid = node.get("id").textValue();
        this.type = node.get("type").textValue();
        this.content = node.get("content");
        this.style = "unknown";
        this.name = node.get("content").get("name").textValue();
        this.origin = origin;
    }

    public TypeEntity(Map<String, Object> node)
    {
        ObjectMapper mapper = new ObjectMapper();
        this.pid = node.get("id").toString();
        this.type = node.get("type").toString();
        this.content = mapper.valueToTree(node.get("content"));
        this.authors = (ArrayList<String>) node.get("authors");
        this.date = Long.parseLong(node.get("date").toString());
        this.desc = node.get("desc").toString();
        this.style = node.get("style").toString();
        this.name = node.get("name").toString();
        this.origin = node.get("origin").toString();
    }

    public String getPid(){
        return this.pid;
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

    public String getName(){
        return this.name;
    }

    public String getOrigin(){
        return this.origin;
    }

    public String getDescription(){
        return this.desc;
    }

	public long getDate(){
		return this.date;
	}

    public void setStyle(String style){
        this.style = style;
    }

	public void setDescription(String desc){
		this.desc = desc;
	}

    public void setAuthors(ArrayList<String> authors){
		this.authors = authors;
	}

	public void setAliases(ArrayList<String> aliases){
		this.aliases = aliases;
	}

	public void setDate(long date){
		this.date = date;
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

    public HashMap<String,Object> serializeSearch() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        HashMap<String, Object> typeSearch = new HashMap<>();
        typeSearch.put("id", this.pid);
        typeSearch.put("name", this.name);
        typeSearch.put("type", this.type);
        typeSearch.put("date", this.date);
        typeSearch.put("desc", this.desc);
        typeSearch.put("origin", this.origin);
        typeSearch.put("authors", this.authors.toArray(new String[0]));
        typeSearch.put("content", this.content);
        typeSearch.put("style", this.style);
        return typeSearch;
    }
}
