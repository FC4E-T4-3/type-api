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

import com.fce4.dtrtoolkit.Entities.GeneralEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fce4.dtrtoolkit.Entities.TypeEntity;
import com.fce4.dtrtoolkit.TypeSearch;
import com.fce4.dtrtoolkit.Entities.UnitEntity;
import com.fce4.dtrtoolkit.Entities.TaxonomyEntity;
import com.fce4.dtrtoolkit.Taxonomies.TaxonomyGraph;

@Component
public class EoscExtractor implements BaseExtractor {

    Logger logger = Logger.getLogger(EoscExtractor.class.getName());
    ArrayList<HashMap<String, Object>> typeList = new ArrayList<>();
    ArrayList<HashMap<String, Object>> unitList = new ArrayList<>();
    ArrayList<HashMap<String, Object>> taxonomyList = new ArrayList<>();
    ArrayList<HashMap<String, Object>> generalList = new ArrayList<>();

    @Autowired
    private TypeSearch typeSearch;

    @Autowired
    private TaxonomyGraph taxonomyGraph;
    
    public void extractTypes(String url, List<Object> types, ArrayList<Object> units, ArrayList<Object> taxonomy, ArrayList<Object> general, String dtr) throws Exception{
        int typeCounter = 0;
        int unitCounter = 0;
        int taxonomyCounter = 0;
        int generalCounter = 0;

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
                GeneralEntity generalEntity = createGeneralEntity(jsonNode, dtr);
                generalList.add(generalEntity.serializeSearch());
                generalCounter +=1;
                typeCounter+=1;
            }
            if(units.contains(jsonNode.get("type").textValue())){
                UnitEntity unitEntity = createUnitEntity(jsonNode, dtr);
                unitList.add(unitEntity.serializeSearch());
                GeneralEntity generalEntity = createGeneralEntity(jsonNode, dtr);
                generalList.add(generalEntity.serializeSearch());
                generalCounter +=1;
                unitCounter+=1;
            }
            if(taxonomy.contains(jsonNode.get("type").textValue())){
                TaxonomyEntity taxonomyEntity = createTaxonomyEntity(jsonNode, dtr);
                taxonomyGraph.addNode(taxonomyEntity);
                GeneralEntity generalEntity = createGeneralEntity(jsonNode, dtr);
                generalList.add(generalEntity.serializeSearch());
                generalCounter +=1;
                taxonomyCounter+=1;
            }
            if(general.contains(jsonNode.get("type").textValue())){
                GeneralEntity generalEntity = createGeneralEntity(jsonNode, dtr);
                generalList.add(generalEntity.serializeSearch());
                generalCounter +=1;
            }
        }

        taxonomyGraph.generateRelations();
        for(TaxonomyEntity t : taxonomyGraph.getTaxonomy().values()){
            taxonomyList.add(t.serializeSearch());
        }    
        
        typeSearch.upsertList(typeList, "types");
        typeSearch.upsertList(unitList, "units");
        typeSearch.upsertList(taxonomyList, "taxonomy");
        typeSearch.upsertList(generalList, "general");

        logger.info(String.format("Added %s types from DTR '%s'.", typeCounter, dtr));
        logger.info(String.format("Added %s units from DTR '%s'.", unitCounter, dtr));
        logger.info(String.format("Added %s taxonomy nodes from DTR '%s'.", taxonomyCounter, dtr));
        logger.info(String.format("Added %s general entities from DTR '%s'.", generalCounter, dtr));
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

    public GeneralEntity createGeneralEntity(JsonNode node, String dtr) {
        String pid = node.get("id").textValue();
        String type = node.get("type").textValue();
        String origin = dtr;
        long timestamp = 0;
        ArrayList<String> authors = new ArrayList<String>();
        ArrayList<String> aliases = new ArrayList<String>();
        ArrayList<String> taxonomies = new ArrayList<String>();

        String desc = "";

        JsonNode content = node.get("content");
        String name = content.get("name").textValue();

        if(content.has("provenance")){
            JsonNode provenance = content.get("provenance");
            if(provenance.has("contributors")){
                for(JsonNode i : provenance.get("contributors")){
                    authors.add(i.get("Name").textValue());
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

        if(content.has("Aliases")){
            for(JsonNode i : content.get("Aliases")){
                aliases.add(i.textValue());
            }
        }
        if(content.has("Taxonomies")){
            for(JsonNode i : content.get("Taxonomies")){
                taxonomies.add(i.textValue());
            }
        }

        return new GeneralEntity(pid, type, origin, name, timestamp, desc, authors, aliases, taxonomies, node);
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
        String derivation = "";

        JsonNode content = node.get("content");
        String name = content.get("name").textValue();

        if(content.has("provenance")){
            JsonNode provenance = content.get("provenance");
            if(provenance.has("contributors")){
                for(JsonNode i : provenance.get("contributors")){
                    authors.add(i.get("Name").textValue());
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
            if(unitDesc.has("Derivation")){
                desc = unitDesc.get("Derivation").textValue();
            }
            if(unitDesc.has("Definition")){
                derivation = unitDesc.get("Definition").textValue();
            }
        }
        
        return new UnitEntity(pid, type, origin, name, timestamp, desc, authors, derivation, unitSymbol, quantity, quantitySymbol);
    }

    public TaxonomyEntity createTaxonomyEntity(JsonNode node, String dtr){
        String pid = node.get("id").textValue();
        String type = node.get("type").textValue();
        String origin = dtr;
        long timestamp = 0;
        ArrayList<String> authors = new ArrayList<String>();
        String desc = ""; 
        String reference = "";
        ArrayList<String> parents = new ArrayList<String>();

        JsonNode content = node.get("content");
        String name = content.get("name").textValue();
        if(content.has("description")){
           desc = content.get("description").textValue();
        }
        if(content.has("provenance")){
            JsonNode provenance = content.get("provenance");
            if(provenance.has("contributors")){
                for(JsonNode i : provenance.get("contributors")){
                    authors.add(i.get("Name").textValue());
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
        if(content.has("reference")){
            reference = content.get("reference").textValue();
        }
        if(content.has("parents")){
            for(JsonNode i : content.get("parents")){
                parents.add(i.textValue());
            }
        }
        
        return new TaxonomyEntity(pid, type, origin, name, timestamp, desc, reference, authors, parents);
    }

    public void extractTypeFields(TypeEntity type){
        ArrayList<String> authors = new ArrayList<String>();
        ArrayList<String> taxonomies = new ArrayList<String>();
        ArrayList<String> aliases = new ArrayList<String>();

        String fundamentalType = "undefined";

        JsonNode content = type.getContent();
        if(content.has("Schema")){
            fundamentalType = content.get("Schema").get("Type").textValue();
        }
        type.setFundamentalType(fundamentalType);

        if(content.has("description")){
           type.setDesc(content.get("description").textValue());
        }
        if(content.has("provenance")){
            JsonNode provenance = content.get("provenance");
            if(provenance.has("contributors")){
                for(JsonNode i : provenance.get("contributors")){
                    authors.add(i.get("Name").textValue());
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
        if(content.has("Aliases")){
            for(JsonNode i : content.get("Aliases")){
                aliases.add(i.textValue());
            }
        }
        if(content.has("Taxonomies")){
            for(JsonNode i : content.get("Taxonomies")){
                taxonomies.add(i.textValue());
            }
        }
        type.setTaxonomies(taxonomies);
        if(content.has("MeasuredUnits")){
            type.setUnit(content.get("MeasuredUnits").get(0).textValue());
        }
        type.setAuthors(authors);
        type.setAliases(aliases);
        return;
    }
}