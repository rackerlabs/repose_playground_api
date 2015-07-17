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
}
