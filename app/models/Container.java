package models;

import com.google.common.collect.ImmutableSet;
import com.spotify.docker.client.*;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import exceptions.InternalServerException;
import helpers.Helpers;
import org.joda.time.DateTime;
import play.Logger;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by dimi5963 on 2/10/16.
 */
public class Container {

    /**
     * Spin up a repose container
     * @param user
     * @param filters
     * @param version
     */
    public String createReposeContainer(User user, Map<String, String> filters, String version) throws InternalServerException {
        Logger.debug("create repose instance " + user + " and version: " + version);

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
            //write our filters to repose config directory
            filters.entrySet().stream().forEach(
                    e -> {
                        try {
                            File file = Paths.get(
                                    Helpers.getReposeConfigDirectory(user.tenant).toString(),
                                    e.getKey()).toFile();
                            FileWriter fileWriter = new FileWriter(file);
                            fileWriter.write(e.getValue());
                            fileWriter.close();
                        } catch (IOException ioe) {
                            Logger.error("Unable to write " + e.getKey());
                        }
                    }
            );

            //write docker file for new image
            try {
                Logger.debug("Update repose version in dockerfile");

                Files.copy(
                        play.Play.application().path().toPath().resolve("carina").
                                resolve("repose-image").resolve("Dockerfile"),
                        Helpers.getReposeImageDirectory(user.tenant).resolve("Dockerfile"),
                        StandardCopyOption.REPLACE_EXISTING);

                List<String> dockerfileLines = Files.readAllLines(
                        Helpers.getReposeImageDirectory(user.tenant).resolve("Dockerfile"));
                for(int dockerfileLineCount = 0; dockerfileLineCount < dockerfileLines.size(); dockerfileLineCount ++) {
                    if(dockerfileLines.get(dockerfileLineCount).contains("REPOSE_VERSION")){
                        dockerfileLines.set(dockerfileLineCount,
                                dockerfileLines.get(dockerfileLineCount).replace("REPOSE_VERSION", version));
                    }
                }
                Files.write(Helpers.getReposeImageDirectory(user.tenant).resolve("Dockerfile"), dockerfileLines);
            } catch (IOException e) {
                e.printStackTrace();
                throw new InternalServerException(e.getLocalizedMessage());
            }

            //remove all containers with image
            List<com.spotify.docker.client.messages.Container> containerList = docker.listContainers(
                    DockerClient.ListContainersParam.allContainers());

            Predicate<com.spotify.docker.client.messages.Container> hasImage = c -> c.image().equals("repose-" + user.tenant + "-" + version);

            List<com.spotify.docker.client.messages.Container> containersToRemove = containerList.stream().filter(hasImage).collect(Collectors.toList());
            containersToRemove.forEach(container1 -> {
                try {
                    docker.killContainer(container1.id());
                    Logger.info("removed " + container1.id());
                }catch(DockerException | InterruptedException de ){
                    Logger.error("failed to stop " + de.getLocalizedMessage());
                }
                try {
                    docker.removeContainer(container1.id());
                    Logger.info("removed " + container1.id());
                }catch(DockerException | InterruptedException de ){
                    Logger.error("failed to stop " + de.getLocalizedMessage());
                }
            });

            //build an image with repose
            try {
                Logger.info("Build image with " + cluster + " at " + DateTime.now());
                try {
                    docker.removeImage("repose-" + user.tenant + "-" + version, true, true);
                } catch(ImageNotFoundException infe) {
                    Logger.warn("Image was not found.  Should be ok. " +
                            infe.getImage() + " " + infe.getLocalizedMessage());
                } catch(DockerRequestException dre) {
                    Logger.warn("Failed request. " +
                            dre.message() + " " + dre.getLocalizedMessage());
                }
                docker.build(
                        Paths.get(Helpers.getReposeImageDirectory(user.tenant).toString()),
                        "repose-" + user.tenant + "-" + version);

                Map<String, List<PortBinding>> portBindings = new HashMap<String, List<PortBinding>>();

                PortBinding randomPort = PortBinding.randomPort("0.0.0.0");

                portBindings.put("8080/tcp", new ArrayList<PortBinding>() {
                    {
                        add(randomPort);
                    }
                });

                //link by name
                HostConfig hostConfig = HostConfig.builder().
                        portBindings(portBindings).
                        links("repose-origin-" + user.tenant + "-" + version.replace('.', '-')).
                        build();

                ContainerConfig containerConfig = ContainerConfig.builder()
                        .image("repose-" + user.tenant + "-" + version)
                        .hostConfig(hostConfig).exposedPorts(ImmutableSet.of(randomPort.hostPort()))
                        .build();
                ContainerCreation creation = docker.createContainer(containerConfig,
                        "repose-" + user.tenant + "-" + version.replace('.','_'));
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


    /**
     * Spin up a origin container
     * @param user
     * @param version
     */
    public void createOriginContainer(User user, String version) throws InternalServerException {
        Logger.debug("create origin instance " + user);

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

            //write docker file for new image
            try {
                Files.copy(
                        play.Play.application().path().toPath().resolve("carina").
                                resolve("origin-image").resolve("Dockerfile"),
                        Helpers.getOriginImageDirectory(user.tenant).resolve("Dockerfile"),
                        StandardCopyOption.REPLACE_EXISTING);
                Files.copy(
                        play.Play.application().path().toPath().resolve("carina").
                                resolve("origin-image").resolve("backend.js"),
                        Helpers.getOriginImageDirectory(user.tenant).resolve("backend.js"),
                        StandardCopyOption.REPLACE_EXISTING);
                Files.copy(
                        play.Play.application().path().toPath().resolve("carina").
                                resolve("origin-image").resolve("package.json"),
                        Helpers.getOriginImageDirectory(user.tenant).resolve("package.json"),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
                throw new InternalServerException(e.getLocalizedMessage());
            }

            //remove all containers with image
            List<com.spotify.docker.client.messages.Container> containerList = docker.listContainers(
                    DockerClient.ListContainersParam.allContainers());

            Predicate<com.spotify.docker.client.messages.Container> hasImage = c -> c.image().equals("repose-origin-" + user.tenant + "-" + version);

            List<com.spotify.docker.client.messages.Container> containersToRemove = containerList.stream().filter(hasImage).collect(Collectors.toList());
            containersToRemove.forEach(container1 -> {
                try {
                    docker.killContainer(container1.id());
                    Logger.info("removed " + container1.id());
                }catch(DockerException | InterruptedException de ){
                    Logger.error("failed to stop " + de.getLocalizedMessage());
                }
                try {
                    docker.removeContainer(container1.id());
                    Logger.info("removed " + container1.id());
                }catch(DockerException | InterruptedException de ){
                    Logger.error("failed to stop " + de.getLocalizedMessage());
                }
            });

            //build an image with origin service
            try {
                Logger.info("Build image with " + cluster + " at " + DateTime.now());
                try {
                    docker.removeImage("repose-origin-" + user.tenant + "-" + version, true, true);
                } catch(ImageNotFoundException infe) {
                    Logger.warn("Image was not found.  Should be ok. " +
                            infe.getImage() + " " + infe.getLocalizedMessage());
                } catch(DockerRequestException dre) {
                    Logger.warn("Failed request. " +
                            dre.message() + " " + dre.getLocalizedMessage());
                }
                docker.build(
                        Paths.get(Helpers.getOriginImageDirectory(user.tenant).toString()),
                        "repose-origin-" + user.tenant + "-" + version);

                ContainerConfig containerConfig = ContainerConfig.builder()
                        .image("repose-origin-" + user.tenant + "-" + version)
                        .build();
                //name has underscores because of ip addy
                ContainerCreation creation = docker.createContainer(containerConfig,
                        "repose-origin-" + user.tenant + "-" + version.replace('.','-'));
                final String id = creation.id();
                docker.startContainer(id);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (DockerCertificateException | DockerException | InterruptedException e) {
            e.printStackTrace();
            throw new InternalServerException(e.getMessage());
        }
    }

}
