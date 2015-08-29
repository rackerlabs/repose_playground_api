package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.JSONPObject;
import helpers.Helpers;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import play.libs.F;
import play.libs.F.Function;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import javax.tools.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Application extends Controller {

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

    public F.Promise<Result> componentsByVersion(String id) {
        F.Promise<Result> resultPromise = WS.url(
                "https://maven.research.rackspacecloud.com/content/repositories/releases/org/openrepose/filter-bundle/" +
                        id +
                        "/filter-bundle-" + id + ".pom").get().map(
                new Function<WSResponse, Result>() {
                    @Override
                    public Result apply(WSResponse wsResponse) throws Throwable {
                        List<String> componentList = new ArrayList<String>();
                        ObjectMapper mapper = new ObjectMapper();
                        Document document = wsResponse.asXml();
                        NodeList nodeList = document.getElementsByTagName("dependency");
                        for (int i = 0; i < nodeList.getLength(); i++) {
                            Node node = nodeList.item(i);
                            if (node.getNodeType() == Node.ELEMENT_NODE) {
                                // do something with the current element
                                for(int j = 0; j < node.getChildNodes().getLength(); j++){
                                    Node artifactId = node.getChildNodes().item(j);
                                    if(artifactId.getNodeName() == "artifactId"){
                                        componentList.add(artifactId.getTextContent());
                                    }
                                }
                            }
                        }
                        return ok((JsonNode)mapper.valueToTree(componentList));
                    }
                }
        );
        return resultPromise;
    }

    /**
     * Supported are:
     * - ip-identity
     * - uri-normalization
     * - content-normalization
     * - header-normalization
     * - header-identity
     * - header-id-mapping
     * - uri-identity
     * - destination-router
     * - uri-stripper
     * @param id
     * @param componentId
     * @return
     */
    public F.Promise<Result> component(String id, String componentId) {
        F.Promise<Result> resultPromise = WS.url(
                "https://raw.githubusercontent.com/rackerlabs/repose/repose-" +
                        id +"/repose-aggregator/components/filters/" +
                        componentId + "/src/main/resources/META-INF/schema/config/bindings.xjb").get().flatMap(
                new Function<WSResponse, F.Promise<Result>>() {
                    @Override
                    public F.Promise<Result> apply(WSResponse wsResponse) throws Throwable {
                        List<String> componentList = new ArrayList<String>();
                        Document document = wsResponse.asXml();
                        NodeList nodeList = document.getElementsByTagName("bindings");
                        String schemaLocation = "https://raw.githubusercontent.com/rackerlabs/repose/repose-" +
                                id + "/repose-aggregator/components/filters/" + componentId +
                                "/src/main/resources/META-INF/schema/config/" + nodeList.
                                item(0).getAttributes().
                                getNamedItem("schemaLocation").getTextContent();

                        return WS.url(
                                "https://raw.githubusercontent.com/rackerlabs/repose/repose-" +
                                        id + "/repose-aggregator/components/filters/" + componentId +
                                        "/src/main/resources/META-INF/schema/config/" + schemaLocation).get().map(
                                new Function<WSResponse, Result>() {
                                    @Override
                                    public Result apply(WSResponse innerWsResponse) throws Throwable {
                                        JSONObject parentJson = new JSONObject();
                                        List<String> componentList = new ArrayList<String>();
                                        ObjectMapper mapper = new ObjectMapper();

                                        JSONObject object = Helpers.generateJSONTree(parentJson,
                                                innerWsResponse.asXml());
                                        ObjectNode result = (ObjectNode)Json.parse(object.toString());
                                        return ok(result);
                                    }
                                }
                        );
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

