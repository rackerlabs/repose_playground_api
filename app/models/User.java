package models;

import com.avaje.ebean.Model;
import org.joda.time.DateTime;
import play.data.validation.Constraints;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Column;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by dimi5963 on 8/25/15.
 */
@Entity
public class User extends Model {
    @Id
    public Long id;

    @Column(length = 255, unique = true, nullable = false)
    @Constraints.MaxLength(255)
    @Constraints.Required
    @Constraints.MinLength(4)
    public String username;

    @Column(length = 64, nullable = false)
    private byte[] shaPassword;

    @Column(nullable = false)
    public String token;

    @Column(nullable = true)
    public String tenant;

    @Column(nullable = false)
    public DateTime expireDate;

    public void setPassword(String password) {
        this.shaPassword = getSha512(password);
    }

    public void setUsername(String username) {
        this.username = username.toLowerCase();
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setExpireDate(DateTime expireDate){
        this.expireDate = expireDate;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public static final Finder<Long, User> find = new Finder<Long, User>(
            Long.class, User.class);

    public static User findByNameAndPassword(String username, String password) {
        return find
                .where()
                .eq("username", username.toLowerCase())
                .eq("shaPassword", getSha512(password))
                .findUnique();
    }

    public static User findByName(String username) {
        return find
                .where()
                .eq("username", username.toLowerCase())
                .findUnique();
    }

    public static User findByToken(String token) {
        return find
                .where()
                .eq("token", token)
                .findUnique();
    }

    public static boolean isValid(String token){
        User user = findByToken(token);
        return user != null && user.expireDate.isAfterNow();
    }

    public static byte[] getSha512(String value) {
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
