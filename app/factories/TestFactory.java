package factories;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.ImplementedBy;
import models.TestRequest;

import java.util.List;
import java.util.Map;

/**
 * Created by dimi5963 on 3/6/16.
 */
@ImplementedBy(TestFactoryImpl.class)
public interface TestFactory {

    TestRequest translateRequest(JsonNode requestBody);

    Map<String, ?> generateDebugMessageMap(List<String> httpDebugLogList);

}
