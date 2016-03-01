package models;

import com.fasterxml.jackson.databind.JsonNode;
import exceptions.InternalServerException;
import helpers.Helpers;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by dimi5963 on 1/12/16.
 */
public class Carina {

    @Deprecated
    public Cluster getClusterByName(String clusterName, User user, boolean isAdmin, boolean createIfDoesNotExist)
            throws InternalServerException {
        Logger.info("Get cluster " + clusterName);

        //check to see if a cluster exists for this user
        Cluster reposeCluster = Cluster.findByUserandName(user.id, clusterName);
        if(reposeCluster == null) {
            //does it exist?
            if(!doesClusterExist(clusterName, user) && !createIfDoesNotExist){
                //oops
                throw new InternalServerException("Cluster doesn't exist");
            } else if(!doesClusterExist(clusterName, user) && createIfDoesNotExist) {
                try {
                    createCluster(clusterName, user);
                }catch(InterruptedException ie) {
                    ie.printStackTrace();
                    throw new InternalServerException(ie.getLocalizedMessage());
                }
            }
            //get repose cluster data
            //spin up container
            //get docker instances and return all containers named repose-playground
            //get cluster creds
            F.Promise<Cluster> clusterPromise = getClusterZip("https://app.getcarina.com/clusters/" +
                    user.username + "/" + clusterName + "/zip", user, clusterName, isAdmin);
            Logger.info("test cluster promise: " + clusterPromise);
            F.Promise<Cluster> resultPromise = clusterPromise.map(
                    new F.Function<Cluster, Cluster>() {
                        public Cluster apply(Cluster cluster) throws Throwable {
                            Logger.info("results from all cluster calls: " + cluster);
                            return cluster;
                        }
                    }
            ).recover(
                    new F.Function<Throwable, Cluster>() {
                        //no cluster by this name exists.  Empty all the things
                        @Override
                        public Cluster apply(Throwable throwable) throws Throwable {
                            throw new InternalServerException("{'message':'could not retrieve cluster'}");
                            //return ok((JsonNode) (new ObjectMapper().valueToTree(new ArrayList<String>())));
                        }
                    }
            );
            reposeCluster = resultPromise.get(30000);
        }
        Logger.info("Return cluster " + reposeCluster);
        return reposeCluster;
    }

    @Deprecated
    private boolean doesClusterExist(String clusterName, User user) throws InternalServerException{
        JsonNode statusNode = new helpers.Carina().getCluster(clusterName, user);
        if(statusNode != null) {
            return statusNode.asText().equals("active");
        }else {
            return false;
        }
    }

    @Deprecated
    public boolean createCluster(String clusterName, User user) throws InternalServerException, InterruptedException {
        Logger.info("Create cluster " + clusterName + " with " + user.token);
        CarinaRequest carinaRequest = new CarinaRequest(clusterName, false, user.username);

        F.Promise<String> statusPromise = WS.url("https://app.getcarina.com/clusters/" +
                user.username)
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
                        //return internalServerError(
                        //        Helpers.buildJsonResponse("message", "We are currently experiencing difficulties.  Please try again later."));
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


    @Deprecated
    private F.Promise<Cluster> getClusterZip(final String url, User user, String clusterName, boolean isAdmin) {
        Logger.info("Get cluster zip from " + url + " for " + clusterName);
        return WS.url(url)
                .setHeader("x-auth-token", user.token)
                .setHeader("x-content-type", "application/json")
                .get().flatMap(new F.Function<WSResponse, F.Promise<Cluster>>() {
                    public F.Promise<Cluster> apply(WSResponse response) throws Throwable {
                        Logger.info("getClusterZip::response for " + user);
                        Logger.info("getClusterZip::body: " + response.getStatus() + " " + response.getBody());
                        switch(response.getStatus()) {
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
                                                        Logger.info("Response from zip call: " + innerResponse.getStatus());
                                                        switch (innerResponse.getStatus()) {
                                                            case 200:
                                                                Cluster reposeCluster = new Cluster();
                                                                reposeCluster.setName(clusterName);
                                                                reposeCluster.setUser(user.id);
                                                                try {
                                                                    unzip(innerResponse.getBodyAsStream(), reposeCluster, user, isAdmin);
                                                                } catch (Exception e){
                                                                    e.printStackTrace();
                                                                    Logger.error(e.getMessage());
                                                                }
                                                                Logger.info("repose cluster: " + reposeCluster.toString());
                                                                return reposeCluster;
                                                            default:
                                                                Logger.info("here2");
                                                                return null;
                                                        }
                                                    }
                                                }
                                        ).recover(
                                                new F.Function<Throwable, Cluster>() {
                                                    //no cluster by this name exists.  Empty all the things
                                                    @Override
                                                    public Cluster apply(Throwable throwable) throws Throwable {
                                                        throw new InternalServerException("{'message':'could not retrieve cluster zip'}");
                                                        //return ok((JsonNode) (new ObjectMapper().valueToTree(new ArrayList<String>())));
                                                    }
                                                }
                                        );
                            case 404:
                                return null;
                            default:
                                return null;
                        }
                    }
                });
    }


    public void unzip(InputStream responseStream, Cluster cluster, User user, boolean isAdmin) throws IOException {
        Logger.info("In unzip for cluster: " + cluster);
        Reader reader = null;
        StringWriter writer = new StringWriter();
        ZipInputStream unzippedResponse = null;
        String charset = "UTF-8";
        try {
            unzippedResponse = new ZipInputStream(responseStream);
            reader = new InputStreamReader(unzippedResponse, charset);
            ZipEntry file = unzippedResponse.getNextEntry();
            Logger.info("Zip file: " + file.getName());

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
                                Logger.info(Helpers.getCarinaDirectory(user.tenant).resolve(file.getName()).toString());
                                cluster.setCert_directory(Helpers.getCarinaDirectoryWithCluster(user.tenant,
                                        file.getName().substring(0, file.getName().lastIndexOf("/"))).toString());
                            }
                            break;
                        case "cert.pem":
                            if(!isAdmin) {
                                Helpers.createFileInCarina(file, writer, user);
                                Logger.info(Helpers.getCarinaDirectory(user.tenant).resolve(file.getName()).toString());
                            }
                            break;
                        case "key.pem":
                            if(!isAdmin) {
                                Helpers.createFileInCarina(file, writer, user);
                                Logger.info(Helpers.getCarinaDirectory(user.tenant).resolve(file.getName()).toString());
                            }
                            break;
                        case "ca-key.pem":
                            if(!isAdmin) {
                                Helpers.createFileInCarina(file, writer, user);
                                Logger.info(Helpers.getCarinaDirectory(user.tenant).resolve(file.getName()).toString());
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
                                    cluster.setUri(s.trim().split("=")[1].replace("tcp", "https"));
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
        cluster.save();
        Logger.info("Cluster to save: " + cluster.toString());
    }
}
