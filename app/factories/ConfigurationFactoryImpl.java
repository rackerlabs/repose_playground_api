package factories;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import exceptions.NotFoundException;
import models.Configuration;
import models.Filter;
import models.User;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import play.Logger;
import play.mvc.Http;
import repositories.FilterRepository;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by dimi5963 on 3/2/16.
 */
public class ConfigurationFactoryImpl implements ConfigurationFactory {

    private final XmlFactory xmlFactory;
    private final DocumentBuilder documentBuilder;
    private final FilterRepository filterRepository;

    @Inject
    public ConfigurationFactoryImpl(XmlFactory xmlFactory, FilterRepository filterRepository)
            throws InternalServerException{
        this.xmlFactory = xmlFactory;
        this.filterRepository = filterRepository;
        try {
            this.documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            Logger.warn("Unable to create document builder");
            throw new InternalServerException("Unable to create document builder");
        }

    }

    @Override
    public List<Configuration> translateConfigurationsFromUpload(User user, String reposeVersion,
                                                                 Http.MultipartFormData body)
            throws NotFoundException, InternalServerException {
        Logger.debug("Translate configurations for " + user + ", version " + reposeVersion);
        try{
            Integer.parseInt(reposeVersion.split(Pattern.quote("."))[0]);
        } catch(NumberFormatException nfe) {
            throw new InternalServerException("Invalid version specified.");
        }
        if(body.getFiles() != null && body.getFiles().size() > 0) {
            int majorVersion = Integer.parseInt(reposeVersion.split(Pattern.quote("."))[0]);
            //get the first one.  others don't matter since it's a single file upload
            Http.MultipartFormData.FilePart reposeZip = body.getFiles().get(0);
            Logger.debug("get file for: " + reposeZip.getFile().getAbsolutePath());

            List<Configuration> filterXml = unzip(reposeZip.getFile());
            for(Configuration configuration: filterXml){
                switch (configuration.getName()) {
                    case "system-model.cfg.xml":
                        Logger.debug("update system model listening node and destination");
                        String content = configuration.getXml();
                        configuration.setXml(updateSystemModelXml(user, reposeVersion, content));
                        break;
                    case "container.cfg.xml":
                        Logger.debug("update container config");
                        configuration.setXml(generateContainerXml(majorVersion));
                        break;
                    case "log4j2.xml":
                    case "log4j.properties":
                        Logger.debug("update logging config");
                        configuration.setXml(generateLoggingXml(majorVersion));
                        break;
                    default:
                        Logger.debug("configuration file " + configuration.getName() + " is ignored");
                }
            }

            return  filterXml;
        }

        throw new exceptions.NotFoundException("No zip files");
    }

    @Override
    public List<Configuration> translateConfigurationsFromJson(User user, String reposeVersion, JsonNode node)
            throws NotFoundException, InternalServerException {
        Logger.debug("Translate configurations for " + user + ", version " + reposeVersion);
        try{
            Integer.parseInt(reposeVersion.split(Pattern.quote("."))[0]);
        } catch(NumberFormatException nfe) {
            throw new InternalServerException("Invalid version specified.");
        }

        int majorVersion = Integer.parseInt(reposeVersion.split(Pattern.quote("."))[0]);

        List<Configuration> filterXml = parseFilters(node);
        filterXml.add(new Configuration("system-model.cfg.xml",
                generateSystemModelXml(filterXml, majorVersion, user, reposeVersion)));
        filterXml.add(new Configuration("container.cfg.xml",
                generateContainerXml(majorVersion)));
        if(majorVersion < 7)
            filterXml.add(new Configuration("log4j.properties",
                    generateLoggingXml(majorVersion)));
        else
            filterXml.add(new Configuration("log4j2.xml",
                    generateLoggingXml(majorVersion)));

        return  filterXml;
    }

    @Override
    public String updateSystemModelXml(User user, String versionId, String systemModelContent)
            throws InternalServerException {
        //get new doc builder
        Document document;
        try {
            document = this.documentBuilder.parse(
                    new InputSource(new StringReader( systemModelContent))
            );
        } catch (SAXException | IOException e) {
            Logger.error("Unable to create xml document");
            e.printStackTrace();
            throw new InternalServerException("Unable to parse system model");
        }

        //get nodes and update port to 8080
        NodeList nodeList = document.getElementsByTagName("node");
        for(int nodeId = 0; nodeId < nodeList.getLength(); nodeId ++ ){
            nodeList.item(nodeId).getAttributes().getNamedItem("http-port").setTextContent("8080");
        }

        //get nodes and update destination hostname and port to 8000
        NodeList endpointList = document.getElementsByTagName("endpoint");
        for(int endpointId = 0; endpointId < endpointList.getLength(); endpointId ++ ){
            endpointList.item(endpointId).getAttributes().getNamedItem("port").setTextContent("8000");
            endpointList.item(endpointId).getAttributes().getNamedItem("hostname").
                    setTextContent("repose-origin-" + user.tenant + "-" + versionId.replace('.','-'));
        }

        Logger.debug("Updated system model: " + xmlFactory.convertDocumentToString(document));

        return xmlFactory.convertDocumentToString(document);
    }

    @Override
    public String generateContainerXml(int majorVersion) throws InternalServerException{
        //get new doc builder
        Document document = this.documentBuilder.newDocument();
        Element rootElement;
        if(majorVersion >= 7)
            rootElement = document.createElementNS("http://docs.openrepose.org/repose/container/v2.0",
                    "repose-container");
        else
            rootElement =
                    document.createElementNS("http://docs.rackspacecloud.com/repose/container/v2.0",
                            "repose-container");
        //root element
        document.appendChild(rootElement);
        //add deployment-config
        Element deploymentConfig = document.createElement("deployment-config");
        deploymentConfig.setAttribute("connection-timeout", "30000");
        deploymentConfig.setAttribute("read-timeout", "30000");
        rootElement.appendChild(deploymentConfig);

        //add deployment-directory
        Element deploymentDirectory = document.createElement("deployment-directory");
        deploymentDirectory.setAttribute("auto-clean", "true");
        deploymentDirectory.setTextContent("/var/repose");
        deploymentConfig.appendChild(deploymentDirectory);

        //add artifact-directory
        Element artifactDirectory = document.createElement("artifact-directory");
        artifactDirectory.setAttribute("check-interval", "15000");
        artifactDirectory.setTextContent("/usr/share/repose/filters");
        deploymentConfig.appendChild(artifactDirectory);


        //add logging-configuration
        Element loggingConfiguration = document.createElement("logging-configuration");
        if(majorVersion >= 7)
            loggingConfiguration.setAttribute("href", "file:///etc/repose/log4j2.xml");
        else
            loggingConfiguration.setAttribute("href", "log4j.properties");
        deploymentConfig.appendChild(loggingConfiguration);


        Logger.debug("Updated container: " + xmlFactory.convertDocumentToString(document));

        return xmlFactory.convertDocumentToString(document);
    }

    @Override
    public String generateLoggingXml(int majorVersion) throws InternalServerException{
        if(majorVersion >= 7) {
            //log4j2.xml
            //get new doc builder
            Document document = this.documentBuilder.newDocument();

            Element rootElement = document.createElement("Configuration");
            rootElement.setAttribute("monitorInterval", "15");
            document.appendChild(rootElement);

            Element appenders = document.createElement("Appenders");
            rootElement.appendChild(appenders);

            Element consoleAppender = document.createElement("Console");
            consoleAppender.setAttribute("name", "STDOUT");
            appenders.appendChild(consoleAppender);

            Element patternLayout = document.createElement("PatternLayout");
            patternLayout.setAttribute("pattern", "%d %-4r [%t] %-5p %c - %m%n");
            consoleAppender.appendChild(patternLayout);

            appenders.appendChild(xmlFactory.addElement(document, "RollingFile", new HashMap<String, String>() {
                {
                    put("name", "MainRollingFile");
                    put("fileName", "/var/log/repose/current.log");
                    put("filePattern", "/var/log/repose/current-%d{yyyy-MM-dd_HHmmss}.log");
                }
            }, new ArrayList<Element>(){
                {
                    add(xmlFactory.addElement(document, "PatternLayout", new HashMap<String, String>() {
                        {
                            put("pattern", "%d %-4r [%t] %-5p %c - %m%n");
                        }
                    }, Optional.empty()));
                    add(xmlFactory.addElement(document, "Policies", new HashMap<>(),
                            new ArrayList<Element>() {
                        {
                            add(xmlFactory.addElement(document, "SizeBasedTriggeringPolicy",
                                    new HashMap<String, String>() {
                                        {
                                            put("size", "200 MB");
                                        }
                                    }, Optional.empty()));
                        }
                            }
                    ));
                    add(xmlFactory.addElement(document, "DefaultRolloverStrategy",
                            new HashMap<String, String>() {
                                {
                                    put("max", "2");
                                }
                            }, Optional.empty()));
                }
            }));

            appenders.appendChild(xmlFactory.addElement(document, "RollingFile", new HashMap<String, String>() {
                {
                    put("name", "IntraFilterRollingFile");
                    put("fileName", "/var/log/repose/intra-filter.log");
                    put("filePattern", "/var/log/repose/intra-filter-%d{yyyy-MM-dd_HHmmss}.log");
                }
            }, new ArrayList<Element>(){
                {
                    add(xmlFactory.addElement(document, "PatternLayout", new HashMap<String, String>() {
                        {
                            put("pattern", "%d %-4r [%t] %-5p %c - %m%n");
                        }
                    }, Optional.empty()));
                    add(xmlFactory.addElement(document, "Policies", new HashMap<>(),
                            new ArrayList<Element>() {
                                {
                                    add(xmlFactory.addElement(document, "SizeBasedTriggeringPolicy",
                                            new HashMap<String, String>() {
                                                {
                                                    put("size", "200 MB");
                                                }
                                            }, Optional.empty()));
                                }
                            }
                    ));
                    add(xmlFactory.addElement(document, "DefaultRolloverStrategy",
                            new HashMap<String, String>() {
                                {
                                    put("max", "2");
                                }
                            }, Optional.empty()));
                }
            }));

            appenders.appendChild(xmlFactory.addElement(document, "RollingFile", new HashMap<String, String>() {
                {
                    put("name", "HttpRollingFile");
                    put("fileName", "/var/log/repose/http-debug.log");
                    put("filePattern", "/var/log/repose/http-debug-%d{yyyy-MM-dd_HHmmss}.log");
                }
            }, new ArrayList<Element>(){
                {
                    add(xmlFactory.addElement(document, "PatternLayout", new HashMap<String, String>() {
                        {
                            put("pattern", "%d %-4r [%t] %-5p %c - %m%n");
                        }
                    }, Optional.empty()));
                    add(xmlFactory.addElement(document, "Policies", new HashMap<>(),
                            new ArrayList<Element>() {
                                {
                                    add(xmlFactory.addElement(document, "SizeBasedTriggeringPolicy",
                                            new HashMap<String, String>() {
                                                {
                                                    put("size", "200 MB");
                                                }
                                            }, Optional.empty()));
                                }
                            }
                    ));
                    add(xmlFactory.addElement(document, "DefaultRolloverStrategy",
                            new HashMap<String, String>() {
                                {
                                    put("max", "2");
                                }
                            }, Optional.empty()));
                }
            }));

            appenders.appendChild(xmlFactory.addElement(document, "RollingFile", new HashMap<String, String>() {
                {
                    put("name", "ErrorRollingFile");
                    put("fileName", "/var/log/repose/error.log");
                    put("filePattern", "/var/log/repose/error-%d{yyyy-MM-dd_HHmmss}.log");
                }
            }, new ArrayList<Element>(){
                {
                    add(xmlFactory.addElement(document, "PatternLayout", new HashMap<String, String>() {
                        {
                            put("pattern", "%d %-4r [%t] %-5p %c - %m%n");
                        }
                    }, Optional.empty()));
                    add(xmlFactory.addElement(document, "Policies", new HashMap<>(),
                            new ArrayList<Element>() {
                                {
                                    add(xmlFactory.addElement(document, "SizeBasedTriggeringPolicy",
                                            new HashMap<String, String>() {
                                                {
                                                    put("size", "200 MB");
                                                }
                                            }, Optional.empty()));
                                }
                            }
                    ));
                    add(xmlFactory.addElement(document, "DefaultRolloverStrategy",
                            new HashMap<String, String>() {
                                {
                                    put("max", "2");
                                }
                            }, Optional.empty()));
                    add(xmlFactory.addElement(document, "Filters", new HashMap<>(), Optional.of(
                            xmlFactory.addElement(document, "ThresholdFilter", new HashMap<String, String>() {
                                {
                                    put("level", "ERROR");
                                    put("onMatch", "ACCEPT");
                                }
                            }, Optional.<Element>empty())
                    )));
                }
            }));

            Element loggers = document.createElement("Loggers");
            rootElement.appendChild(loggers);

            Element rootRootElement = xmlFactory.addElement(document, "Root", new HashMap<String, String>() {
                {
                    put("level", "DEBUG");
                }
            }, Optional.of(xmlFactory.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "ErrorRollingFile");
                }
            }, Optional.<Element>empty())));

            rootRootElement.appendChild(xmlFactory.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "MainRollingFile");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(rootRootElement);

            rootElement.appendChild(xmlFactory.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "STDOUT");
                }
            }, Optional.<Element>empty()));

            rootElement.appendChild(xmlFactory.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "MainRollingFile");
                }
            }, Optional.<Element>empty()));

            rootElement.appendChild(xmlFactory.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "IntraFilterRollingFile");
                }
            }, Optional.<Element>empty()));

            rootElement.appendChild(xmlFactory.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "HttpRollingFile");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(xmlFactory.addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "com.sun.jersey");
                    put("level", "off");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(xmlFactory.addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "net.sf.ehcache");
                    put("level", "error");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(xmlFactory.addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "org.apache");
                    put("level", "debug");
                }
            }, Optional.of(xmlFactory.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "HttpRollingFile");
                }
            }, Optional.<Element>empty()))));

            loggers.appendChild(xmlFactory.addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "org.eclipse.jetty");
                    put("level", "off");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(xmlFactory.addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "org.springframework");
                    put("level", "debug");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(xmlFactory.addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "org.openrepose");
                    put("level", "debug");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(xmlFactory.addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "intrafilter-logging");
                    put("level", "trace");
                }
            }, Optional.of(xmlFactory.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "IntraFilterRollingFile");
                }
            }, Optional.<Element>empty()))));


            Logger.debug("Updated logging: " + xmlFactory.convertDocumentToString(document));

            return xmlFactory.convertDocumentToString(document);
        } else {
            //log4j.properties
            return "log4j.rootLogger=DEBUG, consoleOut\n" +
                    "\n" +
                    "#Jetty Logging Turned Off\n" +
                    "log4j.logger.org.eclipse.jetty=OFF\n" +
                    "log4j.logger.com.sun.jersey=OFF\n" +
                    "log4j.logger.org.springframework=WARN\n" +
                    "log4j.logger.org.apache=DEBUG\n" +
                    "log4j.logger.org.openrepose=DEBUG\n" +
                    "log4j.logger.intrafilter-logging=TRACE\n" +
                    "\n" +
                    "# Console\n" +
                    "log4j.appender.consoleOut=org.apache.log4j.ConsoleAppender\n" +
                    "log4j.appender.consoleOut.layout=org.apache.log4j.PatternLayout\n" +
                    "log4j.appender.consoleOut.layout.ConversionPattern=%d %-4r [%t] %-5p %c %x - %m%n";
        }
    }

    @Override
    public String generateSystemModelXml(List<Configuration> filterNames,
                                         int majorVersion, User user,
                                         String versionId) throws InternalServerException {
        //get new doc builder
        Document document;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            Logger.error("Unable to create xml document");
            e.printStackTrace();
            throw new InternalServerException("Unable to create xml model");
        }
        Element rootElement;
        if(majorVersion >= 7)
            rootElement = document.createElementNS("http://docs.openrepose.org/repose/system-model/v2.0",
                    "system-model");
        else
            rootElement =
                    document.createElementNS("http://docs.rackspacecloud.com/repose/system-model/v2.0",
                            "system-model");
        //root element
        document.appendChild(rootElement);
        //add repose-cluster
        Element reposeCluster = document.createElement("repose-cluster");
        reposeCluster.setAttribute("id", "try-it-now");
        rootElement.appendChild(reposeCluster);

        //add nodes.  Right now there's just one.  Support for multiple is next
        Element reposeClusterNodes = document.createElement("nodes");
        reposeCluster.appendChild(reposeClusterNodes);
        Element reposeClusterNode = document.createElement("node");
        reposeClusterNode.setAttribute("id", "try-it-now-1");
        reposeClusterNode.setAttribute("hostname", "localhost");
        reposeClusterNode.setAttribute("http-port", "8080");
        reposeClusterNodes.appendChild(reposeClusterNode);

        //add filters
        Element filters = document.createElement("filters");
        reposeCluster.appendChild(filters);
        for(Configuration filter: filterNames) {
            Element filterElement = document.createElement("filter");
            //split out the name.cfg.xml and put in the name
            filterElement.setAttribute("name", filter.getName().split(Pattern.quote("."))[0]);
            filterElement.setAttribute("configuration", filter.getName());
            filters.appendChild(filterElement);
        }

        //add destination.  Right now there's just one.  Support for multiple is going to be there someday
        Element destinations = document.createElement("destinations");
        reposeCluster.appendChild(destinations);
        Element endpoint = document.createElement("endpoint");
        endpoint.setAttribute("id", "try-it-now-target");
        endpoint.setAttribute("protocol", "http");
        endpoint.setAttribute("hostname", "repose-origin-" + user.tenant + "-" + versionId.replace('.','-'));
        endpoint.setAttribute("port", "8000");
        endpoint.setAttribute("root-path", "/");
        endpoint.setAttribute("default", "true");
        destinations.appendChild(endpoint);

        return xmlFactory.convertDocumentToString(document);
    }

    private List<Configuration> unzip(File zippedFile) {
        Logger.info("unzip " + zippedFile.getName());
        List<Configuration> filterXml = new ArrayList<>();
        try {
            InputStream inputStream = new FileInputStream(zippedFile);
            ZipInputStream zis = new ZipInputStream(inputStream);
            ZipEntry zipEntry;
            byte[] buffer = new byte[1024];
            int read;
            while ((zipEntry = zis.getNextEntry())!= null) {
                Logger.debug("read " + zipEntry.getName());
                StringBuilder s = new StringBuilder();
                while ((read = zis.read(buffer, 0, 1024)) >= 0) {
                    s.append(new String(buffer, 0, read));
                }
                String[] zipEntryTokens = zipEntry.getName().split(Pattern.quote("/"));
                filterXml.add(new Configuration(zipEntryTokens[zipEntryTokens.length - 1], s.toString()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filterXml;
    }

    private List<Configuration> parseFilters(JsonNode node) throws InternalServerException{
        Map<String, Document> filterXmlMap = new HashMap<>();

        if(node != null){
            //create new instance of doc factory
            DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
            //get new doc builder
            DocumentBuilder icBuilder;
            try {
                if (node.isArray()) {
                    Iterator<JsonNode> jsonNodeIterator = node.elements();
                    while (jsonNodeIterator.hasNext()) {
                        Document filterXml;
                        JsonNode jsonNode = jsonNodeIterator.next();
                        Logger.debug("JSON ENTRY: " + jsonNode);
                        JsonNode name = jsonNode.get("filter");
                        Filter filter = filterRepository.findByName(name.textValue());
                        if (filter != null) {
                            //does filter already set in the map
                            filterXml = filterXmlMap.get(filter.name + ".cfg.xml");
                            if (filterXml == null) {
                                icBuilder = icFactory.newDocumentBuilder();
                                filterXml = icBuilder.newDocument();
                                filterXmlMap.put(filter.name + ".cfg.xml", filterXml);
                            }
                            //iterate through each token of the name and create an xml tree if one does not exist.
                            Logger.debug(jsonNode.get("name").asText());
                            String[] nameTokens = jsonNode.get("name").asText().split(Pattern.quote("."));
                            Iterator<String> nameIterator = Arrays.asList(nameTokens).iterator();
                            Element currentElement = filterXml.getDocumentElement();
                            while(nameIterator.hasNext()){
                                String nameToken = nameIterator.next();
                                if(currentElement == filterXml.getDocumentElement()) {
                                    //this is the root element
                                    if(filterXml.getDocumentElement() == null) {
                                        //this document is empty!  add a new one
                                        Element rootElement = filterXml.createElementNS(filter.namespace, nameToken);
                                        filterXml.appendChild(rootElement);
                                        currentElement = rootElement;
                                    } else if(!currentElement.getNodeName().equals(nameToken)){
                                        //not root nameToken.  gotta add
                                        currentElement = xmlFactory.insertElement(currentElement,
                                                filterXml, nameToken,
                                                jsonNode.get("value").asText(),
                                                jsonNode.get("type").asText(),
                                                !nameIterator.hasNext());
                                    }
                                } else {
                                    currentElement = xmlFactory.insertElement(currentElement,
                                            filterXml, nameToken,
                                            jsonNode.get("value").asText(),
                                            jsonNode.get("type").asText(),
                                            !nameIterator.hasNext());
                                }
                            }
                        }

                        Logger.debug("get the name :" + name);

                    }
                }
            } catch(ParserConfigurationException pce){
                pce.printStackTrace();
                Logger.error("Unable to parse request filter list");
                throw new InternalServerException("Unable to parse request filter list");
            }
        }


        List<Configuration> configurationList = new ArrayList<>();
        filterXmlMap.forEach((name, doc) ->
                        configurationList.add(new Configuration(name, xmlFactory.convertDocumentToString(doc)))
        );
        return configurationList;
    }
}
