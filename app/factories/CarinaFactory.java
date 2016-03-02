package factories;

import models.User;

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
}
