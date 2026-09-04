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

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TypeSearch {
    
    Logger logger = Logger.getLogger(TypeSearch.class.getName());

    Client typeSenseClient;
    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public TypeSearch(@Value("${typesense.url}") String url, @Value("${typesense.port}") String port,
                      @Value("${typesense.key}") String key) throws Exception {
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
        initTaxonomy();
        initGeneralTypes();
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
        fields.add(new Field().name("aliases").type(FieldTypes.STRING_ARRAY).infix(true));
        fields.add(new Field().name("taxonomies").type(FieldTypes.STRING_ARRAY).facet(true).infix(true));
        fields.add(new Field().name("type").type(FieldTypes.STRING).facet(true));
        fields.add(new Field().name("origin").type(FieldTypes.STRING).facet(true));
        fields.add(new Field().name("description").type(FieldTypes.STRING).infix(true));
        fields.add(new Field().name("unit").type(FieldTypes.STRING).facet(true).infix(true));
        fields.add(new Field().name("style").type(FieldTypes.STRING));
        fields.add(new Field().name("fundamentalType").type(FieldTypes.STRING));

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
        fields.add(new Field().name("quantity").type(FieldTypes.STRING).facet(true).infix(true));

        CollectionSchema collectionSchema = new CollectionSchema();
        collectionSchema.name("units").fields(fields).defaultSortingField("date");
        try{
            typeSenseClient.collections().create(collectionSchema);        
        }
        catch(Exception e){
            logger.info("Collection already exists");
        }
    }

    public void initTaxonomy() throws Exception {
        try{
            typeSenseClient.collections("taxonomy").retrieve();
            typeSenseClient.collections("taxonomy").delete();
        }
        catch(Exception e){
            logger.info("Collection taxonomy did not exist yet. Creating...");
        }

        List<Field> fields = new ArrayList<>();
        fields.add(new Field().name("name").type(FieldTypes.STRING).infix(true));
        fields.add(new Field().name("date").type(FieldTypes.INT64).sort(true));
        fields.add(new Field().name("authors").type(FieldTypes.STRING_ARRAY).facet(true).infix(true));
        fields.add(new Field().name("description").type(FieldTypes.STRING).infix(true));
        fields.add(new Field().name("origin").type(FieldTypes.STRING).facet(true));

        CollectionSchema collectionSchema = new CollectionSchema();
        collectionSchema.name("taxonomy").fields(fields).defaultSortingField("date");
        try{
            typeSenseClient.collections().create(collectionSchema);        
        }
        catch(Exception e){
            logger.info("Collection already exists");
        }
    }

    public void initGeneralTypes() throws Exception {
        try{
            typeSenseClient.collections("general").retrieve();
            typeSenseClient.collections("general").delete();
        }
        catch(Exception e){
            logger.info("Collection general did not exist yet. Creating...");
        }

        List<Field> fields = new ArrayList<>();
        fields.add(new Field().name("name").type(FieldTypes.STRING).infix(true));
        fields.add(new Field().name("date").type(FieldTypes.INT64).sort(true));
        fields.add(new Field().name("authors").type(FieldTypes.STRING_ARRAY).facet(true).infix(true));
        fields.add(new Field().name("description").type(FieldTypes.STRING).infix(true));
        fields.add(new Field().name("origin").type(FieldTypes.STRING).facet(true));
        fields.add(new Field().name("aliases").type(FieldTypes.STRING_ARRAY).infix(true));
        fields.add(new Field().name("taxonomies").type(FieldTypes.STRING_ARRAY).facet(true));

        CollectionSchema collectionSchema = new CollectionSchema();
        collectionSchema.name("general").fields(fields).defaultSortingField("date");
        try{
            typeSenseClient.collections().create(collectionSchema);        
        }
        catch(Exception e){
            logger.info("Collection already exists");
        }
    }

    public void upsertEntry(Map<String, Object> type, String collection) throws Exception{
        typeSenseClient.collections(collection).documents().upsert(type);
    }

    public void upsertList(ArrayList<HashMap<String, Object>> typeList, String collection) throws Exception {
        ImportDocumentsParameters importDocumentsParameters = new ImportDocumentsParameters();
        importDocumentsParameters.action("upsert");
        typeSenseClient.collections(collection).documents().import_(typeList, importDocumentsParameters);
    }

    /**
     * Search for types in the repository with a query.
     */
    public ArrayList<Object> search(String query, String[] queryBy, Map<String,String> filterBy, String collection, Boolean infix) throws Exception{
        ArrayList<Object> resultList = new ArrayList<Object> ();
        SearchParameters searchParameters = new SearchParameters()
                                        .q(query)
                                        .queryBy(StringUtils.join(queryBy, ','))
                                        .infix("always")
                                        .perPage(250)
                                        .page(1);
        if(!filterBy.isEmpty()){
            String filterString = "";
            int size = filterBy.size();
            int counter = 0;
            for(String i : filterBy.keySet()){
                filterString = filterString + i + ":[" + filterBy.get(i) + "]";                
                counter +=1;
                if(counter < size){
                    filterString = filterString + " && ";
                }
            }
            searchParameters.setFilterBy(filterString);
        }

        //Since TypeSense works via pages, we collect all results from all pages while setting the perPage value to the max value.
       
        SearchResult searchResult = typeSenseClient.collections(collection).documents().search(searchParameters);
        for(SearchResultHit hit : searchResult.getHits()){
            resultList.add(mapper.writeValueAsString(hit.getDocument()));
        }
        while(searchResult.getHits().size() > 0){
            searchParameters.setPage(searchParameters.getPage()+1);
            searchResult = typeSenseClient.collections(collection).documents().search(searchParameters);
            for(SearchResultHit hit : searchResult.getHits()){
                resultList.add(mapper.writeValueAsString(hit.getDocument()));
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

    public ArrayList<Object> getAllTypes(String collection) throws Exception{
        String[] q = {"name"};
        Map<String,String> fb = new HashMap<String, String>();
        return search("*", q, fb , collection, false);
    }
}
