package com.fce4.dtrtoolkit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fce4.dtrtoolkit.Extractors.EoscExtractor;
import com.fce4.dtrtoolkit.Extractors.LegacyExtractor;
import com.fce4.dtrtoolkit.Taxonomies.TaxonomyEntity;
import com.fce4.dtrtoolkit.Taxonomies.TaxonomyGraph;
import com.fce4.dtrtoolkit.Validators.EoscValidator;
import com.fce4.dtrtoolkit.Validators.LegacyValidator;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Service
public class TypeService {

    ArrayList<HashMap<String, Object>> typeList = new ArrayList<>();
    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private LegacyValidator legacyValidator;

    @Autowired
    private EoscValidator eoscValidator;

    @Autowired
    private TypeSearch typeSearch;

    @Autowired
    private LegacyExtractor legacyExtractor;
    @Autowired
    private EoscExtractor eoscExtractor;

    @Autowired
    private TaxonomyGraph taxonomyGraph;

    private String config="src/main/config/config.toml";
    
    Logger logger = Logger.getLogger(TypeService.class.getName());

    @PostConstruct
    public void init() throws IOException, InterruptedException, Exception{
        logger.info(new File(".").getAbsolutePath());
        //refreshRepository();
    }

    /**
     * Refreshes the full contents of the cache, harvesting the env_file.
     * @throws InterruptedException
     * @throws IOException
     */
    @Scheduled(fixedRate = 48, timeUnit = TimeUnit.HOURS)
    public void refreshRepository() throws IOException, InterruptedException, Exception{
        logger.info("Refreshing Cache");
        typeList.clear();
        //Setting a new timestamp, should a new logfile be necessary
        Date currentDate = new Date(System.currentTimeMillis());
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM");
		System.setProperty("timestamp", df.format(currentDate));
        taxonomyGraph.clear();
        TomlParseResult result = Toml.parse(Paths.get(config));
      
        for(var i : result.entrySet()){
			try {
				TomlTable t = TomlTable.class.cast(i.getValue());
                String dtr = i.getKey();
                ArrayList<Object> units = new ArrayList<Object>();
                ArrayList<Object> taxonomy = new ArrayList<Object>();
				String url = t.getString("url");
				String suffix = t.getString("suffix");
				List<Object> types = t.getArray("types").toList();
				String style = t.getString("style");
                if(t.contains("units")){
                    units = new ArrayList<Object>(t.getArray("units").toList());
                }

                if(t.contains("taxonomy")){
                    taxonomy = new ArrayList<Object>(t.getArray("taxonomy").toList());
                }

                logger.info(String.format("extracting %s", url));

                switch(style){
                    case "legacy":
                        legacyExtractor.extractTypes(url+suffix, types, dtr);
                        break;
                    case "eosc":
                        eoscExtractor.extractTypes(url+suffix, types, units, taxonomy, dtr);
                        break;
                    default:
                        logger.warning(String.format("DTR with style '%s' can not be imported. Please use one of the offered options.", style));
                        break;
                }
			} catch (Exception e) {
            	logger.warning(e.toString());
            }
        }
        logger.info("Refreshing Cache successful.");
    }

    /**
     * Adds a single data type to the repository. Either for selective refreshing, or to add valid types not in the configured DTR's.
     * Only works for handle's that forward to a type in a cordra instance.
     * @param pid the PID to add/refresh in the cache.
     * @throws InterruptedException
     * @throws IOException
     */
    public void addType(String pid, String collection) throws Exception{
        logger.info(String.format("Adding Type %s to the cache", pid));

        String uri = "http://hdl.handle.net/" + pid + "?locatt=view:json";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .timeout(Duration.ofSeconds(60))
            .uri(URI.create(uri))
            .build();
            HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());

        /*After the first request, we receive the URL to the type in its DTR. Since we need the full specification, the parameter "?full"
        needs to be set to true to get all the information necessary. Thus, the second request.*/
        
        if(!response.headers().map().containsKey("location")){
            logger.warning(String.format("Requested Handle %s does not exist", pid));
            throw new IOException(String.format("Requested Handle %s does not exist.", pid));
        }
        String dtrUrl = response.headers().map().get("location").get(0);
        request = HttpRequest.newBuilder()
            .GET()
            .timeout(Duration.ofSeconds(60))
            .uri(URI.create(dtrUrl + "?full=true"))
            .build();
        response = client.send(request,HttpResponse.BodyHandlers.ofString());

        JsonNode root = mapper.readTree(response.body());
        if(dtrUrl.contains("dtr-test.pidconsortium") || dtrUrl.contains("dtr-pit.pidconsortium")){
            TypeEntity typeEntity = legacyExtractor.createEntity(root, dtrUrl);
            legacyExtractor.extractFields(typeEntity);
            typeSearch.upsertEntry(typeEntity.serializeSearch(), collection);
        }
        else if(dtrUrl.contains("typeregistry.lab.pidconsortium")){
            if(root.get("type").textValue().equals("MeasurementUnit")){
                UnitEntity unitEntity = eoscExtractor.createUnitEntity(root, dtrUrl);
                typeSearch.upsertEntry(unitEntity.serializeSearch(), collection);
            }
            else if(root.get("type").textValue().equals("TaxonomyNode")){
                TaxonomyEntity taxonomyEntity = eoscExtractor.createTaxonomyEntity(root, dtrUrl);
                taxonomyGraph.addNode(taxonomyEntity);
                taxonomyGraph.generateRelations();
                typeSearch.upsertEntry(taxonomyEntity.serializeSearch(), collection);
            }
            else{
                TypeEntity typeEntity = eoscExtractor.createTypeEntity(root, dtrUrl);
                eoscExtractor.extractTypeFields(typeEntity);
                typeSearch.upsertEntry(typeEntity.serializeSearch(), collection);
            }
        }
        else{
            logger.warning("PID does not describe a type or is not supported by this application.");
        }
        logger.info(String.format("Adding Type %s to the cache was successful", pid));
    }

    /**
     * Retrieve the description of a type from the repo.
     * @param pid the PID to add/refresh in the cache.
     * @param refresh flag, if type should be refreshed
     * @throws InterruptedException
     * @throws IOException
     */
    public JsonNode getDescription(String pid, Boolean refresh) throws Exception{
        checkAdd(pid, refresh, "types");
        Map<String, Object> type = typeSearch.get(pid, "types");
        TypeEntity typeEntity = new TypeEntity(type);
        return typeEntity.serialize();
    }

    public JsonNode getUnit(String pid, Boolean refresh) throws Exception {
        checkAdd(pid, refresh, "units");
        Map<String, Object> unit = typeSearch.get(pid, "units");
        UnitEntity unitEntity = new UnitEntity(unit);
        return unitEntity.serialize();
    }

    public JsonNode getTaxonomyNode(String pid, Boolean refresh) throws Exception {
        checkAdd(pid, refresh, "taxonomy");
        TaxonomyEntity taxonomyEntity = taxonomyGraph.get(pid);
        return mapper.valueToTree(taxonomyEntity.serializeSearch());
    }

    public JsonNode getTaxonomySubtree(String pid) throws Exception{
        checkAdd(pid, false, "taxonomies");
        return mapper.valueToTree(taxonomyGraph.getSubtree(pid));
    }

    public ArrayList<Object> getTypesTaxonomy(String pid, Boolean getSubtree) throws Exception{        
        Map<String, String> filterBy = new HashMap<String, String>();
        if(getSubtree){
            Set<String> subtree = taxonomyGraph.getSubtreePIDs(pid);
            filterBy.put("taxonomies", subtree.toString());
        }
        else{
            filterBy.put("taxonomies", pid);
        }
        return search("*", new String[]{"name"}, filterBy, "types", true);
    }

    /**
     * Construct and return the validation schema of a type from the repo.
     * @param pid the PID to add/refresh in the cache.
     * @param refresh flag, if type should be refreshed
     */
    public ObjectNode getValidation(String pid, Boolean refresh) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        checkAdd(pid, refresh, "types");
        TypeEntity typeEntity = new TypeEntity(typeSearch.get(pid, "types"));
        String style = typeEntity.getStyle();
        switch(style){
            case "legacy":
                root = legacyValidator.validation(pid);
                break;
            case "eosc":
                root = eoscValidator.validation(pid);
                break;
        }
        return root;
    }

    /**
     * Helper function avoiding repeated code. Adds a PID to the repo if conditions demand it.
     * @param pid the PID to add/refresh in the cache.
     * @param refresh flag, if type should be refreshed
     */
    public void checkAdd(String pid, Boolean refresh, String collection) throws Exception {
        if(!typeSearch.has(pid, collection) || refresh){
            logger.info(String.format("Retrieving pid %s via handle and caching...", pid));
            addType(pid, collection);
        }
    }

    /**
     * Search for types in the repository with a query.
     * @param identifier the PID to add/refresh in the cache.
     */
    public ArrayList<Object> search(String query, String[] queryBy, Map<String,String> filterBy, String collection, Boolean infix) throws Exception{
        return typeSearch.search(query, queryBy, filterBy, collection, infix);
    }

    /**
     * Validates a JSON object against a type schema.
     * @param pid The PID of the type against which the object should be validated
     * @param object The JSON object that is to be validated
     * @throws Exception
     */
    public String validate(String pid, Object object) throws Exception{
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchema schema = factory.getSchema(getValidation(pid,false).toString());
        JsonNode node = mapper.valueToTree(object);
        Set<ValidationMessage> errors = schema.validate(node);
        if(errors.size()>0){
            return errors.toString();
        }
        return "Valid";
    }
}