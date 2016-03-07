package factories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import models.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static play.Logger.info;

/**
 * Created by dimi5963 on 3/6/16.
 */
public class TestFactoryImpl implements TestFactory {

    @Override
    public Test translateRequest(JsonNode requestBody) {
        ArrayNode requestHeaders = (ArrayNode) requestBody.get("headers");

        Map<String, String> headers = new HashMap<>();
        requestHeaders.forEach(request -> {
            if (!request.get("name").asText().isEmpty())
                headers.put(request.get("name").asText(), request.get("value").asText());
        });

        return new Test(requestBody.get("method").asText(), requestBody.get("url").asText(),
                requestBody.get("body").asText(), headers);
    }

    @Override
    public Map<String, ?> generateDebugMessageMap(List<String> httpDebugLogList) {

        Map<String, ?> debugMessageMap = new HashMap<String, List<?>>() {
            {
                put("poolMessages", new ArrayList<String>());
                put("externalRequests", new ArrayList<Map<String, List<String>>>());
            }
        };
        boolean requestMessageStarted = false;
        boolean responseMessageStarted = false;
        for (String entry : httpDebugLogList) {
            //check if connection pool entry
            info("Entry: " + entry);
            if (entry.contains("org.apache.http.impl.conn.PoolingClientConnectionManager")) {
                ((List<String>) debugMessageMap.get("poolMessages")).add(entry);
            }
            //check for request
            if (entry.contains("org.apache.http.wire -  >>")) {
                //set response message to false so that we don't log multiple messages in response log
                responseMessageStarted = false;
                if (!requestMessageStarted) {
                    //start message logging
                    ((List<Map<String, List<String>>>) debugMessageMap.get("externalRequests")).add(
                            new HashMap<String, List<String>>() {
                                {
                                    put(
                                            "request",
                                            new ArrayList<String>() {
                                                {
                                                    add(entry.substring(
                                                            entry.indexOf("org.apache.http.wire -  >>") +
                                                                    "org.apache.http.wire -  ".length()));
                                                }
                                            }
                                    );
                                }
                            });
                    requestMessageStarted = true;
                } else {
                    //append
                    int externalRequestsSize =
                            ((List<Map<String, List<String>>>) debugMessageMap.get("externalRequests")).size();
                    Map<String, List<String>> requestResponseLogs =
                            ((List<Map<String, List<String>>>) debugMessageMap.get("externalRequests")).
                                    get(externalRequestsSize - 1);
                    requestResponseLogs.get("request").add(
                            entry.substring(entry.indexOf("org.apache.http.wire -  >>") +
                                    "org.apache.http.wire -  ".length()));

                    //Logger.info("request logs entry: " + requestLogs.size() + " and last entry: " + requestLogs.get(requestLogs.size() - 1));
                    //String request = requestLogs.get(requestLogs.size() -1).concat("\n").concat(entry);
                    //requestLogs.set(requestLogs.size() - 1, request);
                    //((Map<String, List<String>>) debugMessageMap.get("externalRequests")).put("request", requestLogs);
                }
            }
            //check for response
            if (entry.contains("org.apache.http.wire -  <<")) {
                //is there a request message in progress?
                if (!responseMessageStarted) {
                    if (requestMessageStarted) {
                        //yes so set it to stop so that we don't log multiple messages in the request log
                        requestMessageStarted = false;
                    }
                    int externalRequestsSize =
                            ((List<Map<String, List<String>>>) debugMessageMap.get("externalRequests")).size();
                    Map<String, List<String>> requestResponseLogs =
                            ((List<Map<String, List<String>>>) debugMessageMap.get("externalRequests")).
                                    get(externalRequestsSize - 1);
                    requestResponseLogs.put("response", new ArrayList<String>() {
                        {
                            add(entry.substring(
                                    entry.indexOf("org.apache.http.wire -  <<") +
                                            "org.apache.http.wire -  ".length()));
                        }
                    });

                    responseMessageStarted = true;
                } else {
                    //append
                    int externalRequestsSize =
                            ((List<Map<String, List<String>>>) debugMessageMap.get("externalRequests")).size();
                    Map<String, List<String>> requestResponseLogs =
                            ((List<Map<String, List<String>>>) debugMessageMap.get("externalRequests")).
                                    get(externalRequestsSize - 1);
                    requestResponseLogs.get("response").add(
                            entry.substring(entry.indexOf("org.apache.http.wire -  <<") +
                                    "org.apache.http.wire -  ".length()));

                }
            }
        }

        return debugMessageMap;
    }
}
