package factories;

import exceptions.InternalServerException;
import play.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by dimi5963 on 3/7/16.
 */
public class VersionFactoryImpl implements VersionFactory{


    @Override
    public String getVersionUrl() {
        return play.Play.application().configuration().getString("version.endpoint");
    }

    @Override
    public String getVersionAuthToken() {
        return play.Play.application().configuration().getString("oauth.token");
    }
}
