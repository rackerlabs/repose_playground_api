package clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import exceptions.NotFoundException;
import factories.ICarinaFactory;
import factories.IClusterFactory;
import models.CarinaRequest;
import models.Cluster;
import models.User;
import play.Logger;
import play.libs.F;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class CarinaClient implements ICarinaClient {
    @Inject
    WSClient wsClient;

    private final IClusterFactory clusterFactory;
    private final ICarinaFactory carinaFactory;


    @Inject
    public CarinaClient(IClusterFactory clusterFactory, ICarinaFactory carinaFactory){
        this.clusterFactory = clusterFactory;
        this.carinaFactory = carinaFactory;
    }

    @Override
    public boolean createCluster(String clusterName, User user) throws InternalServerException, InterruptedException {
        if(user == null || clusterName == null)
            throw new InternalServerException("Required parameters were no provided.");
        Logger.debug("Create cluster " + clusterName + " with " + user.token);
        CarinaRequest carinaRequest = new CarinaRequest(clusterName, false, user.username);

        String carinaUserUrl = clusterFactory.getCarinaUserUrl(user.username);

        if(carinaUserUrl == null)
            throw new InternalServerException("Carina user url is misconfigured.");

        F.Promise<String> statusPromise = wsClient.url(carinaUserUrl)
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
                                        "We are currently experiencing difficulties.  " +
                                                "Please try again later.");
                            }
                        }
                );

        String status = statusPromise.get(30000);
        //TODO: convert to akka.  Don't block current thread
        if(status.equals("error"))
            throw new InternalServerException("Cluster ended up in error state");

        while(!status.equals("active") && !status.equals("error")){
            JsonNode statusNode = getCluster(clusterName, user);
            if(statusNode != null) {
                status = statusNode.asText();
            } else {
                throw new InternalServerException("Unable to get status");
            }
            if (status.equals("error")){
                throw new InternalServerException("Cluster ended up in error state");
            } else if(status.equals("active")){
                break;
            }
            Thread.sleep(1000);
        }
        return true;
    }

    @Override
    public Cluster getClusterWithZip(User user, String clusterName)
            throws NotFoundException, InternalServerException{
        if(user == null || clusterName == null)
            throw new InternalServerException("Required parameters were not provided.");
        String carinaZipUrl = clusterFactory.getCarinaZipUrl(user.username, clusterName);
        if(carinaZipUrl == null)
            throw new InternalServerException("Carina zip url is misconfigured.");
        Logger.error("Get cluster zip from " + carinaZipUrl + " for " + clusterName);
        return wsClient.url(carinaZipUrl)
                .setHeader("x-auth-token", user.token)
                .get().map(new F.Function<WSResponse, Cluster>() {
                    public Cluster apply(WSResponse response) throws Throwable {
                        Logger.debug("getClusterZip::response for " + user);
                        Logger.debug("getClusterZip::body: " + response.getStatus() + " " + response.getBody());
                        switch (response.getStatus()) {
                            case 201:
                                JsonNode zipResponse = response.asJson();
                                return wsClient.url(
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
                                                                            clusterName, user);
                                                                } catch (InternalServerException | IOException e) {
                                                                    e.printStackTrace();
                                                                    Logger.error(e.getMessage());
                                                                    throw new InternalServerException(e.getLocalizedMessage());
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
        if(user == null || clusterName == null)
            throw new InternalServerException("Required parameters were not provided.");
        F.Promise<JsonNode> result = wsClient.url(clusterFactory.getCarinaClusterUrl(user.username, clusterName))
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
                                throw new NotFoundException("Cluster not found.");
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

    private Cluster unzip(InputStream responseStream, String clusterName, User user)
            throws IOException, InternalServerException {
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
                Logger.debug("Check if file has a directory");
                int fileTokens = file.getName().split("/").length;
                String fileName = file.getName().split("/")[fileTokens - 1];
                String directoryName = (fileTokens > 1) ? file.getName().split("/")[fileTokens - 2] : "";
                if(fileName == null)
                    throw new InternalServerException("Invalid files in response: " + file.getName());

                try {
                    writer = new StringWriter();

                    char[] buffer = new char[10240];
                    for (int length = 0; (length = reader.read(buffer)) > 0; ) {
                        writer.write(buffer, 0, length);
                    }

                    Logger.debug("Create temporary directory and store creds in it");
                    reposeCluster.setCert_directory(
                            carinaFactory.getCarinaDirectoryWithCluster(user.tenant,
                                    directoryName).toString());
                    switch(fileName){
                        case "ca.pem":
                        case "cert.pem":
                        case "key.pem":
                        case "ca-key.pem":
                            carinaFactory.createFileInCarina(file, writer, user);
                            break;
                        case "docker.env":
                            carinaFactory.createFileInCarina(file, writer, user);
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
