package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.Cluster;
import models.Region;
import models.User;
import play.Logger;
import play.libs.F;
import play.libs.F.Function;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Repose extends Controller {

    private static Region region = Region.DFW;

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

            //check to see if a cluster exists for this user
            Cluster reposeCluster = Cluster.findByUser(user.id);
            if(reposeCluster != null){
                //get repose cluster data
                //spin up container
                //get docker instances and return all containers named repose-playground
            }

            //get cluster creds
            F.Promise<Cluster> clusterPromise = getClusterZip("https://app.getcarina.com/clusters/" +
                    user.username + "/ReposeTrial/zip", token, user);
            Logger.info("test cluster promise: " + clusterPromise);
            F.Promise<Result> resultPromise = clusterPromise.map(
                    new Function<Cluster, Result>() {
                        public Result apply(Cluster cluster) throws Throwable {
                            Logger.info("results from all cluster calls: " + cluster);
                            return ok();
                        }
                    }
            ).recover(
                    new Function<Throwable, Result>() {
                        //no cluster by this name exists.  Empty all the things
                        @Override
                        public Result apply(Throwable throwable) throws Throwable {
                            return ok((JsonNode)(new ObjectMapper().valueToTree(new ArrayList<String>())));
                        }
                    }
            );

            return resultPromise.get(30000);

        }
    }

    private F.Promise<Cluster> getClusterZip(final String url, String token, User user) {
        return WS.url(url)
                .setHeader("x-auth-token", token)
                .setHeader("x-content-type", "application/json")
                .get().flatMap(new Function<WSResponse, F.Promise<Cluster>>() {
                    public F.Promise<Cluster> apply(WSResponse response) throws Throwable {
                        Logger.info("response for " + user);
                        Logger.info("body: " + response.getBody());
                        switch(response.getStatus()) {
                            case 200:
                                JsonNode zipResponse = response.asJson();
                                return WS.url(
                                        zipResponse.get("zip_url").asText().replace("\"", ""))
                                        .setHeader("x-auth-token", token)
                                        .setHeader("Accept", "application/zip")
                                        .setHeader("Content-disposition", "attachment; filename=ReposeTrial.zip")
                                        .get().map(
                                                new Function<WSResponse, Cluster>() {
                                                    @Override
                                                    public Cluster apply(WSResponse innerResponse) throws Throwable {
                                                        Cluster reposeCluster = new Cluster();
                                                        reposeCluster.setUser(user.id);
                                                        unzip(innerResponse.getBodyAsStream(), reposeCluster);
                                                        Logger.info("repose cluster: " + reposeCluster.toString());
                                                        return reposeCluster;
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

    private void unzip(InputStream responseStream, Cluster cluster) throws IOException {
        Reader reader = null;
        StringWriter writer = new StringWriter();
        ZipInputStream unzippedResponse = null;
        String charset = "UTF-8";
        try {
            unzippedResponse = new ZipInputStream(responseStream);
            reader = new InputStreamReader(unzippedResponse, charset);
            ZipEntry file = unzippedResponse.getNextEntry();

            while(file != null){
                try {
                    writer = new StringWriter();

                    char[] buffer = new char[10240];
                    for (int length = 0; (length = reader.read(buffer)) > 0; ) {
                        writer.write(buffer, 0, length);
                    }
                    switch(file.getName().split("/")[1]){
                        case "ca.pem":
                            cluster.setCapem(writer.toString());
                            break;
                        case "cert.pem":
                            cluster.setCertpem(writer.toString());
                            break;
                        case "key.pem":
                            cluster.setKeypem(writer.toString());
                            break;
                        case "ca-key.pem":
                            cluster.setCakeypem(writer.toString());
                            break;
                        case "docker.env":
                            cluster.setDockerenv(writer.toString());
                            break;
                        case "docker.cmd":
                            cluster.setDockercmd(writer.toString());
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


