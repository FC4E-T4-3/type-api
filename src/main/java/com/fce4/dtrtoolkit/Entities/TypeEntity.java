package com.fce4.dtrtoolkit.Entities;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class TypeEntity extends GeneralEntity{

    private ArrayList<String> taxonomies = new ArrayList<String>();
    private JsonNode content;
    private String unit = "None";
    private String fundamentalType = "None";
    private String style;

    public TypeEntity(String pid, String prefix, String type, JsonNode content, String style, String origin, String name) {
        this.setPid(pid);
        this.setType(type);
        this.content = content;
        this.setStyle(style);
        this.setOrigin(origin);
        this.setName(name);
    }

    public TypeEntity(Map<String, Object> node) {
        super();
        ObjectMapper mapper = new ObjectMapper();
        this.setPid(node.get("id").toString());
        this.setType(node.get("type").toString());
        this.content = mapper.valueToTree(node.get("content"));
        this.setAuthors((ArrayList<String>) node.get("authors"));
        this.taxonomies = (ArrayList<String>) node.get("taxonomies");
        this.setAliases((ArrayList<String>) node.get("aliases"));
        this.setDate(Long.parseLong(node.get("date").toString()));
        this.setDesc(node.get("description").toString());
        this.setStyle(node.get("style").toString());
        this.setName(node.get("name").toString());
        this.setOrigin(node.get("origin").toString());
        if(node.containsKey("fundamentalType")){
            this.fundamentalType = node.get("fundamentalType").toString();
        }
        if(node.containsKey("unit")){
            this.unit = node.get("unit").toString();
        }
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public ArrayList<String> getTaxonomies() {
        return this.taxonomies;
    }

    public void setTaxonomies(ArrayList<String> taxonomies) {
        this.taxonomies = taxonomies;
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

     public void setFundamentalType(String type) {
        this.fundamentalType = type;
    }

    public String getFundamentalType() {
        return this.fundamentalType;
    }

    public HashMap<String,Object> serializeSearch() throws Exception {
        HashMap<String, Object> typeSearch = new HashMap<>();
        typeSearch.put("id", this.getPid());
        typeSearch.put("name", this.getName());
        typeSearch.put("type", this.getType());
        typeSearch.put("date", this.getDate());
        typeSearch.put("description", this.getDesc());
        typeSearch.put("origin", this.getOrigin());
        typeSearch.put("authors", this.getAuthors().toArray(new String[0]));
        typeSearch.put("aliases", this.getAliases().toArray(new String[0]));
        typeSearch.put("taxonomies", this.taxonomies.toArray(new String[0]));
        typeSearch.put("content", this.content);
        typeSearch.put("style", this.getStyle());
        typeSearch.put("unit", this.unit);
        typeSearch.put("fundamentalType", this.fundamentalType);
        return typeSearch;
    }
}
