package services;

import clients.IDockerClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import factories.IClusterFactory;
import factories.TestFactory;
import models.Cluster;
import models.TestRequest;
import models.User;
import play.Logger;

/**
 * Created by dimi5963 on 3/6/16.
 */
public class TestServiceImpl implements  TestService {

    private final IClusterFactory clusterFactory;
    private final IClusterService clusterService;
    private final IDockerClient dockerClient;
    private final TestFactory testFactory;

    @Inject
    public TestServiceImpl(IClusterService clusterService, IClusterFactory clusterFactory,
                           IDockerClient dockerClient, TestFactory testFactory) {
        this.clusterService = clusterService;
        this.clusterFactory = clusterFactory;
        this.dockerClient = dockerClient;
        this.testFactory = testFactory;
    }

    public ObjectNode testReposeInstance(User user, String containerId, JsonNode requestBody) throws InternalServerException {
        Logger.debug("test repose instance " + containerId);

        if(requestBody == null)
            throw new InternalServerException("Request is malformed.");

        boolean createClusterIfDNE = true;
        String clusterName = clusterFactory.getClusterName();
        if (clusterName != null) {
            Cluster cluster = clusterService.getClusterByName(clusterName, user, createClusterIfDNE);
            if (cluster != null) {
                ObjectNode response = JsonNodeFactory.instance.objectNode();
                response.putPOJO("request", requestBody);
                TestRequest testRequest = testFactory.translateRequest(requestBody);
                return dockerClient.executeTestAgainstRepose(cluster, containerId, testRequest, response);
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