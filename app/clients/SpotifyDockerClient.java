package clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.*;
import exceptions.InternalServerException;
import factories.ICarinaFactory;
import factories.IContainerFactory;
import factories.TestFactory;
import models.Cluster;
import models.Configuration;
import models.TestRequest;
import models.User;
import org.joda.time.DateTime;
import play.libs.Json;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static play.Logger.*;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class SpotifyDockerClient implements IDockerClient {

    private final IContainerFactory<
            com.spotify.docker.client.messages.Container,
            com.spotify.docker.client.messages.ContainerStats> containerFactory;
    private final ICarinaFactory carinaFactory;
    private final TestClient testClient;
    private final TestFactory testFactory;

    @Inject
    public SpotifyDockerClient(IContainerFactory<
            com.spotify.docker.client.messages.Container,
            com.spotify.docker.client.messages.ContainerStats> containerFactory,
                               ICarinaFactory carinaFactory,
                               TestClient testClient,
                               TestFactory testFactory){
        this.containerFactory = containerFactory;
        this.carinaFactory = carinaFactory;
        this.testClient = testClient;
        this.testFactory = testFactory;
    }

    /***
     * Return list of container models that have repose names
     * @param cluster Cluster model
     * @param user User model
     * @return List of Container models
     */
    @Override
    public List<models.Container> getReposeContainers(Cluster cluster, User user) throws InternalServerException {
        debug("Retrieve repose containers for " + cluster);
        final com.spotify.docker.client.DockerClient docker;
        try {
            docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();
            List<com.spotify.docker.client.messages.Container> containerList =
                    docker.listContainers(com.spotify.docker.client.DockerClient.ListContainersParam.allContainers());
            List<models.Container> reposeInstanceList = containerFactory.translateContainers(containerList, user);
            for(models.Container reposeInstance: reposeInstanceList)
                reposeInstance.setContainerStats(getReposeInstanceStats(cluster, reposeInstance.getId()));

            return reposeInstanceList;

        } catch (DockerCertificateException | InterruptedException | DockerException e) {
            error("Unable to retrieve containers: " + e.getLocalizedMessage());
            e.printStackTrace();
            throw new InternalServerException(e.getLocalizedMessage());
        }

    }

    @Override
    public boolean startReposeInstance(Cluster cluster, String containerId) throws InternalServerException {
        final com.spotify.docker.client.DockerClient docker;
        try {
            docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();
            docker.startContainer(containerId);
            return true;
        } catch (DockerCertificateException | DockerException | InterruptedException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }
    }

    @Override
    public boolean stopReposeInstance(Cluster cluster, String containerId) throws InternalServerException {
        final com.spotify.docker.client.DockerClient docker;
        try {
            docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();
            docker.stopContainer(containerId, 5);
            return true;
        } catch (DockerCertificateException | DockerException | InterruptedException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }
    }

    @Override
    public models.ContainerStats getReposeInstanceStats(Cluster cluster, String containerId) throws InternalServerException {
        final com.spotify.docker.client.DockerClient docker;
        try {
            docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();
            com.spotify.docker.client.messages.ContainerStats containerStats = docker.stats(containerId);
            return containerFactory.translateContainerStats(containerStats);
        } catch (DockerCertificateException | InterruptedException | DockerException e) {
            error("Unable to retrieve containers: " + e.getLocalizedMessage());
            e.printStackTrace();
            throw new InternalServerException(e.getLocalizedMessage());
        }
    }

    public List<Configuration> getConfigurationsForInstance(Cluster cluster, String containerId) throws InternalServerException {
        final com.spotify.docker.client.DockerClient docker;
        List<Configuration> configurations = new ArrayList<>();
        try {
        docker = DefaultDockerClient.builder()
        .uri(URI.create(cluster.getUri()))
        .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
        .build();
            List<String> xmlStringList = Arrays.asList(docker.execStart(
                    executeCommand(containerId, docker,
                            new String[]{"sh", "-c", "ls *.xml"})).readFully().split("\n"));
            xmlStringList.forEach(filter -> {
                        try {
                            configurations.add(new Configuration(filter, docker.execStart(
                                    executeCommand(containerId, docker,
                                            new String[]{"sh", "-c", "cat " + filter})).readFully()));
                        } catch (DockerException | InterruptedException e) {
                            e.printStackTrace();
                            error("Unable to retrieve configuration: " + filter);
                        }
                    }
            );
        } catch (DockerCertificateException | DockerException | InterruptedException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }

        return configurations;
    }

    @Override
    public String createOriginInstance(Cluster cluster, User user, String versionId) throws InternalServerException {
        final com.spotify.docker.client.DockerClient docker;

        try {
            docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();

            //write docker file for new image
            generateOriginDockerfile(user);

            //remove all containers with image
            removeRunningContainers(user, versionId, docker, "repose-origin-");

            //build an image with origin service
            return buildOriginImage(cluster, user, versionId, docker);
        } catch (DockerCertificateException | DockerException | InterruptedException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }
    }

    @Override
    public String createReposeInstance(Cluster cluster, User user,
                                       String versionId, List<Configuration> configurationList)
            throws InternalServerException {
        debug("create repose instance " + user + " and version: " + versionId);
        if(configurationList == null)
            throw new InternalServerException("Must provide at least core configs.");
        final com.spotify.docker.client.DockerClient docker;
        try {
            docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();
            //write our filters to repose config directory
            writeFiltersToReposeConfigDirectory(user, configurationList);

            //write docker file for new image
            generateReposeDockerfile(user, versionId);

            //remove all containers with image
            removeRunningContainers(user, versionId, docker, "repose-");

            //build an image with repose
            return buildReposeImage(cluster, user, versionId, docker);
        } catch (DockerCertificateException | DockerException | InterruptedException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }
    }

    @Override
    public ObjectNode executeTestAgainstRepose(Cluster cluster, String containerId, TestRequest testRequest,
                                               ObjectNode response) throws InternalServerException {
        debug("test repose instance");
        final com.spotify.docker.client.DockerClient docker;
        try {
            docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();
            //clean up logs
            cleanUpLogs(containerId, docker);

            //make a request
            ContainerInfo containerInfo = docker.inspectContainer(containerId);
            debug("container info:" + containerInfo);
            debug("container network port info:" + containerInfo.networkSettings().ports());
            debug("port stuff: " + containerInfo.networkSettings().ports().get("8080/tcp"));
            PortBinding portBinding = containerInfo.networkSettings().ports().get("8080/tcp").get(0);
            debug("container info:" + portBinding.hostIp());

            ObjectNode responseNode = testClient.makeTestRequest(testRequest, portBinding.hostIp(), portBinding.hostPort());

            //get the logs out
            //this will get split up into connection pool and request/response messages
            List<String> httpDebugLogList = Arrays.asList(docker.execStart(
                    executeCommand(containerId, docker,
                            new String[]{"cat", "/var/log/repose/http-debug.log"})).readFully().split("\n"));

            Map<String, ?> debugMessageMap = testFactory.generateDebugMessageMap(httpDebugLogList);

            //if line containers ERROR, start a new entry
            List<String> errorLogList = Arrays.asList(docker.execStart(
                    executeCommand(containerId, docker,
                            new String[]{"cat", "/var/log/repose/error.log"})).readFully().split(Pattern.quote("ERROR")));
            List<JsonNode> intraFilterLogList = Arrays.asList(docker.execStart(
                    executeCommand(containerId, docker,
                            new String[]{"cat", "/var/log/repose/intra-filter.log"})).readFully().split("\n")).
                    stream().map(entry -> Json.parse(
                    entry.split(Pattern.quote("TRACE intrafilter-logging - "))[1])).collect(Collectors.toList());


            response.put("current", docker.execStart(
                    executeCommand(containerId, docker,
                            new String[]{"cat", "/var/log/repose/current.log"})).readFully());
            response.putPOJO("error", Json.toJson(errorLogList));
            response.putPOJO("http-debug", Json.toJson(debugMessageMap));
            response.putPOJO("intra-filter", Json.toJson(intraFilterLogList));

            response.putPOJO("response", responseNode);
            debug("response: " + response);
            return response;
        } catch (DockerCertificateException | DockerException | InterruptedException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }
    }

    private String buildOriginImage(Cluster cluster, User user, String versionId, DockerClient docker) throws DockerException, InterruptedException, InternalServerException {
        try {
            debug("Build image with " + cluster + " at " + DateTime.now());
            removeAndBuildImage(user, versionId, docker, "repose-origin-",
                    Paths.get(carinaFactory.getOriginImageDirectory(user.tenant).toString()));

            ContainerConfig containerConfig = ContainerConfig.builder()
                    .image("repose-origin-" + user.tenant + "-" + versionId)
                    .build();
            //name has underscores because of ip addy
            ContainerCreation creation = docker.createContainer(containerConfig,
                    "repose-origin-" + user.tenant + "-" + versionId.replace('.', '-'));
            final String id = creation.id();
            docker.startContainer(id);

            return id;

        } catch (IOException e) {
            error(e.getLocalizedMessage());
            e.printStackTrace();
            throw new InternalServerException("Unable to create origin service.");
        }
    }

    private String buildReposeImage(Cluster cluster, User user, String versionId, DockerClient docker) throws InternalServerException {
        try {
            debug("Build image with " + cluster + " at " + DateTime.now());
            removeAndBuildImage(user, versionId, docker, "repose-",
                    Paths.get(carinaFactory.getReposeImageDirectory(user.tenant).toString()));

            Map<String, List<PortBinding>> portBindings = new HashMap<>();

            PortBinding randomPort = PortBinding.randomPort("0.0.0.0");

            portBindings.put("8080/tcp", new ArrayList<PortBinding>() {
                {
                    add(randomPort);
                }
            });

            //link by name
            HostConfig hostConfig = HostConfig.builder().
                    portBindings(portBindings).
                    links("repose-origin-" + user.tenant + "-" + versionId.replace('.', '-')).
                    build();

            ContainerConfig containerConfig = ContainerConfig.builder()
                    .image("repose-" + user.tenant + "-" + versionId)
                    .hostConfig(hostConfig).exposedPorts(ImmutableSet.of(randomPort.hostPort()))
                    .build();
            ContainerCreation creation = docker.createContainer(containerConfig,
                    "repose-" + user.tenant + "-" + versionId.replace('.','_'));
            final String id = creation.id();
            docker.startContainer(id);
            return id;

        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }
    }

    private void removeAndBuildImage(User user, String versionId, DockerClient docker, String namePrefix,
                                     Path reposeImageDirectory)
            throws DockerException, InterruptedException, IOException {
        try {
            docker.removeImage(namePrefix + user.tenant + "-" + versionId, true, true);
        } catch(ImageNotFoundException infe) {
            warn("Image was not found.  Should be ok. " +
                    infe.getImage() + " " + infe.getLocalizedMessage());
        } catch(DockerRequestException dre) {
            warn("Failed request. " +
                    dre.message() + " " + dre.getLocalizedMessage());
        }
        docker.build(reposeImageDirectory, namePrefix + user.tenant + "-" + versionId);
    }

    private void removeRunningContainers(User user, String versionId, DockerClient docker, String namePrefix) throws DockerException, InterruptedException {
        List<Container> containerList = docker.listContainers(
                DockerClient.ListContainersParam.allContainers());

        Predicate<Container> hasImage =
                c -> c.image().equals(namePrefix + user.tenant + "-" + versionId);

        List<Container> containersToRemove =
                containerList.stream().filter(hasImage).collect(Collectors.toList());
        containersToRemove.forEach(container1 -> {
            try {
                docker.killContainer(container1.id());
                debug("removed " + container1.id());
            } catch (DockerException | InterruptedException de) {
                error("failed to stop " + de.getLocalizedMessage());
            }
            try {
                docker.removeContainer(container1.id());
                debug("removed " + container1.id());
            } catch (DockerException | InterruptedException de) {
                error("failed to stop " + de.getLocalizedMessage());
            }
        });
    }

    private void generateReposeDockerfile(User user, String versionId) throws InternalServerException {
        try {
            debug("Update repose version in dockerfile");

            Files.copy(
                    carinaFactory.getCarinaReposeFile("Dockerfile"),
                    carinaFactory.getReposeImageFile(user.tenant, "Dockerfile"),
                    StandardCopyOption.REPLACE_EXISTING);


            List<String> dockerfileLines = Files.readAllLines(
                    carinaFactory.getReposeImageFile(user.tenant, "Dockerfile"));
            for(int dockerfileLineCount = 0; dockerfileLineCount < dockerfileLines.size(); dockerfileLineCount ++) {
                if(dockerfileLines.get(dockerfileLineCount).contains("REPOSE_VERSION")){
                    dockerfileLines.set(dockerfileLineCount,
                            dockerfileLines.get(dockerfileLineCount).replace("REPOSE_VERSION", versionId));
                }
            }
            Files.write(carinaFactory.getReposeImageFile(user.tenant, "Dockerfile"), dockerfileLines);
        } catch (IOException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getLocalizedMessage());
        }
    }

    private void generateOriginDockerfile(User user) throws InternalServerException {
        for(String file: Arrays.asList("Dockerfile", "backend.js", "package.json")) {
            try {
                Files.copy(
                        carinaFactory.getCarinaOriginFile(file),
                        carinaFactory.getOriginImageFile(user.tenant, file),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
                throw new InternalServerException(e.getLocalizedMessage());
            }

        }
    }

    private void writeFiltersToReposeConfigDirectory(User user, List<Configuration> configurationList) {
        configurationList.forEach(
                configuration -> {
                    try {
                        File file = Paths.get(
                                carinaFactory.getReposeConfigDirectory(user.tenant).toString(),
                                configuration.getName()).toFile();
                        FileWriter fileWriter = new FileWriter(file);
                        fileWriter.write(configuration.getXml());
                        fileWriter.close();
                    } catch (IOException ioe) {
                        error("Unable to write " + configuration.getName());
                    }
                }
        );
    }

    private void cleanUpLogs(String containerId, DockerClient docker) throws DockerException, InterruptedException {
        debug(docker.execStart(
                executeCommand(containerId, docker,
                        new String[]{"sh", "-c", "cat /dev/null > /var/log/repose/current.log"})).readFully());
        debug(docker.execStart(
                executeCommand(containerId, docker,
                        new String[]{"sh", "-c", "cat /dev/null > /var/log/repose/http-debug.log"})).readFully());
        debug(docker.execStart(
                executeCommand(containerId, docker,
                        new String[]{"sh", "-c", "cat /dev/null > /var/log/repose/intra-filter.log"})).readFully());
        debug(docker.execStart(
                executeCommand(containerId, docker,
                        new String[]{"sh", "-c", "cat /dev/null > /var/log/repose/error.log"})).readFully());
    }

    private String executeCommand(String containerId, DockerClient docker, String[] command)
            throws DockerException, InterruptedException{
        return docker.execCreate(
                containerId, command,
                DockerClient.ExecCreateParam.attachStdout(),
                DockerClient.ExecCreateParam.attachStderr());
    }
}
