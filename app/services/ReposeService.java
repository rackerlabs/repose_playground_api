package services;

import clients.IDockerClient;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import factories.IClusterFactory;
import models.Cluster;
import models.Container;
import models.User;
import play.Logger;

import java.util.List;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class ReposeService implements IReposeService{

    private final IClusterFactory clusterFactory;
    private final IClusterService clusterService;
    private final IDockerClient dockerClient;

    @Inject
    public ReposeService(IClusterFactory clusterFactory,
            IClusterService clusterService, IDockerClient dockerClient){
        this.clusterFactory = clusterFactory;
        this.clusterService = clusterService;
        this.dockerClient = dockerClient;

    }

    @Override
    public List<Container> getReposeList(User user) throws InternalServerException{
        Logger.debug("Get repose instances");

        Logger.debug("This is not an admin cluster and we want to create it if it doesn't already exist");
        boolean isAdmin = false;
        boolean createClusterIfDNE = true;
        String clusterName = clusterFactory.getClusterName();

        if(clusterName != null) {
            Cluster cluster = clusterService.getClusterByName(clusterName, user, isAdmin, createClusterIfDNE);
            if(cluster != null)
                return dockerClient.getReposeContainers(cluster, user);
            else{
                Logger.error("No cluster found.  Cluster creation failed and didn't throw an error.");
                throw new InternalServerException("No cluster found.  Cluster creation failed and didn't throw an error.");
            }
        } else {
            Logger.error("What cluster am I supposed to create?  Misconfigured.");
            throw new InternalServerException("What cluster am I supposed to create?  Misconfigured.");
        }

    }
}
