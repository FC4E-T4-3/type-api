package com.fce4.dtrtoolkit.Extractors;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface BaseExtractor {

    ObjectMapper mapper = new ObjectMapper();
    ArrayList<HashMap<String, Object>> typeList = new ArrayList<>();
    HttpClient client = HttpClient.newHttpClient();

    public void extractTypes(String url, List<Object> types, String dtr) throws Exception;
}
