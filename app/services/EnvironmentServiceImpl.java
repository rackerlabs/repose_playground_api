package services;

import com.google.inject.Inject;
import exceptions.InternalServerException;
import models.Cluster;
import models.Configuration;
import models.User;

import java.util.List;

/**
 * Created by dimi5963 on 3/2/16.
 */
public class EnvironmentServiceImpl implements EnvironmentService{

    private final ApplicationService applicationService;

    @Inject
    public EnvironmentServiceImpl(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }


    @Override
    public String generatedOriginEnvironment(Cluster cluster, String versionId, User user,
                                             List<Configuration> configurationList) throws InternalServerException {
        String originId = applicationService.createOriginInstance(cluster, user, versionId);
        if (originId != null)
            return applicationService.createReposeInstance(cluster, user, configurationList, versionId);
        else
            throw new InternalServerException("Unable to start origin service.");

    }
}
