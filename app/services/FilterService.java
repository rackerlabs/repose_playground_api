package services;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.ImplementedBy;
import exceptions.InternalServerException;

import java.util.List;

/**
 * Created by dimi5963 on 3/7/16.
 */
@ImplementedBy(FilterServiceImpl.class)
public interface FilterService {

    List<String> getVersions() throws InternalServerException;
    List<String> getFiltersByVersion(String id) throws InternalServerException;
    JsonNode getComponentData(String versionId, String component) throws InternalServerException;
}
