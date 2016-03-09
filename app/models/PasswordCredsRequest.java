package models;

/**
 * Created by dimi5963 on 8/29/15.
 */
public class PasswordCredsRequest {
    private String username;
    private String password;

    public PasswordCredsRequest(String username, String password){
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
