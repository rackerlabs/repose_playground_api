package clients;

import com.google.inject.Inject;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificateException;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerException;
import factories.IContainerFactory;
import models.Cluster;
import models.Container;
import models.User;
import play.Logger;

import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class SpotifyDockerClient implements IDockerClient {

    private final IContainerFactory<com.spotify.docker.client.messages.Container> containerFactory;

    @Inject
    public SpotifyDockerClient(IContainerFactory<com.spotify.docker.client.messages.Container> containerFactory){
        this.containerFactory = containerFactory;
    }

    /***
     * Return list of container models that have repose names
     * @param cluster Cluster model
     * @param user User model
     * @return List of Container models
     */
    @Override
    public List<Container> getReposeContainers(Cluster cluster, User user)  {
        final com.spotify.docker.client.DockerClient docker;
        try {
            docker = DefaultDockerClient.builder()
                    .uri(URI.create(cluster.getUri()))
                    .dockerCertificates(new DockerCertificates(Paths.get(cluster.getCert_directory())))
                    .build();
            List<com.spotify.docker.client.messages.Container> containerList =
                    docker.listContainers(com.spotify.docker.client.DockerClient.ListContainersParam.allContainers());
            return containerFactory.translateContainers(containerList, user);
        } catch (DockerCertificateException | InterruptedException | DockerException e) {
            Logger.error("Unable to retrieve containers: " + e.getLocalizedMessage());
            e.printStackTrace();
        }

        return new ArrayList<Container>();

    }
}
