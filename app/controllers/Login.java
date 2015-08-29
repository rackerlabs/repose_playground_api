package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import helpers.Helpers;
import models.AuthRequest;
import models.LoginRequest;
import models.PasswordCredsRequest;
import models.User;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.F;
import play.libs.F.Function;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import play.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Login extends Controller {

    public Result me() {
        String token = request().getHeader("Token");
        Logger.debug("Check the user for " + token);
        Logger.debug("The return user is " + User.findByToken(token));
        //check if expired
        if(!User.isValid(token))
            return unauthorized();
        else
        {
            User user = User.findByToken(token);
            Logger.debug(user.toString() + " for " + token);
            return ok(Json.toJson(user));
        }
    }

    public Result auth() {
        Logger.debug("In login controller.  Get login form");
        Form<LoginForm> loginForm= Form.form(LoginForm.class).bindFromRequest();
        Logger.debug("log binding from request");

        if(loginForm.hasErrors()){
            return badRequest(loginForm.errorsAsJson());
        }

        LoginForm form = loginForm.get();
        User user = User.findByNameAndPassword(form.username, form.password);
        if(user == null){
            //find the user in rackspace identity (for now)
            //if found, save the user
            //if not, errorz
            LoginRequest loginRequest = new LoginRequest(
                    new AuthRequest(
                            new PasswordCredsRequest(form.username, form.password)));
            F.Promise<Result> resultPromise = WS.url(
                    "https://identity.api.rackspacecloud.com/v2.0/tokens")
                    .setContentType("application/json")
                    .post(Json.toJson(loginRequest)).map(
                    new Function<WSResponse, Result>() {
                        @Override
                        public Result apply(WSResponse wsResponse) throws Throwable {
                            Logger.debug("response from identity: " +
                                    wsResponse.getStatus() + " " + wsResponse.getStatusText());

                            Logger.debug("response body: " + wsResponse.getBody());

                            switch(wsResponse.getStatus()){
                                case 200:
                                    //save the user
                                    JsonNode userData = wsResponse.asJson();
                                    Logger.debug("save this user: " + userData.get("access").get("token").get("id"));
                                    Logger.debug("save this user: " + userData.get("access").get("user").get("name"));
                                    DateTimeFormatter fmt = new DateTimeFormatterBuilder().append(
                                            ISODateTimeFormat.dateHourMinuteSecondMillis())
                                            .appendLiteral('Z').toFormatter();


                                    User newUser = new User();
                                    newUser.setUsername(
                                            userData.get("access").get("user").get("name").asText());
                                    newUser.setToken(userData.get("access").get("token").get("id").asText());
                                    newUser.setTenant(
                                            userData.get("access").get("token").get("tenant").get("id").asText());
                                    newUser.setExpireDate(
                                            DateTime.parse(
                                                    userData.get("access").get("token").get("expires").asText(), fmt));
                                    newUser.setPassword(form.password);
                                    newUser.save();
                                    Logger.debug("user: " + newUser.toString());
                                    return ok(Json.toJson(newUser));
                                case 401:
                                    Logger.debug("Unauthenticated");
                                    return unauthorized(wsResponse.getBody());
                                default:
                                    return internalServerError(
                                            buildJsonResponse("message", "Unable to authenticate user"));
                            }
                        }
                    }
            );
            return resultPromise.get(30000);
        } else {
            return ok(buildJsonResponse("success", user.username));

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


