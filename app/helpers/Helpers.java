package helpers;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by dimi5963 on 8/15/15.
 */
public class Helpers {

    /**
     *
     * @param jsonObject
     * @param document
     */
    public static JSONObject generateJSONTree(JSONObject jsonObject, Document document) {
        //figure out if current node has
        //get children of current node
        try {
            Node schema = document.getElementsByTagName("xs:schema").item(0);
            NodeList schemaList = schema.getChildNodes();
            for (int i = 0; i < schemaList.getLength(); i++) {
                //get element
                if (schemaList.item(i).getNodeName() == "xs:element") {
                    parseElement(jsonObject, document, schemaList.item(i));
                    break;
                }
            }

        }catch(JSONException e){}

        return jsonObject;

    }

    private static void updateAnnotation(Node node, JSONObject jsonObject) throws JSONException{
        for(int l = 0; l < node.getChildNodes().getLength(); l ++){
            if(node.getChildNodes().item(l).getNodeName() ==
                    "xs:documentation") {
                for(int m = 0; m < node.getChildNodes().item(l).getChildNodes().getLength(); m ++){
                    if(node.getChildNodes().item(l).getChildNodes().item(m).getNodeName() == "html:p"){
                        jsonObject.put("doc",
                                node.getChildNodes().item(l).getChildNodes().item(m).getTextContent());

                    }
                }
            }
        }
    }

    private static void updateAll(Node node, JSONObject jsonObject, Document document) throws JSONException{
        NodeList allNodeList = node.getChildNodes();
        for(int l = 0; l < allNodeList.getLength(); l ++){
            if(allNodeList.item(l).getNodeName() == "xs:element"){
                parseElement(jsonObject, document, allNodeList.item(l));
            }
        }
    }

    private static void updateRestriction(Node node, JSONObject jsonObject) throws JSONException{
        jsonObject.put("type", node.getAttributes().getNamedItem("base").getTextContent());
        for(int l = 0; l < node.getChildNodes().getLength(); l ++){
            if(node.getChildNodes().item(l).getNodeName() ==
                    "xs:minInclusive") {
                jsonObject.put("minInclusive",
                        node.getChildNodes().item(l).getAttributes().getNamedItem("value").getTextContent());
            } else if(node.getChildNodes().item(l).getNodeName() ==
                    "xs:maxInclusive") {
                jsonObject.put("maxInclusive",
                        node.getChildNodes().item(l).getAttributes().getNamedItem("value").getTextContent());
            }
        }
    }

    private static void updateSequence(Node node, JSONObject jsonObject, Document document) throws JSONException{
        JSONObject listObject = new JSONObject();
        jsonObject.put("list", listObject);
        //list of objects.  each child we care about is xs:element
        NodeList allNodeList = node.getChildNodes();
        for(int l = 0; l < allNodeList.getLength(); l ++){
            if(allNodeList.item(l).getNodeName() == "xs:element"){
                listObject.put("minimum",
                        allNodeList.item(l).getAttributes().getNamedItem("minOccurs").getTextContent());
                listObject.put("maximum",
                        allNodeList.item(l).getAttributes().getNamedItem("maxOccurs").getTextContent());
                parseElement(listObject, document, allNodeList.item(l));
            }
        }

    }

    private static void updateAttribute(Node node, JSONObject jsonObject, Document document) throws JSONException {
        JSONObject attributeObject = new JSONObject();
        jsonObject.put(
                node.getAttributes().getNamedItem("name").getTextContent(),
                attributeObject);
        attributeObject.put("xsd-type", "attribute");
        if(node.hasAttributes() &&
                node.getAttributes().getNamedItem("use") != null &&
                node.getAttributes().getNamedItem("use").getTextContent().equals("required")){
            attributeObject.put("required", true);
        } else {
            attributeObject.put("required", false);
        }
        if(node.hasAttributes() &&
                node.getAttributes().getNamedItem("default") != null){
            attributeObject.put("default", node.getAttributes().getNamedItem("default").getTextContent());
        }

        for(int l = 0; l < node.getChildNodes().getLength(); l ++) {
            //if annotation, let's add the doc
            if (node.getChildNodes().item(l).getNodeName() == "xs:annotation") {
                //documentation
                updateAnnotation(node.getChildNodes().item(l), attributeObject);
            }
        }
        parseAttribute(attributeObject, document, node);
    }

    private static void updateElement(JSONObject jsonObject, Document document, Node element) throws JSONException{
        String itemType =
                element.getAttributes().getNamedItem("type").getTextContent().split(":")[1];
        if(element.hasAttributes() &&
                element.getAttributes().getNamedItem("default") != null){
            jsonObject.put("default", element.getAttributes().getNamedItem("default").getTextContent());
        }


        if(itemType.equals("string")){
            jsonObject.put("type", "string");
        } else {
            NodeList nodeList = document.getElementsByTagName("*");
            for(int j = 0; j < nodeList.getLength(); j++){
                if(nodeList.item(j).hasAttributes() &&
                        nodeList.item(j).getAttributes().getLength() > 0 &&
                        nodeList.item(j).getAttributes().getNamedItem("name") != null &&
                        nodeList.item(j).getAttributes().getNamedItem("name").getTextContent().equals(itemType)){
                    //got the child node.  Let's store the name under childNode
                    NodeList childChildNodeList = nodeList.item(j).getChildNodes();
                    for(int k = 0; k < childChildNodeList.getLength(); k ++){
                        //if annotation, let's add the doc
                        if(childChildNodeList.item(k).getNodeName() == "xs:annotation"){
                            //documentation
                            updateAnnotation(childChildNodeList.item(k), jsonObject);
                        } else if(childChildNodeList.item(k).getNodeName() == "xs:all"){
                            //map of objects.  each child we care about is xs:element
                            updateAll(childChildNodeList.item(k), jsonObject, document);
                        } else if(childChildNodeList.item(k).getNodeName() == "xs:restriction"){
                            //check type.
                            updateRestriction(childChildNodeList.item(k), jsonObject);
                        } else if(childChildNodeList.item(k).getNodeName() == "xs:sequence"){
                            updateSequence(childChildNodeList.item(k), jsonObject, document);
                        } else if(childChildNodeList.item(k).getNodeName() == "xs:attribute"){
                            updateAttribute(childChildNodeList.item(k), jsonObject, document);
                        }

                    }
                    break;
                }
            }
        }
    }

    private static void parseAttribute(JSONObject jsonObject, Document document, Node element) throws JSONException{
        updateElement(jsonObject, document, element);
    }

    private static void parseElement(JSONObject jsonObject, Document document, Node element) throws JSONException{
        //root element.  store name
        JSONObject childObject = new JSONObject();
        jsonObject.put(
                element.getAttributes().getNamedItem("name").getTextContent(),
                childObject);

        updateElement(childObject, document, element);
    }
}
