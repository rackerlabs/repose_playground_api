package controllers;

import com.fasterxml.jackson.databind.JsonNode;
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

public class TestRepose extends Controller {

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
            //get carina info from service catalog (not there yet)
            //get the cluster name "ReposeTrial"
            //download credentials
            //get docker instances and return all containers named repose-playground
            User user = User.findByToken(token);

            //check to see if a token exists
            Cluster reposeCluster = Cluster.findByUser(user.id);
            if(reposeCluster != null){
                //get repose cluster data
                //spin up container
                //make a request to get all repose containers from cluster
            }

            F.Promise<Result> resultPromise = WS.url(
                    "https://app.getcarina.com/clusters/" + user.username)
                    .setHeader("x-auth-token", token)
                    .setHeader("x-content-type", "application/json")
                    .get().flatMap(
                            new Function<WSResponse, F.Promise<Result>>() {
                                @Override
                                public F.Promise<Result> apply(WSResponse wsResponse) throws Throwable {
                                    //Logger.info("response from carina with clusters: " +
                                    //        wsResponse.getStatus() + " " + wsResponse.getStatusText());

                                    //Logger.info("response body: " + wsResponse.getBody());
                                    JsonNode clusters = wsResponse.asJson();
                                    List<F.Promise<Cluster>> resultList = new ArrayList<F.Promise<Cluster>>();
                                    for(JsonNode cluster: clusters) {
                                        Logger.info("get data for " + cluster.get("cluster_name"));
                                        resultList.add(getClusterZip("https://app.getcarina.com/clusters/" +
                                                user.username + "/" +
                                                cluster.get("cluster_name").asText().replace("\"", "") +
                                                "/zip", token, user, cluster));
                                    }
                                    return F.Promise.sequence(resultList).map(
                                            new Function<List<Cluster>, Result>() {
                                                public Result apply(List<Cluster> clusters) throws Throwable {
                                                    Logger.info("results from all cluster calls");
                                                    return ok();
                                                }
                                            }
                                    );

                                }
                            }
                    );
            return resultPromise.get(30000);

        }
    }

    private F.Promise<Cluster> getClusterZip(final String url, String token, User user, JsonNode cluster) {
        return WS.url(url)
                .setHeader("x-auth-token", token)
                .setHeader("x-content-type", "application/json")
                .get().flatMap(new Function<WSResponse, F.Promise<Cluster>>() {
                    public F.Promise<Cluster> apply(WSResponse response) throws Throwable {
                        //Logger.info("response for " + cluster.get("cluster_name"));
                        //Logger.info("body: " + response.getBody());
                        JsonNode zipResponse = response.asJson();
                        return WS.url(
                                zipResponse.get("zip_url").asText().replace("\"", ""))
                                .setHeader("x-auth-token", token)
                                .setHeader("Accept", "application/zip")
                                .setHeader("Content-disposition","attachment; filename=" +
                                        cluster.get("cluster_name").asText().replace("\"", "") + ".zip")
                                .get().map(
                                        new Function<WSResponse, Cluster>() {
                                            @Override
                                            public Cluster apply(WSResponse innerResponse) throws Throwable {
                                                Cluster reposeCluster = new Cluster();
                                                Logger.info("results from inner call for " + cluster.get("cluster_name") + user);
                                                reposeCluster.setUser(user.id);
                                                unzip(innerResponse.getBodyAsStream(), reposeCluster);
                                                Logger.info("repose cluster: " + reposeCluster.toString());
                                                return reposeCluster;
                                            }
                                        }
                                );
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


