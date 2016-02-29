package controllers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import exceptions.UnauthorizedException;
import models.User;
import play.Logger;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.IAuthService;
import services.IUserService;

import java.util.ArrayList;
import java.util.List;

public class Login extends Controller {

    private final IUserService userService;
    private final IAuthService authService;

    @Inject
    public Login(IUserService userService, IAuthService authService){

        this.userService = userService;
        this.authService = authService;
    }

    public Result index() {
        Logger.debug("Retrieve the user for " + request().getHeader("Token"));
        String token = request().getHeader("Token");
        Logger.debug("The return user is " +
                userService.findByToken(token));
        //check if expired
        if(!userService.isValid(token)) {
            Logger.warn("User is unauthorized");
            return unauthorized();
        } else {
            User user = userService.findByToken(token);
            if(user != null) {
                Logger.debug("User is authorized: " + user.toString() + " for " + token);
                return ok(Json.toJson(user));
            } else {
                Logger.debug("The only way this could have happened is if token timeout between " +
                        "previous check and now.  Unlikely but possible.");
                return unauthorized();
            }
        }
    }

    public Result create() {
        Logger.debug("In login controller.");
        Form<LoginForm> loginForm= Form.form(LoginForm.class).bindFromRequest();
        Logger.debug("log binding from request: " + loginForm.toString());

        if(loginForm.hasErrors()){
            //Logger.debug("Errors: " + loginForm.errorsAsJson());
            //errorsAsJson throws exceptiosn during unit tests.  I like unit tests so let's build our own json
            //{'key':['message[0]->args[0]']}
            ObjectNode response = JsonNodeFactory.instance.objectNode();
            loginForm.errors().forEach((key, errors) -> {
                ArrayNode errorList = response.objectNode().arrayNode();
                errors.forEach(error -> {
                    if(error.arguments().size() > 0){
                        List<String> argList = new ArrayList<String>();
                        error.arguments().forEach(argument -> argList.add(argument.toString()));
                        errorList.add(error.message().substring("error.".length()) + "->" + String.join(",", argList));
                    } else {
                        errorList.add(error.message().substring("error.".length()));
                    }

                });
                response.putPOJO(key, errorList);
            });
            return badRequest(Json.toJson(response));
        }

        LoginForm form = loginForm.get();
        try{
            return ok(Json.toJson(authService.getUser(form.username, form.password)));
        } catch(UnauthorizedException nfe) {
            ObjectNode response = JsonNodeFactory.instance.objectNode();
            response.put("message", nfe.getLocalizedMessage());
            return unauthorized(Json.toJson(response));
        } catch(InternalServerException ise) {
            ObjectNode response = JsonNodeFactory.instance.objectNode();
            response.put("message", ise.getLocalizedMessage());
            return internalServerError(Json.toJson(response));
        }
    }

    public static class LoginForm {
        @Constraints.Required
        @Constraints.MinLength(3)
        @Constraints.MaxLength(20)
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


