package factories;

import com.google.inject.ImplementedBy;
import org.w3c.dom.Document;

/**
 * Created by dimi5963 on 3/5/16.
 */
@ImplementedBy(XmlFactoryImpl.class)
public interface XmlFactory {

    String convertDocumentToString(Document doc);
}
