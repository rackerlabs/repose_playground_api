package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import models.User;

/**
 * Created by dimi5963 on 3/6/16.
 */
@ImplementedBy(TestServiceImpl.class)
public interface TestService {

    ObjectNode testReposeInstance(User user, String containerId, JsonNode requestBody) throws InternalServerException;
}
