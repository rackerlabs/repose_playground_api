package clients;

import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;

import java.util.List;

/**
 * Created by dimi5963 on 2/29/16.
 */
@ImplementedBy(VersionClientImpl.class)
public interface VersionClient {

    List<String> getVersions() throws InternalServerException;

}
