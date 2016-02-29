package repositories;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.ImplementedBy;
import models.User;

/**
 * Created by dimi5963 on 2/28/16.
 */
@ImplementedBy(UserRepository.class)
public interface IUserRepository {
    User findByToken(String token);

    User findByNameAndPasswordCurrent(String username, String password);

    User findByNameAndPassword(String username, String password);

    User saveUser(JsonNode userData, String username, String password);
}
