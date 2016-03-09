package services;

import clients.IDockerClient;
import exceptions.InternalServerException;
import models.Cluster;
import models.Configuration;
import models.User;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by dimi5963 on 3/4/16.
 */
public class DockerApplicationServiceTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testCreateReposeInstance() throws Exception {

        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        //mock list
        List<Configuration> configurationList = new ArrayList<Configuration>(){
            {
                add(new Configuration("filter-name", "filter-xml"));
                add(new Configuration("filter-name2", "filter-xml2"));
                add(new Configuration("filter-name3", "filter-xml3"));
            }
        };

        IDockerClient dockerClient = mock(IDockerClient.class);

        String reposeId = "2";

        try {
            when(dockerClient.createReposeInstance(any(), any(), anyString(), anyList())).thenReturn(reposeId);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        assertEquals(reposeId,
                new DockerApplicationService(dockerClient).
                        createReposeInstance(cluster, user, configurationList, "1"));

        try{
            verify(dockerClient).createReposeInstance(any(), any(), anyString(), anyList());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testCreateReposeInstanceException() throws Exception {

        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        //mock list
        List<Configuration> configurationList = new ArrayList<Configuration>(){
            {
                add(new Configuration("filter-name", "filter-xml"));
                add(new Configuration("filter-name2", "filter-xml2"));
                add(new Configuration("filter-name3", "filter-xml3"));
            }
        };

        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(dockerClient.createReposeInstance(any(), any(), anyString(), anyList())).
                    thenThrow(new InternalServerException("repose creation failed."));
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        exception.expect(InternalServerException.class);
        exception.expectMessage("repose creation failed.");
        new DockerApplicationService(dockerClient).
                createReposeInstance(cluster, user, configurationList, "1");

        try{
            verify(dockerClient).createReposeInstance(any(), any(), anyString(), anyList());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testCreateOriginInstance() throws Exception {

        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IDockerClient dockerClient = mock(IDockerClient.class);

        String originId = "2";

        try {
            when(dockerClient.createOriginInstance(any(), any(), anyString())).thenReturn(originId);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        assertEquals(originId,
                new DockerApplicationService(dockerClient).
                        createOriginInstance(cluster, user, "1"));

        try{
            verify(dockerClient).createOriginInstance(any(), any(), anyString());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testCreateOriginInstanceException() throws Exception {

        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IDockerClient dockerClient = mock(IDockerClient.class);

        try {
            when(dockerClient.createOriginInstance(any(), any(), anyString())).
                    thenThrow(new InternalServerException("origin creation failed."));
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        exception.expect(InternalServerException.class);
        exception.expectMessage("origin creation failed.");
        new DockerApplicationService(dockerClient).
                createOriginInstance(cluster, user, "1");

        try{
            verify(dockerClient).createOriginInstance(any(), any(), anyString());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testCreateThirdPartyInstance() throws InternalServerException {

        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //mock cluster
        Cluster cluster = new Cluster();
        cluster.setCert_directory("/tmp/test");
        cluster.setName("fake-name");
        cluster.setUri("fake-uri");

        IDockerClient dockerClient = mock(IDockerClient.class);
        exception.expect(InternalServerException.class);
        exception.expectMessage("Not implemented");
        new DockerApplicationService(dockerClient).
                createThirdPartyInstance(cluster, user, "1");

    }
}