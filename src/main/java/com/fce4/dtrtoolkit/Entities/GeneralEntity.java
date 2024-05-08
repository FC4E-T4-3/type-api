package com.fce4.dtrtoolkit.Entities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GeneralEntity {
    private String pid;
    private String type;
    private String origin;
    private String name = "";
    private long date = 0;
    private String desc = "";
    private ArrayList<String> authors = new ArrayList<String>();
    private ArrayList<String> aliases = new ArrayList<String>();

    public GeneralEntity(){

    }

    public GeneralEntity(String pid, String type, String origin, String name, long date, String desc, ArrayList<String> authors, ArrayList<String> aliases) {
        this.pid = pid;
        this.type = type;
        this.origin = origin;
        this.name = name;
        this.date = date;
        this.desc = desc;
        this.authors = authors;
        this.aliases = aliases;
    }

    public GeneralEntity(Map<String, Object> node) {
        ObjectMapper mapper = new ObjectMapper();
        this.pid = node.get("id").toString();
        this.type = node.get("type").toString();
        this.authors = (ArrayList<String>) node.get("authors");
        this.aliases = (ArrayList<String>) node.get("aliases");
        this.date = Long.parseLong(node.get("date").toString());
        this.desc = node.get("description").toString();
        this.name = node.get("name").toString();
        this.origin = node.get("origin").toString();
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public ArrayList<String> getAuthors() {
        return authors;
    }

    public void setAuthors(ArrayList<String> authors) {
        this.authors = authors;
    }

    public ArrayList<String> getAliases() {
        return aliases;
    }

    public void setAliases(ArrayList<String> aliases) {
        this.aliases = aliases;
    }

    /**
     * Serializes an EntityObject to a JsonNode object. The setters and getters are required for the mapper.
     * @return a JsonNode representing the EntityObject.
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
        typeSearch.put("aliases", this.aliases.toArray(new String[0]));
        return typeSearch;
    }
}
