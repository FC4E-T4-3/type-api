package com.fce4.typeapi;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import javax.annotation.PostConstruct;

@Service
public class TypeService {

    @Autowired
    private TypeRepository typeRepository;
    private String envFile;

    Logger logger = Logger.getLogger(TypeService.class.getName());

    @PostConstruct
    public void init() throws IOException, InterruptedException {
        refreshRepository();
    }

    /**
     * Refreshes the full contents of the cache, harvesting the env_file.
     * @throws InterruptedException
     * @throws IOException
     */
    public void refreshRepository() throws IOException, InterruptedException{
        logger.info("Refreshing Cache");

        //Currently only loding one URL. TODO: Read config file.
        String uri = "https://dtr-test.pidconsortium.net/objects?query=*";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .timeout(Duration.ofSeconds(10))
            .uri(URI.create(uri))
            .build();
            HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(response.body());

        for (JsonNode jsonNode : actualObj.get("results")) {
            if(!jsonNode.has("type")){
                continue;
            }
            TypeEntity typeEntity = new TypeEntity(jsonNode);
            typeRepository.save(typeEntity);
        }
        logger.info("Refreshing Cache successful.");
    }

    /**
     * Adds a single data type to the repository. Either for selective refreshing, or to add valid types not in the configured DTR's.
     * @param identifier the PID to add/refresh in the cache.
     * @throws InterruptedException
     * @throws IOException
     */
    public void addType(String pid) throws IOException, InterruptedException{
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
        needs to be set to true to get all the information necessary. Thus the second request.*/
        
        if(!response.headers().map().containsKey("location")){
            logger.info(String.format("Requested Handle %s does not exist", pid));
            throw new IOException(String.format("Requested Handle %s does not exist.", pid));
        }
        String dtrUrl = response.headers().map().get("location").get(0);
        
        request = HttpRequest.newBuilder()
            .GET()
            .timeout(Duration.ofSeconds(10))
            .uri(URI.create(dtrUrl + "?full=true"))
            .build();
        response = client.send(request,HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(response.body());
        if(!jsonNode.has("id") || !jsonNode.has("type") || !jsonNode.has("content")){
            logger.warning(String.format("Requested Handle %s is not a valid type", pid));
            throw new IOException("Handle is not valid type.");
        }
        TypeEntity typeEntity = new TypeEntity(jsonNode);
        addTags(jsonNode);
        typeRepository.save(typeEntity);
    
        logger.info(String.format("Adding Type %s to the cache was successful", pid));

    }

    public void addTags(JsonNode type){

    }

    /**
     * Retrieve the description of a type from the repo.
     * @param identifier the PID to add/refresh in the cache.
     * @param refresh flag, if type should be refreshed
     * @throws InterruptedException
     * @throws IOException
     */
    public JsonNode getDescription(String pid, Boolean refresh) throws IOException, InterruptedException{
        logger.info(String.format("Getting Type Description for %s.", pid));
        if(!typeRepository.hasPid(pid) || refresh){
            logger.info(String.format("Retrieving pid %s via handle and caching...", pid));
            addType(pid);
        }
        return typeRepository.get(pid).serialize();
    }

    /**
     * Construct and return the validation schema of a type from the repo.
     * @param identifier the PID to add/refresh in the cache.
     * @param refresh flag, if type should be refreshed
     */
    public void getValidation(String pid) {

    }

    /**
     * Search for types in the repository with a query.
     * @param identifier the PID to add/refresh in the cache.
     */
    public void search(String query) {

    }
}