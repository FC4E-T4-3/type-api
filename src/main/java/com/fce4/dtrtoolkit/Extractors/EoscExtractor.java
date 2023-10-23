package com.fce4.dtrtoolkit.Extractors;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fce4.dtrtoolkit.TypeEntity;
import com.fce4.dtrtoolkit.TypeSearch;
import com.fce4.dtrtoolkit.UnitEntity;

@Component
public class EoscExtractor implements BaseExtractor {

    Logger logger = Logger.getLogger(EoscExtractor.class.getName());
    ArrayList<HashMap<String, Object>> typeList = new ArrayList<>();
    ArrayList<HashMap<String, Object>> unitList = new ArrayList<>();


    @Autowired
    private TypeSearch typeSearch;
    
    public void extractTypes(String url, List<Object> types, ArrayList<Object> units, String dtr) throws Exception{
        int typeCounter = 0;
        int unitCounter = 0;

        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .timeout(Duration.ofSeconds(20))
            .uri(URI.create(url))
            .build();
            HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(response.body());

        for (JsonNode jsonNode : actualObj.get("results")) {
            if(!jsonNode.has("type")){
                continue;
            }
            if(types.contains(jsonNode.get("type").textValue())){
                TypeEntity typeEntity = createTypeEntity(jsonNode, dtr);
                extractTypeFields(typeEntity);
                typeList.add(typeEntity.serializeSearch());
                typeCounter+=1;
            }
            if(units.contains(jsonNode.get("type").textValue())){
                UnitEntity unitEntity = createUnitEntity(jsonNode, dtr);
                unitList.add(unitEntity.serializeSearch());
                unitCounter+=1;
            }
        }
        typeSearch.upsertList(typeList, "types");
        typeSearch.upsertList(unitList, "units");

        logger.info(String.format("Added %s types from DTR '%s'.", typeCounter, dtr));
        logger.info(String.format("Added %s units from DTR '%s'.", unitCounter, dtr));
    }

    public TypeEntity createTypeEntity(JsonNode node, String dtr) {
        String pid = node.get("id").textValue();
        String prefix = pid.split("/")[0];
        String type = node.get("type").textValue();
        JsonNode content = node.get("content");
        String style = "eosc";
        String name = node.get("content").get("name").textValue();
        String origin = dtr;
        return new TypeEntity(pid, prefix, type, content, style, origin, name);
    }

    public UnitEntity createUnitEntity(JsonNode node, String dtr) {
        String pid = node.get("id").textValue();
        String type = node.get("type").textValue();
        String origin = dtr;

        long timestamp = 0;
        ArrayList<String> authors = new ArrayList<String>();
        String desc = ""; 
        String unitSymbol = "";
        String quantity = "";
        String quantitySymbol = "";

        JsonNode content = node.get("content");
        String name = content.get("name").textValue();

        if(content.has("provenance")){
            JsonNode provenance = content.get("provenance");
            if(provenance.has("contributors")){
                for(JsonNode i : provenance.get("contributors")){
                    authors.add(i.get("name").textValue());
                }
            }
            if(provenance.has("creationDate")){
                String dateString = provenance.get("creationDate").textValue().substring(0, 10);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                try{
                    Date date = format.parse(dateString);
                    timestamp = date.getTime() / 1000L;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        if(content.has("unitDescription")){
            JsonNode unitDesc = content.get("unitDescription");
            if(unitDesc.has("Symbol")){
                unitSymbol = unitDesc.get("Symbol").textValue();
            }
            if(unitDesc.has("Quantity")){
                quantity = unitDesc.get("Quantity").textValue();
            }
            if(unitDesc.has("Dimension Symbol")){
                quantitySymbol = unitDesc.get("Dimension Symbol").textValue();
            }

            if(unitDesc.has("Definition")){
                desc = unitDesc.get("Definition").textValue();
            }
        }
        

        return new UnitEntity(pid, type, origin, name, timestamp, desc, authors, unitSymbol, quantity, quantitySymbol);
    }

    public void extractTypeFields(TypeEntity type){
        ArrayList<String> authors = new ArrayList<String>();

        if(type.getContent().has("description")){
           type.setDesc(type.getContent().get("description").textValue());
        }
        if(type.getContent().has("provenance")){
            JsonNode provenance = type.getContent().get("provenance");
            if(provenance.has("contributors")){
                for(JsonNode i : provenance.get("contributors")){
                    authors.add(i.get("name").textValue());
                }
            }
            if(provenance.has("creationDate")){
                String dateString = provenance.get("creationDate").textValue().substring(0, 10);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                try{
                    Date date = format.parse(dateString);
                    long timestamp = date.getTime() / 1000L;
                    type.setDate(timestamp);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        if(type.getContent().has("MeasuredUnits")){
            type.setUnit(type.getContent().get("MeasuredUnits").get(0).textValue());
        }
        type.setAuthors(authors);
        return;
    }
}