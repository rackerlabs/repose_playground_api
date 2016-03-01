package controllers;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spotify.docker.client.*;
import exceptions.InternalServerException;
import exceptions.NotFoundException;
import helpers.Helpers;
import models.Carina;
import models.Cluster;
import models.User;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by dimi5963 on 3/1/16.
 */
public class Configuration extends Controller {

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


    private Map<String, String> uploadConfigurations(User user, String reposeVersion, Http.MultipartFormData body)
            throws NotFoundException {
        if(body.getFiles() != null && body.getFiles().size() > 0) {
            int majorVersion = Integer.parseInt(reposeVersion.split(Pattern.quote("."))[0]);
            //get the first one.  others don't matter since it's a single file upload
            Http.MultipartFormData.FilePart reposeZip = body.getFiles().get(0);
            Logger.info("get file for: " + reposeZip.getFile().getAbsolutePath());

            //TODO: get filter map and update system-model destination, replace log4j, and container.cfg.xml
            Map<String, String> filterXml = unzip(reposeZip.getFile());
            filterXml.keySet().forEach(name -> {
                if(name.equals("system-model.cfg.xml")){
                    Logger.info("update system model listening node and destination");
                    String content = filterXml.get(name);
                    filterXml.put(name, Helpers.updateSystemModelXml(user, reposeVersion, content));
                } else if (name.equals("container.cfg.xml")) {
                    Logger.info("update container config");
                    filterXml.put(name, Helpers.generateContainerXml(majorVersion));
                } else if (name.equals("log4j2.xml") || name.equals("log4j.properties")){
                    filterXml.put(name, Helpers.generateLoggingXml(majorVersion));
                }
            });

            return  filterXml;
        }

        throw new exceptions.NotFoundException("No zip files");
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
