package clients;

import com.google.inject.Inject;
import exceptions.InternalServerException;
import factories.ComponentFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class ComponentClientImpl implements ComponentClient {
    @Inject
    WSClient wsClient;

    private final ComponentFactory componentFactory;

    @Inject
    public ComponentClientImpl(ComponentFactory componentFactory){
        this.componentFactory = componentFactory;
    }


    @Override
    public List<String> getFiltersByVersion(String versionId) throws InternalServerException {
        Logger.debug("Get filters for " + versionId);
        String url = componentFactory.getFilterPomUrl(versionId);
        return wsClient.url(url)
                .get().map(
                        wsResponse -> {
                            List<String> componentList = new ArrayList<>();
                            Document document = wsResponse.asXml();
                            NodeList nodeList = document.getElementsByTagName("dependency");
                            for (int i = 0; i < nodeList.getLength(); i++) {
                                Node node = nodeList.item(i);
                                if (node.getNodeType() == Node.ELEMENT_NODE) {
                                    for (int j = 0; j < node.getChildNodes().getLength(); j++) {
                                        Node artifactId = node.getChildNodes().item(j);
                                        if ("artifactId".equals(artifactId.getNodeName())) {
                                            componentList.add(artifactId.getTextContent());
                                        }
                                    }
                                }
                            }
                            Logger.debug("Available filters for this version: " + Json.toJson(componentList));
                            List<String> customFilterList = componentFactory.getAvailableFilters();
                            Logger.debug("Available filters for this user: " + Json.toJson(customFilterList));
                            if (customFilterList != null && !customFilterList.isEmpty()) {
                                return componentList.stream().
                                        filter(customFilterList::contains).collect(Collectors.toList());
                            } else {
                                return componentList;
                            }
                        }
                ).recover(
                        throwable -> {
                            throwable.printStackTrace();
                            throw new InternalServerException(
                                    "We are currently experiencing difficulties.  " +
                                            "Please try again later.");
                        }
                ).get(30000);
    }

    @Override
    public Document getComponentXSD(String versionId, String componentId) throws InternalServerException {
        Logger.debug("Get Component Data for " + componentId + " " + versionId);

        String url = componentFactory.getBindingsUrl(versionId, componentId);
        return wsClient.url(url).get().map(
                wsResponse -> {
                    Document document = wsResponse.asXml();
                    NodeList nodeList = document.getElementsByTagName("bindings");
                    return getComponentXSD(versionId, componentId, nodeList.
                            item(0).getAttributes().
                            getNamedItem("schemaLocation").getTextContent());

                }
        ).recover(
                throwable -> {
                    throw new InternalServerException(
                            "We are currently experiencing difficulties.  " +
                                    "Please try again later.");
                }
        ).get(30000);
    }

    private Document getComponentXSD(String versionId, String componentId, String schemaLocation)
            throws InternalServerException{
        String url = componentFactory.getSchemaUrl(versionId, componentId, schemaLocation);
        return wsClient.url(url).get().map(
                WSResponse::asXml
        ).recover(
                throwable -> {
                    throw new InternalServerException(
                            "We are currently experiencing difficulties.  " +
                                    "Please try again later.");
                }
        ).get(30000);
    }
}
