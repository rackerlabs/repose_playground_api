package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.PortBinding;
import exceptions.InternalServerException;
import exceptions.NotFoundException;
import models.Carina;
import models.Cluster;
import models.Region;
import models.User;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.*;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Repose extends Controller {

    private static Region region = Region.DFW;

    /***
     * Repose list will return a list of running and stopped repose instances
     * Currently, repose instances are created per tenant and version (e.g. 1-7.1, 1-7.2, etc.)
     * @implSpec The workflow is:
     *           1. Retrieve the token from a request
     *           2. Retrieve the user for the token
     *           3. Download cluster zip and put it into /tmp/{tenant}/ directory
     *           4. Parse docker.env and get the DOCKER_HOST location
     *           5. Use docker client to retrieve all running repose containers
     * @return
     */
    public Result list() {

        String token = request().getHeader("Token");
        Logger.info("Check the user for " + token);
        Logger.info("The return user is " + User.findByToken(token));
        //check if expired
        if(!User.isValid(token))
            return unauthorized();
        else
        {
            //get user by token.
            User user = User.findByToken(token);

            List<Container> reposeNodes = null;
            try{
                reposeNodes = getReposeInstances(user);

                final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
                ArrayNode arrayNode = nodeFactory.arrayNode();

                if(reposeNodes != null) {
                    reposeNodes.forEach(container -> {
                                container.names().forEach(name -> Logger.info("Specific name: " + name));
                                String[] containerNameTokens = String.join("", container.names()).split(Pattern.quote("/"));
                                String containerName = containerNameTokens[containerNameTokens.length - 1];
                                Logger.info("Name: " + containerName);
                                if(containerName.contains("repose-" + user.tenant)) {
                                    ObjectNode child = nodeFactory.objectNode();
                                    if (container.status().trim().startsWith("Up"))
                                        child.put("status", "Running");
                                    else
                                        child.put("status", "Stopped");
                                    String reposeName = String.join(" ", container.names());
                                    String[] reposeNames = reposeName.split(Pattern.quote("/"));

                                    child.put("repose_name", reposeNames[reposeNames.length - 1]);
                                    child.put("message", container.status());
                                    child.put("version", reposeNames[reposeNames.length - 1].split(Pattern.quote("-"))[2]);
                                    child.put("id", container.id());
                                    arrayNode.add(child);
                                }
                            }
                    );
                }

                return ok(Json.toJson(arrayNode));
            } catch(InternalServerException | DockerException | InterruptedException | UnsupportedEncodingException ise) {
                return internalServerError(ise.getLocalizedMessage());
            }
        }
    }

    /***
     * Repose stop will stop the running instance of repose and all of its linked containers
     * @param id id of the container
     * @return
     */
    public Result stop(String id) {
        Logger.debug("In stop controller");

        String token = request().getHeader("Token");
        Logger.info("Check the user for " + token);
        Logger.info("The return user is " + User.findByToken(token));
        //check if expired
        if(!User.isValid(token))
            return unauthorized();
        else
        {
            //get user by token.
            User user = User.findByToken(token);

            try{
                stopReposeInstance(user, id);

                return ok(Json.toJson("{'message':'success'}"));
            } catch(InternalServerException ise) {
                return internalServerError(ise.getLocalizedMessage());
            }
        }
    }

    /***
     * Repose start will start the running instance of repose and all of its linked containers
     * @param id id of the container
     * @return
     */
    public Result start(String id) {
        Logger.debug("In start controller");

        String token = request().getHeader("Token");
        Logger.info("Check the user for " + token);
        Logger.info("The return user is " + User.findByToken(token));
        //check if expired
        if(!User.isValid(token))
            return unauthorized();
        else
        {
            //get user by token.
            User user = User.findByToken(token);

            try{
                startReposeInstance(user, id);

                return ok(Json.toJson("{'message':'success'}"));
            } catch(InternalServerException ise) {
                return internalServerError(ise.getLocalizedMessage());
            }
        }
    }

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
     * Retrieve configurations for repose id
     *
     * Retrieval is done in xml form, which can then be either zipped up or sent across wrapped in json object
     * @param id
     * @return
     */
    public Result configurations(String id){
        Logger.debug("In configurations controller");

        String token = request().getHeader("Token");
        Logger.info("Check the user for " + token);
        Logger.info("The return user is " + User.findByToken(token));
        //check if expired
        if(!User.isValid(token))
            return unauthorized();
        else
        {
            //get user by token.
            User user = User.findByToken(token);

            try{
                return ok(getConfigurationsForInstance(user, id));
            } catch(InternalServerException ise) {
                return internalServerError(ise.getLocalizedMessage());
            }
        }
    }

    /**
     * Upload configurations for repose id and start repose instance
     *
     * Retrieval is done in xml form, which can then be either zipped up or sent across wrapped in json object
     * @param id
     * @return
     */
    public Result uploadReposeConfigs(String id){
        Logger.debug("In upload repose configs controller");

        Http.MultipartFormData body = request().body().asMultipartFormData();

        String token = request().getHeader("Token");
        Logger.info("Check the user for " + token);
        Logger.info("The return user is " + User.findByToken(token));
        //check if expired
        if(!User.isValid(token))
            return unauthorized();
        else
        {
            //get user by token.
            User user = User.findByToken(token);

            Logger.info("Create new docker instance");
            try {
                new models.Container().createOriginContainer(user, id);
                String reposeId = new models.Container().createReposeContainer(user, uploadConfigurations(user, id, body), id);
                return ok(Json.parse("{\"message\": \"success\",\"id\": \"" + reposeId + "\"}"));
            } catch (NotFoundException e) {
                return badRequest(e.getLocalizedMessage());
            } catch (InternalServerException e) {
                return internalServerError(e.getLocalizedMessage());
            }

        }
    }

    private Map<String, String> uploadConfigurations(User user, String reposeId, Http.MultipartFormData body)
            throws NotFoundException {
        if(body.getFiles() != null && body.getFiles().size() > 0) {
            //get the first one.  others don't matter since it's a single file upload
            Http.MultipartFormData.FilePart reposeZip = body.getFiles().get(0);
            Logger.info("get file for: " + reposeZip.getFile().getAbsolutePath());
            //TODO: get filter map and update system-model destination, replace log4j, and container.cfg.xml
            return unzip(reposeZip.getFile());
        }

        throw new exceptions.NotFoundException("No zip files");
    }


    /***
     * Get cluster by name (that saves to /tmp/tenant)
     * @param user
     */
    private List<Container> getReposeInstances(User user) throws InternalServerException, DockerException, InterruptedException, UnsupportedEncodingException {
        Logger.debug("Get repose instances");

        Cluster cluster = null;
        try {
            cluster = new Carina().getClusterByName(
                    play.Play.application().configuration().getString("user.cluster.name"), user, false, true);
        } catch (InternalServerException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }

        try {
            final DockerClient docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();
            return docker.listContainers(DockerClient.ListContainersParam.allContainers());
        } catch (DockerCertificateException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }
    }

    /***
     * Stop repose instance based on user and container id.
     * Get cluster first and then use its creds to stop the container
     * @param user
     * @param containerId
     * @throws InternalServerException
     */
    private void stopReposeInstance(User user, String containerId) throws InternalServerException{
        Logger.debug("Stop repose instance " + containerId);

        Cluster cluster = null;
        try {
            cluster = new Carina().getClusterByName(
                    play.Play.application().configuration().getString("user.cluster.name"), user, false, false);
        } catch (InternalServerException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }

        try {
            final DockerClient docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();
            docker.stopContainer(containerId, 5);
        } catch (DockerCertificateException | DockerException | InterruptedException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }
    }

    /***
     * Start repose instance based on user and container id.
     * Get cluster first and then use its creds to start the container
     * @param user
     * @param containerId
     * @throws InternalServerException
     */
    private void startReposeInstance(User user, String containerId) throws InternalServerException{
        Logger.debug("Start repose instance " + containerId);

        Cluster cluster = null;
        try {
            cluster = new Carina().getClusterByName(
                    play.Play.application().configuration().getString("user.cluster.name"), user, false, true);
        } catch (InternalServerException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }

        try {
            final DockerClient docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();
            docker.startContainer(containerId);
        } catch (DockerCertificateException | DockerException | InterruptedException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }
    }

    /***
     * Start repose instance based on user and container id.
     * Get cluster first and then use its creds to start the container
     * @param user
     * @param containerId
     * @throws InternalServerException
     */
    private ObjectNode getConfigurationsForInstance(User user, String containerId) throws InternalServerException{
        Logger.debug("Get configurations for " + containerId);
        ObjectNode response = JsonNodeFactory.instance.objectNode();
        List<ObjectNode> configurationList = new ArrayList<>();
        Cluster cluster = null;
        try {
            cluster = new Carina().getClusterByName(
                    play.Play.application().configuration().getString("user.cluster.name"), user, false, true);
        } catch (InternalServerException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }

        try {
            final DockerClient docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();
            List<String> xmlStringList = Arrays.asList(docker.execStart(
                    executeCommand(containerId, docker,
                            new String[]{"sh", "-c", "ls *.xml"})).readFully().split("\n"));
            xmlStringList.forEach(filter -> {
                        try {
                            ObjectNode child = response.objectNode();
                            child.put("name", filter);
                            child.put("xml", docker.execStart(
                                    executeCommand(containerId, docker,
                                            new String[]{"sh", "-c", "cat " + filter})).readFully());
                            configurationList.add(child);
                        } catch (DockerException | InterruptedException e) {
                            e.printStackTrace();
                            Logger.error("Unable to retrieve configuration: " + filter);
                        }
                    }
            );
        } catch (DockerCertificateException | DockerException | InterruptedException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }

        response.put("configs", Json.toJson(configurationList));

        return response;
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
                    play.Play.application().configuration().getString("user.cluster.name"), user, false, false);
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

    private Map<String, String> unzip(File zippedFile) {
        Logger.info("unzip " + zippedFile.getName());
        Map<String, String> filterXml = new HashMap<String, String>();
        try {
            InputStream inputStream = new FileInputStream(zippedFile);
            ZipInputStream zis = new ZipInputStream(inputStream);
            ZipEntry zipEntry;
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((zipEntry = zis.getNextEntry())!= null) {
                StringBuilder s = new StringBuilder();
                Logger.info("read " + zipEntry.getName());
                while ((read = zis.read(buffer, 0, 1024)) >= 0) {
                    s.append(new String(buffer, 0, read));
                }
                String[] zipEntryTokens = zipEntry.getName().split(Pattern.quote("/"));
                filterXml.put(zipEntryTokens[zipEntryTokens.length - 1], s.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filterXml;
    }
}


