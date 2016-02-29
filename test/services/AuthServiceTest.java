package services;

import clients.IIdentityClient;
import exceptions.InternalServerException;
import exceptions.UnauthorizedException;
import models.User;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import repositories.IUserRepository;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by dimi5963 on 2/28/16.
 */
public class AuthServiceTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();


    @Test
    public void testGetUserSuccess() {

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
        IIdentityClient identityClientMock = mock(IIdentityClient.class);
        when(repositoryMock.findByNameAndPasswordCurrent(anyString(), anyString())).thenReturn(null);

        try {
            when(identityClientMock.getUser(any())).thenReturn(user);
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        User returnedUser = null;
        try {
            returnedUser =
                    new AuthService(repositoryMock, identityClientMock).getUser("fake-user", "fake-pass");
        }catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        assertEquals(returnedUser.tenant, user.tenant);
        assertEquals(returnedUser.userid, user.userid);
        assertEquals(returnedUser.username, user.username);

        verify(repositoryMock).findByNameAndPasswordCurrent(anyString(), anyString());
        try{
            verify(identityClientMock).getUser(any());
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }


    }

    @Test
    public void testGetUserAlreadyExists() {

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
        IIdentityClient identityClientMock = mock(IIdentityClient.class);
        when(repositoryMock.findByNameAndPasswordCurrent(anyString(), anyString())).thenReturn(user);

        User returnedUser = null;
        try {
            returnedUser =
                    new AuthService(repositoryMock, identityClientMock).getUser("fake-user", "fake-pass");
        }catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        assertEquals(returnedUser.tenant, user.tenant);
        assertEquals(returnedUser.userid, user.userid);
        assertEquals(returnedUser.username, user.username);

        verify(repositoryMock).findByNameAndPasswordCurrent(anyString(), anyString());
        try{
            verify(identityClientMock, never()).getUser(any());
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetUserNoPassword() {

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
        IIdentityClient identityClientMock = mock(IIdentityClient.class);
        when(repositoryMock.findByNameAndPasswordCurrent(anyString(), anyString())).thenReturn(null);

        try {
            when(identityClientMock.getUser(any())).thenReturn(user);
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        User returnedUser = null;
        try {
            returnedUser =
                    new AuthService(repositoryMock, identityClientMock).getUser("fake-user", null);
        }catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        assertEquals(returnedUser.tenant, user.tenant);
        assertEquals(returnedUser.userid, user.userid);
        assertEquals(returnedUser.username, user.username);

        verify(repositoryMock).findByNameAndPasswordCurrent(anyString(), anyString());
        try{
            verify(identityClientMock).getUser(any());
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetUserNoUsername() {

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
        IIdentityClient identityClientMock = mock(IIdentityClient.class);
        when(repositoryMock.findByNameAndPasswordCurrent(anyString(), anyString())).thenReturn(null);

        try {
            when(identityClientMock.getUser(any())).thenReturn(user);
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        User returnedUser = null;
        try {
            returnedUser =
                    new AuthService(repositoryMock, identityClientMock).getUser(null, "fake-pass");
        }catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        assertEquals(returnedUser.tenant, user.tenant);
        assertEquals(returnedUser.userid, user.userid);
        assertEquals(returnedUser.username, user.username);

        verify(repositoryMock).findByNameAndPasswordCurrent(anyString(), anyString());
        try{
            verify(identityClientMock).getUser(any());
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetUserUnauthenticated() throws UnauthorizedException, InternalServerException{

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
        IIdentityClient identityClientMock = mock(IIdentityClient.class);
        when(repositoryMock.findByNameAndPasswordCurrent(anyString(), anyString())).thenReturn(null);

        try {
            when(identityClientMock.getUser(any())).thenThrow(new UnauthorizedException("unauthorized."));
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        exception.expect(UnauthorizedException.class);
        exception.expectMessage("unauthorized.");
        new AuthService(repositoryMock, identityClientMock).getUser("fake-user", "fake-pass");

        verify(repositoryMock).findByNameAndPasswordCurrent(anyString(), anyString());
        try{
            verify(identityClientMock).getUser(any());
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetUserInternalServerError() throws UnauthorizedException, InternalServerException{

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
        IIdentityClient identityClientMock = mock(IIdentityClient.class);
        when(repositoryMock.findByNameAndPasswordCurrent(anyString(), anyString())).thenReturn(null);

        try {
            when(identityClientMock.getUser(any())).thenThrow(new InternalServerException("everything is hosed."));
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        exception.expect(InternalServerException.class);
        exception.expectMessage("everything is hosed.");
        new AuthService(repositoryMock, identityClientMock).getUser("fake-user", "fake-pass");

        verify(repositoryMock).findByNameAndPasswordCurrent(anyString(), anyString());
        try{
            verify(identityClientMock).getUser(any());
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetUserNoUserData() {
        //set up mocks
        IUserRepository repositoryMock = mock(IUserRepository.class);
        IIdentityClient identityClientMock = mock(IIdentityClient.class);
        when(repositoryMock.findByNameAndPasswordCurrent(anyString(), anyString())).thenReturn(null);

        try {
            when(identityClientMock.getUser(any())).thenReturn(null);
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        try {
            assertNull(new AuthService(repositoryMock, identityClientMock).getUser("fake-user", "fake-pass"));
        }catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        verify(repositoryMock).findByNameAndPasswordCurrent(anyString(), anyString());
        try{
            verify(identityClientMock).getUser(any());
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetUserApiKeySuccess() {

        //set up mock user
        String apiKey = "fake-api";

        //set up mocks
        IUserRepository repositoryMock = mock(IUserRepository.class);
        IIdentityClient identityClientMock = mock(IIdentityClient.class);

        try {
            when(identityClientMock.getUserApiKey(anyString(), anyString())).thenReturn(apiKey);
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        try {
            assertEquals(apiKey, new AuthService(repositoryMock, identityClientMock).getUserApiKey(
                    "fake-token", "fake-tenant-user"));
        }catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        try{
            verify(identityClientMock).getUserApiKey(anyString(), anyString());
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

    }

    @Test
    public void testGetUserApiKeyNoToken() {

        //set up mock user
        String apiKey = "fake-api";

        //set up mocks
        IUserRepository repositoryMock = mock(IUserRepository.class);
        IIdentityClient identityClientMock = mock(IIdentityClient.class);

        try {
            when(identityClientMock.getUserApiKey(anyString(), anyString())).thenReturn(apiKey);
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        try {
            assertEquals(apiKey, new AuthService(repositoryMock, identityClientMock).getUserApiKey(
                    null, "fake-tenant-user"));
        }catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        try{
            verify(identityClientMock).getUserApiKey(anyString(), anyString());
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetUserApiKeyUnauthenticated()  throws UnauthorizedException, InternalServerException{

        //set up mocks
        IUserRepository repositoryMock = mock(IUserRepository.class);
        IIdentityClient identityClientMock = mock(IIdentityClient.class);

        try {
            when(identityClientMock.getUserApiKey(anyString(), anyString())).thenThrow(
                    new UnauthorizedException("unable to authenticate.")
            );
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        exception.expect(UnauthorizedException.class);
        exception.expectMessage("unable to authenticate.");
        new AuthService(repositoryMock, identityClientMock).getUserApiKey(
                "fake-token", "fake-tenant-user");

        try{
            verify(identityClientMock).getUserApiKey(anyString(), anyString());
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetUserApiKeyNoTenantUserId() {

        //set up mock user
        String apiKey = "fake-api";

        //set up mocks
        IUserRepository repositoryMock = mock(IUserRepository.class);
        IIdentityClient identityClientMock = mock(IIdentityClient.class);

        try {
            when(identityClientMock.getUserApiKey(anyString(), anyString())).thenReturn(apiKey);
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        try {
            assertEquals(apiKey, new AuthService(repositoryMock, identityClientMock).getUserApiKey(
                    "fake-token", null));
        }catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        try{
            verify(identityClientMock).getUserApiKey(anyString(), anyString());
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testGetUserApiKeyInternalServerError() throws UnauthorizedException, InternalServerException{

        //set up mocks
        IUserRepository repositoryMock = mock(IUserRepository.class);
        IIdentityClient identityClientMock = mock(IIdentityClient.class);

        try {
            when(identityClientMock.getUserApiKey(anyString(), anyString())).thenThrow(
                    new InternalServerException("everything is hosed.")
            );
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        exception.expect(InternalServerException.class);
        exception.expectMessage("everything is hosed.");
        new AuthService(repositoryMock, identityClientMock).getUserApiKey(
                    "fake-token", "fake-tenant-user");

        try{
            verify(identityClientMock).getUserApiKey(anyString(), anyString());
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }
    }


    @Test
    public void testGetUserApiKeyNoUserData() {

        //set up mocks
        IUserRepository repositoryMock = mock(IUserRepository.class);
        IIdentityClient identityClientMock = mock(IIdentityClient.class);
        try {
            when(identityClientMock.getUserApiKey(anyString(), anyString())).thenReturn(null);
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        try {
            assertNull(new AuthService(repositoryMock, identityClientMock).getUserApiKey(
                    "fake-token", "fake-tenant-user"));
        }catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }

        try{
            verify(identityClientMock).getUserApiKey(anyString(), anyString());
        } catch(UnauthorizedException | InternalServerException e ){
            fail(e.getLocalizedMessage());
        }
    }

}
