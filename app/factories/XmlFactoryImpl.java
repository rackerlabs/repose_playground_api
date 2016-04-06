package factories;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import play.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dimi5963 on 3/5/16.
 */
public class XmlFactoryImpl implements XmlFactory {

    @Override
    public String convertDocumentToString(Document doc) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.getBuffer().toString();//.replaceAll("\n|\r", "");
        }catch(TransformerException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public Element addElement(Document doc, String name, Map<String,
            String> attributeList, Optional<Element> nestedElement) {
        Element element = doc.createElement(name);
        attributeList.forEach((attrName, attrValue) ->
                        element.setAttribute(attrName, attrValue)
        );

        if(nestedElement.isPresent()){
            element.appendChild(nestedElement.get());
        }

        return element;
    }

    @Override
    public Element addElement(Document doc, String name, Map<String, String> attributeList,
                              List<Element> nestedElementList) {

        Element element = doc.createElement(name);
        attributeList.forEach((attrName, attrValue) ->
                        element.setAttribute(attrName, attrValue)
        );

        for(Element nestedElement : nestedElementList){
            element.appendChild(nestedElement);
        }

        return element;

    }

    @Override
    public Element insertElement(Element parentElement, Document document,
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

}
