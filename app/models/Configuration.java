package models;

/**
 * Created by dimi5963 on 3/2/16.
 */
public class Configuration {
    private String name;
    private String xml;

    public Configuration(String name, String xml) {
        this.name = name;
        this.xml = xml;
    }

    public String getName() {
        return name;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) { this.xml = xml; }
}
