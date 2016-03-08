package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import exceptions.NotFoundException;
import factories.ConfigurationFactory;
import models.ReposeEnvironmentType;
import models.User;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.*;

import java.util.List;
import java.util.regex.Pattern;

public class Application extends Controller {
    private final ConfigurationFactory configurationFactory;
    private final FilterService filterService;
    private final IUserService userService;
    private final IReposeService reposeService;

    @Inject
    public Application(ConfigurationFactory configurationFactory,
                       FilterService filterService, IUserService userService, IReposeService reposeService){

        this.configurationFactory = configurationFactory;
        this.filterService = filterService;
        this.userService = userService;
        this.reposeService = reposeService;
    }


    public Result versions() {
        Logger.debug("Retrieve available repose versions");

        String token = request().getHeader("Token");
        Logger.debug("Check the user for " + token);
        //check if expired
        if(!userService.isValid(token)) {
            Logger.warn("Invalid user: " + token);
            return unauthorized();
        } else {
            try{
                List<String> versions = filterService.getVersions();
                if(versions == null)
                    return notFound();
                return ok(Json.toJson(versions));
            } catch(InternalServerException ise) {
                ObjectNode response = JsonNodeFactory.instance.objectNode();
                response.put("message", ise.getLocalizedMessage());
                return internalServerError(Json.toJson(response));
            }

        }
    }

    public Result componentsByVersion(String id) {
        Logger.debug("Version id " + id.split(Pattern.quote("."))[0]);

        String token = request().getHeader("Token");
        Logger.debug("Check the user for " + token);
        //check if expired
        if(!userService.isValid(token)) {
            Logger.warn("Invalid user: " + token);
            return unauthorized();
        } else {
            try{
                List<String> filters = filterService.getFiltersByVersion(id);
                if(filters == null)
                    return notFound();
                return ok(Json.toJson(filters));
            } catch(InternalServerException ise) {
                ObjectNode response = JsonNodeFactory.instance.objectNode();
                response.put("message", ise.getLocalizedMessage());
                return internalServerError(Json.toJson(response));
            }

        }
    }

    /**
     * Supported are:
     * - ip-identity
     * - uri-normalization
     * - content-normalization
     * - header-normalization
     * - header-identity
     * - header-id-mapping
     * - uri-identity
     * - destination-router
     * - uri-stripper
     * @param id
     * @param componentId
     * @return
     */
    public Result component(String id, String componentId) {
        Logger.debug("Get " + componentId + " json representation for version " + id);

        String token = request().getHeader("Token");
        Logger.debug("Check the user for " + token);
        //check if expired
        if(!userService.isValid(token)) {
            Logger.warn("Invalid user: " + token);
            return unauthorized();
        } else {
            try{
                JsonNode componentData = filterService.getComponentData(id, componentId);
                if(componentData == null)
                    return notFound();
                Logger.debug("Get component Data json: " + Json.toJson(componentData));
                return ok(Json.toJson(componentData));
            } catch(InternalServerException ise) {
                ObjectNode response = JsonNodeFactory.instance.objectNode();
                response.put("message", ise.getLocalizedMessage());
                return internalServerError(Json.toJson(response));
            }

        }
    }

    /**
     * Build repose instance.
     * @param id version id
     * @return
     */
    public Result build(String id)  {
        /**
         * We build out the repose instance here.
         * 1. we convert json payload to proper xmls
         * 2. we create container, log4j, and system-model appropriately
         * 3. we get the cluster for the user
         * 4. we create a repose instance
         * 5. we create a repose origin service instance
         */
        Logger.debug("Let's create a repose instance.");

        Http.RequestBody requestBody = request().body();

        if(requestBody == null)
            return badRequest();

        JsonNode jsonRequest = requestBody.asJson();
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
                try {
                    //here, figure out which service to call based on the request (i'll provide the type that will
                    //be generated based on type of request (if docker file is provided for third party, if request
                    //to generate identity is provided, if origin docker file or url is provided
                    //by default take the generated origin type
                    Logger.debug("Retrieve configuration list from json body");
                    List<models.Configuration> configurationList =
                            configurationFactory.translateConfigurationsFromJson(user, id, jsonRequest);

                    String reposeId = reposeService.setUpReposeEnvironment(ReposeEnvironmentType.GENERATED_ORIGIN,
                            user, id, configurationList);
                    if(reposeId == null) {
                        ObjectNode response = JsonNodeFactory.instance.objectNode();
                        response.put("message", "unable to create repose environment.");
                        return internalServerError(Json.toJson(response));
                    } else {
                        ObjectNode response = JsonNodeFactory.instance.objectNode();
                        response.put("message", "success");
                        response.put("id", reposeId);
                        return ok(Json.toJson(response));
                    }
                } catch(NotFoundException nfe){
                    ObjectNode response = JsonNodeFactory.instance.objectNode();
                    response.put("message", nfe.getLocalizedMessage());
                    return badRequest(Json.toJson(response));
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

