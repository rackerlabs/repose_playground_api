package services;

import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import exceptions.UnauthorizedException;
import models.User;

/**
 * Created by dimi5963 on 2/28/16.
 */
@ImplementedBy(AuthService.class)
public interface IAuthService {
    User getUser(String username, String password) throws UnauthorizedException, InternalServerException;
    String getUserApiKey(String token, String tenantUserId) throws InternalServerException, UnauthorizedException;
}
