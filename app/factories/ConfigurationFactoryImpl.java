package factories;

import com.google.inject.Inject;
import exceptions.NotFoundException;
import helpers.Helpers;
import models.Configuration;
import models.User;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import play.Logger;
import play.mvc.Http;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by dimi5963 on 3/2/16.
 */
public class ConfigurationFactoryImpl implements ConfigurationFactory {

    private final XmlFactory xmlFactory;

    @Inject
    public ConfigurationFactoryImpl(XmlFactory xmlFactory) {
        this.xmlFactory = xmlFactory;
    }

    @Override
    public List<Configuration> translateConfigurations(User user, String reposeVersion, Http.MultipartFormData body)
            throws NotFoundException {
        if(body.getFiles() != null && body.getFiles().size() > 0) {
            int majorVersion = Integer.parseInt(reposeVersion.split(Pattern.quote("."))[0]);
            //get the first one.  others don't matter since it's a single file upload
            Http.MultipartFormData.FilePart reposeZip = body.getFiles().get(0);
            Logger.debug("get file for: " + reposeZip.getFile().getAbsolutePath());

            List<Configuration> filterXml = unzip(reposeZip.getFile());
            filterXml.forEach(configuration -> {
                if (configuration.getName().equals("system-model.cfg.xml")) {
                    Logger.info("update system model listening node and destination");
                    String content = configuration.getXml();
                    configuration.setXml(updateSystemModelXml(user, reposeVersion, content));
                } else if (configuration.getName().equals("container.cfg.xml")) {
                    Logger.debug("update container config");
                    configuration.setXml(generateContainerXml(majorVersion));
                } else if (configuration.getName().equals("log4j2.xml") ||
                        configuration.getName().equals("log4j.properties")) {
                    configuration.setXml(generateLoggingXml(majorVersion));
                }
            });

            return  filterXml;
        }

        throw new exceptions.NotFoundException("No zip files");
    }

    @Override
    public String updateSystemModelXml(User user, String versionId, String systemModelContent) {
        //get new doc builder
        Document document = null;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new InputSource(new StringReader( systemModelContent))
            );
        } catch (SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
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

        Logger.info("Updated system model: " + xmlFactory.convertDocumentToString(document));

        return xmlFactory.convertDocumentToString(document);
    }

    @Override
    public String generateContainerXml(int majorVersion) {
        //get new doc builder
        Document document = null;
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        Element rootElement = null;
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

        return xmlFactory.convertDocumentToString(document);
    }

    @Override
    public String generateLoggingXml(int majorVersion) {
        if(majorVersion >= 7) {
            //log4j2.xml
            //get new doc builder
            Document document = null;
            try {
                document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
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

            appenders.appendChild(Helpers.addAppenders(document, "RollingFile", new HashMap<String, String>() {
                {
                    put("name", "MainRollingFile");
                    put("fileName", "/var/log/repose/current.log");
                    put("filePattern", "/var/log/repose/current-%d{yyyy-MM-dd_HHmmss}.log");
                }
            }));

            appenders.appendChild(Helpers.addAppenders(document, "RollingFile", new HashMap<String, String>() {
                {
                    put("name", "IntraFilterRollingFile");
                    put("fileName", "/var/log/repose/intra-filter.log");
                    put("filePattern", "/var/log/repose/intra-filter-%d{yyyy-MM-dd_HHmmss}.log");
                }
            }));

            appenders.appendChild(Helpers.addAppenders(document, "RollingFile", new HashMap<String, String>() {
                {
                    put("name", "HttpRollingFile");
                    put("fileName", "/var/log/repose/http-debug.log");
                    put("filePattern", "/var/log/repose/http-debug-%d{yyyy-MM-dd_HHmmss}.log");
                }
            }));

            Element errorElement = Helpers.addAppenders(document, "RollingFile", new HashMap<String, String>() {
                {
                    put("name", "ErrorRollingFile");
                    put("fileName", "/var/log/repose/error.log");
                    put("filePattern", "/var/log/repose/error-%d{yyyy-MM-dd_HHmmss}.log");
                }
            });
            errorElement.appendChild(Helpers.addElement(document, "Filters", new HashMap<String, String>(), Optional.of(
                    Helpers.addElement(document, "ThresholdFilter", new HashMap<String, String>() {
                        {
                            put("level", "ERROR");
                            put("onMatch", "ACCEPT");
                        }
                    }, Optional.<Element>empty())
            )));

            appenders.appendChild(errorElement);

            Element loggers = document.createElement("Loggers");
            rootElement.appendChild(loggers);

            Element rootRootElement = Helpers.addElement(document, "Root", new HashMap<String, String>() {
                {
                    put("level", "DEBUG");
                }
            }, Optional.of(Helpers.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "ErrorRollingFile");
                }
            }, Optional.<Element>empty())));

            rootRootElement.appendChild(Helpers.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "MainRollingFile");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(rootRootElement);

            rootElement.appendChild(Helpers.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "STDOUT");
                }
            }, Optional.<Element>empty()));

            rootElement.appendChild(Helpers.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "MainRollingFile");
                }
            }, Optional.<Element>empty()));

            rootElement.appendChild(Helpers.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "IntraFilterRollingFile");
                }
            }, Optional.<Element>empty()));

            rootElement.appendChild(Helpers.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "HttpRollingFile");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(Helpers.addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "com.sun.jersey");
                    put("level", "off");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(Helpers.addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "net.sf.ehcache");
                    put("level", "error");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(Helpers.addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "org.apache");
                    put("level", "debug");
                }
            }, Optional.of(Helpers.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "HttpRollingFile");
                }
            }, Optional.<Element>empty()))));

            loggers.appendChild(Helpers.addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "org.eclipse.jetty");
                    put("level", "off");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(Helpers.addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "org.springframework");
                    put("level", "debug");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(Helpers.addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "org.openrepose");
                    put("level", "debug");
                }
            }, Optional.<Element>empty()));

            loggers.appendChild(Helpers.addElement(document, "Logger", new HashMap<String, String>() {
                {
                    put("name", "intrafilter-logging");
                    put("level", "trace");
                }
            }, Optional.of(Helpers.addElement(document, "AppenderRef", new HashMap<String, String>() {
                {
                    put("ref", "IntraFilterRollingFile");
                }
            }, Optional.<Element>empty()))));
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

    private List<Configuration> unzip(File zippedFile) {
        Logger.info("unzip " + zippedFile.getName());
        List<Configuration> filterXml = new ArrayList<Configuration>();
        try {
            InputStream inputStream = new FileInputStream(zippedFile);
            ZipInputStream zis = new ZipInputStream(inputStream);
            ZipEntry zipEntry;
            byte[] buffer = new byte[1024];
            int read = 0;
            while ((zipEntry = zis.getNextEntry())!= null) {
                StringBuilder s = new StringBuilder();
                Logger.info("read " + zipEntry.getName());
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

}
