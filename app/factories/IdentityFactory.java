package factories;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class IdentityFactory implements IIdentityFactory {

    @Override
    public String getIdentityAuthUrl() {
        return play.Play.application().configuration().getString("identity.url") +
                play.Play.application().configuration().getString("identity.version") +
                play.Play.application().configuration().getString("identity.tokens.endpoint");
    }

    @Override
    public String getIdentityApiAuthUrl(String tenantUserId) {
        return play.Play.application().configuration().getString("identity.url") +
                play.Play.application().configuration().getString("identity.version") +
                play.Play.application().configuration().getString("identity.users.endpoint") +
                tenantUserId +
                play.Play.application().configuration().getString("identity.apikey.endpoint");
    }
}
