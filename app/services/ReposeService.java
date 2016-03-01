package services;

import clients.IDockerClient;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import factories.IClusterFactory;
import factories.IContainerFactory;
import models.Cluster;
import models.Container;
import models.User;
import play.Logger;

import java.util.List;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class ReposeService implements IReposeService{

    private final IContainerFactory containerFactory;
    private final IClusterFactory clusterFactory;
    private final IClusterService clusterService;
    private final IDockerClient dockerClient;

    @Inject
    public ReposeService(
            IContainerFactory containerFactory, IClusterFactory clusterFactory,
            IClusterService clusterService, IDockerClient dockerClient){
        this.containerFactory = containerFactory;
        this.clusterFactory = clusterFactory;
        this.clusterService = clusterService;
        this.dockerClient = dockerClient;

    }

    @Override
    public List<Container> getReposeList(User user) throws InternalServerException{
        Logger.debug("Get repose instances");

        Cluster cluster = null;
        cluster = clusterService.getClusterByName(
                clusterFactory.getClusterName(), user, false, true);
        return dockerClient.getReposeContainers(cluster, user);

    }
}
