package services;

import clients.IDockerClient;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import factories.ConfigurationFactory;
import factories.IClusterFactory;
import models.*;
import play.Logger;

import java.util.List;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class ReposeService implements IReposeService {

    private final IClusterFactory clusterFactory;
    private final IClusterService clusterService;
    private final IDockerClient dockerClient;
    private final EnvironmentService environmentService;
    private final ConfigurationFactory configurationFactory;

    @Inject
    public ReposeService(IClusterFactory clusterFactory,
                         IClusterService clusterService, IDockerClient dockerClient,
                         EnvironmentService environmentService,
                         ConfigurationFactory configurationFactory) {
        this.clusterFactory = clusterFactory;
        this.clusterService = clusterService;
        this.dockerClient = dockerClient;
        this.environmentService = environmentService;
        this.configurationFactory = configurationFactory;
    }

    @Override
    public List<Container> getReposeList(User user) throws InternalServerException {
        Logger.debug("Get repose instances");

        Logger.debug("We want to create the cluster if it doesn't already exist");
        boolean createClusterIfDNE = true;
        String clusterName = clusterFactory.getClusterName();

        if (clusterName != null) {
            Cluster cluster = clusterService.getClusterByName(clusterName, user, createClusterIfDNE);
            if (cluster != null)
                return dockerClient.getReposeContainers(cluster, user);
            else {
                Logger.error("No cluster found.  Cluster creation failed and didn't throw an error.");
                throw new InternalServerException("No cluster found.  Cluster creation failed and didn't throw an error.");
            }
        } else {
            Logger.error("What cluster am I supposed to create?  Misconfigured.");
            throw new InternalServerException("What cluster am I supposed to create?  Misconfigured.");
        }

    }

    @Override
    public boolean stopReposeInstance(User user, String containerId) throws InternalServerException {
        Logger.debug("Stop repose instance " + containerId);

        Logger.debug("We want to create the cluster if it doesn't already exist");
        boolean createClusterIfDNE = true;
        String clusterName = clusterFactory.getClusterName();

        if (clusterName != null) {
            Cluster cluster = clusterService.getClusterByName(clusterName, user, createClusterIfDNE);
            if (cluster != null)
                return dockerClient.stopReposeInstance(cluster, containerId);
            else {
                Logger.error("No cluster found.  Cluster creation failed and didn't throw an error.");
                throw new InternalServerException("No cluster found.  Cluster creation failed and didn't throw an error.");
            }
        } else {
            Logger.error("What cluster am I supposed to create?  Misconfigured.");
            throw new InternalServerException("What cluster am I supposed to create?  Misconfigured.");
        }
    }

    @Override
    public boolean startReposeInstance(User user, String containerId) throws InternalServerException {
        Logger.debug("Start repose instance " + containerId);

        Logger.debug("We want to create the cluster if it doesn't already exist");
        boolean createClusterIfDNE = true;
        String clusterName = clusterFactory.getClusterName();

        if (clusterName != null) {
            Cluster cluster = clusterService.getClusterByName(clusterName, user, createClusterIfDNE);
            if (cluster != null)
                return dockerClient.startReposeInstance(cluster, containerId);
            else {
                Logger.error("No cluster found.  Cluster creation failed and didn't throw an error.");
                throw new InternalServerException("No cluster found.  Cluster creation failed and didn't throw an error.");
            }
        } else {
            Logger.error("What cluster am I supposed to create?  Misconfigured.");
            throw new InternalServerException("What cluster am I supposed to create?  Misconfigured.");
        }
    }

    @Override
    public ContainerStats getInstanceStats(User user, String containerId) throws InternalServerException {
        Logger.debug("Return repose instance stats for " + containerId);

        Logger.debug("We want to create the cluster if it doesn't already exist");
        boolean createClusterIfDNE = true;
        String clusterName = clusterFactory.getClusterName();
        if (clusterName != null) {
            Cluster cluster = clusterService.getClusterByName(clusterName, user, createClusterIfDNE);
            if (cluster != null)
                return dockerClient.getReposeInstanceStats(cluster, containerId);
            else {
                Logger.error("No cluster found.  Cluster creation failed and didn't throw an error.");
                throw new InternalServerException("No cluster found.  Cluster creation failed and didn't throw an error.");
            }
        } else {
            Logger.error("What cluster am I supposed to create?  Misconfigured.");
            throw new InternalServerException("What cluster am I supposed to create?  Misconfigured.");
        }
    }

    @Override
    public String setUpReposeEnvironment(ReposeEnvironmentType reposeEnvironmentType,
                                         User user, String versionId, List<Configuration> configurationList)
            throws InternalServerException {
        //create repose container
        //create origin container
        Logger.debug("Set up repose instance for version " + versionId + " with type " + reposeEnvironmentType);

        Logger.debug("We want to create the cluster if it doesn't already exist");
        boolean createClusterIfDNE = true;
        String clusterName = clusterFactory.getClusterName();

        if (clusterName != null) {
            Cluster cluster = clusterService.getClusterByName(clusterName, user, createClusterIfDNE);
            if (cluster != null) {
                switch(reposeEnvironmentType){
                    case GENERATED_ORIGIN:
                        return environmentService.generatedOriginEnvironment(cluster,
                                versionId, user, configurationList);
                    case GENERATED_THIRDPARTIES:
                        throw new InternalServerException("Currently not implemented.");
                    case MIXED_THIRD_PARTIES:
                        throw new InternalServerException("Currently not implemented.");
                    case SPECIFIED_ORIGIN:
                        throw new InternalServerException("Currently not implemented.");
                    case SPECIFIED_ORIGIN_GENERATED_THIRD_PARTIES:
                        throw new InternalServerException("Currently not implemented.");
                    case SPECIFIED_ORIGIN_MIXED_THIRD_PARTIES:
                        throw new InternalServerException("Currently not implemented.");
                    case SPECIFIED_ORIGIN_SPECIFIED_THIRD_PARTIES:
                        throw new InternalServerException("Currently not implemented.");
                    default:
                        return environmentService.generatedOriginEnvironment(cluster,
                                versionId, user, configurationList);

                }
            } else {
                Logger.error("No cluster found.  Cluster creation failed and didn't throw an error.");
                throw new InternalServerException("No cluster found.  Cluster creation failed and didn't throw an error.");
            }
        } else {
            Logger.error("What cluster am I supposed to create?  Misconfigured.");
            throw new InternalServerException("What cluster am I supposed to create?  Misconfigured.");
        }
    }
}
