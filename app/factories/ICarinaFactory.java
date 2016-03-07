package factories;

import com.google.inject.ImplementedBy;
import models.User;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.zip.ZipEntry;

/**
 * Created by dimi5963 on 2/29/16.
 */
@ImplementedBy(CarinaFactory.class)
public interface ICarinaFactory {

    boolean createFileInCarina(ZipEntry file, StringWriter data, User user);

    Path getCarinaDirectory(String tenant);

    Path getCarinaDirectoryWithCluster(String tenant, String cluster);

    Path getCarinaOriginFile(String file);

    Path getOriginImageFile(String tenant, String file) throws IOException;

    Path getOriginImageDirectory(String tenant) throws IOException;

    Path getReposeConfigDirectory(String tenant) throws IOException;

    Path getReposeImageDirectory(String tenant) throws IOException;

    Path getCarinaReposeFile(String file);

    Path getReposeImageFile(String tenant, String file) throws IOException;
}
