package com.fce4.dtrtoolkit.Entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UnitEntity extends GeneralEntity{
    private String unitSymbol = "";
    private String quantity = "";
    private String quantitySymbol = "";
    private String derivation = "";

    public UnitEntity(String pid, String type, String origin, String name, long date, 
                        String desc, ArrayList<String> authors, String derivation,
                        String unitSymbol, String quantity, String quantitySymbol){
        this.setPid(pid);
        this.setType(type);
        this.setOrigin(origin);
        this.setName(name);
        this.setDate(date);
        this.setDesc(desc);
        this.setAuthors(authors);
        this.derivation = derivation;
        this.unitSymbol = unitSymbol;
        this.quantity = quantity;
        this.quantitySymbol = quantitySymbol;
    }

    public UnitEntity(Map<String, Object> unit){
        this.setPid(unit.get("id").toString());
        this.setType(unit.get("type").toString());
        this.setOrigin(unit.get("origin").toString());
        this.setName(unit.get("name").toString());
        this.setDate(Long.parseLong(unit.get("date").toString()));
        this.setDesc(unit.get("description").toString());
        this.derivation = unit.get("derivation").toString();
        this.setAuthors((ArrayList<String>) unit.get("authors"));
        this.unitSymbol = unit.get("unitSymbol").toString();
        this.quantity = unit.get("quantity").toString();
        this.quantitySymbol = unit.get("quantitySymbol").toString();
    }

    public String getDerivation() {
        return this.derivation;
    }

    public void setDerivation(String deriv) {
        this.derivation = deriv;
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

    public HashMap<String,Object> serializeSearch() throws Exception {
        HashMap<String, Object> typeSearch = new HashMap<>();
        typeSearch.put("id", this.getPid());
        typeSearch.put("name", this.getName());
        typeSearch.put("type", this.getType());
        typeSearch.put("date", this.getDate());
        typeSearch.put("description", this.getDesc());
        typeSearch.put("origin", this.getOrigin());
        typeSearch.put("authors", this.getAuthors().toArray(new String[0]));
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
