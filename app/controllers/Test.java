package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.PortBinding;
import exceptions.InternalServerException;
import models.Carina;
import models.Cluster;
import models.User;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;

import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by dimi5963 on 3/1/16.
 */
public class Test extends Controller {

    /***
     * Repose test will do the following:
     * 1. empty current.log, http-debug.log, intra-filter.log
     * 2. make a request based on json to running repose node (get by {id})
     * 3. put response in response json
     * 4. put current.log in current json
     * 5. put http-debug.log in http json
     * 6. put intra-filter.log in intra-filter json
     * 7. return all the jsons
     *
     * @param id container id
     * @return
     */
    @Deprecated
    public Result test(String id){
        Logger.debug("In test controller.  Get test form: " + id);
        JsonNode requestBody = request().body().asJson();
        Logger.debug(request().body().asJson().toString());

        //make a request to our container and get response data
        /**
         * Response data includes:
         * - http-debug.log
         * - intra-filter.log
         * - response info
         */
        String token = request().getHeader("Token");

        User user = User.findByToken(token);


        try {
            return ok(Json.toJson(testReposeInstance(user, id, requestBody)));
        } catch (InternalServerException e) {
            e.printStackTrace();
            return internalServerError(e.getLocalizedMessage());
        }
    }


    /**
     * Test reponse instance with a request
     * @param user user id
     * @param containerId container to test
     * @param requestBody json request to parse and execute
     * @return Json response with request, response, and log data
     * @throws InternalServerException
     */
    private ObjectNode testReposeInstance(User user, String containerId, JsonNode requestBody) throws InternalServerException{
        Logger.debug("test repose instance " + containerId);
        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.put("request", requestBody);

        Cluster cluster = null;
        try {
            cluster = new Carina().getClusterByName(
                    play.Play.application().configuration().getString("user.cluster.name"), user, false);
        } catch (InternalServerException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }

        try {
            final DockerClient docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();
            //clean up logs
            Logger.debug(docker.execStart(
                    executeCommand(containerId, docker,
                            new String[]{"sh", "-c", "cat /dev/null > /var/log/repose/current.log" })).readFully());
            Logger.debug(docker.execStart(
                    executeCommand(containerId, docker,
                            new String[]{"sh", "-c", "cat /dev/null > /var/log/repose/http-debug.log"})).readFully());
            Logger.debug(docker.execStart(
                    executeCommand(containerId, docker,
                            new String[]{"sh", "-c", "cat /dev/null > /var/log/repose/intra-filter.log" })).readFully());
            Logger.debug(docker.execStart(
                    executeCommand(containerId, docker,
                            new String[]{"sh", "-c", "cat /dev/null > /var/log/repose/error.log" })).readFully());
            //make a request
            ContainerInfo containerInfo = docker.inspectContainer(containerId);
            Logger.debug("container info:" + containerInfo);
            Logger.debug("container network port info:" + containerInfo.networkSettings().ports());
            Logger.debug("port stuff: " + containerInfo.networkSettings().ports().get("8080/tcp"));
            PortBinding portBinding = (PortBinding)(containerInfo.networkSettings().ports().get("8080/tcp").get(0));
            Logger.debug("container info:" + portBinding.hostIp());
            WSRequest wsRequest = WS.url(
                    "http://" + portBinding.hostIp() + ":" + portBinding.hostPort() + requestBody.get("url").asText())
                    .setMethod(requestBody.get("method").asText());
            ArrayNode requestHeaders = (ArrayNode)requestBody.get("headers");
            requestHeaders.forEach(request -> {
                if(!request.get("name").asText().isEmpty())
                    wsRequest.setHeader(request.get("name").asText(), request.get("value").asText());
            });

            F.Promise<ObjectNode> resultPromise = wsRequest.execute().map(
                    new F.Function<WSResponse, ObjectNode>() {
                        @Override
                        public ObjectNode apply(WSResponse wsResponse) throws Throwable {
                            ObjectNode responseNode = response.objectNode();

                            responseNode.put("url", wsResponse.getUri().toString());
                            responseNode.put("responseBody", wsResponse.getBody());
                            responseNode.put("responseHeaders",
                                    Json.toJson(wsResponse.getAllHeaders()));
                            responseNode.put("responseStatus", wsResponse.getStatus());
                            responseNode.put("responseStatusText", wsResponse.getStatusText());
                            return responseNode;

                        }
                    }
            );

            ObjectNode responseNode = resultPromise.get(30000);

            //get the logs out
            //this will get split up into connection pool and request/response messages
            List<String> httpDebugLogList = Arrays.asList(docker.execStart(
                    executeCommand(containerId, docker,
                            new String[]{"cat", "/var/log/repose/http-debug.log"})).readFully().split("\n"));
            Map<String, ?> debugMessageMap = new HashMap<String, Object>(){
                {
                    put("poolMessages", new ArrayList<String>());
                    put("externalRequests", new ArrayList<Map<String, List<String>>>());
                }
            };
            boolean requestMessageStarted = false;
            boolean responseMessageStarted = false;
            for(String entry: httpDebugLogList) {
                //check if connection pool entry
                Logger.info("Entry: " + entry);
                if(entry.contains("org.apache.http.impl.conn.PoolingClientConnectionManager")){
                    ((List<String>)debugMessageMap.get("poolMessages")).add(entry);
                }
                //check for request
                if(entry.contains("org.apache.http.wire -  >>")){
                    //set response message to false so that we don't log multiple messages in response log
                    responseMessageStarted = false;
                    if(!requestMessageStarted) {
                        //start message logging
                        ((List<Map<String, List<String>>>) debugMessageMap.get("externalRequests")).add(
                                new HashMap<String, List<String>>(){
                                    {
                                        put(
                                                "request",
                                                new ArrayList<String>() {
                                                    {
                                                        add(entry.substring(
                                                                entry.indexOf("org.apache.http.wire -  >>") +
                                                                        "org.apache.http.wire -  ".length()));
                                                    }
                                                }
                                        );
                                    }
                                });
                        requestMessageStarted = true;
                    } else {
                        //append
                        int externalRequestsSize =
                                ((List<Map<String, List<String>>>)debugMessageMap.get("externalRequests")).size();
                        Map<String,List<String>> requestResponseLogs =
                                ((List<Map<String, List<String>>>)debugMessageMap.get("externalRequests")).
                                        get(externalRequestsSize - 1);
                        requestResponseLogs.get("request").add(
                                entry.substring(entry.indexOf("org.apache.http.wire -  >>") +
                                        "org.apache.http.wire -  ".length()));

                        //Logger.info("request logs entry: " + requestLogs.size() + " and last entry: " + requestLogs.get(requestLogs.size() - 1));
                        //String request = requestLogs.get(requestLogs.size() -1).concat("\n").concat(entry);
                        //requestLogs.set(requestLogs.size() - 1, request);
                        //((Map<String, List<String>>) debugMessageMap.get("externalRequests")).put("request", requestLogs);
                    }
                }
                //check for response
                if(entry.contains("org.apache.http.wire -  <<")){
                    //is there a request message in progress?
                    if(!responseMessageStarted) {
                        if (requestMessageStarted) {
                            //yes so set it to stop so that we don't log multiple messages in the request log
                            requestMessageStarted = false;
                        }
                        int externalRequestsSize =
                                ((List<Map<String, List<String>>>)debugMessageMap.get("externalRequests")).size();
                        Map<String,List<String>> requestResponseLogs =
                                ((List<Map<String, List<String>>>)debugMessageMap.get("externalRequests")).
                                        get(externalRequestsSize - 1);
                        requestResponseLogs.put("response",new ArrayList<String>() {
                            {
                                add(entry.substring(
                                        entry.indexOf("org.apache.http.wire -  <<") +
                                                "org.apache.http.wire -  ".length()));
                            }
                        });

                        responseMessageStarted = true;
                    } else {
                        //append
                        int externalRequestsSize =
                                ((List<Map<String, List<String>>>)debugMessageMap.get("externalRequests")).size();
                        Map<String,List<String>> requestResponseLogs =
                                ((List<Map<String, List<String>>>)debugMessageMap.get("externalRequests")).
                                        get(externalRequestsSize - 1);
                        requestResponseLogs.get("response").add(
                                entry.substring(entry.indexOf("org.apache.http.wire -  <<") +
                                        "org.apache.http.wire -  ".length()));

                    }
                }
            }


            //if line containers ERROR, start a new entry
            List<String> errorLogList = Arrays.asList(docker.execStart(
                    executeCommand(containerId, docker,
                            new String[]{"cat", "/var/log/repose/error.log"})).readFully().split(Pattern.quote("ERROR")));
            List<JsonNode> intraFilterLogList = Arrays.asList(docker.execStart(
                    executeCommand(containerId, docker,
                            new String[]{"cat", "/var/log/repose/intra-filter.log"})).readFully().split("\n")).
                    stream().map(entry -> Json.parse(entry.split(Pattern.quote("TRACE intrafilter-logging - "))[1])).collect(Collectors.toList());


            response.put("current", docker.execStart(
                    executeCommand(containerId, docker,
                            new String[]{"cat", "/var/log/repose/current.log"})).readFully());
            response.put("error", Json.toJson(errorLogList));
            response.put("http-debug", Json.toJson(debugMessageMap));
            response.put("intra-filter", Json.toJson(intraFilterLogList));


            Logger.debug("response: " + responseNode);
            response.put("response", responseNode);
            Logger.debug("response: " + response);
            return response;
        } catch (DockerCertificateException | DockerException | InterruptedException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }
    }


    private String executeCommand(String containerId, DockerClient docker, String[] command) throws DockerException, InterruptedException{
        return docker.execCreate(
                containerId, command,
                DockerClient.ExecCreateParam.attachStdout(),
                DockerClient.ExecCreateParam.attachStderr());
    }

}
