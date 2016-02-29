package services;

import clients.IIdentityClient;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import exceptions.UnauthorizedException;
import models.AuthRequest;
import models.LoginRequest;
import models.PasswordCredsRequest;
import models.User;
import play.Logger;
import play.libs.ws.WSClient;
import repositories.IUserRepository;

/**
 * Created by dimi5963 on 1/12/16.
 */
public class AuthService implements IAuthService {
    private final repositories.IUserRepository userRepository;
    private final clients.IIdentityClient identityClient;

    @Inject WSClient wsClient;

    @Inject
    public AuthService(IUserRepository userRepository,
                       IIdentityClient identityClient) {
        this.userRepository = userRepository;
        this.identityClient = identityClient;
    }

    public User getUser(String username, String password) throws UnauthorizedException, InternalServerException{
        Logger.debug("Get user");
        User user = userRepository.findByNameAndPasswordCurrent(username, password);
        if(user == null){
            return identityClient.getUser(new LoginRequest(
                    new AuthRequest(
                            new PasswordCredsRequest(username, password))));
        } else {
            return user;

        }
    }

    public String getUserApiKey(String token, String tenantUserId) throws UnauthorizedException, InternalServerException{
        Logger.debug("get api key for " + tenantUserId);
        return identityClient.getUserApiKey(token, tenantUserId);
    }
}
