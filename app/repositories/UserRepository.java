package repositories;

import com.fasterxml.jackson.databind.JsonNode;
import models.User;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by dimi5963 on 2/28/16.
 */
public class UserRepository implements IUserRepository {

    @Override
    public User findByToken(String token) {
        return User.
                find
                .where()
                .eq("token", token)
                .findUnique();
    }

    @Override
    public User findByNameAndPasswordCurrent(String username, String password) {
        return User.
                find
                .where()
                .eq("username", username.toLowerCase())
                .eq("shaPassword", getSha512(password))
                .gt("expireDate", DateTime.now())
                .findUnique();
    }

    @Override
    public User findByNameAndPassword(String username, String password) {
        return User.
                find
                .where()
                .eq("username", username.toLowerCase())
                .eq("shaPassword", getSha512(password))
                .findUnique();
    }

    @Override
    public User saveUser(JsonNode userData, String username, String password) {
        DateTimeFormatter fmt = new DateTimeFormatterBuilder().append(
                ISODateTimeFormat.dateHourMinuteSecondMillis())
                .appendLiteral('Z').toFormatter();

        User newUser = findByNameAndPassword(username, password);
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

        return newUser;
    }

    private byte[] getSha512(String value) {
        try {
            return MessageDigest.getInstance("SHA-512").digest(value.getBytes("UTF-8"));
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
