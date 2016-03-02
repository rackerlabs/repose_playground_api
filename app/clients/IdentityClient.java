package clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import exceptions.UnauthorizedException;
import factories.IIdentityFactory;
import models.LoginRequest;
import models.User;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import repositories.IUserRepository;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class IdentityClient implements IIdentityClient {
    @Inject WSClient wsClient;
    private final IIdentityFactory identityFactory;
    private final IUserRepository userRepository;
    private final String validateUserUrl;

    @Inject
    public IdentityClient(IIdentityFactory identityFactory,
                          IUserRepository userRepository){
        this.identityFactory = identityFactory;
        this.userRepository = userRepository;
        this.validateUserUrl = identityFactory.getIdentityAuthUrl();
    }

    public User getUser(LoginRequest loginRequest) throws UnauthorizedException, InternalServerException {
        Logger.debug("making a request to get user");
        F.Promise<User> resultPromise = wsClient.url(validateUserUrl)
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
                                        JsonNode userData = null;
                                        try {
                                            userData = wsResponse.asJson();
                                            return userRepository.saveUser(userData,
                                                    loginRequest.getAuth().getPasswordCredentials().getUsername(),
                                                    loginRequest.getAuth().getPasswordCredentials().getPassword());
                                        } catch (RuntimeException jme) {
                                            Logger.error("Unable to parse response body: " + wsResponse.getBody());
                                            Logger.error(jme.getLocalizedMessage());
                                            jme.printStackTrace();
                                            throw new InternalServerException("Unable to authenticate user.");
                                        }
                                    case 401:
                                        Logger.debug("Unauthenticated");
                                        throw new UnauthorizedException("Unable to authenticate user.");
                                    default:
                                        throw new InternalServerException(
                                                "Unable to authenticate user.");
                                }
                            }
                        }
                ).recover(
                        new F.Function<Throwable, User>() {
                            //Everything is down!
                            @Override
                            public User apply(Throwable throwable) throws Throwable {
                                throw throwable;
                            }
                        }
                );
        return resultPromise.get(30000);
    }

    public String getUserApiKey(String token, String tenantUserId) throws UnauthorizedException, InternalServerException {
        Logger.debug("making a request to get user key");
        String apiKeyUrl = identityFactory.getIdentityApiAuthUrl(tenantUserId);
        F.Promise<String> resultPromise = wsClient.url(apiKeyUrl)
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
                                        JsonNode userData = null;
                                        try {
                                            userData = wsResponse.asJson();
                                            Logger.debug("get api key for this user: " +
                                                    userData.get("RAX-KSKEY:apiKeyCredentials").get("apiKey"));
                                            return userData.get("RAX-KSKEY:apiKeyCredentials").get("apiKey").asText();
                                        } catch (RuntimeException jme) {
                                            Logger.error("Unable to parse response body: " + wsResponse.getBody());
                                            Logger.error(jme.getLocalizedMessage());
                                            jme.printStackTrace();
                                            throw new InternalServerException("Unable to retrive api key for user.");
                                        }
                                    case 401:
                                        Logger.debug("Unauthenticated");
                                        throw new UnauthorizedException("Unable to retrive api key for user.");
                                    default:
                                        throw new InternalServerException("Unable to retrive api key for user.");
                                }
                            }
                        }
                ).recover(
                        new F.Function<Throwable, String>() {
                            //Everything is down!
                            @Override
                            public String apply(Throwable throwable) throws Throwable {
                                throw throwable;
                            }
                        }
                );
        return resultPromise.get(30000);
    }

}
