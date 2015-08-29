package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import helpers.Helpers;
import models.User;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.F;
import play.libs.F.Function;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import play.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Login extends Controller {

    public Result index() {
        Logger.debug("In login controller.  Get login form");
        Form<LoginForm> loginForm= Form.form(LoginForm.class).bindFromRequest();
        Logger.debug("log binding from request");

        if(loginForm.hasErrors()){
            return badRequest(loginForm.errorsAsJson());
        }

        LoginForm form = loginForm.get();
        User user = User.findByNameAndPassword(form.username, form.password);
        if(user == null){
            //find the user in rackspace identity (for now)
            //if found, save the user
            //if not, errorz
            return notFound(buildJsonResponse("failure", "User not found"));
        } else {
            return ok(buildJsonResponse("success", user.username));

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
    }
}


