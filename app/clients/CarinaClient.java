package clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import exceptions.NotFoundException;
import factories.IClusterFactory;
import helpers.Helpers;
import models.CarinaRequest;
import models.Cluster;
import models.User;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import repositories.IClusterRepository;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class CarinaClient implements ICarinaClient {

    private final IClusterFactory clusterFactory;
    private final IClusterRepository clusterRepository;


    @Inject
    public CarinaClient(IClusterFactory clusterFactory, IClusterRepository clusterRepository){
        this.clusterFactory = clusterFactory;
        this.clusterRepository = clusterRepository;
    }

    @Override
    public boolean createCluster(String clusterName, User user) throws InternalServerException, InterruptedException {
        Logger.debug("Create cluster " + clusterName + " with " + user.token);
        CarinaRequest carinaRequest = new CarinaRequest(clusterName, false, user.username);

        F.Promise<String> statusPromise = WS.url(clusterFactory.getCarinaUserUrl(user.username))
                .setHeader("x-auth-token", user.token)
                .setHeader("x-content-type", "application/json")
                .post(Json.toJson(carinaRequest)).map(
                        new F.Function<WSResponse, String>() {
                            @Override
                            public String apply(WSResponse wsResponse) throws Throwable {
                                Logger.debug("response from cluster create: " +
                                        wsResponse.getStatus() + " " + wsResponse.getStatusText());

                                Logger.debug("response body: " + wsResponse.getBody());
                                Logger.debug("Keep checking until we get to success (or error) state");

                                return wsResponse.asJson().get("status").asText();

                            }
                        }
                ).recover(
                        new F.Function<Throwable, String>() {
                            //Everything is down!
                            @Override
                            public String apply(Throwable throwable) throws Throwable {
                                throw new InternalServerException(
                                        "{'message': 'We are currently experiencing difficulties.  " +
                                                "Please try again later.'}");
                            }
                        }
                );

        String status = statusPromise.get(30000);
        while(!status.equals("active") && !status.equals("error")){
            JsonNode statusNode = new helpers.Carina().getCluster(clusterName, user);
            if(statusNode != null) {
                status = statusNode.asText();
            }else if (status.equals("error")){
                throw new InternalServerException("Cluster ended up in error state");
            }else {
                throw new InternalServerException("Unable to get status");
            }
            Thread.sleep(1000);
        }
        return true;
    }

    @Override
    public Cluster getClusterWithZip(String url, User user, String clusterName, boolean isAdmin)
            throws NotFoundException, InternalServerException{
        Logger.debug("Get cluster zip from " + url + " for " + clusterName);
        return WS.url(url)
                .setHeader("x-auth-token", user.token)
                .setHeader("x-content-type", "application/json")
                .get().map(new F.Function<WSResponse, Cluster>() {
                    public Cluster apply(WSResponse response) throws Throwable {
                        Logger.debug("getClusterZip::response for " + user);
                        Logger.debug("getClusterZip::body: " + response.getStatus() + " " + response.getBody());
                        switch (response.getStatus()) {
                            case 201:
                                JsonNode zipResponse = response.asJson();
                                return WS.url(
                                        zipResponse.get("zip_url").asText().replace("\"", ""))
                                        .setHeader("x-auth-token", user.token)
                                        .setHeader("Accept", "application/zip")
                                        .setHeader("Content-disposition", "attachment; filename=" + clusterName + ".zip")
                                        .get().map(
                                                new F.Function<WSResponse, Cluster>() {
                                                    @Override
                                                    public Cluster apply(WSResponse innerResponse) throws Throwable {
                                                        Logger.debug("Response from zip call: " + innerResponse.getStatus());
                                                        switch (innerResponse.getStatus()) {
                                                            case 200:
                                                                try {
                                                                    return unzip(
                                                                            innerResponse.getBodyAsStream(),
                                                                            clusterName, user, isAdmin);
                                                                } catch (Exception e) {
                                                                    e.printStackTrace();
                                                                    Logger.error(e.getMessage());
                                                                }
                                                            default:
                                                                throw new InternalServerException("Could not retrieve cluster zip.");
                                                        }
                                                    }
                                                }
                                        ).recover(
                                                new F.Function<Throwable, Cluster>() {
                                                    //no cluster by this name exists.  Empty all the things
                                                    @Override
                                                    public Cluster apply(Throwable throwable) throws Throwable {
                                                        throw throwable;
                                                    }
                                                }
                                        ).get(30000);
                            case 404:
                                throw new NotFoundException("Cluster zip does not exist.");
                            default:
                                throw new InternalServerException("Could not retrieve cluster zip.");
                        }
                    }
                }).recover(
                        new F.Function<Throwable, Cluster>() {
                            //no cluster by this name exists.  Empty all the things
                            @Override
                            public Cluster apply(Throwable throwable) throws Throwable {
                                throw throwable;
                            }
                        }
                ).get(30000);
    }

    @Override
    public JsonNode getCluster(String clusterName, User user) throws InternalServerException {
        F.Promise<JsonNode> result = WS.url(clusterFactory.getCarinaClusterUrl(user.username, clusterName))
                .setHeader("x-auth-token", user.token)
                .get().map(new F.Function<WSResponse, JsonNode>() {
                    @Override
                    public JsonNode apply(WSResponse response) throws Throwable {
                        Logger.debug("response for " + user);
                        Logger.debug("body: " + response.getStatus() + " " + response.getBody());
                        switch (response.getStatus()) {
                            case 200:
                                return response.asJson().get("status");
                            case 404:
                                throw new NotFoundException("Cluster not found");
                            default:
                                throw new InternalServerException("Didn't expect that!");
                        }
                    }
                }).recover(
                        new F.Function<Throwable, JsonNode>() {
                            //Everything is down!
                            @Override
                            public JsonNode apply(Throwable throwable) throws Throwable {
                                throw throwable;
                            }
                        }
                );
        return result.get(30000);
    }


    private Cluster unzip(InputStream responseStream, String clusterName, User user, boolean isAdmin) throws IOException {
        Logger.debug("In unzip for cluster: " + clusterName);
        Cluster reposeCluster = new Cluster();
        reposeCluster.setName(clusterName);
        reposeCluster.setUser(user.id);

        Reader reader = null;
        StringWriter writer = new StringWriter();
        ZipInputStream unzippedResponse = null;
        String charset = "UTF-8";
        try {
            unzippedResponse = new ZipInputStream(responseStream);
            reader = new InputStreamReader(unzippedResponse, charset);
            ZipEntry file = unzippedResponse.getNextEntry();
            Logger.debug("Zip file: " + file.getName());

            while(file != null){
                try {
                    writer = new StringWriter();

                    char[] buffer = new char[10240];
                    for (int length = 0; (length = reader.read(buffer)) > 0; ) {
                        writer.write(buffer, 0, length);
                    }

                    switch(file.getName().split("/")[1]){
                        case "ca.pem":
                            if(!isAdmin) {
                                Helpers.createFileInCarina(file, writer, user);
                                Logger.debug(Helpers.getCarinaDirectory(user.tenant).resolve(file.getName()).toString());
                                reposeCluster.setCert_directory(Helpers.getCarinaDirectoryWithCluster(user.tenant,
                                        file.getName().substring(0, file.getName().lastIndexOf("/"))).toString());
                            }
                            break;
                        case "cert.pem":
                            if(!isAdmin) {
                                Helpers.createFileInCarina(file, writer, user);
                                Logger.debug(Helpers.getCarinaDirectory(user.tenant).resolve(file.getName()).toString());
                            }
                            break;
                        case "key.pem":
                            if(!isAdmin) {
                                Helpers.createFileInCarina(file, writer, user);
                                Logger.debug(Helpers.getCarinaDirectory(user.tenant).resolve(file.getName()).toString());
                            }
                            break;
                        case "ca-key.pem":
                            if(!isAdmin) {
                                Helpers.createFileInCarina(file, writer, user);
                                Logger.debug(Helpers.getCarinaDirectory(user.tenant).resolve(file.getName()).toString());
                            }
                            break;
                        case "docker.env":
                            //TODO: get the URI from docker.env
                            if(!isAdmin) {
                                Helpers.createFileInCarina(file, writer, user);
                                Logger.info(Helpers.getCarinaDirectory(user.tenant).resolve(file.getName()).toString());
                            }
                            String[] dockerEnv = writer.toString().split("\\n");
                            for(String s: dockerEnv){
                                if(s.trim().startsWith("export DOCKER_HOST")){
                                    reposeCluster.setUri(s.trim().split("=")[1].replace("tcp", "https"));
                                    break;
                                }
                            }
                            break;
                        default:
                            Logger.error("Invalid file found: " + file.getName());
                            break;
                    }
                } finally {
                    writer.close();
                    writer = null;
                }
                file = unzippedResponse.getNextEntry();
            }


        } finally {
            unzippedResponse.closeEntry();
            unzippedResponse.close();
            reader.close();
        }
        return reposeCluster;
    }
}
