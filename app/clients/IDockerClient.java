package clients;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import models.*;

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

    List<Configuration> getConfigurationsForInstance(Cluster cluster, String containerId) throws InternalServerException;

    String createOriginInstance(Cluster cluster, User user, String versionId) throws InternalServerException;

    String createReposeInstance(Cluster cluster, User user, String versionId,
                                List<Configuration> configurationList) throws InternalServerException;

    ObjectNode executeTestAgainstRepose(Cluster cluster, String containerId,
                                        TestRequest testRequest, ObjectNode response) throws InternalServerException;
}
