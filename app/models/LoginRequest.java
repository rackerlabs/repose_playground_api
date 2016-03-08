package models;

/**
 * Created by dimi5963 on 8/29/15.
 */
public class LoginRequest {
    private AuthRequest auth;

    public LoginRequest(AuthRequest auth){
        this.auth = auth;
    }

    public AuthRequest getAuth() {
        return auth;
    }
}
