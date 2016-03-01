package models;

import com.avaje.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by dimi5963 on 11/28/15.
 */
@Entity
public class Cluster extends Model {
    @Id
    public Long id;

    @Column(length = 1496, nullable = false)
    public String uri;

    @Column(length = 1496, nullable = false)
    public String cert_directory;

    @Column(length = 1000, nullable = false)
    public String name;

    @Column(nullable = false)
    public Long user;

    @Column(nullable = true)
    public String config_directory;

    public void setUri(String uri) { this.uri = uri; }
    public void setCert_directory(String cert_directory) { this.cert_directory = cert_directory; }
    public void setConfig_directory(String config_directory) { this.config_directory = config_directory; }
    public void setUser(Long user) { this.user = user; }
    public void setName(String name) { this.name = name; }

    public String getName() { return name; }
    public String getUri() { return uri; }
    public String getCert_directory() { return cert_directory; }
    public String getConfig_directory() { return config_directory; }

    public static final Finder<Long, Cluster> find = new Finder<Long, Cluster>(
            Long.class, Cluster.class);

    @Deprecated
    public static Cluster findByUserandName(Long userId, String name) {
        return find
                .where()
                .eq("user", userId)
                .eq("name", name)
                .findUnique();
    }

    public String toString(){
        StringBuilder carinaBuilder = new StringBuilder();
        carinaBuilder.append("Cluster: ");
        carinaBuilder.append("id => ");
        carinaBuilder.append(id);
        carinaBuilder.append("user => ");
        carinaBuilder.append(user);
        carinaBuilder.append("name => ");
        carinaBuilder.append(name);
        carinaBuilder.append("cert_directory => ");
        carinaBuilder.append(cert_directory);
        carinaBuilder.append("config_directory => ");
        carinaBuilder.append(config_directory);
        carinaBuilder.append("uri => ");
        carinaBuilder.append(uri);

        return carinaBuilder.toString();
    }


}
