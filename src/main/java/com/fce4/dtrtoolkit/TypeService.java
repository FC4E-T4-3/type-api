package com.fce4.dtrtoolkit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fce4.dtrtoolkit.Entities.GeneralEntity;
import com.fce4.dtrtoolkit.Entities.TypeEntity;
import com.fce4.dtrtoolkit.Entities.UnitEntity;
import com.fce4.dtrtoolkit.Extractors.EoscExtractor;
import com.fce4.dtrtoolkit.Extractors.LegacyExtractor;
import com.fce4.dtrtoolkit.Entities.TaxonomyEntity;
import com.fce4.dtrtoolkit.Taxonomies.TaxonomyGraph;
import com.fce4.dtrtoolkit.Validators.EoscValidator;
import com.fce4.dtrtoolkit.Validators.LegacyValidator;
import com.github.underscore.Json;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
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

    ArrayList<Object> basicTypes = new ArrayList<>();
    ArrayList<Object> compositeTypes = new ArrayList<>();

    static ObjectMapper mapper = new ObjectMapper();

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

    private String config = "src/main/config/config.toml";

    Logger logger = Logger.getLogger(TypeService.class.getName());


    @PostConstruct
    public void init() throws IOException, InterruptedException, Exception {
        logger.info(new File(".").getAbsolutePath());
        //refreshRepository();
    }

    /**
     * Refreshes the full contents of the cache, harvesting the env_file.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    @Scheduled(fixedRate = 48, timeUnit = TimeUnit.HOURS)
    public void refreshRepository() throws IOException, InterruptedException, Exception {
        try {
            logger.info("Refreshing Cache");
            logger.info("Refreshing Indexer");
            typeSearch.initTypesense();
            Date currentDate = new Date(System.currentTimeMillis());
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM");
            System.setProperty("timestamp", df.format(currentDate));
            taxonomyGraph.clear();
            TomlParseResult result = Toml.parse(Paths.get(config));

            for (var i : result.entrySet()) {
                try {
                    TomlTable t = TomlTable.class.cast(i.getValue());
                    String dtr = i.getKey();
                    ArrayList<Object> units = new ArrayList<Object>();
                    ArrayList<Object> types = new ArrayList<>();
                    ArrayList<Object> taxonomy = new ArrayList<Object>();
                    ArrayList<Object> general = new ArrayList<Object>();
                    String url = t.getString("url");
                    String suffix = t.getString("suffix");
                    String style = t.getString("style");

                    if (t.contains("basicTypes")) {
                        types.addAll(t.getArray("basicTypes").toList());
                        basicTypes.addAll(t.getArray("basicTypes").toList());
                    }
                    if (t.contains("compositeTypes")) {
                        types.addAll(t.getArray("compositeTypes").toList());
                        compositeTypes.addAll(t.getArray("compositeTypes").toList());
                    }
                    if (t.contains("units")) {
                        units.addAll(t.getArray("units").toList());
                    }
                    if (t.contains("taxonomy")) {
                        taxonomy.addAll(t.getArray("taxonomy").toList());
                    }
                    if (t.contains("general")) {
                        general.addAll(t.getArray("general").toList());
                    }

                    legacyValidator.setTypes(basicTypes, compositeTypes);
                    eoscValidator.setTypes(basicTypes, compositeTypes);

                    logger.info(String.format("extracting %s", url));

                    switch (style) {
                        case "legacy":
                            legacyExtractor.extractTypes(url + suffix, types, url);
                            break;
                        case "eosc":
                            eoscExtractor.extractTypes(url + suffix, types, units, taxonomy, general, url);
                            break;
                        default:
                            logger.warning(String.format("DTR with style '%s' can not be imported. Please use one of the offered options.", style));
                            break;
                    }
                } catch (Exception e) {
                    logger.warning(e.toString());
                }
            }

            cacheSchemas();

            logger.info("Refreshing Cache successful.");
        } catch (Exception e) {
            logger.severe("Unexpected error occurred in scheduled task: " + e.getMessage());
        }
    }

    public void cacheSchemas() throws Exception {
        ArrayList<Object> allTypes = typeSearch.getAllTypes("types");

        for (Object i : allTypes) {
            JsonNode obj = mapper.readTree(i.toString());
            try {
                String style = obj.get("style").textValue();
                if (style.equals("eosc")) {
                    String id = obj.get("id").toString().replace("\"", "");
                    cacheSchema(id);
                }
            } catch (Exception e) {
                logger.warning("Error caching schema: " + obj.get("id").toString().replace("\"", "") + e.getMessage());
                //logger.warning(i.toString());
            }
        }
    }

    public void cacheSchema(String pid) throws Exception{
        ObjectNode schema = getValidation(pid, false, false);
        Map<String, Object> type = typeSearch.get(pid, "types");
        type.put("schema", schema);
        typeSearch.upsertEntry(type, "types");
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
        String dtrUrl = "";
        HttpClient client = HttpClient.newHttpClient();;
        HttpRequest request;
        HttpResponse<String> response;

        //Check if the type is already in the cache. If yes, just fetch it from the DTR. Otherwise, the long way via Handle must be taken.
        if(typeSearch.has(pid, collection)){
            GeneralEntity type = new GeneralEntity(typeSearch.get(pid, "general"));
            dtrUrl = type.getOrigin();
            String uri =  dtrUrl +  "objects/" + pid + "?full=true";
            request = HttpRequest.newBuilder()
                    .GET()
                    .timeout(Duration.ofSeconds(60))
                    .uri(URI.create(uri))
                    .build();
            response = client.send(request,HttpResponse.BodyHandlers.ofString());
        }
        else{
            String uri = "http://hdl.handle.net/" + pid + "?locatt=view:json";

            request = HttpRequest.newBuilder()
                    .GET()
                    .timeout(Duration.ofSeconds(60))
                    .uri(URI.create(uri))
                    .build();
            response = client.send(request,HttpResponse.BodyHandlers.ofString());

            /*After the first request, we receive the URL to the type in its DTR. Since we need the full specification, the parameter "?full"
            needs to be set to true to get all the information necessary. Thus, the second request.*/

            if(!response.headers().map().containsKey("location")){
                logger.warning(String.format("Requested Handle %s does not exist", pid));
                throw new IOException(String.format("Requested Handle %s does not exist.", pid));
            }
            dtrUrl = response.headers().map().get("location").get(0);
            request = HttpRequest.newBuilder()
                    .GET()
                    .timeout(Duration.ofSeconds(60))
                    .uri(URI.create(dtrUrl + "?full=true"))
                    .build();
            response = client.send(request,HttpResponse.BodyHandlers.ofString());
        }

        JsonNode root = mapper.readTree(response.body());
        logger.info(root.toString());
        if(dtrUrl.contains("dtr-test.pidconsortium") || dtrUrl.contains("dtr-pit.pidconsortium")){
            TypeEntity typeEntity = legacyExtractor.createEntity(root, dtrUrl);
            legacyExtractor.extractFields(typeEntity);
            typeSearch.upsertEntry(typeEntity.serializeSearch(), collection);
        }
        else{
            dtrUrl = "https://" + dtrUrl.split("/")[2]+"/";
            logger.info(dtrUrl);
            GeneralEntity generalEntity = eoscExtractor.createGeneralEntity(root, dtrUrl);
            typeSearch.upsertEntry(generalEntity.serializeSearch(), "general");

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
        logger.info(String.format("Adding Type %s to the cache was successful", pid));
    }

    /**
     * Retrieve the description of a type from the repo.
     * @param pid the PID to add/refresh in the cache.
     * @param refresh flag, if type should be refreshed
     * @throws InterruptedException
     * @throws IOException
     */
    public JsonNode getDescription(String pid, Boolean refresh, Boolean refreshChildren) throws Exception{
        checkAdd(pid, refresh, refreshChildren, "types");
        Map<String, Object> type = typeSearch.get(pid, "types");
        TypeEntity typeEntity = new TypeEntity(type);
        return typeEntity.serialize();
    }

    public JsonNode getUnit(String pid, Boolean refresh) throws Exception {
        checkAdd(pid, refresh, false, "units");
        Map<String, Object> unit = typeSearch.get(pid, "units");
        UnitEntity unitEntity = new UnitEntity(unit);
        return unitEntity.serialize();
    }

    public JsonNode getTaxonomyNode(String pid, Boolean refresh) throws Exception {
        checkAdd(pid, refresh, false, "taxonomy");
        TaxonomyEntity taxonomyEntity = taxonomyGraph.get(pid);
        return mapper.valueToTree(taxonomyEntity.serializeSearch());
    }

    public JsonNode getTaxonomySubtree(String pid) throws Exception{
        checkAdd(pid, false, false,"taxonomy");
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
        return search("*", new String[]{"name"}, filterBy, "general", true);
    }

    /**
     * Construct and return the validation schema of a type from the repo.
     * @param pid the PID to add/refresh in the cache.
     * @param refresh flag, if type should be refreshed
     */
    public ObjectNode getValidation(String pid, Boolean refresh, Boolean refreshChildren) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        checkAdd(pid, refresh, refreshChildren, "types");
        TypeEntity typeEntity = new TypeEntity(typeSearch.get(pid, "types"));
        String style = typeEntity.getStyle();
        try{
            root = switch (style) {
                case "legacy" -> legacyValidator.validation(pid);
                case "eosc" -> eoscValidator.validation(pid);
                default -> root;
            };
        } catch (Exception e) {
            throw new Exception(e);
        }
        return root;
    }

    public String getMimeString(String pid) throws Exception{
        JsonNode type = mapper.valueToTree(typeSearch.get(pid, "general"));
        if(type.get("type").textValue().equals("ExtendedMimeType")){
            return type.get("name").textValue();
        }
        return "Selected Type is not a valid Extended MIME Type.";
    }

    /**
     * Helper function avoiding repeated code. Adds a PID to the repo if conditions demand it.
     * If desired or necessary, refresh all children types (For schema elements)
     * @param pid the PID to add/refresh in the cache.
     * @param refresh flag, if type should be refreshed
     * @param refreshChildren flag, if type's children should be refreshed
     */
    public void checkAdd(String pid, Boolean refresh, Boolean refreshChildren, String collection) throws Exception {
        if(!typeSearch.has(pid, collection)){
            logger.info(String.format("Retrieving pid %s via handle and caching...", pid));
            if(collection.equals("types")){
                addAllChildren(pid);
            }
            else{
                addType(pid, collection);
            }
            cacheSchema(pid);
        }
        else{
            if(refreshChildren){
                if(collection.equals("types")){
                    addAllChildren(pid);
                }
                else{
                    addType(pid, collection);
                }
                cacheSchema(pid);
                return;
            }
            if(refresh){
                addType(pid, collection);
                if(collection.equals("types")){
                    cacheSchema(pid);
                }
            }
        }
    }

    /**
     * Recursively refresh/add all children of a schema element type
     * @param pid the PID to add/refresh in the cache.
     */
    public void addAllChildren(String pid) throws Exception{
        addType(pid, "types");
        Map<String, Object> type = typeSearch.get(pid, "types");
        ObjectNode node = mapper.valueToTree(type.get("content"));
        if(node.has("Schema")){
            if(node.get("Schema").has("Properties")){
                ArrayNode properties = mapper.valueToTree(node.get("Schema").get("Properties"));
                for(JsonNode i : properties){
                    if(i.has("Type")){
                        addAllChildren(i.get("Type").textValue());
                    }
                }
            }
        }
    }

    public JsonNode retrieve(String pid, String collection) throws Exception{
        //Map<String, Object> type = typeSearch.get(pid, collection);
        return mapper.valueToTree(typeSearch.get(pid, collection));
    }

    /**
     * Search for types in the repository with a query.
     */
    public ArrayList<Object> search(String query, String[] queryBy, Map<String,String> filterBy, String collection, Boolean infix) throws Exception{
        return typeSearch.search(query, queryBy, filterBy, collection, infix);
    }

    public JsonNode resolveRefs(JsonNode schemaNode) throws IOException, InterruptedException {
        if (schemaNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) schemaNode;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if ("$ref".equals(entry.getKey()) && entry.getValue().isTextual()) {
                    String refUrl = entry.getValue().asText();
                    System.out.println("Resolving: " + refUrl);

                    try {
                        HttpClient client = HttpClient.newBuilder()
                                .followRedirects(HttpClient.Redirect.NORMAL)
                                .connectTimeout(Duration.ofSeconds(20))
                                .build();

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(refUrl))
                                .GET()
                                .build();

                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                        int statusCode = response.statusCode();
                        logger.info("Response Code: " + statusCode);

                        if (statusCode == 200) {
                            String responseBody = response.body();
                            JsonNode refSchema = mapper.readTree(responseBody);

                            if (refSchema.isEmpty()) {
                                logger.warning("Fetched schema is empty for URL: " + refUrl);
                            } else {
                                // Replace $ref with actual schema content
                                return resolveRefs(refSchema);
                            }
                        } else {
                            logger.warning("Failed to fetch schema. Response code: " + statusCode);
                        }
                    } catch (Exception e) {
                        logger.severe("Error fetching schema: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    objectNode.set(entry.getKey(), resolveRefs(entry.getValue()));
                }
            }
        } else if (schemaNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) schemaNode;
            for (int i = 0; i < arrayNode.size(); i++) {
                arrayNode.set(i, resolveRefs(arrayNode.get(i)));
            }
        }
        return schemaNode;
    }

    /**
     * Loads the root schema, resolves all $refs, and returns the expanded schema.
     */
    public JsonNode loadAndExpandSchema(String schemaUrl) throws IOException, InterruptedException {
        System.out.println("Loading schema: " + schemaUrl);
        JsonNode rootSchema = mapper.readTree(schemaUrl);
        return resolveRefs(rootSchema);
    }

    /**
     * Validates a JSON object against a type schema.
     * @param pid The PID of the type against which the object should be validated
     * @param object The JSON object that is to be validated
     * @throws Exception
     */
    public String validate(String pid, Object object, Boolean refresh, Boolean refreshChildren) throws Exception {
        try {
            checkAdd(pid, refresh, refreshChildren, "types");
            // Load schema and resolve all $ref
            JsonNode expandedSchema = loadAndExpandSchema(getValidation(pid, refresh, refreshChildren).toString());

            // Convert JSON object to JsonNode
            JsonNode node = mapper.valueToTree(object);

            // Validate using JSON Schema Validator
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
            JsonSchema schema = factory.getSchema(expandedSchema.toString());
            Set<ValidationMessage> errors = schema.validate(node);
            return errors.isEmpty() ? "Valid" : errors.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}