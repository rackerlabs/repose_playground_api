package clients;

import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import exceptions.UnauthorizedException;
import models.LoginRequest;
import models.User;

/**
 * Created by dimi5963 on 2/29/16.
 */
@ImplementedBy(IdentityClient.class)
public interface IIdentityClient {
    User getUser(LoginRequest loginRequest) throws UnauthorizedException, InternalServerException;

    String getUserApiKey(String token, String tenantUserId) throws UnauthorizedException, InternalServerException;
}
