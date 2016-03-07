package factories;

import models.User;
import play.Logger;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.ZipEntry;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class CarinaFactory implements ICarinaFactory {

    @Override
    public boolean createFileInCarina(ZipEntry file, StringWriter data, User user) {
        try {
            String parentDirectories = file.getName().substring(0, file.getName().lastIndexOf("/"));
            if(!Files.isDirectory(getCarinaDirectory(user.tenant).resolve(Paths.get(parentDirectories)))){
                Files.createDirectories(getCarinaDirectory(user.tenant).resolve(Paths.get(parentDirectories)));
            }
            return Files.write(getCarinaDirectory(user.tenant).resolve(file.getName()), Arrays.asList(data.toString().split("\n"))) != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Path getCarinaDirectory(String tenant) {
        return Paths.get("/tmp", tenant);
    }

    @Override
    public Path getCarinaDirectoryWithCluster(String tenant, String cluster) {
        return getCarinaDirectory(tenant).resolve(cluster);
    }

    @Override
    public Path getCarinaOriginFile(String file) {
        return play.Play.application().path().toPath().resolve("carina").
                resolve("origin-image").resolve(file);
    }

    @Override
    public Path getOriginImageFile(String tenant, String file) throws IOException {
        return getOriginImageDirectory(tenant).resolve(file);
    }

    @Override
    public Path getOriginImageDirectory(String tenant) throws IOException {
        try {
            return Files.createDirectories(getCarinaDirectory(tenant).resolve("origin_image"));
        } catch (IOException e) {
            Logger.error("Unable to create origin image directory " + e.getLocalizedMessage());
            throw e;
        }
    }

    @Override
    public Path getReposeConfigDirectory(String tenant) throws IOException {
        try {
            return Files.createDirectories(getReposeImageDirectory(tenant).resolve("repose_config"));
        } catch (IOException e) {
            Logger.error("Unable to create repose config directory " + e.getLocalizedMessage());
            throw e;
        }
    }

    @Override
    public Path getReposeImageDirectory(String tenant) throws IOException {
        try {
            return Files.createDirectories(getCarinaDirectory(tenant).resolve("repose_image"));
        } catch (IOException e) {
            Logger.error("Unable to create repose image directory " + e.getLocalizedMessage());
            throw e;
        }
    }

    @Override
    public Path getCarinaReposeFile(String file){
        return play.Play.application().path().toPath().resolve("carina").
                resolve("repose-image").resolve(file);
    }

    @Override
    public Path getReposeImageFile(String tenant, String file) throws IOException {
        return getReposeImageDirectory(tenant).resolve(file);
    }
}
