package repositories;

import com.google.inject.ImplementedBy;
import models.Cluster;

/**
 * Created by dimi5963 on 2/28/16.
 */
@ImplementedBy(ClusterRepository.class)
public interface IClusterRepository {

    Cluster findByUserandName(Long userId, String name);

    void save(Cluster cluster);
}
