package com.fce4.dtrtoolkit;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.typesense.api.*;
import org.typesense.model.*;
import org.typesense.resources.*;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TypeSearch {
    
    Logger logger = Logger.getLogger(TypeService.class.getName());

    Client typeSenseClient;

    public TypeSearch() throws Exception {
        ArrayList<Node> nodes = new ArrayList<>();
        nodes.add(
        new Node(
            "http",
            "141.5.103.83",
            "8108"
            )
        );   
        Configuration configuration = new Configuration(nodes, Duration.ofSeconds(2),"xyz");
        typeSenseClient = new Client(configuration);
        initTypesense();
    }

    public void initTypesense() throws Exception{
        
        if(typeSenseClient.collections().retrieve().length > 0) {
            typeSenseClient.collections("types").delete();
        }
        List<Field> fields = new ArrayList<>();
        fields.add(new Field().name("name").type(FieldTypes.STRING).infix(true));
        fields.add(new Field().name("date").type(FieldTypes.INT64).sort(true));
        fields.add(new Field().name("authors").type(FieldTypes.STRING_ARRAY).facet(true).infix(true));
        fields.add(new Field().name("type").type(FieldTypes.STRING).facet(true));
        fields.add(new Field().name("origin").type(FieldTypes.STRING).facet(true));
        fields.add(new Field().name("desc").type(FieldTypes.STRING).infix(true));
        fields.add(new Field().name("content").type(FieldTypes.STRING).infix(true));


        CollectionSchema collectionSchema = new CollectionSchema();
        collectionSchema.name("types").fields(fields).defaultSortingField("date");
        try{
            typeSenseClient.collections().create(collectionSchema);        
        }
        catch(Exception e){
            logger.info("Collection already exists");
        }
    }

    public void upsertList(ArrayList<HashMap<String, Object>> typeList) throws Exception {
        ImportDocumentsParameters importDocumentsParameters = new ImportDocumentsParameters();
        importDocumentsParameters.action("upsert");
        typeSenseClient.collections("types").documents().import_(typeList, importDocumentsParameters);
    }

    /**
     * Search for types in the repository with a query.
     * @param identifier the PID to add/refresh in the cache.
     */
    public ArrayList<Object> search(String query, String[] queryBy, Boolean infix) throws Exception{

        ArrayList<Object> resultList = new ArrayList<Object> ();
        SearchParameters searchParameters = new SearchParameters()
                                        .q(query)
                                        .queryBy(StringUtils.join(queryBy, ','))
                                        .infix("always")
                                        .perPage(250)
                                        .page(1);
        SearchResult searchResult = typeSenseClient.collections("types").documents().search(searchParameters);
        for(SearchResultHit hit : searchResult.getHits()){
            resultList.add(hit.getDocument().get("content"));
        }
        while(searchResult.getHits().size() > 0){
            searchParameters.setPage(searchParameters.getPage()+1);
            searchResult = typeSenseClient.collections("types").documents().search(searchParameters);
            for(SearchResultHit hit : searchResult.getHits()){
                resultList.add(hit.getDocument().get("content"));
            }
        }
        return resultList;
    }
}
