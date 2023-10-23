package com.fce4.dtrtoolkit;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.typesense.api.*;
import org.typesense.model.*;
import org.typesense.resources.*;

@Component
public class TypeSearch {
    
    Logger logger = Logger.getLogger(TypeSearch.class.getName());

    Client typeSenseClient;

    @Autowired
    public TypeSearch(@Value("${typesense.url}") String url, @Value("${typesense.port}") String port, @Value("${typesense.key}") String key) throws Exception {
        ArrayList<Node> nodes = new ArrayList<>();
        nodes.add(
        new Node(
            "http",
            url,
            port
            )
        );   
        Configuration configuration = new Configuration(nodes, Duration.ofSeconds(20),key);
        typeSenseClient = new Client(configuration);
        initTypesense();
    }

    public void initTypesense() throws Exception{
        initTypes();
        initUnits();
    }

    public void initTypes() throws Exception{
        try{
            typeSenseClient.collections("types").retrieve();
            typeSenseClient.collections("types").delete();
        }
        catch(Exception e) {
            logger.info("Collection types did not exist yet. Creating...");
        }

        List<Field> fields = new ArrayList<>();
        fields.add(new Field().name("name").type(FieldTypes.STRING).infix(true));
        fields.add(new Field().name("date").type(FieldTypes.INT64).sort(true));
        fields.add(new Field().name("authors").type(FieldTypes.STRING_ARRAY).facet(true).infix(true));
        fields.add(new Field().name("type").type(FieldTypes.STRING).facet(true));
        fields.add(new Field().name("origin").type(FieldTypes.STRING).facet(true));
        fields.add(new Field().name("description").type(FieldTypes.STRING).infix(true));

        CollectionSchema collectionSchema = new CollectionSchema();
        collectionSchema.name("types").fields(fields).defaultSortingField("date");
        try{
            typeSenseClient.collections().create(collectionSchema);        
        }
        catch(Exception e){
            logger.info("Collection already exists");
        }
    }

    public void initUnits() throws Exception {
        try{
            typeSenseClient.collections("units").retrieve();
            typeSenseClient.collections("units").delete();
        }
        catch(Exception e) {
            logger.info("Collection units did not exist yet. Creating...");
        }

        List<Field> fields = new ArrayList<>();
        fields.add(new Field().name("name").type(FieldTypes.STRING).infix(true));
        fields.add(new Field().name("date").type(FieldTypes.INT64).sort(true));
        fields.add(new Field().name("authors").type(FieldTypes.STRING_ARRAY).facet(true).infix(true));
        fields.add(new Field().name("description").type(FieldTypes.STRING).infix(true));
        fields.add(new Field().name("type").type(FieldTypes.STRING).facet(true));
        fields.add(new Field().name("origin").type(FieldTypes.STRING).facet(true));
        // fields.add(new Field().name("unitSymbol").type(FieldTypes.STRING).infix(true));
        // fields.add(new Field().name("quantity").type(FieldTypes.STRING).infix(true));
        // fields.add(new Field().name("dimensionSymbol").type(FieldTypes.STRING).infix(true));


        CollectionSchema collectionSchema = new CollectionSchema();
        collectionSchema.name("units").fields(fields).defaultSortingField("date");
        try{
            typeSenseClient.collections().create(collectionSchema);        
        }
        catch(Exception e){
            logger.info("Collection already exists");
        }
    }

    public void upsertEntry(HashMap<String, Object> type, String collection) throws Exception{
        typeSenseClient.collections(collection).documents().upsert(type);
        return;
    }

    public void upsertList(ArrayList<HashMap<String, Object>> typeList, String collection) throws Exception {
        ImportDocumentsParameters importDocumentsParameters = new ImportDocumentsParameters();
        importDocumentsParameters.action("upsert");
        typeSenseClient.collections(collection).documents().import_(typeList, importDocumentsParameters);
    }

    /**
     * Search for types in the repository with a query.
     * @param identifier the PID to add/refresh in the cache.
     */
    public ArrayList<Object> searchSimple(String query, String[] queryBy, String collection, Boolean infix) throws Exception{

        ArrayList<Object> resultList = new ArrayList<Object> ();
        SearchParameters searchParameters = new SearchParameters()
                                        .q(query)
                                        .queryBy(StringUtils.join(queryBy, ','))
                                        .infix("always")
                                        .perPage(250)
                                        .page(1);
        
        //Since TypeSense works via pages, we collect all results from all pages while setting the perPage value to the max value.
        SearchResult searchResult = typeSenseClient.collections(collection).documents().search(searchParameters);
        for(SearchResultHit hit : searchResult.getHits()){
            resultList.add(hit.getDocument().get("content"));
        }
        while(searchResult.getHits().size() > 0){
            searchParameters.setPage(searchParameters.getPage()+1);
            searchResult = typeSenseClient.collections(collection).documents().search(searchParameters);
            for(SearchResultHit hit : searchResult.getHits()){
                resultList.add(hit.getDocument().get("content"));
            }
        }
        return resultList;
    }

    public Map<String, Object> get(String pid, String collection) throws Exception {
       return this.typeSenseClient.collections(collection).documents(URLEncoder.encode(pid, StandardCharsets.UTF_8.toString())).retrieve();
    }

    public boolean has(String pid, String collection) throws IOException{
        try{
            this.typeSenseClient.collections(collection).documents(URLEncoder.encode(pid, StandardCharsets.UTF_8.toString())).retrieve();
            return true;
        }
        catch(Exception e) {
            return false;
        }
    }
}
