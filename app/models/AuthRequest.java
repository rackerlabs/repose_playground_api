package models;

/**
 * Created by dimi5963 on 8/29/15.
 */
public class AuthRequest {
    private PasswordCredsRequest passwordCredentials;

    public AuthRequest(PasswordCredsRequest passwordCredentials){
        this.passwordCredentials = passwordCredentials;
    }

    public PasswordCredsRequest getPasswordCredentials() {
        return passwordCredentials;
    }

    public void setPasswordCredentials(PasswordCredsRequest passwordCredentials) {
        this.passwordCredentials = passwordCredentials;
    }
}
