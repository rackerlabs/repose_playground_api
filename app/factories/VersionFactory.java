package factories;

import com.google.inject.ImplementedBy;

/**
 * Created by dimi5963 on 3/7/16.
 */
@ImplementedBy(VersionFactoryImpl.class)
public interface VersionFactory {

    String getVersionUrl();

    String getVersionAuthToken();
}
