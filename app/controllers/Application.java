package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import play.libs.F;
import play.libs.F.Function;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Application extends Controller {

    public Result index() {
        return ok(index.render("Your new application is ready."));
    }

    public F.Promise<Result> versions() {
        F.Promise<Result> resultPromise = WS.url("https://api.github.com/repos/rackerlabs/repose/tags").get().map(
                new Function<WSResponse, Result>(){
                    public Result apply(WSResponse response){
                        Iterator<JsonNode> result = response.asJson().elements();
                        ObjectMapper mapper = new ObjectMapper();
                        List<String> tagList = new ArrayList<String>();
                        while(result.hasNext()){
                            tagList.add(result.next().findValue("name").textValue().replaceAll("repose-", "").replaceAll("papi-", ""));
                        }
                        JsonNode results = mapper.valueToTree(tagList);
                        return ok(results);
                    }
                }
        );
        return resultPromise;
    }

    public Result build(String id)  {
        String results = "";
        try {
            Process proc = Runtime.getRuntime().exec("docker build -t repose_img_1 .");
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            // read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                results += s;
            }

            // read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                results += "Error: " + s;
            }
            //return ok(results);
            try {
                Process proc2 = Runtime.getRuntime().exec("docker run -d -p 80:8000 -t repose_img_1");
                BufferedReader stdInput2 = new BufferedReader(new
                        InputStreamReader(proc2.getInputStream()));

                BufferedReader stdError2 = new BufferedReader(new
                        InputStreamReader(proc2.getErrorStream()));

                // read the output from the command
                System.out.println("Here is the standard output of the command:\n");
                String s2 = null;
                while ((s2 = stdInput2.readLine()) != null) {
                    results += s2;
                }

                // read any errors from the attempted command
                System.out.println("Here is the standard error of the command (if any):\n");
                while ((s2 = stdError2.readLine()) != null) {
                    results += "Error 2: " + s2;
                }
                return ok(results);

            } catch (Exception e1){
                return internalServerError(results);
            }
        } catch (IOException ioe){
            return internalServerError(results);
        }
    }

}

