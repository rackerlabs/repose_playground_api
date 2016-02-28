package repositories;

import models.User;

/**
 * Created by dimi5963 on 2/28/16.
 */
public class UserRepositoryEbean implements  UserRepository{

    @Override
    public boolean isValid(String token) {
        User user = findByToken(token);
        return user != null && user.expireDate.isAfterNow();
    }

    @Override
    public User findByToken(String token) {
        return User.
                find
                .where()
                .eq("token", token)
                .findUnique();
    }
}
