package services;

import models.User;
import org.joda.time.DateTime;
import org.junit.Test;
import repositories.IUserRepository;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by dimi5963 on 2/28/16.
 */
public class UserServiceTest {

    @Test
    public void testIsValueTrue() {

        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");
        user.setExpireDate(DateTime.now().plus(1000));

        //set up mocks
        IUserRepository repositoryMock = mock(IUserRepository.class);
        when(repositoryMock.findByToken(anyString())).thenReturn(user);

        assertTrue(new UserService(repositoryMock).isValid("fake-token"));

        verify(repositoryMock).findByToken(anyString());
    }

    @Test
    public void testIsValueExpired() {

        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        user.setExpireDate(DateTime.now().minus(1000));

        //set up mocks
        IUserRepository repositoryMock = mock(IUserRepository.class);
        when(repositoryMock.findByToken(anyString())).thenReturn(user);

        assertFalse(new UserService(repositoryMock).isValid("fake-token"));

        verify(repositoryMock).findByToken(anyString());
    }

    @Test
    public void testIsValidFalse() {

        //set up mocks
        IUserRepository repositoryMock = mock(IUserRepository.class);
        when(repositoryMock.findByToken(anyString())).thenReturn(null);

        assertFalse(new UserService(repositoryMock).isValid("fake-token"));

        verify(repositoryMock).findByToken(anyString());
    }

    @Test
    public void testIsValidNullToken() {

        //set up mocks
        IUserRepository repositoryMock = mock(IUserRepository.class);
        when(repositoryMock.findByToken(anyString())).thenReturn(null);

        assertFalse(new UserService(repositoryMock).isValid(null));

        verify(repositoryMock).findByToken(anyString());
    }

    @Test
    public void testFindByTokenExists() {

        //set up mock user
        User user = new User();
        user.setTenant("111");
        user.setPassword("pass");
        user.setToken("fake-token");
        user.setUserid("1");
        user.setUsername("fake-user");

        //set up mocks
        IUserRepository repositoryMock = mock(IUserRepository.class);
        when(repositoryMock.findByToken(anyString())).thenReturn(user);

        assertEquals(new UserService(repositoryMock).findByToken("fake-token"), user);

        verify(repositoryMock).findByToken(anyString());
    }

    @Test
    public void testFindByTokenNotExists() {

        //set up mocks
        IUserRepository repositoryMock = mock(IUserRepository.class);
        when(repositoryMock.findByToken(anyString())).thenReturn(null);

        assertNull(new UserService(repositoryMock).findByToken("fake-token"));

        verify(repositoryMock).findByToken(anyString());
    }

}
