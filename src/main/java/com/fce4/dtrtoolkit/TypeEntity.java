package com.fce4.dtrtoolkit;

import java.util.ArrayList;

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
    private ArrayList<String> taxonomies = new ArrayList<String>();
    private ArrayList<String> authors;
    private ArrayList<String> aliases;
    private JsonNode content;
    private String unit = "None";

    public TypeEntity(String pid, String prefix, String type, JsonNode content, String style, String origin, String name) {
        this.pid = pid;
        this.type = type;
        this.content = content;
        this.style = style;
        this.origin = origin;
        this.name = name;
    }

    public TypeEntity(JsonNode node, String style, String origin) {
        this.pid = node.get("id").textValue();
        this.type = node.get("type").textValue();
        this.content = node.get("content");
        this.style = style;
        this.name = node.get("content").get("name").textValue();
        this.origin = origin;
    }

    public TypeEntity(JsonNode node, String origin) {
        this.pid = node.get("id").textValue();
        this.type = node.get("type").textValue();
        this.content = node.get("content");
        this.style = "unknown";
        this.name = node.get("content").get("name").textValue();
        this.origin = origin;
    }

    public TypeEntity(Map<String, Object> node) {
        ObjectMapper mapper = new ObjectMapper();
        this.pid = node.get("id").toString();
        this.type = node.get("type").toString();
        this.content = mapper.valueToTree(node.get("content"));
        this.authors = (ArrayList<String>) node.get("authors");
        this.taxonomies = (ArrayList<String>) node.get("taxonomies");
        this.date = Long.parseLong(node.get("date").toString());
        this.desc = node.get("description").toString();
        this.style = node.get("style").toString();
        this.name = node.get("name").toString();
        this.origin = node.get("origin").toString();
        if(node.containsKey("unit")){
            this.unit = node.get("unit").toString();
        }
    }

    public String getPid() {
        return this.pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStyle() {
        return this.style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getOrigin() {
        return this.origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDate() {
        return this.date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public ArrayList<String> getAuthors() {
        return this.authors;
    }

    public void setAuthors(ArrayList<String> authors) {
        this.authors = authors;
    }

    public ArrayList<String> getTaxonomies() {
        return this.taxonomies;
    }

    public void setTaxonomies(ArrayList<String> taxonomies) {
        this.taxonomies = taxonomies;
    }

    public ArrayList<String> getAliases() {
        return this.aliases;
    }

    public void setAliases(ArrayList<String> aliases) {
        this.aliases = aliases;
    }

    public JsonNode getContent() {
        return this.content;
    }

    public void setContent(JsonNode content) {
        this.content = content;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getUnit() {
        return this.unit;
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
        HashMap<String, Object> typeSearch = new HashMap<>();
        typeSearch.put("id", this.pid);
        typeSearch.put("name", this.name);
        typeSearch.put("type", this.type);
        typeSearch.put("date", this.date);
        typeSearch.put("description", this.desc);
        typeSearch.put("origin", this.origin);
        typeSearch.put("authors", this.authors.toArray(new String[0]));
        typeSearch.put("taxonomies", this.taxonomies.toArray(new String[0]));
        typeSearch.put("content", this.content);
        typeSearch.put("style", this.style);
        typeSearch.put("unit", this.unit);
        return typeSearch;
    }
}
