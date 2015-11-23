package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Region;
import models.User;
import play.Logger;
import play.data.validation.Constraints;
import play.libs.F;
import play.libs.F.Function;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.ArrayList;
import java.util.List;

public class Repose extends Controller {

    private static Region region = Region.DFW;

    public Result list() {
        String token = request().getHeader("Token");
        Logger.info("Check the user for " + token);
        Logger.info("The return user is " + User.findByToken(token));
        //check if expired
        if(!User.isValid(token))
            return unauthorized();
        else
        {
            //get carina info from service catalog (not there yet)
            //get the cluster name "ReposeTrial"
            //download credentials
            //get docker instances and return all containers named repose-playground
            User user = User.findByToken(token);
            F.Promise<Result> resultPromise = WS.url(
                    "https://dfw.servers.api.rackspacecloud.com/v2/" + user.tenant + "/servers/detail")
                    .setHeader("x-auth-token", token)
                    .get().map(
                            new Function<WSResponse, Result>() {
                                @Override
                                public Result apply(WSResponse wsResponse) throws Throwable {
                                    Logger.info("response from servers: " +
                                            wsResponse.getStatus() + " " + wsResponse.getStatusText());

                                    Logger.info("response body: " + wsResponse.getBody());

                                    switch (wsResponse.getStatus()) {
                                        case 200:
                                            //save the servers
                                            List<JsonNode> serverList = new ArrayList<JsonNode>();
                                            JsonNode serverData = wsResponse.asJson();
                                            Logger.info("show servers: " + serverData);
                                            //filter out only those with repose-playground metadata
                                            for(final JsonNode server : serverData.get("servers")) {
                                                if(server.get("metadata").has("ReposeLabel") &&
                                                        server.get("metadata").get("ReposeLabel")
                                                                .equals("repose-playground")){
                                                    serverList.add(server);
                                                }
                                            }
                                            return ok(Json.toJson(serverList));
                                        case 401:
                                            Logger.info("Unauthenticated");
                                            return unauthorized(wsResponse.getBody());
                                        default:
                                            return internalServerError(
                                                    buildJsonResponse("message", "Unable to retrieve server data"));
                                    }
                                }
                            }
                    );
            return resultPromise.get(30000);
        }
    }

    private static ObjectNode buildJsonResponse(String type, String message) {
        ObjectNode wrapper = Json.newObject();
        ObjectNode msg = Json.newObject();
        msg.put("message", message);
        wrapper.put(type, msg);
        return wrapper;
    }

    public static class LoginForm {
        @Constraints.Required
        @Constraints.MinLength(3)
        public String username;

        @Constraints.Required
        @Constraints.MinLength(6)
        public String password;
    }
}


