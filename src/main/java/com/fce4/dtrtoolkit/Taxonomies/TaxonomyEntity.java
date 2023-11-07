package com.fce4.dtrtoolkit.Taxonomies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TaxonomyEntity {
    private String pid;
    private String type;
    private String origin;
    private String name = "";
    private long date = 0;
    private String desc = "";
    private ArrayList<String> authors = new ArrayList<String>();
    private ArrayList<String> parentsString = new ArrayList<String>();
    private Set<TaxonomyEntity> parents = new HashSet<TaxonomyEntity>();
    private Set<TaxonomyEntity> children = new HashSet<TaxonomyEntity>();

    public TaxonomyEntity(String pid, String type, String origin, String name, long date, String desc, ArrayList<String> authors, ArrayList<String> parentsString, Set<TaxonomyEntity> parents, Set<TaxonomyEntity> children) {
        this.pid = pid;
        this.type = type;
        this.origin = origin;
        this.name = name;
        this.date = date;
        this.desc = desc;
        this.authors = authors;
        this.parentsString = parentsString;
        this.parents = parents;
        this.children = children;
    }

    public TaxonomyEntity(String pid, String type, String origin, String name, long date, String desc, ArrayList<String> authors, ArrayList<String> parentsString) {
        this.pid = pid;
        this.type = type;
        this.origin = origin;
        this.name = name;
        this.date = date;
        this.desc = desc;
        this.authors = authors;
        this.parentsString = parentsString;
    }

    public TaxonomyEntity(Map<String, Object> taxonomyEntity){
        this.pid = taxonomyEntity.get("id").toString();
        this.type = taxonomyEntity.get("type").toString();
        this.origin = taxonomyEntity.get("origin").toString();
        this.name = taxonomyEntity.get("name").toString();
        this.date = Long.parseLong(taxonomyEntity.get("date").toString());
        this.desc = taxonomyEntity.get("description").toString();
        this.authors = (ArrayList<String>) taxonomyEntity.get("authors");
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

    public  ArrayList<String> getParentsString() {
        return this.parentsString;
    }

    public void setParentsString(ArrayList<String> parentsString) {
        this.parentsString = parentsString;
    }

    public Set<TaxonomyEntity> getParents() {
        return this.parents;
    }

    public void setParents(Set<TaxonomyEntity> parents) {
        this.parents = parents;
    }

    public Set<TaxonomyEntity> getChildren() {
        return this.children;
    }

    public void setChildren(Set<TaxonomyEntity> children) {
        this.children = children;
    }

    public void addParent(TaxonomyEntity t){
        this.parents.add(t);
    }

    public void addChild(TaxonomyEntity t){
        this.children.add(t);
    }

    @Override
    public String toString() {
        ArrayList<String> parents = new ArrayList<String>();
        ArrayList<String> children = new ArrayList<String>();

        for(TaxonomyEntity t : getParents()){
            parents.add(t.getName());
        }
        for(TaxonomyEntity t : getChildren()){
            children.add(t.getName());
        }
        return "{" +
            " id='" + getPid() + "'" +
            " name='" + getName() + "'" +
            " parents='" + parents + "'" +
            " children='" + children + "'" +
            "}";
    }

    /**
     * Serializes a UnitObject to a JsonNode object. The setters and getters are required for the mapper.
     * @return a JsonNode representing the TaxonomyObject.
     */
    public JsonNode serialize() {
        ObjectMapper mapper = new ObjectMapper(); 
        JsonNode node = mapper.convertValue(this, JsonNode.class);
        return node;
    }

    public HashMap<String,Object> serializeSearch() throws Exception {
        HashMap<String,String> parents = new HashMap<String,String>();
        HashMap<String,String> children = new HashMap<String,String>();

        for(TaxonomyEntity t : getParents()){
            parents.put(t.getPid(), t.getName());
        }
        for(TaxonomyEntity t : getChildren()){
            children.put(t.getPid(), t.getName());
        }

        HashMap<String, Object> typeSearch = new HashMap<>();
        typeSearch.put("id", this.pid);
        typeSearch.put("name", this.name);
        typeSearch.put("type", this.type);
        typeSearch.put("date", this.date);
        typeSearch.put("description", this.desc);
        typeSearch.put("origin", this.origin);
        typeSearch.put("authors", this.authors.toArray(new String[0]));
        typeSearch.put("parents", parents);
        typeSearch.put("children", children);

        return typeSearch;
    }
}