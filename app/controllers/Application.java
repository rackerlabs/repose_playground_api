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

import java.io.*;
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

        BufferedWriter output = null;
        try {
            try{
                String tempFile = "Dockerfile";
                //Override the Dockerfile
                File file = new File(tempFile);
                output = new BufferedWriter(new FileWriter(file));
                output.write("# Dockerfile for Repose (www.openrepose.org)\n");
                output.append("\n");
                output.append("FROM ubuntu\n");
                output.append("\n");
                output.append("MAINTAINER Jenny Vo (jenny.vo@rackspace.com)\n");
                output.append("\n");
                output.append("ENV REPOSE_VER "+id+"\n");
                output.append("RUN apt-get install -y wget\n");
                output.append("RUN wget -O - http://repo.openrepose.org/debian/pubkey.gpg | apt-key add - && echo \"deb http://repo.openrepose.org/debian stable main\" > /etc/apt/sources.list.d/openrepose.list\n");
                output.append("RUN apt-get update && apt-get install -y repose-valve=${REPOSE_VER} repose-filter-bundle=${REPOSE_VER} repose-extensions-filter-bundle=${REPOSE_VER}\n");
                output.append("\n");
                output.append("# Remove default Repose configuration files\n");
                output.append("RUN rm /etc/repose/*.cfg.xml\n");
                output.append("\n");
                output.append("# Copy our configuration files in.\n");
                output.append("ADD ./repose_configs/*.cfg.xml /etc/repose/\n");
                output.append("\n");
                output.append("# Expose Port 8000 -- Change this to use other ports for Repose\n");
                output.append("EXPOSE 8000\n");
                output.append("\n");
                output.append("# Start Repose\n");
                output.append("CMD java -jar /usr/share/repose/repose-valve.jar\n");
                output.close();

            }catch(Exception e){
                // if any error occurs
                e.printStackTrace();
            }

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
        } catch (Exception ioe){
            return internalServerError(results);
        }

    }

}

