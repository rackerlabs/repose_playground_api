package clients;

import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import models.Cluster;
import models.Container;
import models.ContainerStats;
import models.User;

import java.util.List;

/**
 * Created by dimi5963 on 2/29/16.
 */
@ImplementedBy(SpotifyDockerClient.class)
public interface IDockerClient {

    List<Container> getReposeContainers(Cluster cluster, User user) throws InternalServerException;

    boolean startReposeInstance(Cluster cluster, String containerId) throws InternalServerException;

    boolean stopReposeInstance(Cluster cluster, String containerId) throws InternalServerException;

    ContainerStats getReposeInstanceStats(Cluster cluster, String containerId) throws InternalServerException;

}
