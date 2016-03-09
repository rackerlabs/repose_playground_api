package factories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.InternalServerException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import play.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by dimi5963 on 3/7/16.
 */
public class ComponentFactoryImpl implements ComponentFactory{

    private final List<String> nativeElements = Arrays.asList("string", "double", "boolean", "anyURI");
    private final List<String> attributeElements = Arrays.asList("minOccurs", "maxOccurs", "use", "default");

    @Override
    public String getBindingsUrl(String versionId, String componentId) {
        return play.Play.application().configuration().getString("bindings.url.endpoint") +
                versionId +
                play.Play.application().configuration().getString("bindings.url.filtersuri") +
                componentId +
                play.Play.application().configuration().getString("bindings.url.bindingslocation");
    }

    @Override
    public String getSchemaUrl(String versionId, String componentId, String schemaLocation) {
        return play.Play.application().configuration().getString("bindings.url.endpoint") +
                versionId +
                play.Play.application().configuration().getString("bindings.url.filtersuri") +
                componentId +
                play.Play.application().configuration().getString("bindings.url.schemalocation") +
                schemaLocation;
    }

    @Override
    public String getFilterPomUrl(String versionId) throws InternalServerException {
        try {
            if (Integer.parseInt(versionId.split(Pattern.quote("."))[0]) >= 7) {
                Logger.debug("use new pom location.");
                return getV2FilterEndpoint() + versionId + "/filter-bundle-" + versionId + ".pom";
            } else {
                Logger.debug("use old pom location.");
                return getV1FilterEndpoint() + versionId + "/filter-bundle-" + versionId + ".pom";
            }
        }catch(NumberFormatException nfe){
            nfe.printStackTrace();
            Logger.error("Invalid version specified.");
            throw new InternalServerException("Invalid version specified.");
        }

    }

    @Override
    public List<String> getAvailableFilters() {
        List<String> filterListString = play.Play.application().configuration().getStringList("filter.list");
        if (filterListString != null)
            return filterListString;
        else
            return new ArrayList<>();
    }

    @Override
    public JsonNode generateJSONTree(String filterName, ObjectNode parentJson, Document document) {
        //figure out if current node has
        //get children of current node
        Logger.debug("Generate JSON tree");
        Logger.debug("Figure out if current node has children and retrieve them.");
        Node schema = document.getElementsByTagName("xs:schema").item(0);
        NodeList schemaList = schema.getChildNodes();
        for (int i = 0; i < schemaList.getLength(); i++) {
            Logger.debug("Get schema child element");
            if ("xs:element".equals(schemaList.item(i).getNodeName())) {
                Logger.debug("This is the starting point of xsd.  Le'go! " + schemaList.item(i));
                parseElement(parentJson, document, schemaList.item(i));
                break;
            }
        }

        //save the filter namespace
        //Helpers.saveFilterNamespace(filterName, schema.getAttributes().getNamedItem("targetNamespace").getTextContent());

        return parentJson;
    }

    private void parseElement(ObjectNode jsonNode, Document document, Node element){
        Logger.debug("In parseElement. Let's iterate through its attributes");
        for(int attr = 0; attr < element.getAttributes().getLength(); attr++){
            Logger.debug("Attribute: " +
                    element.getAttributes().item(attr).getNodeName() + " = " +
                    element.getAttributes().item(attr).getTextContent());
        }

        String elementName = element.getAttributes().getNamedItem("name").getTextContent();
        Logger.debug("Add the name for " + elementName);
        addToJsonObject(jsonNode, "name", elementName);

        if(element.getAttributes().getNamedItem("type") != null){
            String elementType = element.getAttributes().getNamedItem("type").getTextContent();
            Logger.debug("We have a type for " + elementName + " and it's " + elementType);
            String[] elementTypeTokens = elementType.split(":");
            String itemType = elementTypeTokens.length > 1 ? elementTypeTokens[1] : elementTypeTokens[0];

            Logger.debug("Check if " + itemType + " is native");
            if(!nativeElements.contains(itemType)){
                Logger.debug(itemType + " is not native.  Get its child.");
                Node child = retrieveChild(elementType, document);

                if(child != null && child.getNodeName().contains("complexType")){
                    Logger.debug("It's a complex type.  Create new array.");
                    ArrayNode jsonArray = jsonNode.arrayNode();
                    addToJsonObject(jsonNode, "items", jsonArray);
                    Logger.debug("Parse the complex type child element");
                    parseComplexType(jsonNode, jsonArray, document, child);
                } else {
                    Logger.debug("It's a simple type");
                    parseSimpleType(jsonNode, document, child);
                }

            } else {
                Logger.debug(itemType + " is native.  Add it.");
                addToJsonObject(jsonNode, "type", itemType);
            }
        }

        for(int attr = 0; attr < element.getAttributes().getLength(); attr++){
            if(attributeElements.contains(element.getAttributes().item(attr).getNodeName())){
                Logger.debug("Attribute: " +
                        element.getAttributes().item(attr).getNodeName() + " = " +
                        element.getAttributes().item(attr).getTextContent());
                addToJsonObject(jsonNode,
                        element.getAttributes().item(attr).getNodeName(),
                        element.getAttributes().item(attr).getTextContent());
            }
        }

        Logger.debug("Apparently, we also do nested elements... for " + element.getNodeName());
        for(int childNode = 0; childNode < element.getChildNodes().getLength(); childNode++){
            String[] nodeTokens = element.getChildNodes().item(childNode).getNodeName().split(":");
            String nodeName = nodeTokens.length > 1? nodeTokens[1]: nodeTokens[0];
            Logger.debug("Looking for our special snowflakes and found " + nodeName);
            switch(nodeName){
                case "annotation":
                    Logger.debug("We found docs!  Always good!");
                    updateAnnotation(element.getChildNodes().item(childNode), jsonNode);
                    break;
                case "#text":
                    Logger.warn("We found text! For now ignore");
                    break;
                default:
                    Logger.error("We found another nested element.  Fix the thing!");
                    break;
            }
        }
    }

    private <T> void addToJsonObject(ObjectNode jsonNode, String name, T object){
        jsonNode.putPOJO(name, object);
    }

    private Node retrieveChild(String elementType, Document document) {
        String[] elementTypeTokens = elementType.split(":");
        Logger.debug("Element type: " + elementType + " with token count: " + elementTypeTokens.length);
        String itemType = elementTypeTokens.length > 1 ? elementTypeTokens[1] : elementTypeTokens[0];

        Logger.debug("Check if " + itemType + " is a native element");
        if(nativeElements.contains(itemType)){
            Logger.debug("it is!");
            return null;
        }
        Logger.debug("it is not!");
        NodeList nodeList = document.getElementsByTagName("*");
        for(int j = 0; j < nodeList.getLength(); j++) {
            //TODO: figure out if this element check is required
            if (!nodeList.item(j).getNodeName().contains("element") &&
                    nodeList.item(j).hasAttributes() &&
                    nodeList.item(j).getAttributes().getLength() > 0 &&
                    nodeList.item(j).getAttributes().getNamedItem("name") != null &&
                    nodeList.item(j).getAttributes().getNamedItem("name").getTextContent().equals(itemType)) {
                Logger.debug("Found element type: " +
                                nodeList.item(j).getAttributes().getNamedItem("name") +
                                " element node " +
                                nodeList.item(j).getNodeName()
                );
                Logger.debug("We are ignoring xs:element in this search because we are assuming only 1 global element");
                return nodeList.item(j);
            }
        }
        return null;

    }

    private void parseComplexType(ObjectNode parentJsonNode,
                                  ArrayNode jsonArray, Document document, Node element){
        Logger.debug("We're in the complex type.  Let's check it out by iterating through " +
                element.getChildNodes().getLength() + " of its children");
        for(int childNode = 0; childNode < element.getChildNodes().getLength(); childNode++){
            String[] nodeTokens = element.getChildNodes().item(childNode).getNodeName().split(":");
            String nodeName = nodeTokens.length > 1? nodeTokens[1]: nodeTokens[0];
            Logger.debug("Looking for our special snowflakes and found " + nodeName);
            switch(nodeName){
                case "all":
                    Logger.debug("We found a set of elements. Yay!");
                    updateAll(element.getChildNodes().item(childNode), jsonArray, document);
                    break;
                case "sequence":
                    Logger.debug("We found a list of elements. Yay!");
                    updateSequence(element.getChildNodes().item(childNode), jsonArray, document);
                    break;
                case "annotation":
                    Logger.debug("We found docs!  Always good!");
                    updateAnnotation(element.getChildNodes().item(childNode), parentJsonNode);
                    break;
                case "attribute":
                    Logger.debug("We found an attribute. Yay!");
                    ObjectNode jsonObject = jsonArray.objectNode();
                    updateAttribute(element.getChildNodes().item(childNode), jsonObject, document);
                    jsonArray.add(jsonObject);
                case "simpleContent":
                    Logger.debug("Simple content.  Add everything");
                    updateSimpleContent(element.getChildNodes().item(childNode), jsonArray, document);
                    break;
                case "choice":
                    Logger.debug("Choice.  Radio buttons ftw!");
                    updateChoice(element.getChildNodes().item(childNode), jsonArray, document);
                default:
                    Logger.warn("We didn't match the complex type! " + nodeName + " TODO for later.");
                    break;
            }
        }
        Logger.info(element.getChildNodes().getLength() + "");
        Logger.info(element.getNodeName());
        Logger.info(element.getAttributes().getNamedItem("name").getTextContent());


    }

    private void parseSimpleType(ObjectNode jsonNode, Document document, Node element) {
        Logger.debug("We're in the simple type.  Let's check it out.");
        for(int childNode = 0; childNode < element.getChildNodes().getLength(); childNode++){
            String[] nodeTokens = element.getChildNodes().item(childNode).getNodeName().split(":");
            String nodeName = nodeTokens.length > 1? nodeTokens[1]: nodeTokens[0];
            Logger.info("Looking for our special snowflakes and found " + nodeName);
            switch(nodeName){
                case "list":
                    Logger.info("Oooh, we got a select button!  Yay!");
                    updateList(element.getChildNodes().item(childNode), jsonNode, document);
                    addToJsonObject(jsonNode, "type", "multi-select");
                    break;
                case "restriction":
                    Logger.info("We found a restriction. Yay!");
                    updateRestriction(element.getChildNodes().item(childNode), jsonNode);
                    break;
                case "annotation":
                    Logger.info("We found docs!  Always good!");
                    updateAnnotation(element.getChildNodes().item(childNode), jsonNode);
                    break;
                default:
                    break;
            }
        }
    }

    private void updateAnnotation(Node node, ObjectNode jsonNode){
        Logger.debug("Let's update the docs for " + node.getTextContent());
        String doc = "";
        for(int l = 0; l < node.getChildNodes().getLength(); l ++){
            if("xs:documentation".equals(node.getChildNodes().item(l).getNodeName())) {
                Logger.debug("Found x:documentation.");
                for(int m = 0; m < node.getChildNodes().item(l).getChildNodes().getLength(); m ++){
                    if("html:p".equals(node.getChildNodes().item(l).getChildNodes().item(m).getNodeName())) {
                        doc = doc.concat(node.getChildNodes().item(l).getChildNodes().item(m).getTextContent());

                    }
                }
            } else {
                Logger.warn("Found something else.");
            }
        }
        if(doc.length() > 0) {
            Logger.debug("Documentation is made.  Add it to json.");
            addToJsonObject(jsonNode, "doc", doc);
        }
    }

    private void updateList(Node node, ObjectNode jsonObject, Document document) {
        Logger.info("Let's get everything into a list");
        String elementType = node.getAttributes().getNamedItem("itemType").getTextContent();
        String[] elementTypeTokens = elementType.split(":");
        String itemType = elementTypeTokens.length > 1 ? elementTypeTokens[1] : elementTypeTokens[0];
        Logger.debug("Check if " + itemType + " is native");
        if(!nativeElements.contains(itemType)){
            Node child = retrieveChild(elementType, document);

            if(child != null && child.getNodeName().contains("complexType")) {
                Logger.debug("child is " + child.getNodeName());
                Logger.debug("It's a complex type");
                ArrayNode innerJsonArray = jsonObject.arrayNode();
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

    private void updateAll(Node node, ArrayNode jsonArray, Document document){
        Logger.debug("Let's update element set (xs:all)");
        NodeList allNodeList = node.getChildNodes();
        Logger.debug("Children count " + allNodeList.getLength());
        for(int l = 0; l < allNodeList.getLength(); l ++){
            if(allNodeList.item(l).getNodeName().contains("element")){
                ObjectNode jsonObject = jsonArray.objectNode();
                Logger.debug("Check if minOccurs attribute exists.  By default it's 1");
                if(allNodeList.item(l).hasAttributes() &&
                        allNodeList.item(l).getAttributes().getNamedItem("minOccurs") != null){
                    Logger.debug("Found minOccurs.  Add it.");
                    addToJsonObject(jsonObject, "minOccurs",
                            allNodeList.item(l).getAttributes().getNamedItem("minOccurs").getTextContent());
                    if(allNodeList.item(l).getAttributes().getNamedItem("minOccurs").getTextContent().equals("1")){
                        Logger.debug("Found minOccurs and it's set to 1.  Means it's required.  Add it.");
                        addToJsonObject(jsonObject, "required", "required");
                    } else {
                        Logger.debug("Found minOccurs and it's not set to 1.  Means it's optional.  Add it.");
                        addToJsonObject(jsonObject, "required", "optional");
                    }
                } else {
                    Logger.debug("Did not find minOccurs, so we assume it's required.  Add it.");
                    addToJsonObject(jsonObject, "minOccurs", "1");
                    addToJsonObject(jsonObject, "required", "required");
                }

                Logger.debug("Parse this element.");
                parseElement(jsonObject, document, allNodeList.item(l));
                Logger.debug("Add " + allNodeList.item(l) + " to list.");
                jsonArray.add(jsonObject);
            } else {
                Logger.warn("Not an element. " + allNodeList.item(l).getNodeName() + " Probably needs to be a TODO");
            }
        }
    }

    private void updateRestriction(Node node, ObjectNode jsonObject) {
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

    private void updateChoice(Node node, ArrayNode jsonArray, Document document){
        Logger.info("Let's get us those radio buttons");
        NodeList allNodeList = node.getChildNodes();
        for(int l = 0; l < allNodeList.getLength(); l ++){
            if(allNodeList.item(l).getNodeName().contains("element")){
                ObjectNode jsonObject = jsonArray.objectNode();
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
                jsonArray.add(jsonObject);
            }
        }
    }

    private void updateSequence(Node node, ArrayNode jsonArray, Document document) {
        Logger.debug("Let's update element set (xs:sequence)");
        NodeList allNodeList = node.getChildNodes();
        Logger.debug("Children count " + allNodeList.getLength());
        for(int l = 0; l < allNodeList.getLength(); l ++){
            if(allNodeList.item(l).getNodeName().contains("element")){
                ObjectNode jsonObject = jsonArray.objectNode();
                Logger.debug("Check if minOccurs attribute exists.  By default it's 1");
                if(allNodeList.item(l).getAttributes().getNamedItem("minOccurs") != null){
                    Logger.debug("Found minOccurs.  Add it.");
                    if(allNodeList.item(l).getAttributes().getNamedItem("minOccurs").getTextContent().equals("1")){
                        Logger.debug("Found minOccurs and it's set to 1.  Means it's required.  Add it.");
                        addToJsonObject(jsonObject, "required", "required");
                    } else {
                        Logger.debug("Found minOccurs and it's not set to 1.  Means it's optional.  Add it.");
                        addToJsonObject(jsonObject, "required", "optional");
                    }
                    addToJsonObject(jsonObject, "minOccurs",
                            allNodeList.item(l).getAttributes().getNamedItem("minOccurs").getTextContent());

                } else {
                    Logger.debug("Did not find minOccurs, so we assume it's optional.  Add it.");
                    addToJsonObject(jsonObject, "required", "optional");
                }
                if(allNodeList.item(l).getAttributes().getNamedItem("maxOccurs") != null &&
                        !allNodeList.item(l).getAttributes().getNamedItem("maxOccurs").getTextContent().equals("1")) {
                    Logger.debug("Found maxOccurs and it's not set to 1.  Add it as list.");
                    addToJsonObject(jsonObject, "type", "list");
                }
                Logger.debug("Parse this element.");
                parseElement(jsonObject, document, allNodeList.item(l));
                Logger.debug("Add " + allNodeList.item(l) + " to list.");
                jsonArray.add(jsonObject);
            }
        }
    }

    private void updateSimpleContent(Node node, ArrayNode jsonArray, Document document){
        Logger.debug("Iterate through children of simple content");
        for(int childNode = 0; childNode < node.getChildNodes().getLength(); childNode++){
            if(node.getChildNodes().item(childNode).getNodeName().contains("extension")){
                Logger.debug("We got an extension.  Update extension");
                updateExtension(node.getChildNodes().item(childNode), jsonArray, document);
            }
            if(node.getChildNodes().item(childNode).getNodeName().contains("attribute")){
                Logger.debug("We got an attribute. Update attribute");
                ObjectNode jsonObject = jsonArray.objectNode();
                updateAttribute(node.getChildNodes().item(childNode), jsonObject, document);
                jsonArray.add(jsonObject);
            }
        }
    }

    /***
     * load the base element as well as all other elements with it
     * @param node
     * @param jsonArray
     * @param document
     */
    private void updateExtension(Node node, ArrayNode jsonArray, Document document){
        Logger.info("Base attribute for the extension is: " +
                node.getAttributes().getNamedItem("base").getTextContent());
        String elementType = node.getAttributes().getNamedItem("base").getTextContent();
        String[] elementTypeTokens = elementType.split(":");
        String itemType = elementTypeTokens.length > 1 ? elementTypeTokens[1] : elementTypeTokens[0];
        Logger.info("Check if " + itemType + " is native");
        if(!nativeElements.contains(itemType)){
            Node child = retrieveChild(elementType, document);

            Logger.debug("child is " + child.getNodeName());
            ObjectNode jsonObject = jsonArray.objectNode();
            if(child != null && child.getNodeName().contains("complexType")) {
                Logger.info("It's a complex type");
                ArrayNode innerJsonArray = jsonArray.arrayNode();
                addToJsonObject(jsonObject, "items", innerJsonArray);
                Logger.info("Retrieve the child element");
                parseComplexType(jsonObject, innerJsonArray, document, child);
            } else {
                addToJsonObject(jsonObject, "name", "value");
                addToJsonObject(jsonObject, "xsd-type", "text");
                parseSimpleType(jsonObject, document, child);
                jsonArray.add(jsonObject);
            }


        }
        //TODO: get all attributes as objects
        for(int childNode = 0; childNode < node.getChildNodes().getLength(); childNode ++){
            if(node.getChildNodes().item(childNode).getNodeName().contains("attribute")){
                ObjectNode jsonObject = jsonArray.objectNode();
                updateAttribute(node.getChildNodes().item(childNode), jsonObject, document);
                jsonArray.add(jsonObject);
            }
        }

    }

    private void updateAttribute(Node node, ObjectNode jsonObject, Document document) {
        Logger.debug("Update attribute.  Add xsd-type as attribute.");
        addToJsonObject(jsonObject, "xsd-type", "attribute");
        Logger.debug("Iterate through attributes.");
        for(int attribute = 0; attribute < node.getAttributes().getLength(); attribute++){
            switch (node.getAttributes().item(attribute).getNodeName()){
                case "use":
                    Logger.debug("Found use attribute.  Add " +
                            node.getAttributes().item(attribute).getTextContent() + " as required.");
                    addToJsonObject(jsonObject, "required", node.getAttributes().item(attribute).getTextContent());
                    break;
                case "type":
                    String elementType = node.getAttributes().getNamedItem("type").getTextContent();
                    Logger.debug("Found type attribute for " + elementType);
                    String[] elementTypeTokens = elementType.split(":");
                    String itemType = elementTypeTokens.length > 1 ? elementTypeTokens[1] : elementTypeTokens[0];
                    Logger.debug("Check if " + itemType + " is a native element");
                    if(nativeElements.contains(itemType)) {
                        Logger.debug("it is!  Add as type");
                        addToJsonObject(jsonObject, "type", itemType);
                    } else {
                        Logger.debug("it is not!  Retrive child");
                        Node child = retrieveChild(elementType, document);

                        if(child != null){
                            Logger.debug("We got a child! " + child.getNodeName());
                            Logger.debug("In Restriction. It's a " + child.getNodeName() + " type");
                            if(child.getNodeName().contains("complexType")){
                                Logger.debug("It's a complex type child!");
                                ArrayNode jsonArray = jsonObject.arrayNode();
                                Logger.debug("Add to node as items array!");
                                addToJsonObject(jsonObject, "items", jsonArray);
                                Logger.debug("Parse child complex type");
                                parseComplexType(jsonObject, jsonArray, document, child);
                            } else {
                                Logger.debug("Parse child simple type");
                                parseSimpleType(jsonObject, document, child);
                            }
                        }

                    }
                    break;
                default:
                    Logger.debug("Found random attribute. " +
                            node.getAttributes().item(attribute).getNodeName() + "  Add it!");
                    addToJsonObject(jsonObject,
                            node.getAttributes().item(attribute).getNodeName(),
                            node.getAttributes().item(attribute).getTextContent());
            }
        }
        Logger.debug("Iterate through children.");
        for(int childNode = 0; childNode < node.getChildNodes().getLength(); childNode++){
            String[] nodeTokens = node.getChildNodes().item(childNode).getNodeName().split(":");
            String nodeName = nodeTokens.length > 1? nodeTokens[1]: nodeTokens[0];
            Logger.debug("Looking for our special snowflakes and found " + nodeName);
            switch(nodeName){
                case "annotation":
                    Logger.debug("We found docs!  Always good! Update annotation");
                    updateAnnotation(node.getChildNodes().item(childNode), jsonObject);
                    break;
                default:
                    Logger.error("What did we just find??? " + nodeName);
                    break;
            }
        }
    }

    private <T> T getJsonObject(ObjectNode jsonObject, String name){
        return (T)jsonObject.get(name);
    }

    private String getV1FilterEndpoint(){
        return play.Play.application().configuration().getString("pom.v1.endpoint");
    }

    private String getV2FilterEndpoint(){
        return play.Play.application().configuration().getString("pom.v2.endpoint");
    }

}
