package services;

import clients.ICarinaClient;
import com.fasterxml.jackson.databind.node.ObjectNode;
import exceptions.InternalServerException;
import exceptions.NotFoundException;
import factories.IClusterFactory;
import models.Cluster;
import models.User;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import play.libs.Json;
import repositories.IClusterRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * Created by dimi5963 on 3/1/16.
 */
public class ClusterServiceTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testGetClusterByNameSuccess() {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        //mock json node for getCluster
        ObjectNode jsonNode = Json.newObject();
        jsonNode.put("status", "active");

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IClusterRepository clusterRepository = mock(IClusterRepository.class);
        ICarinaClient carinaClient = mock(ICarinaClient.class);

        when(clusterFactory.getCarinaZipUrl(anyString(), any())).thenReturn("fake-url");
        when(clusterRepository.findByUserandName(anyLong(), anyString())).thenReturn(null);

        try {
            when(carinaClient.getClusterWithZip(anyString(), any(), anyString(), anyBoolean())).thenReturn(cluster);
            when(carinaClient.createCluster(anyString(), any())).thenReturn(true);
            when(carinaClient.getCluster(anyString(), any())).thenReturn(jsonNode);
        }catch(NotFoundException | InternalServerException | InterruptedException e) {
            fail(e.getLocalizedMessage());
        }

        try {
        Cluster returnedCluster = new ClusterService(clusterRepository, carinaClient, clusterFactory)
                .getClusterByName("fake-cluster", user, false, true);
            assertEquals(returnedCluster.cert_directory, cluster.cert_directory);
            assertEquals(returnedCluster.config_directory, cluster.config_directory);
            assertEquals(returnedCluster.id, cluster.id);
        }catch(InternalServerException e) {
            fail(e.getLocalizedMessage());
        }


        verify(clusterFactory).getCarinaZipUrl(anyString(), any());
        verify(clusterRepository).findByUserandName(anyLong(), anyString());

        try {
            verify(carinaClient).getClusterWithZip(anyString(), any(), anyString(), anyBoolean());
            verify(carinaClient, times(1)).getCluster(anyString(), any());
            verify(carinaClient).createCluster(anyString(), any());
        }catch(NotFoundException | InternalServerException | InterruptedException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetClusterByNameClusterExists() {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IClusterRepository clusterRepository = mock(IClusterRepository.class);
        ICarinaClient carinaClient = mock(ICarinaClient.class);

        when(clusterRepository.findByUserandName(anyLong(), anyString())).thenReturn(cluster);

        try {
            Cluster returnedCluster = new ClusterService(clusterRepository, carinaClient, clusterFactory)
                    .getClusterByName("fake-cluster", user, false, true);
            assertEquals(returnedCluster.cert_directory, cluster.cert_directory);
            assertEquals(returnedCluster.config_directory, cluster.config_directory);
            assertEquals(returnedCluster.id, cluster.id);
        }catch(InternalServerException e) {
            fail(e.getLocalizedMessage());
        }


        verify(clusterFactory, never()).getCarinaZipUrl(anyString(), any());
        verify(clusterRepository).findByUserandName(anyLong(), anyString());

        try {
            verify(carinaClient, never()).getClusterWithZip(anyString(), any(), anyString(), anyBoolean());
            verify(carinaClient, never()).getCluster(anyString(), any());
            verify(carinaClient, never()).createCluster(anyString(), any());
        }catch(NotFoundException | InternalServerException | InterruptedException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetClusterByNameUserNull() throws InternalServerException{
        //set up mock user
        User user = null;

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IClusterRepository clusterRepository = mock(IClusterRepository.class);
        ICarinaClient carinaClient = mock(ICarinaClient.class);

        exception.expect(InternalServerException.class);
        exception.expectMessage("User not provided.");
        new ClusterService(clusterRepository, carinaClient, clusterFactory)
                .getClusterByName("fake-cluster", user, false, true);


        verify(clusterFactory,never()).getCarinaZipUrl(anyString(), any());
        verify(clusterRepository,never()).findByUserandName(anyLong(), anyString());

        try {
            verify(carinaClient, never()).getClusterWithZip(anyString(), any(), anyString(), anyBoolean());
            verify(carinaClient, never()).getCluster(anyString(), any());
            verify(carinaClient,never()).createCluster(anyString(), any());
        }catch(NotFoundException | InternalServerException | InterruptedException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetClusterByNameGetClusterNull() {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IClusterRepository clusterRepository = mock(IClusterRepository.class);
        ICarinaClient carinaClient = mock(ICarinaClient.class);

        when(clusterFactory.getCarinaZipUrl(anyString(), any())).thenReturn("fake-url");
        when(clusterRepository.findByUserandName(anyLong(), anyString())).thenReturn(null);

        try {
            when(carinaClient.getClusterWithZip(anyString(), any(), anyString(), anyBoolean())).thenReturn(cluster);
            when(carinaClient.getCluster(anyString(), any())).thenReturn(null);
            when(carinaClient.createCluster(anyString(), any())).thenReturn(true);
        }catch(NotFoundException | InternalServerException | InterruptedException e) {
            fail(e.getLocalizedMessage());
        }

        try {
            Cluster returnedCluster = new ClusterService(clusterRepository, carinaClient, clusterFactory)
                    .getClusterByName("fake-cluster", user, false, true);
            assertEquals(returnedCluster.cert_directory, cluster.cert_directory);
            assertEquals(returnedCluster.config_directory, cluster.config_directory);
            assertEquals(returnedCluster.id, cluster.id);
        }catch(InternalServerException e) {
            fail(e.getLocalizedMessage());
        }


        verify(clusterFactory).getCarinaZipUrl(anyString(), any());
        verify(clusterRepository).findByUserandName(anyLong(), anyString());

        try {
            verify(carinaClient).getClusterWithZip(anyString(), any(), anyString(), anyBoolean());
            verify(carinaClient, times(1)).getCluster(anyString(), any());
            verify(carinaClient).createCluster(anyString(), any());
        }catch(NotFoundException | InternalServerException | InterruptedException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetClusterByNameGetClusterInactive() {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        //mock json node for getCluster
        ObjectNode jsonNode = Json.newObject();
        jsonNode.put("status", "inactive");

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IClusterRepository clusterRepository = mock(IClusterRepository.class);
        ICarinaClient carinaClient = mock(ICarinaClient.class);

        when(clusterFactory.getCarinaZipUrl(anyString(), any())).thenReturn("fake-url");
        when(clusterRepository.findByUserandName(anyLong(), anyString())).thenReturn(null);

        try {
            when(carinaClient.getClusterWithZip(anyString(), any(), anyString(), anyBoolean())).thenReturn(cluster);
            when(carinaClient.getCluster(anyString(), any())).thenReturn(jsonNode);
            when(carinaClient.createCluster(anyString(), any())).thenReturn(true);
        }catch(NotFoundException | InternalServerException | InterruptedException e) {
            fail(e.getLocalizedMessage());
        }

        try {
            Cluster returnedCluster = new ClusterService(clusterRepository, carinaClient, clusterFactory)
                    .getClusterByName("fake-cluster", user, false, true);
            assertEquals(returnedCluster.cert_directory, cluster.cert_directory);
            assertEquals(returnedCluster.config_directory, cluster.config_directory);
            assertEquals(returnedCluster.id, cluster.id);
        }catch(InternalServerException e) {
            fail(e.getLocalizedMessage());
        }


        verify(clusterFactory).getCarinaZipUrl(anyString(), any());
        verify(clusterRepository).findByUserandName(anyLong(), anyString());

        try {
            verify(carinaClient).getClusterWithZip(anyString(), any(), anyString(), anyBoolean());
            verify(carinaClient, times(1)).getCluster(anyString(), any());
            verify(carinaClient).createCluster(anyString(), any());
        }catch(NotFoundException | InternalServerException | InterruptedException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetClusterByNameGetClusterException() throws InternalServerException {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IClusterRepository clusterRepository = mock(IClusterRepository.class);
        ICarinaClient carinaClient = mock(ICarinaClient.class);

        when(clusterFactory.getCarinaZipUrl(anyString(), any())).thenReturn("fake-url");
        when(clusterRepository.findByUserandName(anyLong(), anyString())).thenReturn(null);

        try {
            when(carinaClient.getClusterWithZip(anyString(), any(), anyString(), anyBoolean())).thenReturn(cluster);
            when(carinaClient.getCluster(anyString(), any())).thenThrow(new InternalServerException("Oops failed"));;
            when(carinaClient.createCluster(anyString(), any())).thenReturn(true);
        }catch(NotFoundException | InternalServerException | InterruptedException e) {
            fail(e.getLocalizedMessage());
        }

        exception.expect(InternalServerException.class);
        exception.expectMessage("Oops failed");
        new ClusterService(clusterRepository, carinaClient, clusterFactory)
                .getClusterByName("fake-cluster", user, false, true);


        verify(clusterFactory).getCarinaZipUrl(anyString(), any());
        verify(clusterRepository).findByUserandName(anyLong(), anyString());

        try {
            verify(carinaClient).getClusterWithZip(anyString(), any(), anyString(), anyBoolean());
            verify(carinaClient).getCluster(anyString(), any());
            verify(carinaClient, never()).createCluster(anyString(), any());
        }catch(NotFoundException | InternalServerException | InterruptedException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetClusterByNameGetClusterNullNotCreateIfDNE()
            throws InternalServerException, NotFoundException, InterruptedException {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        //mock json node for getCluster
        ObjectNode jsonNode = Json.newObject();
        jsonNode.put("status", "active");

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IClusterRepository clusterRepository = mock(IClusterRepository.class);
        ICarinaClient carinaClient = mock(ICarinaClient.class);

        when(clusterFactory.getCarinaZipUrl(anyString(), any())).thenReturn("fake-uri");
        when(clusterRepository.findByUserandName(anyLong(), anyString())).thenReturn(null);

        when(carinaClient.getClusterWithZip(anyString(), any(), anyString(), anyBoolean())).thenReturn(cluster);
        when(carinaClient.createCluster(anyString(), any())).thenReturn(true);
        when(carinaClient.getCluster(anyString(), any())).thenReturn(null);

        exception.expect(InternalServerException.class);
        exception.expectMessage("Cluster doesn't exist.");
        new ClusterService(clusterRepository, carinaClient, clusterFactory)
                .getClusterByName("fake-cluster", user, false, false);


        verify(clusterFactory).getCarinaZipUrl(anyString(), any());
        verify(clusterRepository).findByUserandName(anyLong(), anyString());

        verify(carinaClient).getClusterWithZip(anyString(), any(), anyString(), anyBoolean());
        verify(carinaClient, times(1)).getCluster(anyString(), any());
        verify(carinaClient).createCluster(anyString(), any());

    }

    @Test
    public void testGetClusterByNameGetClusterNotNullNotCreateIfDNE()
            throws InternalServerException, NotFoundException, InterruptedException {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        //mock json node for getCluster
        ObjectNode jsonNode = Json.newObject();
        jsonNode.put("status", "active");

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IClusterRepository clusterRepository = mock(IClusterRepository.class);
        ICarinaClient carinaClient = mock(ICarinaClient.class);

        when(clusterFactory.getCarinaZipUrl(anyString(), any())).thenReturn("fake-url");
        when(clusterRepository.findByUserandName(anyLong(), anyString())).thenReturn(null);

        when(carinaClient.getClusterWithZip(anyString(), any(), anyString(), anyBoolean())).thenReturn(cluster);
        when(carinaClient.createCluster(anyString(), any())).thenReturn(true);
        when(carinaClient.getCluster(anyString(), any())).thenReturn(jsonNode);

        exception.expect(InternalServerException.class);
        exception.expectMessage("Cluster doesn't exist.");
        new ClusterService(clusterRepository, carinaClient, clusterFactory)
                .getClusterByName("fake-cluster", user, false, false);


        verify(clusterFactory).getCarinaZipUrl(anyString(), any());
        verify(clusterRepository).findByUserandName(anyLong(), anyString());

        verify(carinaClient).getClusterWithZip(anyString(), any(), anyString(), anyBoolean());
        verify(carinaClient, times(1)).getCluster(anyString(), any());
        verify(carinaClient).createCluster(anyString(), any());

    }

    @Test
    public void testGetClusterByNameCreateException()
            throws InternalServerException, NotFoundException, InterruptedException {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        //mock json node for getCluster
        ObjectNode jsonNode = Json.newObject();
        jsonNode.put("status", "active");

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IClusterRepository clusterRepository = mock(IClusterRepository.class);
        ICarinaClient carinaClient = mock(ICarinaClient.class);

        when(clusterFactory.getCarinaZipUrl(anyString(), any())).thenReturn("fake-url");
        when(clusterRepository.findByUserandName(anyLong(), anyString())).thenReturn(null);

        when(carinaClient.getClusterWithZip(anyString(), any(), anyString(), anyBoolean())).thenReturn(cluster);
        when(carinaClient.createCluster(anyString(), any())).thenThrow(new InternalServerException("failed to create."));
        when(carinaClient.getCluster(anyString(), any())).thenReturn(jsonNode);

        exception.expect(InternalServerException.class);
        exception.expectMessage("failed to create.");
        new ClusterService(clusterRepository, carinaClient, clusterFactory)
                .getClusterByName("fake-cluster", user, false, true);

        verify(clusterFactory).getCarinaZipUrl(anyString(), any());
        verify(clusterRepository).findByUserandName(anyLong(), anyString());

        verify(carinaClient).getClusterWithZip(anyString(), any(), anyString(), anyBoolean());
        verify(carinaClient, times(1)).getCluster(anyString(), any());
        verify(carinaClient).createCluster(anyString(), any());

    }

    @Test
    public void testGetClusterByNameGetCarinaZipUrlNull()
            throws InternalServerException, NotFoundException, InterruptedException {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        //mock json node for getCluster
        ObjectNode jsonNode = Json.newObject();
        jsonNode.put("status", "active");

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IClusterRepository clusterRepository = mock(IClusterRepository.class);
        ICarinaClient carinaClient = mock(ICarinaClient.class);

        when(clusterFactory.getCarinaZipUrl(anyString(), any())).thenReturn(null);
        when(clusterRepository.findByUserandName(anyLong(), anyString())).thenReturn(null);

        when(carinaClient.getClusterWithZip(anyString(), any(), anyString(), anyBoolean())).thenReturn(cluster);
        when(carinaClient.createCluster(anyString(), any())).thenReturn(true);
        when(carinaClient.getCluster(anyString(), any())).thenReturn(jsonNode);

        Cluster returnedCluster = new ClusterService(clusterRepository, carinaClient, clusterFactory)
                .getClusterByName("fake-cluster", user, false, true);
        assertEquals(returnedCluster.cert_directory, cluster.cert_directory);
        assertEquals(returnedCluster.config_directory, cluster.config_directory);
        assertEquals(returnedCluster.id, cluster.id);


        verify(clusterFactory).getCarinaZipUrl(anyString(), any());
        verify(clusterRepository).findByUserandName(anyLong(), anyString());

        verify(carinaClient).getClusterWithZip(anyString(), any(), anyString(), anyBoolean());
        verify(carinaClient, times(1)).getCluster(anyString(), any());
        verify(carinaClient).createCluster(anyString(), any());
    }

    @Test
    public void testGetClusterByNameGetClusterWithZipNull()
            throws InternalServerException, NotFoundException, InterruptedException {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        //mock json node for getCluster
        ObjectNode jsonNode = Json.newObject();
        jsonNode.put("status", "active");

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IClusterRepository clusterRepository = mock(IClusterRepository.class);
        ICarinaClient carinaClient = mock(ICarinaClient.class);

        when(clusterFactory.getCarinaZipUrl(anyString(), any())).thenReturn("fake-url");
        when(clusterRepository.findByUserandName(anyLong(), anyString())).thenReturn(null);

        try {
            when(carinaClient.getClusterWithZip(
                    anyString(), any(), anyString(), anyBoolean())).thenReturn(null);
            when(carinaClient.createCluster(anyString(), any())).thenReturn(true);
            when(carinaClient.getCluster(anyString(), any())).thenReturn(jsonNode);
        }catch(NotFoundException | InternalServerException | InterruptedException e) {
            fail(e.getLocalizedMessage());
        }

        exception.expect(InternalServerException.class);
        exception.expectMessage("Unable to save new cluster.");
        new ClusterService(clusterRepository, carinaClient, clusterFactory)
                .getClusterByName("fake-cluster", user, false, true);


        verify(clusterFactory).getCarinaZipUrl(anyString(), any());
        verify(clusterRepository).findByUserandName(anyLong(), anyString());

        verify(carinaClient).getClusterWithZip(anyString(), any(), anyString(), anyBoolean());
        verify(carinaClient, times(1)).getCluster(anyString(), any());
        verify(carinaClient).createCluster(anyString(), any());

    }

    @Test
    public void testGetClusterByNameGetClusterWithZipException()
            throws InternalServerException, NotFoundException, InterruptedException {
        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));
        user.id = 1L;

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setConfig_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        //mock json node for getCluster
        ObjectNode jsonNode = Json.newObject();
        jsonNode.put("status", "active");

        IClusterFactory clusterFactory = mock(IClusterFactory.class);
        IClusterRepository clusterRepository = mock(IClusterRepository.class);
        ICarinaClient carinaClient = mock(ICarinaClient.class);

        when(clusterFactory.getCarinaZipUrl(anyString(), any())).thenReturn("fake-url");
        when(clusterRepository.findByUserandName(anyLong(), anyString())).thenReturn(null);

        try {
            when(carinaClient.getClusterWithZip(
                    anyString(), any(), anyString(), anyBoolean()))
                    .thenThrow(new InternalServerException("failed to get cluster with zip."));
            when(carinaClient.createCluster(anyString(), any())).thenReturn(true);
            when(carinaClient.getCluster(anyString(), any())).thenReturn(jsonNode);
        }catch(NotFoundException | InternalServerException | InterruptedException e) {
            fail(e.getLocalizedMessage());
        }

        exception.expect(InternalServerException.class);
        exception.expectMessage("Unable to save new cluster.");
        new ClusterService(clusterRepository, carinaClient, clusterFactory)
                .getClusterByName("fake-cluster", user, false, true);


        verify(clusterFactory).getCarinaZipUrl(anyString(), any());
        verify(clusterRepository).findByUserandName(anyLong(), anyString());

        verify(carinaClient).getClusterWithZip(anyString(), any(), anyString(), anyBoolean());
        verify(carinaClient, times(1)).getCluster(anyString(), any());
        verify(carinaClient).createCluster(anyString(), any());
    }
}