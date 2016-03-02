package services;

import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import models.Cluster;
import models.User;

/**
 * Created by dimi5963 on 2/29/16.
 */
@ImplementedBy(ClusterService.class)
public interface IClusterService {

    Cluster getClusterByName(String clusterName, User user, boolean createIfDoesNotExist)
            throws InternalServerException;

}
