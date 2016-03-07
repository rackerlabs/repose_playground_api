package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import models.User;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.IUserService;
import services.TestService;

/**
 * Created by dimi5963 on 3/1/16.
 */
public class Test extends Controller {

    private final IUserService userService;
    private final TestService testService;

    @Inject
    public Test(IUserService userService, TestService testService){

        this.userService = userService;
        this.testService = testService;
    }

    /***
     * Repose test will do the following:
     * 1. empty current.log, http-debug.log, intra-filter.log
     * 2. make a request based on json to running repose node (get by {id})
     * 3. put response in response json
     * 4. put current.log in current json
     * 5. put http-debug.log in http json
     * 6. put intra-filter.log in intra-filter json
     * 7. return all the jsons
     *
     * @param id container id
     * @return
     */
    @Deprecated
    public Result test(String id){
        Logger.debug("In test controller.  Get test form: " + id);
        JsonNode requestBody = request().body().asJson();
        String token = request().getHeader("Token");
        Logger.debug("Check the user for " + token);
        //check if expired
        if(!userService.isValid(token)) {
            Logger.warn("Invalid user: " + token);
            return unauthorized();
        } else {
            //get user by token.
            User user = userService.findByToken(token);
            if (user != null) {
                Logger.debug("User is authorized: " + user.toString() + " for " + token);
                try {
                    ObjectNode result = testService.testReposeInstance(user, id, requestBody);
                    return ok(Json.toJson(result));
                } catch (InternalServerException ise) {
                    ObjectNode response = JsonNodeFactory.instance.objectNode();
                    response.put("message", ise.getLocalizedMessage());
                    return internalServerError(Json.toJson(response));
                }

            } else {
                Logger.debug("The only way this could have happened is if token timeout between " +
                        "previous check and now.  Unlikely but possible.");
                return unauthorized();
            }
        }
    }

}
