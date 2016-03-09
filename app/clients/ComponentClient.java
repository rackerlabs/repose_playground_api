package clients;

import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import org.w3c.dom.Document;

import java.util.List;

/**
 * Created by dimi5963 on 2/29/16.
 */
@ImplementedBy(ComponentClientImpl.class)
public interface ComponentClient {

    List<String> getFiltersByVersion(String versionId) throws InternalServerException;

    Document getComponentXSD(String versionId, String componentId) throws InternalServerException;

}
