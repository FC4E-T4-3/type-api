package com.fce4.dtrtoolkit.Entities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TaxonomyEntity extends GeneralEntity{
    private String reference = "";
    private ArrayList<String> parentsString = new ArrayList<String>();
    private Set<TaxonomyEntity> parents = new HashSet<TaxonomyEntity>();
    private Set<TaxonomyEntity> children = new HashSet<TaxonomyEntity>();

    public TaxonomyEntity(String pid, String type, String origin, String name, long date, String desc, String reference, ArrayList<String> authors, ArrayList<String> parentsString, Set<TaxonomyEntity> parents, Set<TaxonomyEntity> children) {
        this.setPid(pid);
        this.setType(type);
        this.setOrigin(origin);
        this.setName(name);
        this.reference = reference;
        this.setAuthors(authors);
        this.parentsString = parentsString;
        this.parents = parents;
        this.children = children;
    }

    public TaxonomyEntity(String pid, String type, String origin, String name, long date, String desc, String reference, ArrayList<String> authors, ArrayList<String> parentsString) {
        this.setPid(pid);
        this.setType(type);
        this.setOrigin(origin);
        this.setName(name);
        this.setDate(date);
        this.setDesc(desc);
        this.setAuthors(authors);
        this.reference = reference;
        this.parentsString = parentsString;
    }

    public TaxonomyEntity(Map<String, Object> taxonomyEntity){
        this.setPid(taxonomyEntity.get("id").toString());
        this.setType(taxonomyEntity.get("type").toString());
        this.setOrigin(taxonomyEntity.get("origin").toString());
        this.setName(taxonomyEntity.get("name").toString());
        this.setDate(Long.parseLong(taxonomyEntity.get("date").toString()));
        this.setDesc(taxonomyEntity.get("description").toString());
        this.setAuthors((ArrayList<String>) taxonomyEntity.get("authors"));
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
        typeSearch.put("id", this.getPid());
        typeSearch.put("name", this.getName());
        typeSearch.put("type", this.getType());
        typeSearch.put("date", this.getDate());
        typeSearch.put("reference", this.reference);
        typeSearch.put("description", this.getDesc());
        typeSearch.put("origin", this.getOrigin());
        typeSearch.put("authors", this.getAuthors().toArray(new String[0]));
        typeSearch.put("parents", parents);
        typeSearch.put("children", children);

        return typeSearch;
    }
}