package clients;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import factories.TestFactory;
import models.TestRequest;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;

import java.util.Map;

/**
 * Created by dimi5963 on 3/6/16.
 */
public class TestClientImpl implements  TestClient {

    @Inject
    WSClient wsClient;
    private final TestFactory testFactory;

    @Inject
    public TestClientImpl(TestFactory testFactory){
        this.testFactory = testFactory;
    }

    @Override
    public ObjectNode makeTestRequest(TestRequest testRequest, String ip, String port) {
        WSRequest wsRequest = wsClient.url(
                testFactory.getTestUrl(ip, port, testRequest.getUri()))
                .setMethod(testRequest.getMethod()).setBody(testRequest.getBody());
        Map<String, String> requestHeaders = testRequest.getHeaders();
        requestHeaders.forEach(wsRequest::setHeader);

        F.Promise<ObjectNode> resultPromise = wsRequest.execute().map(
                wsResponse -> {
                    ObjectNode responseNode = Json.newObject();

                    responseNode.put("url", wsResponse.getUri().toString());
                    responseNode.put("responseBody", wsResponse.getBody());
                    ArrayNode arrayNode = Json.newArray();
                    wsResponse.getAllHeaders().forEach((name, value) -> {
                        ObjectNode header = Json.newObject();
                        ArrayNode headerArrayNode = Json.newArray();
                        value.forEach(headerValue -> {
                            headerArrayNode.add(headerValue);
                        });
                        header.putArray(name).addAll(headerArrayNode);
                        arrayNode.add(header);
                    });
                    responseNode.putArray("responseHeaders").addAll(arrayNode);
                    responseNode.put("responseStatus", wsResponse.getStatus());
                    responseNode.put("responseStatusText", wsResponse.getStatusText());
                    return responseNode;
                }
        ).recover(
                throwable -> {
                    throw new InternalServerException(
                            "We are currently experiencing difficulties.  " +
                                    "Please try again later.");
                }
        );

        return resultPromise.get(30000);
    }
}
