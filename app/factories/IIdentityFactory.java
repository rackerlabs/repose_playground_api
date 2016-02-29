package factories;

import com.google.inject.ImplementedBy;

/**
 * Created by dimi5963 on 2/29/16.
 */
@ImplementedBy(IdentityFactory.class)
public interface IIdentityFactory {

    String getIdentityAuthUrl();

    String getIdentityApiAuthUrl(String tenantUserId);

}
