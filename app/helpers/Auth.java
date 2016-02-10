package helpers;

import com.fasterxml.jackson.databind.JsonNode;
import exceptions.InternalServerException;
import exceptions.NotFoundException;
import models.AuthRequest;
import models.LoginRequest;
import models.PasswordCredsRequest;
import models.User;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import java.net.URL;

/**
 * Created by dimi5963 on 1/12/16.
 */
public class Auth {

    public User getUser(String username, String password) throws NotFoundException, InternalServerException{
        Logger.info("Get user");
        User user = User.findByNameAndPasswordCurrent(username, password);
        if(user == null){
            //find the user in rackspace identity (for now)
            //if found, save the user
            //if not, errorz
            LoginRequest loginRequest = new LoginRequest(
                    new AuthRequest(
                            new PasswordCredsRequest(username, password)));
            String url = play.Play.application().configuration().getString("identity.url") +
                    play.Play.application().configuration().getString("identity.tokens.endpoint");
            Logger.info("url: " + url);
            F.Promise<User> resultPromise = WS.url(
                    play.Play.application().configuration().getString("identity.url") +
                            play.Play.application().configuration().getString("identity.tokens.endpoint"))
                    .setContentType("application/json")
                    .post(Json.toJson(loginRequest)).map(
                            new F.Function<WSResponse, User>() {
                                @Override
                                public User apply(WSResponse wsResponse) throws Throwable {
                                    Logger.debug("response from identity: " +
                                            wsResponse.getStatus() + " " + wsResponse.getStatusText());

                                    Logger.debug("response body: " + wsResponse.getBody());

                                    switch (wsResponse.getStatus()) {
                                        case 200:
                                            //save the user
                                            JsonNode userData = wsResponse.asJson();
                                            Logger.debug("save this user: " + userData.get("access").get("token").get("id"));
                                            Logger.debug("save this user: " + userData.get("access").get("user").get("name"));
                                            DateTimeFormatter fmt = new DateTimeFormatterBuilder().append(
                                                    ISODateTimeFormat.dateHourMinuteSecondMillis())
                                                    .appendLiteral('Z').toFormatter();

                                            User newUser = User.findByNameAndPassword(username, password);
                                            if (newUser == null) {
                                                newUser = new User();
                                            }
                                            newUser.setUsername(
                                                    userData.get("access").get("user").get("name").asText());
                                            newUser.setUserid(
                                                    userData.get("access").get("user").get("id").asText());
                                            newUser.setToken(userData.get("access").get("token").get("id").asText());
                                            newUser.setTenant(
                                                    userData.get("access").get("token").get("tenant").get("id").asText());
                                            newUser.setExpireDate(
                                                    DateTime.parse(
                                                            userData.get("access").get("token").get("expires").asText(), fmt));
                                            newUser.setPassword(password);

                                            newUser.save();
                                            Logger.info("user: " + newUser.toString());
                                            return newUser;
                                            //return ok(Json.toJson(newUser));
                                        case 401:
                                            Logger.debug("Unauthenticated");
                                            throw new NotFoundException(wsResponse.getBody());
                                            //return unauthorized(wsResponse.getBody());
                                        default:
                                            throw new InternalServerException("{'message': 'Unable to authenticate user'}");
                                            //return internalServerError(
                                            //        Helpers.buildJsonResponse("message", "Unable to authenticate user"));
                                    }
                                }
                            }
                    ).recover(
                            new F.Function<Throwable, User>() {
                                //Everything is down!
                                @Override
                                public User apply(Throwable throwable) throws Throwable {
                                    throw new InternalServerException(
                                            "{'message': 'We are currently experiencing difficulties.  " +
                                                    "Please try again later.'}");
                                    //return internalServerError(
                                    //        Helpers.buildJsonResponse("message", "We are currently experiencing difficulties.  Please try again later."));
                                }
                            }
                    );
            return resultPromise.get(30000);
        } else {
            return user;

        }
    }

    public String getUserApiKey(String token, String tenantUserId) throws NotFoundException, InternalServerException{
        Logger.info("get api key for " + tenantUserId);
        Logger.info("url: " + play.Play.application().configuration().getString("identity.url") +
                play.Play.application().configuration().getString("identity.users.endpoint") +
                tenantUserId +
                play.Play.application().configuration().getString("identity.apikey.endpoint"));
        F.Promise<String> resultPromise = WS.url(
                play.Play.application().configuration().getString("identity.url") +
                        play.Play.application().configuration().getString("identity.users.endpoint") +
                        tenantUserId +
                        play.Play.application().configuration().getString("identity.apikey.endpoint"))
                .setHeader("x-auth-token", token)
                .get().map(
                        new F.Function<WSResponse, String>() {
                            @Override
                            public String apply(WSResponse wsResponse) throws Throwable {
                                Logger.debug("response from identity: " +
                                        wsResponse.getStatus() + " " + wsResponse.getStatusText());

                                Logger.debug("response body: " + wsResponse.getBody());

                                switch (wsResponse.getStatus()) {
                                    case 200:
                                        JsonNode userData = wsResponse.asJson();
                                        Logger.debug("get api key for this user: " +
                                                userData.get("RAX-KSKEY:apiKeyCredentials").get("apiKey"));
                                        return userData.get("RAX-KSKEY:apiKeyCredentials").get("apiKey").asText();
                                    //return ok(Json.toJson(newUser));
                                    case 401:
                                        Logger.debug("Unauthenticated");
                                        throw new NotFoundException(wsResponse.getBody());
                                        //return unauthorized(wsResponse.getBody());
                                    default:
                                        throw new InternalServerException("{'message': 'Unable to authenticate user'}");
                                        //return internalServerError(
                                        //        Helpers.buildJsonResponse("message", "Unable to authenticate user"));
                                }
                            }
                        }
                ).recover(
                        new F.Function<Throwable, String>() {
                            //Everything is down!
                            @Override
                            public String apply(Throwable throwable) throws Throwable {
                                throw new InternalServerException(
                                        "{'message': 'We are currently experiencing difficulties.  " +
                                                "Please try again later.'}");
                                //return internalServerError(
                                //        Helpers.buildJsonResponse("message", "We are currently experiencing difficulties.  Please try again later."));
                            }
                        }
                );
        return resultPromise.get(30000);
    }
}
