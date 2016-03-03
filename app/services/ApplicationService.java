package services;

import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;
import models.Cluster;
import models.Configuration;
import models.User;

import java.util.List;

/**
 * Created by dimi5963 on 3/2/16.
 */
@ImplementedBy(DockerApplicationService.class)
public interface ApplicationService {

    String createReposeInstance(Cluster cluster, User user, List<Configuration> configurationList, String versionId)
            throws InternalServerException;

    String createOriginInstance(Cluster cluster, User user, String versionId) throws InternalServerException;

    String createThirdPartyInstance(Cluster cluster, User user, String versionId) throws InternalServerException;

}
