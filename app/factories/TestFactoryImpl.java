package factories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import models.TestRequest;
import play.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static play.Logger.debug;

/**
 * Created by dimi5963 on 3/6/16.
 */
public class TestFactoryImpl implements TestFactory {

    @Override
    public TestRequest translateRequest(JsonNode requestBody) {
        debug("Translate request " + requestBody);

        if(requestBody == null)
            return null;

        Map<String, String> headers = new HashMap<>();

        if(requestBody.get("headers") != null) {
            ArrayNode requestHeaders = (ArrayNode) requestBody.get("headers");

            requestHeaders.forEach(request -> {
                if (!request.get("name").asText().isEmpty())
                    headers.put(request.get("name").asText(), request.get("value").asText());
            });
        }

        String method = requestBody.get("method") != null ? requestBody.get("method").asText() : "GET";
        String url = requestBody.get("url") != null ? requestBody.get("url").asText() : "/";
        String body = requestBody.get("body") != null ? requestBody.get("body").asText() : "";

        return new TestRequest(method, url, body, headers);
    }

    @Override
    public Map<String, ?> generateDebugMessageMap(List<String> httpDebugLogList) {
        Map<String, ?> debugMessageMap = new HashMap<String, List<?>>() {
            {
                put("poolMessages", new ArrayList<String>());
                put("externalRequests", new ArrayList<Map<String, List<String>>>());
            }
        };

        if(httpDebugLogList == null)
            return debugMessageMap;

        boolean requestMessageStarted = false;
        boolean responseMessageStarted = false;
        for (String entry : httpDebugLogList) {
            //check if connection pool entry
            debug("Entry: " + entry);
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
                    if(externalRequestsSize > 0) {
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
                        Logger.warn("We haven't received a request yet!  Something is wrong.  Let's ignore it for now");
                    }
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

    @Override
    public String getTestUrl(String ip, String port, String uri) {
        return "http://" + ip + ":" + port + uri;
    }


}
