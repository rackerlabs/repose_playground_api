package models;

import com.avaje.ebean.Model;
import org.joda.time.DateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by dimi5963 on 1/2/16.
 */
@Entity
public class Filter extends Model {
    @Id
    public Long id;

    @Column(length = 1496, nullable = false)
    public String namespace;

    @Column(length = 1496, nullable = false)
    public String name;

    public void setId(Long id) {
        this.id = id;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static final Finder<Long, Filter> find = new Finder<Long, Filter>(
            Long.class, Filter.class);

    public static Filter findByName(String name) {
        return find
                .where()
                .eq("name", name.toLowerCase())
                .findUnique();
    }

}
