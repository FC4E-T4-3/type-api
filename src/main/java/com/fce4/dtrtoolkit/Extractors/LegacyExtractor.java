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

@Component
public class LegacyExtractor implements BaseExtractor {

    Logger logger = Logger.getLogger(LegacyExtractor.class.getName());
    ArrayList<HashMap<String, Object>> typeList = new ArrayList<>();

    @Autowired
    private TypeSearch typeSearch;
    
    public void extractTypes(String url, List<Object> types, String dtr) throws Exception{
        int counter = 0;
        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .timeout(Duration.ofSeconds(10))
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
                TypeEntity typeEntity = createEntity(jsonNode, dtr);
                extractFields(typeEntity);
                typeList.add(typeEntity.serializeSearch());
                counter+=1;
            }
        }
        typeSearch.upsertList(typeList, "types");
        logger.info(String.format("Added %s types from DTR '%s'.", counter, dtr));
    }

    public TypeEntity createEntity(JsonNode node, String dtr) {
        String pid = node.get("id").textValue();
        String prefix = pid.split("/")[0];
        String type = node.get("type").textValue();
        JsonNode content = node.get("content");
        String style = "legacy";
        String name = node.get("content").get("name").textValue();
        String origin = dtr;
        return new TypeEntity(pid, prefix, type, content, style, origin, name);
    }

    public void extractFields(TypeEntity type){
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
        type.setAuthors(authors);
        return;
    }
}