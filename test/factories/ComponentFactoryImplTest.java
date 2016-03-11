package factories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.junit.Test;
import org.w3c.dom.Document;
import play.libs.Json;
import repositories.FilterRepository;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Created by dimi5963 on 3/9/16.
 */
public class ComponentFactoryImplTest {

    private FilterRepository filterRepository = mock(FilterRepository.class);

    @Test
    public void testGenerateJSONTreeAddHeader() throws Exception {

        Document document = new DocumentBuilderFactoryImpl().
                newDocumentBuilder().parse("test_data/add-header/add-header.cfg.xml");

        ObjectNode parentJson = JsonNodeFactory.instance.objectNode();
        JsonNode addHeaderNode =
                new ComponentFactoryImpl(filterRepository).generateJSONTree("add-header", parentJson, document);

        InputStream io = Files.newInputStream(
                Paths.get("test_data/add-header/add-header.json"), StandardOpenOption.READ);

        assertEquals(Json.parse(io), Json.toJson(addHeaderNode));
    }

    @Test
    public void testGenerateJSONTreeIpUser() throws Exception {

        Document document = new DocumentBuilderFactoryImpl().
                newDocumentBuilder().parse("test_data/ip-user/ip-user.cfg.xml");

        ObjectNode parentJson = JsonNodeFactory.instance.objectNode();
        JsonNode ipUserNode =
                new ComponentFactoryImpl(filterRepository).generateJSONTree("ip-user", parentJson, document);

        InputStream io = Files.newInputStream(
                Paths.get("test_data/ip-user/ip-user.json"), StandardOpenOption.READ);

        assertEquals(Json.parse(io), Json.toJson(ipUserNode));

    }

    @Test
    public void testGenerateJSONTreeCompression() throws Exception {

        Document document = new DocumentBuilderFactoryImpl().
                newDocumentBuilder().parse("test_data/compression/compression.cfg.xml");

        ObjectNode parentJson = JsonNodeFactory.instance.objectNode();
        JsonNode compression =
                new ComponentFactoryImpl(filterRepository).generateJSONTree("compression", parentJson, document);

        InputStream io = Files.newInputStream(
                Paths.get("test_data/compression/content-compression.json"), StandardOpenOption.READ);

        assertEquals(Json.parse(io), Json.toJson(compression));

    }

    @Test
    public void testGenerateJSONTreeDestinationRouter() throws Exception {

        Document document = new DocumentBuilderFactoryImpl().
                newDocumentBuilder().parse("test_data/destination-router/destination-router.cfg.xml");

        ObjectNode parentJson = JsonNodeFactory.instance.objectNode();
        JsonNode destinationRouter =
                new ComponentFactoryImpl(filterRepository).generateJSONTree("destination-router", parentJson, document);

        InputStream io = Files.newInputStream(
                Paths.get("test_data/destination-router/destination-router.json"), StandardOpenOption.READ);

        assertEquals(Json.parse(io), Json.toJson(destinationRouter));

    }

    @Test
    public void testGenerateJSONTreeKeystoneV2() throws Exception {

        Document document = new DocumentBuilderFactoryImpl().
                newDocumentBuilder().parse("test_data/keystone-v2/keystone-v2.cfg.xml");

        ObjectNode parentJson = JsonNodeFactory.instance.objectNode();
        JsonNode keytoneV2 =
                new ComponentFactoryImpl(filterRepository).generateJSONTree("keystone-v2", parentJson, document);

        InputStream io = Files.newInputStream(
                Paths.get("test_data/keystone-v2/keystone-v2.json"), StandardOpenOption.READ);

        assertEquals(Json.parse(io).toString(), Json.toJson(keytoneV2).toString());

    }
}