package com.fce4.dtrtoolkit;

import com.fce4.dtrtoolkit.Extractors.*;
import com.fce4.dtrtoolkit.Validators.*;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;

import com.networknt.schema.SpecVersion;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import org.tomlj.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.annotation.PostConstruct;

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
    @Scheduled(fixedRate = 24, timeUnit = TimeUnit.HOURS)
    public void refreshRepository() throws IOException, InterruptedException, Exception{
        logger.info("Refreshing Cache");
        typeList.clear();
        //Setting a new timestamp, should a new logfile be necessary
        Date currentDate = new Date(System.currentTimeMillis());
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM");
		System.setProperty("timestamp", df.format(currentDate));

        TomlParseResult result = Toml.parse(Paths.get(config));
      
        for(var i : result.entrySet()){
			try {
				TomlTable t = TomlTable.class.cast(i.getValue());
                String dtr = i.getKey();
				String url = t.getString("url");
				String suffix = t.getString("suffix");
				List<Object> types = t.getArray("types").toList();
				String style = t.getString("style");

                logger.info(String.format("extracting %s", url));

                switch(style){
                    case "legacy":
                        legacyExtractor.extractTypes(url+suffix, types, dtr);
                        break;
                    case "eosc":
                        eoscExtractor.extractTypes(url+suffix, types, dtr);
                        break;
                    default:
                        logger.warning(String.format("DTR with style '%s' can not be imported. Please use one of the offered options.", style));
                        break;
                }
			} catch (Exception e) {
            	logger.warning(e.toString());
            }
        }
        //typeSearch.upsertList(typeList);
        logger.info("Refreshing Cache successful.");
    }

    /**
     * Adds a single data type to the repository. Either for selective refreshing, or to add valid types not in the configured DTR's.
     * Only works for handle's that forward to a type in a cordra instance.
     * @param pid the PID to add/refresh in the cache.
     * @throws InterruptedException
     * @throws IOException
     */
    public void addType(String pid) throws Exception{
        logger.info(String.format("Adding Type %s to the cache", pid));

        String uri = "https://hdl.handle.net/" + pid + "?locatt=view:json";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .timeout(Duration.ofSeconds(10))
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
            .timeout(Duration.ofSeconds(10))
            .uri(URI.create(dtrUrl + "?full=true"))
            .build();
        response = client.send(request,HttpResponse.BodyHandlers.ofString());

        JsonNode root = mapper.readTree(response.body());

        if(dtrUrl.contains("dtr-test.pidconsortium") || dtrUrl.contains("dtr-pit.pidconsortium")){
            TypeEntity typeEntity = legacyExtractor.createEntity(root, dtrUrl);
            legacyExtractor.extractFields(typeEntity);
            typeSearch.upsertType(typeEntity.serializeSearch());
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
        checkAdd(pid, refresh);
        logger.info("HIER");
        Map<String, Object> type = typeSearch.get(pid, "types");
        TypeEntity typeEntity = new TypeEntity(type);
        return typeEntity.serialize();
    }

    /**
     * Construct and return the validation schema of a type from the repo.
     * @param pid the PID to add/refresh in the cache.
     * @param refresh flag, if type should be refreshed
     */
    public ObjectNode getValidation(String pid, Boolean refresh) throws Exception {
        
        ObjectNode root = mapper.createObjectNode();
        checkAdd(pid, refresh);
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
    public void checkAdd(String pid, Boolean refresh) throws Exception {
        if(!typeSearch.has(pid, "types") || refresh){
            logger.info(String.format("Retrieving pid %s via handle and caching...", pid));
            addType(pid);            
        }
    }

    /**
     * Search for types in the repository with a query.
     * @param identifier the PID to add/refresh in the cache.
     */
    public ArrayList<Object> search(String query, String[] queryBy, Boolean infix) throws Exception{
        return typeSearch.search(query, queryBy, infix);
    }

    /**
     * Validates a JSON object against a type schema.
     * @param pid The PID of the type against which the object should be validated
     * @param object The JSON object that is to be validated
     * @throws Exception
     */
    public String validate(String pid, Object object) throws Exception{
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchema schema = factory.getSchema(getValidation(pid, false).toString());
        JsonNode node = mapper.valueToTree(object);
        Set<ValidationMessage> errors = schema.validate(node);
        if(errors.size()>0){
            return errors.toString();
        }
        return "Valid";
    }
}