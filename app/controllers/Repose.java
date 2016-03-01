package controllers;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import models.User;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.IReposeService;
import services.IUserService;

public class Repose extends Controller {

    private final IUserService userService;
    private final IReposeService reposeService;

    @Inject
    public Repose(IUserService userService, IReposeService reposeService){

        this.userService = userService;
        this.reposeService = reposeService;
    }

    /***
     * Repose list will return a list of running and stopped repose instances
     * Currently, repose instances are created per tenant and version (e.g. 1-7.1, 1-7.2, etc.)
     * @implSpec The workflow is:
     *           1. Retrieve the token from a request
     *           2. Retrieve the user for the token
     *           3. Download cluster zip and put it into /tmp/{tenant}/ directory
     *           4. Parse docker.env and get the DOCKER_HOST location
     *           5. Use docker client to retrieve all running repose containers
     * @return
     */
    public Result list() {
        Logger.debug("Get repose list");
        String token = request().getHeader("Token");
        Logger.debug("Check the user for " + token);
        //check if expired
        if(!userService.isValid(token)) {
            Logger.warn("Invalid user: " + token);
            return unauthorized();
        } else
        {
            //get user by token.
            User user = userService.findByToken(token);
            if(user != null) {
                Logger.debug("User is authorized: " + user.toString() + " for " + token);
                try{
                    return ok(Json.toJson(reposeService.getReposeList(user)));
                } catch(InternalServerException ise) {
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

    /***
     * Repose stop will stop the running instance of repose and all of its linked containers
     * @param id id of the container
     * @return
     */
    public Result stop(String id) {
        Logger.debug("Stop repose instance");

        String token = request().getHeader("Token");
        Logger.debug("Check the user for " + token);
        //check if expired
        if(!userService.isValid(token)) {
            Logger.warn("Invalid user: " + token);
            return unauthorized();
        } else {
            //get user by token.
            User user = userService.findByToken(token);

            try{
                if(reposeService.stopReposeInstance(user, id))
                    return ok(Json.toJson("{'message':'success'}"));
                else
                    return ok(Json.toJson("{'message': 'failed to stop'}"));
            } catch(InternalServerException ise) {
                return internalServerError(ise.getLocalizedMessage());
            }
        }
    }

    /***
     * Repose start will start the running instance of repose and all of its linked containers
     * @param id id of the container
     * @return
     */
    public Result start(String id) {
        Logger.debug("Stop repose instance");

        String token = request().getHeader("Token");
        Logger.debug("Check the user for " + token);
        //check if expired
        if(!userService.isValid(token)) {
            Logger.warn("Invalid user: " + token);
            return unauthorized();
        } else {
            //get user by token.
            User user = userService.findByToken(token);

            try{
                if(reposeService.startReposeInstance(user, id))
                    return ok(Json.toJson("{'message':'success'}"));
                else
                    return ok(Json.toJson("{'message': 'failed to start'}"));
            } catch(InternalServerException ise) {
                return internalServerError(ise.getLocalizedMessage());
            }
        }
    }
}


