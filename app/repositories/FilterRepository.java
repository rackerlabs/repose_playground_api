package repositories;

import com.google.inject.ImplementedBy;
import models.Filter;

/**
 * Created by dimi5963 on 3/10/16.
 */
@ImplementedBy(FilterRepositoryImpl.class)
public interface FilterRepository {
    Filter findByName(String name);
    String getFilterNamespace(String filterName);
    void saveFilterNamespace(String filterName, String namespace);
}
