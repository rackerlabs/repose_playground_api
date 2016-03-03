package helpers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.Filter;
import models.User;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import play.Logger;
import play.libs.Json;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * Created by dimi5963 on 8/15/15.
 */
@Deprecated
public class Helpers {

    private static List<String> nativeElements = Arrays.asList("string", "double", "boolean", "anyURI");
    private static List<String> attributeElements = Arrays.asList("minOccurs", "maxOccurs", "use", "default");

    public static Filter getFilterNamespace(String filterName){
        return Filter.findByName(filterName);
    }

    private static void saveFilterNamespace(String filterName, String namespace){
        Logger.info("Save filter namespace: " + filterName + " and namespace " + namespace);
        Filter filter = getFilterNamespace(filterName);
        if(filter == null){
            filter = new Filter();
            filter.setName(filterName);
        }
        filter.setNamespace(namespace);
        filter.save();
    }

    /**
     *
     * @param jsonObject
     * @param document
     */
    public static JSONObject generateJSONTree(String filterName, JSONObject jsonObject, Document document) {
        //figure out if current node has
        //get children of current node
        Logger.info("Generate JSON tree");
        Node schema = document.getElementsByTagName("xs:schema").item(0);
        NodeList schemaList = schema.getChildNodes();
        for (int i = 0; i < schemaList.getLength(); i++) {
            //get element
            if (schemaList.item(i).getNodeName() == "xs:element") {
                Logger.info("This is the starting point of xsd.  Le'go!");
                parseElement(jsonObject, document, schemaList.item(i));
                break;
            }
        }

        //save the filter namespace
        saveFilterNamespace(filterName, schema.getAttributes().getNamedItem("targetNamespace").getTextContent());


        return jsonObject;

    }

    private static void updateAnnotation(Node node, JSONObject jsonObject){
        Logger.info("Let's update the docs for " + node.getTextContent());
        String doc = "";
        for(int l = 0; l < node.getChildNodes().getLength(); l ++){
            if(node.getChildNodes().item(l).getNodeName() ==
                    "xs:documentation") {
                for(int m = 0; m < node.getChildNodes().item(l).getChildNodes().getLength(); m ++){
                    if(node.getChildNodes().item(l).getChildNodes().item(m).getNodeName() == "html:p"){
                        doc = doc.concat(node.getChildNodes().item(l).getChildNodes().item(m).getTextContent());

                    }
                }
            }
        }
        if(doc.length() > 0) {
            addToJsonObject(jsonObject, "doc", doc);
        }
    }

    private static void updateList(Node node, JSONObject jsonObject, Document document) {
        Logger.info("Let's get everything into a list");
        String elementType = node.getAttributes().getNamedItem("itemType").getTextContent();
        String[] elementTypeTokens = elementType.split(":");
        String itemType = elementTypeTokens.length > 1 ? elementTypeTokens[1] : elementTypeTokens[0];
        Logger.info("Check if " + itemType + " is native");
        if(!nativeElements.contains(itemType)){
            Node child = retrieveChild(elementType, document);

            Logger.info("child is " + child.getNodeName());
            if(child != null && child.getNodeName().contains("complexType")) {
                Logger.info("It's a complex type");
                JSONArray innerJsonArray = new JSONArray();
                addToJsonObject(jsonObject, "items", innerJsonArray);
                Logger.info("Retrieve the child element");
                parseComplexType(jsonObject, innerJsonArray, document, child);
            } else {
                addToJsonObject(jsonObject, "name", "value");
                addToJsonObject(jsonObject, "xsd-type", "text");
                parseSimpleType(jsonObject, document, child);
            }


        }
    }

    private static void updateAll(Node node, JSONArray jsonArray, Document document){
        NodeList allNodeList = node.getChildNodes();
        for(int l = 0; l < allNodeList.getLength(); l ++){
            if(allNodeList.item(l).getNodeName().contains("element")){
                JSONObject jsonObject = new JSONObject();
                Logger.info("Check if minOccurs attribute exists.  By default it's 1");
                if(allNodeList.item(l).hasAttributes() &&
                        allNodeList.item(l).getAttributes().getNamedItem("minOccurs") != null){
                    addToJsonObject(jsonObject, "minOccurs",
                            allNodeList.item(l).getAttributes().getNamedItem("minOccurs").getTextContent());
                    if(allNodeList.item(l).getAttributes().getNamedItem("minOccurs").getTextContent().equals("1")){
                        addToJsonObject(jsonObject, "required", "required");
                    } else {
                        addToJsonObject(jsonObject, "required", "optional");
                    }
                } else {
                    addToJsonObject(jsonObject, "minOccurs", "1");
                    addToJsonObject(jsonObject, "required", "required");
                }

                parseElement(jsonObject, document, allNodeList.item(l));
                jsonArray.put(jsonObject);
            }
        }
    }

    private static void updateRestriction(Node node, JSONObject jsonObject) {
        Logger.info("Update restriction for " + node.getNodeName());
        String[] baseTypeTokens = node.getAttributes().getNamedItem("base").getTextContent().split(":");
        String baseType = baseTypeTokens.length > 1 ? baseTypeTokens[1] : baseTypeTokens[0];
        addToJsonObject(jsonObject, "type", baseType);
        for(int childNode = 0; childNode < node.getChildNodes().getLength(); childNode ++){
            String[] elementTypeTokens = node.getChildNodes().item(childNode).getNodeName().split(":");
            String itemType = elementTypeTokens.length > 1 ? elementTypeTokens[1] : elementTypeTokens[0];
            Logger.info("Restriction type " + itemType);
            switch(itemType){
                case "enumeration":
                    List<String> enumerationList = getJsonObject(jsonObject, "enumeration");
                    Logger.info("let's get some enumeration up in here. " +
                                    node.getChildNodes().item(childNode).getAttributes().getNamedItem("value").getTextContent()
                    );
                    if(enumerationList == null){
                        enumerationList = new ArrayList<String>();
                        enumerationList.add(
                                node.getChildNodes().item(childNode).getAttributes().getNamedItem("value").getTextContent()
                        );
                    } else {
                        Logger.info("enumeration: " + enumerationList);
                        enumerationList.add(
                                node.getChildNodes().item(childNode).getAttributes().getNamedItem("value").getTextContent());
                    }
                    addToJsonObject(jsonObject, itemType, enumerationList);
                    addToJsonObject(jsonObject, "type", "select");
                    break;
                case "#text":
                    break;
                default:
                    addToJsonObject(jsonObject,
                            itemType,
                            node.getChildNodes().item(childNode).getAttributes().getNamedItem("value").getTextContent());

            }
        }
    }

    private static void updateChoice(Node node, JSONArray jsonArray, Document document){
        Logger.info("Let's get us those radio buttons");
        NodeList allNodeList = node.getChildNodes();
        for(int l = 0; l < allNodeList.getLength(); l ++){
            if(allNodeList.item(l).getNodeName().contains("element")){
                JSONObject jsonObject = new JSONObject();
                if(allNodeList.item(l).getAttributes().getNamedItem("minOccurs") != null){
                    if(allNodeList.item(l).getAttributes().getNamedItem("minOccurs").getTextContent().equals("1")){
                        addToJsonObject(jsonObject, "required", "required");
                    } else {
                        addToJsonObject(jsonObject, "required", "optional");
                    }
                    addToJsonObject(jsonObject, "minOccurs",
                            allNodeList.item(l).getAttributes().getNamedItem("minOccurs").getTextContent());
                    addToJsonObject(jsonObject, "minOccurs",
                            allNodeList.item(l).getAttributes().getNamedItem("minOccurs").getTextContent());

                } else {
                    addToJsonObject(jsonObject, "required", "optional");
                }
                addToJsonObject(jsonObject, "type", "radio");
                parseElement(jsonObject, document, allNodeList.item(l));
                jsonArray.put(jsonObject);
            }
        }
    }

    private static void updateSequence(Node node, JSONArray jsonArray, Document document) {
        NodeList allNodeList = node.getChildNodes();
        for(int l = 0; l < allNodeList.getLength(); l ++){
            if(allNodeList.item(l).getNodeName().contains("element")){
                JSONObject jsonObject = new JSONObject();
                if(allNodeList.item(l).getAttributes().getNamedItem("minOccurs") != null){
                    if(allNodeList.item(l).getAttributes().getNamedItem("minOccurs").getTextContent().equals("1")){
                        addToJsonObject(jsonObject, "required", "required");
                    } else {
                        addToJsonObject(jsonObject, "required", "optional");
                    }
                    addToJsonObject(jsonObject, "minOccurs",
                            allNodeList.item(l).getAttributes().getNamedItem("minOccurs").getTextContent());
                    addToJsonObject(jsonObject, "minOccurs",
                            allNodeList.item(l).getAttributes().getNamedItem("minOccurs").getTextContent());

                } else {
                    addToJsonObject(jsonObject, "required", "optional");
                }
                if(allNodeList.item(l).getAttributes().getNamedItem("maxOccurs") != null &&
                        !allNodeList.item(l).getAttributes().getNamedItem("maxOccurs").getTextContent().equals("1"))
                    addToJsonObject(jsonObject, "type", "list");
                parseElement(jsonObject, document, allNodeList.item(l));
                jsonArray.put(jsonObject);
            }
        }
    }

    private static void updateSimpleContent(Node node, JSONArray jsonArray, Document document){
        Logger.info("Iterate through children of simple content");
        for(int childNode = 0; childNode < node.getChildNodes().getLength(); childNode++){
            if(node.getChildNodes().item(childNode).getNodeName().contains("extension")){
                Logger.info("We got an extension");
                updateExtension(node.getChildNodes().item(childNode), jsonArray, document);
            }
            if(node.getChildNodes().item(childNode).getNodeName().contains("attribute")){
                JSONObject jsonObject = new JSONObject();
                updateAttribute(node.getChildNodes().item(childNode), jsonObject, document);
                jsonArray.put(jsonObject);
            }
        }
    }

    /***
     * load the base element as well as all other elements with it
     * @param node
     * @param jsonArray
     * @param document
     */
    private static void updateExtension(Node node, JSONArray jsonArray, Document document){
        Logger.info("Base attribute for the extension is: " +
                node.getAttributes().getNamedItem("base").getTextContent());
        String elementType = node.getAttributes().getNamedItem("base").getTextContent();
        String[] elementTypeTokens = elementType.split(":");
        String itemType = elementTypeTokens.length > 1 ? elementTypeTokens[1] : elementTypeTokens[0];
        Logger.info("Check if " + itemType + " is native");
        if(!nativeElements.contains(itemType)){
            Node child = retrieveChild(elementType, document);

            Logger.info("child is " + child.getNodeName());
            JSONObject jsonObject = new JSONObject();
            if(child != null && child.getNodeName().contains("complexType")) {
                Logger.info("It's a complex type");
                JSONArray innerJsonArray = new JSONArray();
                addToJsonObject(jsonObject, "items", innerJsonArray);
                Logger.info("Retrieve the child element");
                parseComplexType(jsonObject, innerJsonArray, document, child);
            } else {
                addToJsonObject(jsonObject, "name", "value");
                addToJsonObject(jsonObject, "xsd-type", "text");
                parseSimpleType(jsonObject, document, child);
                jsonArray.put(jsonObject);
            }


        }
        //TODO: get all attributes as objects
        for(int childNode = 0; childNode < node.getChildNodes().getLength(); childNode ++){
            if(node.getChildNodes().item(childNode).getNodeName().contains("attribute")){
                JSONObject jsonObject = new JSONObject();
                updateAttribute(node.getChildNodes().item(childNode), jsonObject, document);
                jsonArray.put(jsonObject);
            }
        }

    }

    private static void updateAttribute(Node node, JSONObject jsonObject, Document document) {
        addToJsonObject(jsonObject, "xsd-type", "attribute");
        for(int attribute = 0; attribute < node.getAttributes().getLength(); attribute++){
            switch (node.getAttributes().item(attribute).getNodeName()){
                case "use":
                    addToJsonObject(jsonObject, "required", node.getAttributes().item(attribute).getTextContent());
                    break;
                case "type":
                    //TODO: check if native.  If not, go parse
                    String elementType = node.getAttributes().getNamedItem("type").getTextContent();
                    String[] elementTypeTokens = elementType.split(":");
                    String itemType = elementTypeTokens.length > 1 ? elementTypeTokens[1] : elementTypeTokens[0];
                    if(nativeElements.contains(itemType)) {
                        addToJsonObject(jsonObject, "type", itemType);
                    } else {
                        Node child = retrieveChild(elementType, document);

                        if(child != null){
                            Logger.info("In Restriction. It's a " + child.getNodeName() + " type");
                            if(child.getNodeName().contains("complexType")){
                                JSONArray jsonArray = new JSONArray();
                                addToJsonObject(jsonObject, "items", jsonArray);
                                Logger.info("Retrieve the child element");
                                parseComplexType(jsonObject, jsonArray, document, child);
                            } else {
                                parseSimpleType(jsonObject, document, child);
                            }
                        }

                    }
                    break;
                default:
                    addToJsonObject(jsonObject,
                            node.getAttributes().item(attribute).getNodeName(),
                            node.getAttributes().item(attribute).getTextContent());
            }
        }
        for(int childNode = 0; childNode < node.getChildNodes().getLength(); childNode++){
            String[] nodeTokens = node.getChildNodes().item(childNode).getNodeName().split(":");
            String nodeName = nodeTokens.length > 1? nodeTokens[1]: nodeTokens[0];
            Logger.info("Looking for our special snowflakes and found " + nodeName);
            switch(nodeName){
                case "annotation":
                    Logger.info("We found docs!  Always good!");
                    updateAnnotation(node.getChildNodes().item(childNode), jsonObject);
                    break;
                default:
                    Logger.error("What did we just find??? " + nodeName);
                    break;
            }
        }
    }

    private static void parseElement(JSONObject jsonObject, Document document, Node element){
        Logger.info("In parseElement. Let's iterate through its attributes");
        for(int attr = 0; attr < element.getAttributes().getLength(); attr++){
            Logger.info("Attribute: " +
                    element.getAttributes().item(attr).getNodeName() + " = " +
                    element.getAttributes().item(attr).getTextContent());
        }

        String elementName = element.getAttributes().getNamedItem("name").getTextContent();
        Logger.info("Add the name for " + elementName);
        addToJsonObject(jsonObject, "name", elementName);

        if(element.getAttributes().getNamedItem("type") != null){
            Logger.info("We have a type for " + elementName);
            String elementType = element.getAttributes().getNamedItem("type").getTextContent();
            String[] elementTypeTokens = elementType.split(":");
            String itemType = elementTypeTokens.length > 1 ? elementTypeTokens[1] : elementTypeTokens[0];

            Logger.info("Check if " + itemType + " is native");
            if(!nativeElements.contains(itemType)){
                Node child = retrieveChild(elementType, document);

                if(child != null && child.getNodeName().contains("complexType")){
                    Logger.info("It's a complex type");
                    JSONArray jsonArray = new JSONArray();
                    addToJsonObject(jsonObject, "items", jsonArray);
                    Logger.info("Retrieve the child element");
                    parseComplexType(jsonObject, jsonArray, document, child);
                } else {
                    parseSimpleType(jsonObject, document, child);
                }

            } else {
                addToJsonObject(jsonObject, "type", itemType);
            }
        }

        for(int attr = 0; attr < element.getAttributes().getLength(); attr++){
            if(attributeElements.contains(element.getAttributes().item(attr).getNodeName())){
                Logger.info("Attribute: " +
                        element.getAttributes().item(attr).getNodeName() + " = " +
                        element.getAttributes().item(attr).getTextContent());
                addToJsonObject(jsonObject,
                        element.getAttributes().item(attr).getNodeName(),
                        element.getAttributes().item(attr).getTextContent());
            }
        }

        Logger.info("Apparently, we also do nested elements...");
        for(int childNode = 0; childNode < element.getChildNodes().getLength(); childNode++){
            String[] nodeTokens = element.getChildNodes().item(childNode).getNodeName().split(":");
            String nodeName = nodeTokens.length > 1? nodeTokens[1]: nodeTokens[0];
            Logger.info("Looking for our special snowflakes and found " + nodeName);
            switch(nodeName){
                case "annotation":
                    Logger.info("We found docs!  Always good!");
                    updateAnnotation(element.getChildNodes().item(childNode), jsonObject);
                    break;
                case "#text":
                    break;
                default:
                    Logger.error("We found another nested element.  Fix the thing!");
                    break;
            }
        }
    }

    private static void parseSimpleType(JSONObject jsonObject, Document document, Node element) {
        Logger.info("We're in the simple type.  Let's check it out.");
        for(int childNode = 0; childNode < element.getChildNodes().getLength(); childNode++){
            String[] nodeTokens = element.getChildNodes().item(childNode).getNodeName().split(":");
            String nodeName = nodeTokens.length > 1? nodeTokens[1]: nodeTokens[0];
            Logger.info("Looking for our special snowflakes and found " + nodeName);
            switch(nodeName){
                case "list":
                    Logger.info("Oooh, we got a select button!  Yay!");
                    updateList(element.getChildNodes().item(childNode), jsonObject, document);
                    addToJsonObject(jsonObject, "type", "multi-select");
                    break;
                case "restriction":
                    Logger.info("We found a restriction. Yay!");
                    updateRestriction(element.getChildNodes().item(childNode), jsonObject);
                    break;
                case "annotation":
                    Logger.info("We found docs!  Always good!");
                    updateAnnotation(element.getChildNodes().item(childNode), jsonObject);
                    break;
                default:
                    break;
            }
        }
    }

    /***
     * This method takes in an array and checks which type of complexType it is.
     * Possibilities are simpleContent, all, sequence
     * all is a set of elements that occur once
     * sequence is a list of elements that occur more than once
     * simpleContent is one element
     * @param parentJsonObject
     * @param jsonArray
     * @param document
     * @param element
     */
    private static void parseComplexType(JSONObject parentJsonObject,
                                         JSONArray jsonArray, Document document, Node element){
        Logger.info("We're in the complex type.  Let's check it out.");
        for(int childNode = 0; childNode < element.getChildNodes().getLength(); childNode++){
            String[] nodeTokens = element.getChildNodes().item(childNode).getNodeName().split(":");
            String nodeName = nodeTokens.length > 1? nodeTokens[1]: nodeTokens[0];
            Logger.info("Looking for our special snowflakes and found " + nodeName);
            switch(nodeName){
                case "all":
                    Logger.info("We found a set of elements. Yay!");
                    updateAll(element.getChildNodes().item(childNode), jsonArray, document);
                    break;
                case "sequence":
                    Logger.info("We found a list of elements. Yay!");
                    updateSequence(element.getChildNodes().item(childNode), jsonArray, document);
                    break;
                case "annotation":
                    Logger.info("We found docs!  Always good!");
                    updateAnnotation(element.getChildNodes().item(childNode), parentJsonObject);
                    break;
                case "attribute":
                    Logger.info("We found an attribute. Yay!");
                    JSONObject jsonObject = new JSONObject();
                    updateAttribute(element.getChildNodes().item(childNode), jsonObject, document);
                    jsonArray.put(jsonObject);
                case "simpleContent":
                    Logger.info("Simple content.  Add everything");
                    updateSimpleContent(element.getChildNodes().item(childNode), jsonArray, document);
                    break;
                case "choice":
                    Logger.info("Choice.  Radio buttons ftw!");
                    updateChoice(element.getChildNodes().item(childNode), jsonArray, document);
                default:
                    break;
            }
        }
        Logger.info(element.getChildNodes().getLength() + "");
        Logger.info(element.getNodeName());
        Logger.info(element.getAttributes().getNamedItem("name").getTextContent());


    }

    /***
     * This method checks whether the element has any children that are complexType
     *
     * @param elementType
     * @param document
     * @return
     */
    private static Node retrieveChild(String elementType, Document document) {
        String[] elementTypeTokens = elementType.split(":");
        Logger.info("Element type: " + elementType + " with token count: " + elementTypeTokens.length);
        String itemType = elementTypeTokens.length > 1 ? elementTypeTokens[1] : elementTypeTokens[0];

        if(nativeElements.contains(itemType)){
            return null;
        }
        NodeList nodeList = document.getElementsByTagName("*");
        for(int j = 0; j < nodeList.getLength(); j++) {
            if (!nodeList.item(j).getNodeName().contains("element") &&
                    nodeList.item(j).hasAttributes() &&
                    nodeList.item(j).getAttributes().getLength() > 0 &&
                    nodeList.item(j).getAttributes().getNamedItem("name") != null &&
                    nodeList.item(j).getAttributes().getNamedItem("name").getTextContent().equals(itemType)) {
                Logger.info("Found element type: " +
                                nodeList.item(j).getAttributes().getNamedItem("name") +
                                " element node " +
                                nodeList.item(j).getNodeName()
                );
                Logger.info("We are ignoring xs:element in this search because we are assuming only 1 global element");
                return nodeList.item(j);
            }
        }
        return null;

    }

    private static <T> void addToJsonObject(JSONObject jsonObject, String name, T object){
        try {
            jsonObject.put(name, object);
        } catch(JSONException ex){
            Logger.error("We just errored out: " + ex.getMessage());
            ex.printStackTrace();
        }

    }

    private static<T> T getJsonObject(JSONObject jsonObject, String name){
        try {
            return (T)jsonObject.get(name);
        } catch(JSONException ex){
            Logger.error("We just errored out: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }

    }


    public static ObjectNode buildJsonResponse(String type, String message) {
        ObjectNode wrapper = Json.newObject();
        ObjectNode msg = Json.newObject();
        msg.put("message", message);
        wrapper.put(type, msg);
        return wrapper;
    }

    @Deprecated
    public static boolean createFileInCarina(ZipEntry file, StringWriter data, User user){
        try {
            String parentDirectories = file.getName().substring(0, file.getName().lastIndexOf("/"));
            if(!Files.isDirectory(getCarinaDirectory(user.tenant).resolve(Paths.get(parentDirectories)))){
                Files.createDirectories(getCarinaDirectory(user.tenant).resolve(Paths.get(parentDirectories)));
            }
            return Files.write(getCarinaDirectory(user.tenant).resolve(file.getName()), Arrays.asList(data.toString().split("\n"))) != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Deprecated
    public static Path getCarinaDirectory(String tenant){
        return Paths.get("/tmp", tenant);

    }

    @Deprecated
    public static Path getReposeConfigDirectory(String tenant) throws IOException{
        try {
            return Files.createDirectories(getReposeImageDirectory(tenant).resolve("repose_config"));
        } catch (IOException e) {
            Logger.error("Unable to create repose config directory " + e.getLocalizedMessage());
            throw e;
        }
    }

    @Deprecated
    public static Path getReposeImageDirectory(String tenant) throws IOException{
        try {
            return Files.createDirectories(getCarinaDirectory(tenant).resolve("repose_image"));
        } catch (IOException e) {
            Logger.error("Unable to create repose image directory " + e.getLocalizedMessage());
            throw e;
        }
    }

    @Deprecated
    public static Path getOriginImageDirectory(String tenant) throws IOException{
        try {
            return Files.createDirectories(getCarinaDirectory(tenant).resolve("origin_image"));
        } catch (IOException e) {
            Logger.error("Unable to create origin image directory " + e.getLocalizedMessage());
            throw e;
        }
    }

    @Deprecated
    public static Path getCarinaDirectoryWithCluster(String tenant, String cluster){
        return getCarinaDirectory(tenant).resolve(cluster);

    }



    public static String generateLoggingXml(int majorVersion) {
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


    public static String generateContainerXml(int majorVersion){
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

    public static String generateSystemModelXml(Set<String> filterNames, int majorVersion, User user, String versionId){
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

    public static String updateSystemModelXml(User user, String versionId, String systemModelContent){
        //get new doc builder
        Document document = null;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new InputSource(new StringReader( systemModelContent))
            );
        } catch (SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }

        //get nodes and update port to 8080
        NodeList nodeList = document.getElementsByTagName("node");
        for(int nodeId = 0; nodeId < nodeList.getLength(); nodeId ++ ){
            nodeList.item(nodeId).getAttributes().getNamedItem("http-port").setTextContent("8080");
        }

        //get nodes and update destination hostname and port to 8000
        NodeList endpointList = document.getElementsByTagName("endpoint");
        for(int endpointId = 0; endpointId < endpointList.getLength(); endpointId ++ ){
            endpointList.item(endpointId).getAttributes().getNamedItem("port").setTextContent("8000");
            endpointList.item(endpointId).getAttributes().getNamedItem("hostname").
                    setTextContent("repose-origin-" + user.tenant + "-" + versionId.replace('.','-'));
        }

        Logger.info("Updated system model: " + convertDocumentToString(document));

        return convertDocumentToString(document);

    }


    public static Element addAppenders(Document document, String appender, Map<String, String> attributes ){
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

    public static Element addElement(Document doc, String name, Map<String, String> attributeList, Optional<Element> nestedElement){

        Element element = doc.createElement(name);
        attributeList.forEach((attrName, attrValue) ->
                        element.setAttribute(attrName, attrValue)
        );

        if(nestedElement.isPresent()){
            element.appendChild(nestedElement.get());
        }

        return element;
    }

    public static Element insertElement(Element parentElement, Document document,
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

    public static Element getRealParentElement(Element currentElement, int order, Document document){
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

    public static String convertDocumentToString(Document doc) {
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

    public static void printDocument(Document doc) {
        Logger.info(convertDocumentToString(doc));
    }


}
