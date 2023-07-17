package com.fce4.dtrtoolkit;

import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class TypeEntity {
    
    private String pid;
    private String prefix;
    private String type;
    private String style;
    private String origin;
    private String name;
    private long date;
    private String desc;
    private ArrayList<String> authors;
    private ArrayList<String> aliases;
    private JsonNode content;

    public TypeEntity(String pid, String prefix, String type, JsonNode content, String style, String origin, String name)
    {
        this.pid = pid;
        this.prefix = prefix;
        this.type = type;
        this.content = content;
        this.style = style;
        this.origin = origin;
        this.name = name;
        extractFields();
    }

    public TypeEntity(JsonNode node, String style, String origin)
    {
        this.pid = node.get("id").textValue();
        this.prefix = pid.split("/")[0];
        this.type = node.get("type").textValue();
        this.content = node.get("content");
        this.style = style;
        this.name = node.get("content").get("name").textValue();
        this.origin = origin;
        extractFields();
    }

    public TypeEntity(JsonNode node, String origin)
    {
        this.pid = node.get("id").textValue();
        this.prefix = pid.split("/")[0];
        this.type = node.get("type").textValue();
        this.content = node.get("content");
        this.style = "unknown";
        this.name = node.get("content").get("name").textValue();
        this.origin = origin;
        extractFields();
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

    public String getName(){
        return this.name;
    }

    public String getOrigin(){
        return this.origin;
    }

    public void setStyle(String style){
        this.style = style;
    }

    private void extractFields(){
        //System.out.println(this.content);

        this.authors = new ArrayList<String>();
        this.aliases = new ArrayList<String>();

        if(getContent().has("description")){
            this.desc = getContent().get("description").textValue();
        }
        if(getContent().has("provenance")){
            JsonNode provenance = getContent().get("provenance");
            if(provenance.has("contributors")){
                for(JsonNode i : provenance.get("contributors")){
                    this.authors.add(i.get("name").textValue());
                }
            }
            if(provenance.has("creationDate")){
                String dateString = provenance.get("creationDate").textValue().substring(0, 10);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                try{
                    Date date = format.parse(dateString);
                    long timestamp = date.getTime() / 1000L;
                    this.date = timestamp;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
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

    public HashMap<String,Object> serializeSearch(){
        HashMap<String, Object> typeSearch = new HashMap<>();
        typeSearch.put("id", this.pid);
        typeSearch.put("name", this.name);
        typeSearch.put("type", this.type);
        typeSearch.put("date", this.date);
        typeSearch.put("desc", this.desc);
        typeSearch.put("origin", this.origin);
        typeSearch.put("authors", this.authors.toArray(new String[0]));
        return typeSearch;
    }
}
