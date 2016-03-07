package services;

import clients.IDockerClient;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import factories.IClusterFactory;
import models.Cluster;
import models.Configuration;
import models.User;
import play.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dimi5963 on 3/2/16.
 */
public class ConfigurationServiceImpl implements ConfigurationService {

    private final IClusterService clusterService;
    private final IClusterFactory clusterFactory;
    private final IDockerClient dockerClient;

    @Inject
    public ConfigurationServiceImpl(IClusterService clusterService,
                                    IClusterFactory clusterFactory,
                                    IDockerClient dockerClient){
        this.clusterService = clusterService;
        this.clusterFactory = clusterFactory;
        this.dockerClient = dockerClient;
    }

    /***
     * Start repose instance based on user and container id.
     * Get cluster first and then use its creds to start the container
     * @param user
     * @param containerId
     * @throws InternalServerException
     */
    @Override
    public List<Configuration> getConfigurationsForInstance(User user, String containerId) throws InternalServerException {
        Logger.debug("Get configurations for " + containerId);
        ObjectNode response = JsonNodeFactory.instance.objectNode();
        List<Configuration> configurationList = new ArrayList<Configuration>();
        String clusterName = clusterFactory.getClusterName();
        boolean createClusterIfDNE = true;

        if(clusterName != null) {
            Cluster cluster = clusterService.getClusterByName(clusterName, user, createClusterIfDNE);
            if(cluster != null) {
                configurationList = dockerClient.getConfigurationsForInstance(cluster, containerId);

            } else {
                Logger.error("No cluster found.  Cluster creation failed and didn't throw an error.");
                throw new InternalServerException("No cluster found.  Cluster creation failed and didn't throw an error.");
            }
        } else {
            Logger.error("What cluster am I supposed to create?  Misconfigured.");
            throw new InternalServerException("What cluster am I supposed to create?  Misconfigured.");
        }
        return configurationList;
    }
}
