package helpers;

import com.fasterxml.jackson.databind.JsonNode;
import exceptions.InternalServerException;
import models.User;
import play.Logger;
import play.libs.F;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

/**
 * Created by dimi5963 on 2/9/16.
 */
public class Carina {
    public JsonNode getCluster(String clusterName, User user) throws InternalServerException {
        F.Promise<JsonNode> result = WS.url("https://app.getcarina.com/clusters/" +
                user.username + "/" + clusterName)
                .setHeader("x-auth-token", user.token)
                .get().map(new F.Function<WSResponse, JsonNode>() {
                    @Override
                    public JsonNode apply(WSResponse response) throws Throwable {
                        Logger.info("response for " + user);
                        Logger.info("body: " + response.getStatus() + " " + response.getBody());
                        switch (response.getStatus()) {
                            case 200:
                                return response.asJson().get("status");
                            case 404:
                                return null;
                            default:
                                return null;
                        }
                    }
                });
        return result.get(30000);
    }
}
