package factories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import org.w3c.dom.Document;

import java.util.List;

/**
 * Created by dimi5963 on 3/7/16.
 */
@ImplementedBy(ComponentFactoryImpl.class)
public interface ComponentFactory {

    String getBindingsUrl(String versionId, String componentId);

    String getSchemaUrl(String versionId, String componentId, String schemaLocation);

    JsonNode generateJSONTree(String filterName, ObjectNode parentJson, Document document);

    String getFilterPomUrl(String versionId) throws InternalServerException;

    List<String> getAvailableFilters();
}
