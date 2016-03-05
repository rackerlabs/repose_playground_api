package controllers;

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
import services.ConfigurationService;
import services.IReposeService;
import services.IUserService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dimi5963 on 3/1/16.
 */
public class Configuration extends Controller {

    private final IUserService userService;
    private final IReposeService reposeService;
    private final ConfigurationService configurationService;
    private final ConfigurationFactory configurationFactory;

    @Inject
    public Configuration(IUserService userService,
                         IReposeService reposeService, ConfigurationService configurationService,
                         ConfigurationFactory configurationFactory){

        this.userService = userService;
        this.reposeService = reposeService;
        this.configurationService = configurationService;
        this.configurationFactory = configurationFactory;
    }

    /**
     * Retrieve configurations for repose id
     *
     * Retrieval is done in xml form, which can then be either zipped up or sent across wrapped in json object
     * @param id
     * @return
     */
    public Result configurations(String id){
        Logger.debug("In configurations controller");

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
                    List<models.Configuration> configurationList =
                            configurationService.getConfigurationsForInstance(user, id);
                    if(configurationList != null)
                        return ok(Json.toJson(configurationList));
                    else
                        return ok(Json.toJson(new ArrayList<models.Configuration>()));
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

    /**
     * Upload configurations for repose id and start repose instance
     *
     * Retrieval is done in xml form, which can then be either zipped up or sent across wrapped in json object
     * @param id
     * @return
     */
    public Result uploadReposeConfigs(String id){
        Logger.debug("In upload repose configs controller.  Create repose instance for version " + id);

        Http.MultipartFormData configurations = request().body().asMultipartFormData();
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
                    List<models.Configuration> configurationList =
                            configurationFactory.translateConfigurations(user, id, configurations);

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
