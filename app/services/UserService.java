package services;

import com.google.inject.Inject;
import models.User;
import repositories.UserRepository;

/**
 * Created by dimi5963 on 2/28/16.
 */
public class UserService implements IUserService{
    private final UserRepository userRepository;

    @Inject
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isValid(String token) {
        return userRepository.isValid(token);
    }

    @Override
    public User findByToken(String token) {
        return userRepository.findByToken(token);
    }
}
