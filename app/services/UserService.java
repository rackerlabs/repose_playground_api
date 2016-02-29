package services;

import com.google.inject.Inject;
import models.User;
import repositories.IUserRepository;

/**
 * Created by dimi5963 on 2/28/16.
 */
public class UserService implements IUserService{
    private final IUserRepository IUserRepository;

    @Inject
    public UserService(IUserRepository IUserRepository) {
        this.IUserRepository = IUserRepository;
    }

    public boolean isValid(String token) {
        User user = findByToken(token);
        return user != null && user.expireDate.isAfterNow();
    }

    @Override
    public User findByToken(String token) {
        return IUserRepository.findByToken(token);
    }
}
