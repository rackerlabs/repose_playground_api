package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.InternalServerException;
import exceptions.NotFoundException;
import helpers.Auth;
import models.User;
import play.Logger;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class Login extends Controller {

    public Result index() {
        String token = request().getHeader("Token");
        Logger.debug("Check the user for " + token);
        Logger.debug("The return user is " + User.findByToken(token));
        //check if expired
        if(!User.isValid(token))
            return unauthorized();
        else
        {
            User user = User.findByToken(token);
            Logger.debug(user.toString() + " for " + token);
            return ok(Json.toJson(user));
        }
    }

    public Result create() {
        Logger.debug("In login controller.  Get login form: " + request().body().toString());
        Form<LoginForm> loginForm= Form.form(LoginForm.class).bindFromRequest();
        Logger.debug("log binding from request");

        if(loginForm.hasErrors()){
            return badRequest(loginForm.errorsAsJson());
        }

        LoginForm form = loginForm.get();
        User user = null;
        try{
            return ok(Json.toJson(new Auth().getUser(form.username, form.password)));
        } catch(NotFoundException nfe) {
            return unauthorized(nfe.getMessage());
        } catch(InternalServerException ise) {
            return internalServerError(ise.getMessage());
        }
    }


    private static ObjectNode buildJsonResponse(String type, String message) {
        ObjectNode wrapper = Json.newObject();
        ObjectNode msg = Json.newObject();
        msg.put("message", message);
        wrapper.put(type, msg);
        return wrapper;
    }

    public static class LoginForm {
        @Constraints.Required
        @Constraints.MinLength(3)
        public String username;

        @Constraints.Required
        @Constraints.MinLength(6)
        public String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}


