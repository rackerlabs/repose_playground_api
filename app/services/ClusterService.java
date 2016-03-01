package services;

import clients.ICarinaClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import exceptions.NotFoundException;
import factories.IClusterFactory;
import models.Cluster;
import models.User;
import play.Logger;
import repositories.IClusterRepository;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class ClusterService implements IClusterService {

    private final IClusterRepository clusterRepository;
    private final ICarinaClient carinaClient;
    private final IClusterFactory clusterFactory;

    @Inject
    public ClusterService(IClusterRepository clusterRepository,
                          ICarinaClient carinaClient, IClusterFactory clusterFactory) {
        this.clusterRepository = clusterRepository;
        this.carinaClient = carinaClient;
        this.clusterFactory = clusterFactory;
    }


    /**
     * Request to retrieve user cluster by its name
     * @param clusterName
     * @param user
     * @param isAdmin
     * @param createIfDoesNotExist
     * @return
     * @throws InternalServerException
     */
    @Override
    public Cluster getClusterByName(String clusterName, User user, boolean isAdmin, boolean createIfDoesNotExist)
            throws InternalServerException {
        Logger.debug("Get cluster " + clusterName);

        if(user == null)
            throw new InternalServerException("User not provided.");

        Logger.debug("check to see if a cluster exists for this user");
        Cluster reposeCluster = clusterRepository.findByUserandName(user.id, clusterName);
        if(reposeCluster == null) {
            //does it exist?
            if(!doesClusterExist(clusterName, user)) {
                if (createIfDoesNotExist) {
                    try {
                        carinaClient.createCluster(clusterName, user);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                        throw new InternalServerException(ie.getLocalizedMessage());
                    }
                } else {
                    //oops
                    throw new InternalServerException("Cluster doesn't exist.");
                }
            }
            try {
                reposeCluster = carinaClient.getClusterWithZip(
                        clusterFactory.getCarinaZipUrl(user.username, clusterName),
                        user, clusterName, isAdmin);
                if(reposeCluster != null)
                    clusterRepository.save(reposeCluster);
                else
                    throw new InternalServerException("No cluster available to save.");
            }catch(NotFoundException | InternalServerException e ){
                Logger.error("Unable to save new cluster. " + e.getLocalizedMessage());
                e.printStackTrace();
                throw new InternalServerException("Unable to save new cluster.");
            }
        }
        Logger.debug("Return cluster " + reposeCluster);
        return reposeCluster;
    }

    private boolean doesClusterExist(String clusterName, User user) throws InternalServerException {
        JsonNode statusNode = carinaClient.getCluster(clusterName, user);
        if(statusNode != null) {
            return statusNode.asText().equals("active");
        }else {
            return false;
        }

    }


}
