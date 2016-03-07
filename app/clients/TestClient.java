package clients;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.ImplementedBy;
import models.TestRequest;

/**
 * Created by dimi5963 on 3/6/16.
 */
@ImplementedBy(TestClientImpl.class)
public interface TestClient {

    ObjectNode makeTestRequest(TestRequest testRequest, String ip, String port);
}
