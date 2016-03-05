package clients;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import exceptions.InternalServerException;
import factories.ICarinaFactory;
import factories.IContainerFactory;
import models.Cluster;
import models.Configuration;
import models.Container;
import models.ContainerStats;
import models.User;
import org.joda.time.DateTime;
import play.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class SpotifyDockerClient implements IDockerClient {

    private final IContainerFactory<
            com.spotify.docker.client.messages.Container,
            com.spotify.docker.client.messages.ContainerStats> containerFactory;
    private final ICarinaFactory carinaFactory;

    @Inject
    public SpotifyDockerClient(IContainerFactory<
            com.spotify.docker.client.messages.Container,
            com.spotify.docker.client.messages.ContainerStats> containerFactory,
                               ICarinaFactory carinaFactory){
        this.containerFactory = containerFactory;
        this.carinaFactory = carinaFactory;
    }

    /***
     * Return list of container models that have repose names
     * @param cluster Cluster model
     * @param user User model
     * @return List of Container models
     */
    @Override
    public List<Container> getReposeContainers(Cluster cluster, User user) throws InternalServerException {
        Logger.debug("Retrieve repose containers for " + cluster);
        final com.spotify.docker.client.DockerClient docker;
        try {
            docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();
            List<com.spotify.docker.client.messages.Container> containerList =
                    docker.listContainers(com.spotify.docker.client.DockerClient.ListContainersParam.allContainers());
            List<Container> reposeInstanceList = containerFactory.translateContainers(containerList, user);
            for(Container reposeInstance: reposeInstanceList)
                reposeInstance.setContainerStats(getReposeInstanceStats(cluster, reposeInstance.getId()));

            return reposeInstanceList;

        } catch (DockerCertificateException | InterruptedException | DockerException e) {
            Logger.error("Unable to retrieve containers: " + e.getLocalizedMessage());
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
    public ContainerStats getReposeInstanceStats(Cluster cluster, String containerId) throws InternalServerException {
        final com.spotify.docker.client.DockerClient docker;
        try {
            docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();
            com.spotify.docker.client.messages.ContainerStats containerStats = docker.stats(containerId);
            return containerFactory.translateContainerStats(containerStats);
        } catch (DockerCertificateException | InterruptedException | DockerException e) {
            Logger.error("Unable to retrieve containers: " + e.getLocalizedMessage());
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
                            Logger.error("Unable to retrieve configuration: " + filter);
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

            //remove all containers with image
            List<com.spotify.docker.client.messages.Container> containerList = docker.listContainers(
                    DockerClient.ListContainersParam.allContainers());

            Predicate<com.spotify.docker.client.messages.Container> hasImage =
                    c -> c.image().equals("repose-origin-" + user.tenant + "-" + versionId);

            List<com.spotify.docker.client.messages.Container> containersToRemove =
                    containerList.stream().filter(hasImage).collect(Collectors.toList());
            containersToRemove.forEach(container1 -> {
                try {
                    docker.killContainer(container1.id());
                    Logger.debug("removed " + container1.id());
                }catch(DockerException | InterruptedException de ){
                    Logger.error("failed to stop " + de.getLocalizedMessage());
                }
                try {
                    docker.removeContainer(container1.id());
                    Logger.debug("removed " + container1.id());
                }catch(DockerException | InterruptedException de ){
                    Logger.error("failed to stop " + de.getLocalizedMessage());
                }
            });

            //build an image with origin service
            try {
                Logger.debug("Build image with " + cluster + " at " + DateTime.now());
                try {
                    docker.removeImage("repose-origin-" + user.tenant + "-" + versionId, true, true);
                } catch(ImageNotFoundException infe) {
                    Logger.warn("Image was not found.  Should be ok. " +
                            infe.getImage() + " " + infe.getLocalizedMessage());
                } catch(DockerRequestException dre) {
                    Logger.warn("Failed request. " +
                            dre.message() + " " + dre.getLocalizedMessage());
                }
                docker.build(
                        Paths.get(carinaFactory.getOriginImageDirectory(user.tenant).toString()),
                        "repose-origin-" + user.tenant + "-" + versionId);

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
                Logger.error(e.getLocalizedMessage());
                e.printStackTrace();
                throw new InternalServerException("Unable to create origin service.");
            }
        } catch (DockerCertificateException | DockerException | InterruptedException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }
    }

    @Override
    public String createReposeInstance(Cluster cluster, User user,
                                       String versionId, List<Configuration> configurationList)
            throws InternalServerException {
        Logger.debug("create repose instance " + user + " and version: " + versionId);
        if(configurationList == null)
            throw new InternalServerException("Must provide at least core configs.");
        final com.spotify.docker.client.DockerClient docker;
        try {
            docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();
            //write our filters to repose config directory
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
                            Logger.error("Unable to write " + configuration.getName());
                        }
                    }
            );

            //write docker file for new image
            try {
                Logger.debug("Update repose version in dockerfile");

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

            //remove all containers with image
            List<com.spotify.docker.client.messages.Container> containerList = docker.listContainers(
                    DockerClient.ListContainersParam.allContainers());

            Predicate<com.spotify.docker.client.messages.Container> hasImage =
                    c -> c.image().equals("repose-" + user.tenant + "-" + versionId);

            List<com.spotify.docker.client.messages.Container> containersToRemove =
                    containerList.stream().filter(hasImage).collect(Collectors.toList());
            containersToRemove.forEach(container1 -> {
                try {
                    docker.killContainer(container1.id());
                    Logger.debug("removed " + container1.id());
                } catch (DockerException | InterruptedException de) {
                    Logger.error("failed to stop " + de.getLocalizedMessage());
                }
                try {
                    docker.removeContainer(container1.id());
                    Logger.debug("removed " + container1.id());
                } catch (DockerException | InterruptedException de) {
                    Logger.error("failed to stop " + de.getLocalizedMessage());
                }
            });

            //build an image with repose
            try {
                Logger.debug("Build image with " + cluster + " at " + DateTime.now());
                try {
                    docker.removeImage("repose-" + user.tenant + "-" + versionId, true, true);
                } catch(ImageNotFoundException infe) {
                    Logger.warn("Image was not found.  Should be ok. " +
                            infe.getImage() + " " + infe.getLocalizedMessage());
                } catch(DockerRequestException dre) {
                    Logger.warn("Failed request. " +
                            dre.message() + " " + dre.getLocalizedMessage());
                }
                docker.build(
                        Paths.get(
                                carinaFactory.getReposeImageDirectory(user.tenant).toString()),
                        "repose-" + user.tenant + "-" + versionId);

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
        } catch (DockerCertificateException | DockerException | InterruptedException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }
    }

    private String executeCommand(String containerId, DockerClient docker, String[] command)
            throws DockerException, InterruptedException{
        return docker.execCreate(
                containerId, command,
                DockerClient.ExecCreateParam.attachStdout(),
                DockerClient.ExecCreateParam.attachStderr());
    }
}
