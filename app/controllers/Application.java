package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.InternalServerException;
import helpers.Helpers;
import models.Filter;
import models.User;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import play.Logger;
import play.libs.F;
import play.libs.F.Function;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.*;
import java.util.regex.Pattern;

public class Application extends Controller {

    public F.Promise<Result> versions() {
        F.Promise<Result> resultPromise = WS.url("https://api.github.com/repos/rackerlabs/repose/tags")
                .setHeader("Authorization", "token " + play.Play.application().configuration().getString("oauth.token")).get().map(
                new Function<WSResponse, Result>(){
                    public Result apply(WSResponse response){
                        Logger.info(response.getAllHeaders().toString());
                        Iterator<JsonNode> result = response.asJson().elements();
                        ObjectMapper mapper = new ObjectMapper();
                        List<String> tagList = new ArrayList<String>();
                        while(result.hasNext()){
                            tagList.add(result.next().findValue("name").textValue().replaceAll("repose-", "").replaceAll("papi-", ""));
                        }
                        JsonNode results = mapper.valueToTree(tagList);
                        return ok(results);
                    }
                }
        );
        return resultPromise;
    }

    public F.Promise<Result> componentsByVersion(String id) {
        Logger.info("Version id " + id.split(Pattern.quote("."))[0]);
        F.Promise<Result> resultPromise = null;
        if (Integer.parseInt(id.split(Pattern.quote("."))[0]) >= 7) {
            resultPromise = WS.url(
                    "https://maven.research.rackspacecloud.com/content/repositories/releases/org/openrepose/filter-bundle/" +
                            id +
                            "/filter-bundle-" + id + ".pom")
                    .get().map(
                    new Function<WSResponse, Result>() {
                        @Override
                        public Result apply(WSResponse wsResponse) throws Throwable {
                            String filterListString = play.Play.application().configuration().getString("filter.list");
                            List<String> filterList = new ArrayList<String>();
                            if(filterListString != null)
                                filterList = Arrays.asList(filterListString);
                            List<String> componentList = new ArrayList<String>();
                            ObjectMapper mapper = new ObjectMapper();
                            Document document = wsResponse.asXml();
                            NodeList nodeList = document.getElementsByTagName("dependency");
                            for (int i = 0; i < nodeList.getLength(); i++) {
                                Node node = nodeList.item(i);
                                if (node.getNodeType() == Node.ELEMENT_NODE) {
                                    for (int j = 0; j < node.getChildNodes().getLength(); j++) {
                                        Node artifactId = node.getChildNodes().item(j);
                                        if (artifactId.getNodeName() == "artifactId") {
                                            if(filterList.isEmpty() || (!filterList.isEmpty() && filterList.contains(artifactId.getTextContent())))
                                                componentList.add(artifactId.getTextContent());
                                        }
                                    }
                                }
                            }
                            return ok((JsonNode) mapper.valueToTree(componentList));
                        }
                    }
            );
        } else {
            resultPromise = WS.url(
                    "https://maven.research.rackspacecloud.com/content/repositories/releases/com/rackspace/papi/components/filter-bundle/" +
                            id +
                            "/filter-bundle-" + id + ".pom").get().map(
                    new Function<WSResponse, Result>() {
                        @Override
                        public Result apply(WSResponse wsResponse) throws Throwable {
                            switch(wsResponse.getStatus()){
                                case 200:
                                    List<String> componentList = new ArrayList<String>();
                                    ObjectMapper mapper = new ObjectMapper();
                                    Document document = wsResponse.asXml();
                                    NodeList nodeList = document.getElementsByTagName("dependency");
                                    for (int i = 0; i < nodeList.getLength(); i++) {
                                        Node node = nodeList.item(i);
                                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                                            // do something with the current element
                                            for (int j = 0; j < node.getChildNodes().getLength(); j++) {
                                                Node artifactId = node.getChildNodes().item(j);
                                                if (artifactId.getNodeName() == "artifactId") {
                                                    componentList.add(artifactId.getTextContent());
                                                }
                                            }
                                        }
                                    }
                                    return ok((JsonNode) mapper.valueToTree(componentList));
                                case 404:
                                    return notFound();
                                default:
                                    return notFound();
                            }

                        }
                    }
            );
        }
        return resultPromise;
    }

    /**
     * Supported are:
     * - ip-identity
     * - uri-normalization
     * - content-normalization
     * - header-normalization
     * - header-identity
     * - header-id-mapping
     * - uri-identity
     * - destination-router
     * - uri-stripper
     * @param id
     * @param componentId
     * @return
     */
    public F.Promise<Result> component(String id, String componentId) {
        F.Promise<Result> resultPromise = WS.url(
                "https://raw.githubusercontent.com/rackerlabs/repose/repose-" +
                        id +"/repose-aggregator/components/filters/" +
                        componentId + "/src/main/resources/META-INF/schema/config/bindings.xjb").get().flatMap(
                new Function<WSResponse, F.Promise<Result>>() {
                    @Override
                    public F.Promise<Result> apply(WSResponse wsResponse) throws Throwable {
                        List<String> componentList = new ArrayList<String>();
                        Document document = wsResponse.asXml();
                        NodeList nodeList = document.getElementsByTagName("bindings");
                        String schemaLocation = "https://raw.githubusercontent.com/rackerlabs/repose/repose-" +
                                id + "/repose-aggregator/components/filters/" + componentId +
                                "/src/main/resources/META-INF/schema/config/" + nodeList.
                                item(0).getAttributes().
                                getNamedItem("schemaLocation").getTextContent();

                        return WS.url(
                                "https://raw.githubusercontent.com/rackerlabs/repose/repose-" +
                                        id + "/repose-aggregator/components/filters/" + componentId +
                                        "/src/main/resources/META-INF/schema/config/" + schemaLocation).get().map(
                                new Function<WSResponse, Result>() {
                                    @Override
                                    public Result apply(WSResponse innerWsResponse) throws Throwable {
                                        JSONObject parentJson = new JSONObject();
                                        List<String> componentList = new ArrayList<String>();
                                        ObjectMapper mapper = new ObjectMapper();

                                        JSONObject object = Helpers.generateJSONTree(componentId, parentJson,
                                                innerWsResponse.asXml());
                                        ObjectNode result = (ObjectNode)Json.parse(object.toString());
                                        return ok(result);
                                    }
                                }
                        );
                    }
                }
        );
        return resultPromise;
    }

    /**
     * Build repose instance.
     * @param id version id
     * @return
     */
    public Result build(String id)  {
        /**
         * We build out the repose instance here.
         * 1. we convert json payload to proper xmls
         * 2. we create container, log4j, and system-model appropriately
         * 3. we get the cluster for the user
         * 4. we create a repose instance
         * 5. we create a repose origin service instance
         */
        Logger.trace("Let's create a repose instance.");
        Logger.info(play.Play.application().path().getAbsolutePath());

        String token = request().getHeader("Token");

        User user = User.findByToken(token);

        if(user != null) {
            JsonNode jsonRequest = request().body().asJson();
            Logger.info(request().body().asJson().toString());

            Map<String, String> filters = getFilterXmls(jsonRequest);
            Logger.info("Number of filters: " + filters.size());

            int majorVersion = Integer.parseInt(id.split(Pattern.quote("."))[0]);

            //create system model configuration with the specified filters (right now order doesn't matter)
            Logger.info("Create system-model xml.  Hardcode for now, expose more features later :)");
            filters.put("system-model.cfg.xml", Helpers.generateSystemModelXml(filters.keySet(), majorVersion, user, id));

            //create containers configuration
            Logger.info("Create repose-container xml.  Hardcode for now, expose more features later :)");
            filters.put("container.cfg.xml", Helpers.generateContainerXml(majorVersion));

            //create logging configuration
            Logger.info("If version < 7, use log4j.properties.  Else, use log4j2.xml");
            if (majorVersion >= 7)
                filters.put("log4j2.xml", Helpers.generateLoggingXml(majorVersion));
            else
                filters.put("log4j.properties", Helpers.generateLoggingXml(majorVersion));

            filters.forEach((name, filter) ->
                            Logger.info("Filter: " + name + " and xml: " + filter)
            );

            //spin up a new container in admin cluster
            //copy over creds for tenant
            //copy over xmls
            //use new container to spin up repose container
            Logger.info("Log into user account and return " +
                    play.Play.application().configuration().getString("user.cluster.name") + " cluster");

            Logger.info("Create new docker instance");
            try {
                new models.Container().createOriginContainer(user, id);
                String reposeId = new models.Container().createReposeContainer(user, filters, id);
                return ok(Json.parse("{\"message\": \"success\",\"id\": \"" + reposeId + "\"}"));
            } catch (InternalServerException e) {
                return internalServerError(e.getLocalizedMessage());
            }
        } else {
            return unauthorized();
        }


    }

    private Map<String, String> getFilterXmls(JsonNode node) {
        Map<String, Document> filterXmlMap = new HashMap<>();

        //create new instance of doc factory
        DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
        //get new doc builder
        DocumentBuilder icBuilder;
        try {
            if (node.isArray()) {
                Iterator<JsonNode> jsonNodeIterator = node.elements();
                while (jsonNodeIterator.hasNext()) {
                    Document filterXml = null;
                    JsonNode jsonNode = jsonNodeIterator.next();
                    JsonNode name = jsonNode.get("filter");
                    Filter filter = Filter.findByName(name.textValue());
                    if (filter != null) {
                        //does filter already set in the map
                        filterXml = filterXmlMap.get(filter.name + ".cfg.xml");
                        if (filterXml == null) {
                            icBuilder = icFactory.newDocumentBuilder();
                            filterXml = icBuilder.newDocument();
                            filterXmlMap.put(filter.name + ".cfg.xml", filterXml);
                        }
                        //iterate through each token of the name and create an xml tree if one does not exist.
                        Logger.info(jsonNode.get("name").asText());
                        String[] nameTokens = jsonNode.get("name").asText().split(Pattern.quote("."));
                        Iterator<String> nameIterator = Arrays.asList(nameTokens).iterator();
                        Element currentElement = filterXml.getDocumentElement();
                        while(nameIterator.hasNext()){
                            String nameToken = nameIterator.next();
                            if(currentElement == filterXml.getDocumentElement()) {
                                //this is the root element
                                if(filterXml.getDocumentElement() == null) {
                                    //this document is empty!  add a new one
                                    Element rootElement = filterXml.createElementNS(filter.namespace, nameToken);
                                    filterXml.appendChild(rootElement);
                                    currentElement = rootElement;
                                } else if(!currentElement.getNodeName().equals(nameToken)){
                                    //not root nameToken.  gotta add
                                    currentElement = Helpers.insertElement(currentElement,
                                            filterXml, nameToken,
                                            jsonNode.get("value").asText(),
                                            jsonNode.get("type").asText(),
                                            !nameIterator.hasNext());
                                }
                            } else {
                                currentElement = Helpers.insertElement(currentElement,
                                        filterXml, nameToken,
                                        jsonNode.get("value").asText(),
                                        jsonNode.get("type").asText(),
                                        !nameIterator.hasNext());
                            }
                        }
                        Helpers.printDocument(filterXml);
                    }

                    Logger.info("get the name :" + name);

                }
            }
        } catch(ParserConfigurationException pce){
            pce.printStackTrace();
        }
        Map<String, String> filterMap = new HashMap<>();
        filterXmlMap.forEach((name, doc) ->
                        filterMap.put(name, Helpers.convertDocumentToString(doc))
        );
        return filterMap;
    }
}

