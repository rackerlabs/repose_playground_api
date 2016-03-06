package factories;

import com.google.inject.ImplementedBy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by dimi5963 on 3/5/16.
 */
@ImplementedBy(XmlFactoryImpl.class)
public interface XmlFactory {

    String convertDocumentToString(Document doc);

    Element addElement(Document doc, String name, Map<String, String> attributeList, Optional<Element> nestedElement);

    Element addElement(Document doc, String name, Map<String, String> attributeList,
                       List<Element> nestedElementList);
}
