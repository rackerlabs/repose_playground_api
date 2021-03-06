package models;

import com.avaje.ebean.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by dimi5963 on 1/2/16.
 */
@Entity
public class Filter extends Model {

    public Filter(String name){
        this(name, null);
    }

    public Filter(String name, String namespace){
        this.name = name;
        this.namespace = namespace;
    }

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

    public Long getId() {
        return id;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static final Finder<Long, Filter> find = new Finder<Long, Filter>(
            Long.class, Filter.class);

}
