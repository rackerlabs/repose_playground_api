package factories;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.ImplementedBy;
import models.Test;

import java.util.List;
import java.util.Map;

/**
 * Created by dimi5963 on 3/6/16.
 */
@ImplementedBy(TestFactoryImpl.class)
public interface TestFactory {

    Test translateRequest(JsonNode requestBody);

    Map<String, ?> generateDebugMessageMap(List<String> httpDebugLogList);

}
