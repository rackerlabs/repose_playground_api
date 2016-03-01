package clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import models.Cluster;
import models.User;

/**
 * Created by dimi5963 on 2/29/16.
 */
@ImplementedBy(CarinaClient.class)
public interface ICarinaClient {

    boolean createCluster(String clusterName, User user) throws InternalServerException, InterruptedException;

    Cluster getClusterWithZip(final String url, User user, String clusterName, boolean isAdmin);

    JsonNode getCluster(String clusterName, User user) throws InternalServerException;
}
