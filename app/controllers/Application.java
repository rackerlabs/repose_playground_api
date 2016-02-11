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
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
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
            filters.put("system-model.cfg.xml", generateSystemModelXml(filters.keySet(), majorVersion, user, id));

            //create containers configuration
            Logger.info("Create repose-container xml.  Hardcode for now, expose more features later :)");
            filters.put("container.cfg.xml", generateContainerXml(majorVersion));

            //create logging configuration
            Logger.info("If version < 7, use log4j.properties.  Else, use log4j2.xml");
            if (majorVersion >= 7)
                filters.put("log4j2.xml", generateLoggingXml(majorVersion));
            else
                filters.put("log4j.properties", generateLoggingXml(majorVersion));

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

    private Element addAppenders(Document document, String appender, Map<String, String> attributes ){
        Element rollingFileAppender = document.createElement(appender);
        attributes.forEach((attrName, attrValue) ->
                rollingFileAppender.setAttribute(attrName, attrValue)
        );
        Element patternLayout = document.createElement("PatternLayout");
        patternLayout.setAttribute("pattern", "%d %-4r [%t] %-5p %c - %m%n");

        rollingFileAppender.appendChild(patternLayout);

        Element policies = document.createElement("Policies");
        rollingFileAppender.appendChild(policies);


        Element sizedBasedTriggeringPolicy = document.createElement("SizeBasedTriggeringPolicy");
        sizedBasedTriggeringPolicy.setAttribute("size", "200 MB");
        policies.appendChild(sizedBasedTriggeringPolicy);

        Element defaultRolloverStrategy = document.createElement("DefaultRolloverStrategy");
        defaultRolloverStrategy.setAttribute("max", "2");
        rollingFileAppender.appendChild(defaultRolloverStrategy);

        return rollingFileAppender;

    }

    private String generateLoggingXml(int majorVersion) {
        if(majorVersion >= 7) {
            //log4j2.xml
            //get new doc builder
            Document document = null;
            try {
                document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            Element rootElement = document.createElement("Configuration");
            rootElement.setAttribute("monitorInterval", "15");
            document.appendChild(rootElement);

            Element appenders = document.createElement("Appenders");
            rootElement.appendChild(appenders);

            Element consoleAppender = document.createElement("Console");
            consoleAppender.setAttribute("name", "STDOUT");
            appenders.appendChild(consoleAppender);

            Element patternLayout = document.createElement("PatternLayout");
            patternLayout.setAttribute("pattern", "%d %-4r [%t] %-5p %c - %m%n");
            consoleAppender.appendChild(patternLayout);

            appenders.appendChild(addAppenders(document, "RollingFile", new HashMap<String, String>() {
                {
                    put("name", "MainRollingFile");
                    put("fileName", "/var/log/repose/current.log");
                    put("filePattern", "/var/log/repose/current-%d{yyyy-MM-dd_HHmmss}.log");
                }
            }));

            appenders.appendChild(addAppenders(document, "RollingFile", new HashMap<String, String>() {
                {
                    put("name", "IntraFilterRollingFile");
                    put("fileName", "/var/log/repose/intra-filter.log");
                    put("filePattern", "/var/log/repose/intra-filter-%d{yyyy-MM-dd_HHmmss}.log");
                }
            }));

            appenders.appendChild(addAppenders(document, "RollingFile", new HashMap<String, String>() {
                {
                    put("name", "HttpRollingFile");
                    put("fileName", "/var/log/repose/http-debug.log");
                    put("filePattern", "/var/log/repose/http-debug-%d{yyyy-MM-dd_HHmmss}.log");
                }
            }));

            Element errorElement = addAppenders(document, "RollingFile", new HashMap<String, String>() {
                {
                    put("name", "ErrorRollingFile");
                    put("fileName", "/var/log/repose/error.log");
                    put("filePattern", "/var/log/repose/error-%d{yyyy-MM-dd_HHmmss}.log");
                }
            });
            errorElement.appendChild(addElement(document, "Filters", new HashMap<String, String>(), Optional.of(
                    addElement(document, "ThresholdFilter", new HashMap<String, String>() {
                        {
                            put("level", "ERROR");
                            put("onMatch", "ACCEPT");
                        }
                    }, Optional.<Element>empty())
            )));

            appenders.appendChild(errorElement);

            Element loggers = document.createElement("Loggers");
            rootElement.appendChild(loggers);

            Element rootRootElement = addElement(document, "Root", new HashMap<String, String>() {
                {
                    put("level", "DEBUG");
                }
            }, Optional.of(addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "ErrorRollingFile");
                }
            }, Optional.<Element>empty())));

            rootRootElement.appendChild(addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "MainRollingFile");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(rootRootElement);

            rootElement.appendChild(addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "STDOUT");
                }
            }, Optional.<Element>empty()));

            rootElement.appendChild(addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "MainRollingFile");
                }
            }, Optional.<Element>empty()));

            rootElement.appendChild(addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "IntraFilterRollingFile");
                }
            }, Optional.<Element>empty()));

            rootElement.appendChild(addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "HttpRollingFile");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "com.sun.jersey");
                    put("level", "off");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "net.sf.ehcache");
                    put("level", "error");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "org.apache");
                    put("level", "debug");
                }
            }, Optional.of(addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "HttpRollingFile");
                }
            }, Optional.<Element>empty()))));

            loggers.appendChild(addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "org.eclipse.jetty");
                    put("level", "off");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "org.springframework");
                    put("level", "debug");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "org.openrepose");
                    put("level", "debug");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "intrafilter-logging");
                    put("level", "trace");
                }
            }, Optional.of(addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "IntraFilterRollingFile");
                }
            }, Optional.<Element>empty()))));
            return convertDocumentToString(document);
        } else {
            //log4j.properties
            return "log4j.rootLogger=DEBUG, consoleOut\n" +
                    "\n" +
                    "#Jetty Logging Turned Off\n" +
                    "log4j.logger.org.eclipse.jetty=OFF\n" +
                    "log4j.logger.com.sun.jersey=OFF\n" +
                    "log4j.logger.org.springframework=WARN\n" +
                    "log4j.logger.org.apache=DEBUG\n" +
                    "log4j.logger.org.openrepose=DEBUG\n" +
                    "log4j.logger.intrafilter-logging=TRACE\n" +
                    "\n" +
                    "# Console\n" +
                    "log4j.appender.consoleOut=org.apache.log4j.ConsoleAppender\n" +
                    "log4j.appender.consoleOut.layout=org.apache.log4j.PatternLayout\n" +
                    "log4j.appender.consoleOut.layout.ConversionPattern=%d %-4r [%t] %-5p %c %x - %m%n";
        }
    }

    private Element addElement(Document doc, String name, Map<String, String> attributeList, Optional<Element> nestedElement){

        Element element = doc.createElement(name);
        attributeList.forEach((attrName, attrValue) ->
                element.setAttribute(attrName, attrValue)
        );

        if(nestedElement.isPresent()){
            element.appendChild(nestedElement.get());
        }

        return element;
    }

    private String generateContainerXml(int majorVersion){
        //get new doc builder
        Document document = null;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Element rootElement = null;
        if(majorVersion >= 7)
            rootElement = document.createElementNS("http://docs.openrepose.org/repose/container/v2.0",
                            "repose-container");
        else
            rootElement =
                    document.createElementNS("http://docs.rackspacecloud.com/repose/container/v2.0",
                            "repose-container");
        //root element
        document.appendChild(rootElement);
        //add deployment-config
        Element deploymentConfig = document.createElement("deployment-config");
        deploymentConfig.setAttribute("connection-timeout", "30000");
        deploymentConfig.setAttribute("read-timeout", "30000");
        rootElement.appendChild(deploymentConfig);

        //add deployment-directory
        Element deploymentDirectory = document.createElement("deployment-directory");
        deploymentDirectory.setAttribute("auto-clean", "true");
        deploymentDirectory.setTextContent("/var/repose");
        deploymentConfig.appendChild(deploymentDirectory);

        //add artifact-directory
        Element artifactDirectory = document.createElement("artifact-directory");
        artifactDirectory.setAttribute("check-interval", "15000");
        artifactDirectory.setTextContent("/usr/share/repose/filters");
        deploymentConfig.appendChild(artifactDirectory);


        //add logging-configuration
        Element loggingConfiguration = document.createElement("logging-configuration");
        if(majorVersion >= 7)
            loggingConfiguration.setAttribute("href", "file:///etc/repose/log4j2.xml");
        else
            loggingConfiguration.setAttribute("href", "log4j.properties");
        deploymentConfig.appendChild(loggingConfiguration);

        return convertDocumentToString(document);

    }

    private String generateSystemModelXml(Set<String> filterNames, int majorVersion, User user, String versionId){
        //get new doc builder
        Document document = null;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Element rootElement = null;
        if(majorVersion >= 7)
            rootElement = document.createElementNS("http://docs.openrepose.org/repose/system-model/v2.0",
                    "system-model");
        else
            rootElement =
                    document.createElementNS("http://docs.rackspacecloud.com/repose/system-model/v2.0",
                            "system-model");
        //root element
        document.appendChild(rootElement);
        //add repose-cluster
        Element reposeCluster = document.createElement("repose-cluster");
        reposeCluster.setAttribute("id", "try-it-now");
        rootElement.appendChild(reposeCluster);

        //add nodes.  Right now there's just one.  Support for multiple is next
        Element reposeClusterNodes = document.createElement("nodes");
        reposeCluster.appendChild(reposeClusterNodes);
        Element reposeClusterNode = document.createElement("node");
        reposeClusterNode.setAttribute("id", "try-it-now-1");
        reposeClusterNode.setAttribute("hostname", "localhost");
        reposeClusterNode.setAttribute("http-port", "8080");
        reposeClusterNodes.appendChild(reposeClusterNode);

        //add filters
        Element filters = document.createElement("filters");
        reposeCluster.appendChild(filters);
        for(String filter: filterNames) {
            Element filterElement = document.createElement("filter");
            //split out the name.cfg.xml and put in the name
            filterElement.setAttribute("name", filter.split(Pattern.quote("."))[0]);
            filterElement.setAttribute("configuration", filter);
            filters.appendChild(filterElement);
        }

        //add destination.  Right now there's just one.  Support for multiple is going to be there someday
        Element destinations = document.createElement("destinations");
        reposeCluster.appendChild(destinations);
        Element endpoint = document.createElement("endpoint");
        endpoint.setAttribute("id", "try-it-now-target");
        endpoint.setAttribute("protocol", "http");
        endpoint.setAttribute("hostname", "repose-origin-" + user.tenant + "-" + versionId.replace('.','-'));
        endpoint.setAttribute("port", "8000");
        endpoint.setAttribute("root-path", "/");
        endpoint.setAttribute("default", "true");
        destinations.appendChild(endpoint);

        return convertDocumentToString(document);

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
                                    currentElement = insertElement(currentElement,
                                            filterXml, nameToken,
                                            jsonNode.get("value").asText(),
                                            jsonNode.get("type").asText(),
                                            !nameIterator.hasNext());
                                }
                            } else {
                                currentElement = insertElement(currentElement,
                                        filterXml, nameToken,
                                        jsonNode.get("value").asText(),
                                        jsonNode.get("type").asText(),
                                        !nameIterator.hasNext());
                            }
                        }
                        printDocument(filterXml);
                    }

                    Logger.info("get the name :" + name);

                }
            }
        } catch(ParserConfigurationException pce){
            pce.printStackTrace();
        }
        Map<String, String> filterMap = new HashMap<>();
        filterXmlMap.forEach((name, doc) ->
                filterMap.put(name, convertDocumentToString(doc))
        );
        return filterMap;
    }

    private Element insertElement(Element parentElement, Document document,
                                  String elementName, String elementValue, String valueType, boolean isLast){
        Logger.trace("In insertElement for " + parentElement +
                " with " + elementName + " and " + elementValue + " (type " + valueType + ")");
        Element currentElement = null;

        if(isLast){
            String patternString = "(.*)\\[(\\d+)\\]$";
            Logger.trace("Check if parent is a list of grandparent " + patternString);
            Matcher matcher = Pattern.compile(patternString).matcher(elementName);
            if(matcher.find()){
                //get the real parent.
                Element realParentElement =
                        getRealParentElement(parentElement, Integer.parseInt(matcher.group(2)), document);

                switch(valueType){
                    case "text":
                        realParentElement.setTextContent(elementValue);
                        break;
                    case "attribute":
                        realParentElement.setAttribute(matcher.group(1), elementValue);
                        break;
                    default:
                        Logger.error(valueType + " is not defined.");
                }
            } else {
                //not a list
                switch(valueType){
                    case "text":
                        parentElement.setTextContent(elementValue);
                        break;
                    case "attribute":
                        parentElement.setAttribute(elementName, elementValue);
                        break;
                    default:
                        Logger.error(valueType + " is not defined.");
                }
            }
        } else {
            Logger.trace("Check if parent has childnodes that equal to " + elementName);
            for(int child = 0; child < parentElement.getChildNodes().getLength(); child ++) {
                if(parentElement.getChildNodes().item(child).getNodeName().equals(elementName)){
                    //element found
                    return (Element)parentElement.getChildNodes().item(child);
                }
            }
            //not found
            //check if it's a list first
            if(elementName.contains("[") && elementName.contains("]")){
                Logger.trace("element is a list");
                elementName = elementName.split(Pattern.quote("["))[0];
            } else {
                Logger.trace("not yet added.");
                currentElement = document.createElement(elementName);
                parentElement.appendChild(currentElement);
            }
        }


        return currentElement;
    }

    private Element getRealParentElement(Element currentElement, int order, Document document){
        if(currentElement.getParentNode().getChildNodes().getLength() >= order){
            //we haven't created this node yet. Let's do it.
            for(int child = 0;
                child <= order - (currentElement.getParentNode().getChildNodes().getLength());
                child ++ ){
                Element newElement = document.createElement(currentElement.getNodeName());
                currentElement.getParentNode().appendChild(newElement);
            }
        }
        return (Element)currentElement.getParentNode().getChildNodes().item(order);
    }

    private String convertDocumentToString(Document doc) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.getBuffer().toString().replaceAll("\n|\r", "");
        }catch(TransformerException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void printDocument(Document doc) {
        Logger.info(convertDocumentToString(doc));
    }
}

