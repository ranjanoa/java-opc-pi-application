package com.cimpor.opc.utils;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PiWebApiClient {

    private final OkHttpClient client;
    private final String baseUrl;
    private final String username;
    private final String password;
    private final Gson gson = new Gson();

    public PiWebApiClient(String baseUrl, String username, String password) {
        this.client = new OkHttpClient.Builder().build(); // Optionally ignore SSL here if verify=False
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.username = username;
        this.password = password;
    }

    public List<Map<String, String>> searchTags(String query) throws IOException {
        String url = baseUrl + "/search?q=" + query + "&scope=pi&count=200";
        String credential = Credentials.basic(username, password);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
            JsonArray items = json.getAsJsonArray("Items");
            
            List<Map<String, String>> results = new ArrayList<>();
            if (items != null) {
                for (JsonElement element : items) {
                    JsonObject item = element.getAsJsonObject();
                    String name = item.has("Name") ? item.get("Name").getAsString() : 
                                 (item.has("name") ? item.get("name").getAsString() : "");
                    String webId = item.has("WebId") ? item.get("WebId").getAsString() : 
                                  (item.has("webId") ? item.get("webId").getAsString() : "");
                                  
                    if (!name.isEmpty() && !webId.isEmpty()) {
                        Map<String, String> map = new HashMap<>();
                        map.put("name", name);
                        map.put("webId", webId);
                        results.add(map);
                    }
                }
            }
            return results;
        }
    }
}
