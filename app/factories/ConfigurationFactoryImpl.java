package factories;

import exceptions.NotFoundException;
import helpers.Helpers;
import models.Configuration;
import models.User;
import play.Logger;
import play.mvc.Http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by dimi5963 on 3/2/16.
 */
public class ConfigurationFactoryImpl implements ConfigurationFactory {
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
                    configuration.setXml(Helpers.updateSystemModelXml(user, reposeVersion, content));
                } else if (configuration.getName().equals("container.cfg.xml")) {
                    Logger.debug("update container config");
                    configuration.setXml(Helpers.generateContainerXml(majorVersion));
                } else if (configuration.getName().equals("log4j2.xml") ||
                        configuration.getName().equals("log4j.properties")) {
                    configuration.setXml(Helpers.generateLoggingXml(majorVersion));
                }
            });

            return  filterXml;
        }

        throw new exceptions.NotFoundException("No zip files");
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
