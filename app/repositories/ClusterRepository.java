package repositories;

import models.Cluster;

/**
 * Created by dimi5963 on 2/28/16.
 */
public class ClusterRepository implements IClusterRepository {


    @Override
    public Cluster findByUserandName(Long userId, String name) {
        return Cluster.
                find
                .where()
                .eq("user", userId)
                .eq("name", name)
                .findUnique();
    }

    @Override
    public void save(Cluster cluster) {
        cluster.save();
    }
}
