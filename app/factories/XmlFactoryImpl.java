package factories;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
            return writer.getBuffer().toString().replaceAll("\n|\r", "");
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
}
