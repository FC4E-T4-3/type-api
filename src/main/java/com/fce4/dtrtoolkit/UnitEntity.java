package com.fce4.dtrtoolkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UnitEntity {
    private String pid;
    private String type;
    private String origin;
    private String name = "";
    private long date = 0;
    private String desc = "";
    private ArrayList<String> authors = new ArrayList<String>();
    private String unitSymbol = "";
    private String quantity = "";
    private String quantitySymbol = "";
    private String derivation = "";

    public UnitEntity(String pid, String type, String origin, String name, long date, 
                        String desc, ArrayList<String> authors, String derivation,
                        String unitSymbol, String quantity, String quantitySymbol){
        this.pid = pid;
        this.type = type;
        this.origin = origin;
        this.name = name;
        this.date = date;
        this.desc = desc;
        this.authors = authors;
        this.derivation = derivation;
        this.unitSymbol = unitSymbol;
        this.quantity = quantity;
        this.quantitySymbol = quantitySymbol;
    }

    public UnitEntity(Map<String, Object> unit){
        this.pid = unit.get("id").toString();
        this.type = unit.get("type").toString();
        this.origin = unit.get("origin").toString();
        this.name = unit.get("name").toString();
        this.date = Long.parseLong(unit.get("date").toString());
        this.desc = unit.get("description").toString();
        this.derivation = unit.get("derivation").toString();
        this.authors = (ArrayList<String>) unit.get("authors");
        this.unitSymbol = unit.get("unitSymbol").toString();
        this.quantity = unit.get("quantity").toString();
        this.quantitySymbol = unit.get("quantitySymbol").toString();
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

    public String getDerivation() {
        return this.desc;
    }

    public void setDerivation(String deriv) {
        this.derivation = deriv;
    }


    public ArrayList<String> getAuthors() {
        return this.authors;
    }

    public void setAuthors(ArrayList<String> authors) {
        this.authors = authors;
    }

    public String getUnitSymbol() {
        return this.unitSymbol;
    }

    public void setUnitSymbol(String unitSymbol) {
        this.unitSymbol = unitSymbol;
    }

    public String getQuantity() {
        return this.quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getQuantitySymbol() {
        return this.quantitySymbol;
    }

    public void setQuantitySymbol(String quantitySymbol) {
        this.quantitySymbol = quantitySymbol;
    }
     
    /**
     * Serializes a UnitObject to a JsonNode object. The setters and getters are required for the mapper.
     * @return a JsonNode representing the UNitObject.
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
        typeSearch.put("quantity", this.quantity);
        typeSearch.put("derivation", this.derivation);
        
        if(!"unitSymbol".equals("")){
            typeSearch.put("unitSymbol", this.unitSymbol);
        }
        if(!"quantitySymbol".equals("")){
            typeSearch.put("quantitySymbol", this.quantitySymbol);
        }
        return typeSearch;
    }
}
