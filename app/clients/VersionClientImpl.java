package clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import exceptions.InternalServerException;
import factories.VersionFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import play.Logger;
import play.libs.Json;
import play.libs.ws.WSClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by dimi5963 on 2/29/16.
 */
public class VersionClientImpl implements VersionClient {
    @Inject
    WSClient wsClient;


    private final VersionFactory versionFactory;

    @Inject
    public VersionClientImpl(VersionFactory versionFactory){
        this.versionFactory = versionFactory;
    }

    @Override
    public List<String> getVersions() throws InternalServerException{
        String url = versionFactory.getVersionUrl();
        String token = versionFactory.getVersionAuthToken();
        return wsClient.url(url)
                .setHeader("Authorization", "token " + token).get().map(
                        response -> {
                            Iterator<JsonNode> result = response.asJson().elements();
                            List<String> tagList = new ArrayList<>();
                            while (result.hasNext()) {
                                tagList.add(result.next().findValue("name").textValue().replaceAll("repose-", "").replaceAll("papi-", ""));
                            }
                            return tagList;
                        }).recover(
                        throwable -> {
                            throw new InternalServerException(
                                    "We are currently experiencing difficulties.  " +
                                            "Please try again later.");
                        }
                ).get(30000);
    }

}
