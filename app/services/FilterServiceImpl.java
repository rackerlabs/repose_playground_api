package services;

import clients.ComponentClient;
import clients.VersionClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import factories.ComponentFactory;
import factories.XmlFactory;
import org.w3c.dom.Document;
import play.Logger;

import java.util.List;

/**
 * Created by dimi5963 on 3/7/16.
 */
public class FilterServiceImpl implements FilterService {

    private final VersionClient versionClient;
    private final ComponentClient componentClient;
    private final ComponentFactory componentFactory;
    private final XmlFactory xmlFactory;

    @Inject
    public FilterServiceImpl(VersionClient versionClient, ComponentClient componentClient,
                             ComponentFactory componentFactory, XmlFactory xmlFactory){
        this.versionClient = versionClient;
        this.componentClient = componentClient;
        this.componentFactory = componentFactory;
        this.xmlFactory = xmlFactory;
    }

    @Override
    public List<String> getVersions() throws InternalServerException{
        return versionClient.getVersions();
    }

    @Override
    public List<String> getFiltersByVersion(String versionId) throws InternalServerException {
        if(versionId == null)
            throw new InternalServerException("Version not specified.");
        return componentClient.getFiltersByVersion(versionId);
    }

    @Override
    public JsonNode getComponentData(String versionId, String component) throws InternalServerException {
        Logger.debug("Get Component Data for " + component + " " + versionId);

        if(versionId == null || component == null)
            throw new InternalServerException("Version or component not specified.");

        Logger.debug("Retrieve component xsd");
        Document componentXsd = componentClient.getComponentXSD(versionId, component);
        Logger.debug("Returned " + xmlFactory.convertDocumentToString(componentXsd));

        ObjectNode parentJson = JsonNodeFactory.instance.objectNode();

        //JsonNode componentData = Helpers.generateJSONTree(component, parentJson, componentXsd);
        return componentFactory.generateJSONTree(component, parentJson, componentXsd);
    }
}
