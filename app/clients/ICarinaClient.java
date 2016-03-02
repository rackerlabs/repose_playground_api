package clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import exceptions.NotFoundException;
import models.Cluster;
import models.User;

/**
 * Created by dimi5963 on 2/29/16.
 */
@ImplementedBy(CarinaClient.class)
public interface ICarinaClient {

    boolean createCluster(String clusterName, User user) throws InternalServerException, InterruptedException;

    Cluster getClusterWithZip(User user, String clusterName)
            throws NotFoundException, InternalServerException;

    JsonNode getCluster(String clusterName, User user) throws InternalServerException;
}
