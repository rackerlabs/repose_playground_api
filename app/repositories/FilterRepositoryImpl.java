package repositories;

import models.Filter;
import play.Logger;

/**
 * Created by dimi5963 on 3/10/16.
 */
public class FilterRepositoryImpl implements FilterRepository {
    @Override
    public Filter findByName(String name) {
        return Filter.
                find
                .where()
                .eq("name", name.toLowerCase())
                .findUnique();
    }

    @Override
    public String getFilterNamespace(String filterName) {
        Filter filter = findByName(filterName);
        if(filter != null)
            return filter.getNamespace();
        else
            return null;
    }

    @Override
    public void saveFilterNamespace(String filterName, String namespace) {
        Logger.debug("Save filter namespace: " + filterName + " and namespace " + namespace);
        Filter filter = findByName(filterName);
        if(filter == null)
            filter = new Filter(filterName, namespace);
        else
            filter.setNamespace(namespace);
        filter.save();
    }
}
