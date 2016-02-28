package repositories;

import com.google.inject.ImplementedBy;
import models.User;

/**
 * Created by dimi5963 on 2/28/16.
 */
@ImplementedBy(UserRepositoryEbean.class)
public interface UserRepository {
    boolean isValid(String token);
    User findByToken(String token);
}
