package dataaccess;

import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthDAOTest{
    private DataAccess db;

    @BeforeEach
    void setUp() throws Exception{
        db = new MySqlDataAccess();
        db.clear();
    }

    @Test
    void createAuthPositive() throws Exception{
        UserData user = new UserData("testuser", "password", "test@example.com");
        db.createUser(user);

        AuthData auth = new AuthData("token123", "testuser");
        db.createAuth(auth);

        AuthData found = db.getAuth("token123");
        assertEquals("testuser", found.username());
    }

    @Test
    void createAuthNegativeDuplicate() throws Exception
    {
        UserData user = new UserData("testuser", "password", "test@example.com");
        db.createUser(user);

        AuthData auth = new AuthData("token123", "testuser");
        db.createAuth(auth);
                assertThrows(DataAccessException.class, () -> db.createAuth(auth));
    }

    @Test
    void getAuthNegativeNotFound() throws Exception{
        assertNull(db.getAuth("nonexistent-token"));
    }

    @Test
    void deleteAuthPositive() throws Exception {
        UserData user = new UserData("testuser", "password", "test@example.com");
        db.createUser(user);

        AuthData auth = new AuthData("token123", "testuser");
        db.createAuth(auth);

        db.deleteAuth("token123");

        assertNull(db.getAuth("token123"));
    }
}