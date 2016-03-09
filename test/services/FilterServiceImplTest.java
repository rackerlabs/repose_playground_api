package services;

import clients.ComponentClient;
import clients.VersionClient;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.InternalServerException;
import factories.ComponentFactory;
import factories.XmlFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by dimi5963 on 3/8/16.
 */
public class FilterServiceImplTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testGetVersionsSuccess() throws Exception {

        List<String> versions = new ArrayList<String>(){
            {
                add("1.0");
                add("2.0");
                add("3.0");
            }
        };

        VersionClient versionClient = mock(VersionClient.class);
        ComponentClient componentClient = mock(ComponentClient.class);
        ComponentFactory componentFactory = mock(ComponentFactory.class);
        XmlFactory xmlFactory = mock(XmlFactory.class);

        when(versionClient.getVersions()).thenReturn(versions);

        assertEquals(versions, new FilterServiceImpl(versionClient, componentClient, componentFactory,
                xmlFactory).getVersions());

        verify(versionClient).getVersions();
    }

    @Test
    public void testGetVersionsException() throws Exception {
        VersionClient versionClient = mock(VersionClient.class);
        ComponentClient componentClient = mock(ComponentClient.class);
        ComponentFactory componentFactory = mock(ComponentFactory.class);
        XmlFactory xmlFactory = mock(XmlFactory.class);

        when(versionClient.getVersions()).thenThrow(new InternalServerException(
                "failed to get versions"
        ));

        exception.expect(InternalServerException.class);
        exception.expectMessage("failed to get versions");
        new FilterServiceImpl(versionClient, componentClient, componentFactory,
                xmlFactory).getVersions();

    }

    @Test
    public void testGetFiltersByVersionSuccess() throws Exception {

        List<String> filters = new ArrayList<String>(){
            {
                add("add-header");
                add("compression");
                add("ip-user");
            }
        };

        VersionClient versionClient = mock(VersionClient.class);
        ComponentClient componentClient = mock(ComponentClient.class);
        ComponentFactory componentFactory = mock(ComponentFactory.class);
        XmlFactory xmlFactory = mock(XmlFactory.class);

        when(componentClient.getFiltersByVersion(anyString())).thenReturn(filters);

        assertEquals(filters, new FilterServiceImpl(versionClient, componentClient, componentFactory,
                xmlFactory).getFiltersByVersion("1"));

        verify(componentClient).getFiltersByVersion(anyString());
    }

    @Test
    public void testGetFiltersByVersionVersionNull() throws Exception {

        List<String> filters = new ArrayList<String>(){
            {
                add("add-header");
                add("compression");
                add("ip-user");
            }
        };

        VersionClient versionClient = mock(VersionClient.class);
        ComponentClient componentClient = mock(ComponentClient.class);
        ComponentFactory componentFactory = mock(ComponentFactory.class);
        XmlFactory xmlFactory = mock(XmlFactory.class);

        when(componentClient.getFiltersByVersion(anyString())).thenReturn(filters);

        exception.expect(InternalServerException.class);
        exception.expectMessage("Version not specified.");
        new FilterServiceImpl(versionClient, componentClient, componentFactory,
                xmlFactory).getFiltersByVersion(null);

        verify(componentClient, never()).getFiltersByVersion(anyString());

    }

    @Test
    public void testGetFiltersByVersionException() throws Exception {

        VersionClient versionClient = mock(VersionClient.class);
        ComponentClient componentClient = mock(ComponentClient.class);
        ComponentFactory componentFactory = mock(ComponentFactory.class);
        XmlFactory xmlFactory = mock(XmlFactory.class);

        when(componentClient.getFiltersByVersion(anyString()))
                .thenThrow(new InternalServerException("failed to get filters"));

        exception.expect(InternalServerException.class);
        exception.expectMessage("failed to get filters");
        new FilterServiceImpl(versionClient, componentClient, componentFactory,
                xmlFactory).getFiltersByVersion("1");

        verify(componentClient).getFiltersByVersion(anyString());

    }

    @Test
    public void testGetComponentDataSuccess() throws Exception {

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("message", "test");

        Document doc =
                DocumentBuilderFactory.newInstance().
                        newDocumentBuilder().parse(
                        new InputSource(new StringReader("<test>data</test>")));


        VersionClient versionClient = mock(VersionClient.class);
        ComponentClient componentClient = mock(ComponentClient.class);
        ComponentFactory componentFactory = mock(ComponentFactory.class);
        XmlFactory xmlFactory = mock(XmlFactory.class);

        when(componentFactory.generateJSONTree(anyString(), any(),
                any())).thenReturn(objectNode);
        when(componentClient.getComponentXSD(anyString(), anyString())).
                thenReturn(doc);

        assertEquals(objectNode, new FilterServiceImpl(versionClient, componentClient, componentFactory,
                xmlFactory).getComponentData("1", "2"));

        verify(componentClient).getComponentXSD(anyString(), anyString());
        verify(componentFactory).generateJSONTree(anyString(), any(), any());
    }

    @Test
    public void testGetComponentDataVersionNull() throws Exception {

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("message", "test");

        Document doc =
                DocumentBuilderFactory.newInstance().
                        newDocumentBuilder().parse(
                        new InputSource(new StringReader("<test>data</test>")));


        VersionClient versionClient = mock(VersionClient.class);
        ComponentClient componentClient = mock(ComponentClient.class);
        ComponentFactory componentFactory = mock(ComponentFactory.class);
        XmlFactory xmlFactory = mock(XmlFactory.class);

        when(componentFactory.generateJSONTree(anyString(), any(),
                any())).thenReturn(objectNode);
        when(componentClient.getComponentXSD(anyString(), anyString())).
                thenReturn(doc);

        exception.expect(InternalServerException.class);
        exception.expectMessage("Version or component not specified.");
        new FilterServiceImpl(versionClient, componentClient, componentFactory,
                xmlFactory).getComponentData(null, "2");

        verify(componentClient, never()).getComponentXSD(anyString(), anyString());
        verify(componentFactory, never()).generateJSONTree(anyString(), any(), any());

    }

    @Test
    public void testGetComponentDataComponentNull() throws Exception {

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("message", "test");

        Document doc =
                DocumentBuilderFactory.newInstance().
                        newDocumentBuilder().parse(
                        new InputSource(new StringReader("<test>data</test>")));


        VersionClient versionClient = mock(VersionClient.class);
        ComponentClient componentClient = mock(ComponentClient.class);
        ComponentFactory componentFactory = mock(ComponentFactory.class);
        XmlFactory xmlFactory = mock(XmlFactory.class);

        when(componentFactory.generateJSONTree(anyString(), any(),
                any())).thenReturn(objectNode);
        when(componentClient.getComponentXSD(anyString(), anyString())).
                thenReturn(doc);

        exception.expect(InternalServerException.class);
        exception.expectMessage("Version or component not specified.");
        new FilterServiceImpl(versionClient, componentClient, componentFactory,
                xmlFactory).getComponentData("1", null);

        verify(componentClient, never()).getComponentXSD(anyString(), anyString());
        verify(componentFactory, never()).generateJSONTree(anyString(), any(), any());
    }

    @Test
    public void testGetComponentDataGetComponentException() throws Exception {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("message", "test");

        VersionClient versionClient = mock(VersionClient.class);
        ComponentClient componentClient = mock(ComponentClient.class);
        ComponentFactory componentFactory = mock(ComponentFactory.class);
        XmlFactory xmlFactory = mock(XmlFactory.class);

        when(componentFactory.generateJSONTree(anyString(), any(),
                any())).thenReturn(objectNode);
        when(componentClient.getComponentXSD(anyString(), anyString())).
                thenThrow(new InternalServerException("component xsd failed"));

        exception.expect(InternalServerException.class);
        exception.expectMessage("component xsd failed");
        new FilterServiceImpl(versionClient, componentClient, componentFactory,
                xmlFactory).getComponentData("1", "2");

        verify(componentClient).getComponentXSD(anyString(), anyString());
        verify(componentFactory, never()).generateJSONTree(anyString(), any(), any());

    }

    @Test
    public void testGetComponentDataGenerateJsonNull() throws Exception {

        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("message", "test");

        Document doc =
                DocumentBuilderFactory.newInstance().
                        newDocumentBuilder().parse(
                        new InputSource(new StringReader("<test>data</test>")));


        VersionClient versionClient = mock(VersionClient.class);
        ComponentClient componentClient = mock(ComponentClient.class);
        ComponentFactory componentFactory = mock(ComponentFactory.class);
        XmlFactory xmlFactory = mock(XmlFactory.class);

        when(componentFactory.generateJSONTree(anyString(), any(),
                any())).thenReturn(null);
        when(componentClient.getComponentXSD(anyString(), anyString())).
                thenReturn(doc);

        assertNull(new FilterServiceImpl(versionClient, componentClient, componentFactory,
                xmlFactory).getComponentData("1", "2"));

        verify(componentClient).getComponentXSD(anyString(), anyString());
        verify(componentFactory).generateJSONTree(anyString(), any(), any());
    }
}