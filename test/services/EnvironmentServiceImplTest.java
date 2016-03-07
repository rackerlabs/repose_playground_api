package services;

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
import static org.mockito.Mockito.*;

/**
 * Created by dimi5963 on 3/4/16.
 */
public class EnvironmentServiceImplTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testGeneratedOriginEnvironmentSuccess() throws Exception {

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

        ApplicationService applicationService = mock(ApplicationService.class);

        String originId = "1";
        String reposeId = "2";

        try {
            when(applicationService.createOriginInstance(any(), any(), anyString())).thenReturn(originId);
            when(applicationService.createReposeInstance(any(), any(), anyList(), anyString())).thenReturn(reposeId);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        assertEquals(reposeId,
                new EnvironmentServiceImpl(applicationService).
                        generatedOriginEnvironment(cluster, "1", user, configurationList));

        try{
            verify(applicationService).createOriginInstance(any(), any(), anyString());
            verify(applicationService).createReposeInstance(any(), any(), anyList(), anyString());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGeneratedOriginEnvironmentOriginNull() throws Exception {

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

        ApplicationService applicationService = mock(ApplicationService.class);

        try {
            when(applicationService.createOriginInstance(any(), any(), anyString())).thenReturn(null);
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        exception.expect(InternalServerException.class);
        exception.expectMessage("Unable to start origin service.");
        new EnvironmentServiceImpl(applicationService).
                        generatedOriginEnvironment(cluster, "1", user, configurationList);

        try{
            verify(applicationService).createOriginInstance(any(), any(), anyString());
            verify(applicationService, never()).createReposeInstance(any(), any(), anyList(), anyString());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGeneratedOriginEnvironmentOriginThrewException() throws Exception {

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

        ApplicationService applicationService = mock(ApplicationService.class);

        try {
            when(applicationService.createOriginInstance(any(), any(), anyString())).thenThrow(
                    new InternalServerException("origin failed."));
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        exception.expect(InternalServerException.class);
        exception.expectMessage("origin failed.");
        new EnvironmentServiceImpl(applicationService).
                generatedOriginEnvironment(cluster, "1", user, configurationList);

        try{
            verify(applicationService).createOriginInstance(any(), any(), anyString());
            verify(applicationService, never()).createReposeInstance(any(), any(), anyList(), anyString());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGeneratedOriginEnvironmentReposeException() throws Exception {

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

        ApplicationService applicationService = mock(ApplicationService.class);
        String originId = "1";

        try {
            when(applicationService.createOriginInstance(any(), any(), anyString())).thenReturn(originId);
            when(applicationService.createReposeInstance(any(), any(), anyList(), anyString())).
                    thenThrow(new InternalServerException("repose failed."));
        }catch(InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        exception.expect(InternalServerException.class);
        exception.expectMessage("repose failed.");
        new EnvironmentServiceImpl(applicationService).
                generatedOriginEnvironment(cluster, "1", user, configurationList);

        try{
            verify(applicationService).createOriginInstance(any(), any(), anyString());
            verify(applicationService).createReposeInstance(any(), any(), anyList(), anyString());
        } catch (InternalServerException e) {
            fail(e.getLocalizedMessage());
        }
    }
}