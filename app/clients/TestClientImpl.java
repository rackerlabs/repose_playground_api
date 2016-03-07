package clients;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Test;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;

import java.util.Map;

/**
 * Created by dimi5963 on 3/6/16.
 */
public class TestClientImpl implements  TestClient {


    @Override
    public ObjectNode makeTestRequest(Test test, String ip, String port) {
        WSRequest wsRequest = WS.url(
                "http://" + ip + ":" + port + test.getUri())
                .setMethod(test.getMethod()).setBody(test.getBody());
        Map<String, String> requestHeaders = test.getHeaders();
        requestHeaders.forEach(wsRequest::setHeader);

        F.Promise<ObjectNode> resultPromise;
        resultPromise = wsRequest.execute().map(
                wsResponse -> {
                    ObjectNode responseNode = Json.newObject();

                    responseNode.put("url", wsResponse.getUri().toString());
                    responseNode.put("responseBody", wsResponse.getBody());
                    responseNode.putPOJO("responseHeaders",
                            Json.toJson(wsResponse.getAllHeaders()));
                    responseNode.put("responseStatus", wsResponse.getStatus());
                    responseNode.put("responseStatusText", wsResponse.getStatusText());
                    return responseNode;
                }
        );

        return resultPromise.get(30000);
    }
}
