package services;

import clients.IDockerClient;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import models.Cluster;
import models.Configuration;
import models.User;
import play.Logger;

import java.util.List;

/**
 * Created by dimi5963 on 3/2/16.
 */
public class DockerApplicationService implements ApplicationService {

    private final IDockerClient dockerClient;

    @Inject
    public DockerApplicationService(IDockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public String createReposeInstance(Cluster cluster, User user,
                                       List<Configuration> configurationList, String versionId)
            throws InternalServerException {
        return dockerClient.createReposeInstance(cluster, user, versionId, configurationList);
    }

    @Override
    public String createOriginInstance(Cluster cluster, User user, String versionId) throws InternalServerException {
        Logger.debug("create origin instance " + user + " for version " + versionId);
        return dockerClient.createOriginInstance(cluster, user, versionId);

    }

    @Override
    public String createThirdPartyInstance(Cluster cluster, User user,
                                           String containerId) throws InternalServerException {
        return null;
    }
}
