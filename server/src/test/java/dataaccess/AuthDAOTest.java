package dataaccess;

import model.AuthData;
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
    void createAuth_Positive() throws Exception{
        AuthData auth = new AuthData("token123", "testuser");
        db.createAuth(auth);

        AuthData found = db.getAuth("token123");
        assertEquals("testuser", found.username());
    }

    @Test
    void createAuth_Negative_Duplicate() throws Exception
    {
        AuthData auth = new AuthData("token123", "testuser");
        db.createAuth(auth);
                assertThrows(DataAccessException.class, () -> db.createAuth(auth));
    }

    @Test
    void getAuth_Negative_NotFound() throws Exception{
        assertNull(db.getAuth("nonexistent-token"));
    }

    @Test
    void deleteAuth_Positive() throws Exception {
        AuthData auth = new AuthData("token123", "testuser");
        db.createAuth(auth);
        db.deleteAuth("token123");

        assertNull(db.getAuth("token123"));
    }
}