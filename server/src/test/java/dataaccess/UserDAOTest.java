package dataaccess;

import dataaccess.MySqlDataAccess;
import model.UserData;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class UserDAOTest {

    private DataAccess db;

    @BeforeEach
    void setUp() throws Exception {
        db = new MySqlDataAccess();
        db.clear(); // reset db
    }

    @Test
    void createUser_Positive() throws Exception {
        UserData user = new UserData("testuser", "pass123", "test@example.com");
        db.createUser(user);

        UserData retrieved = db.getUser("testuser");
        assertEquals(user.username(), retrieved.username());
    }

    @Test
    void createUser_Negative_Duplicate() throws Exception {
        UserData user = new UserData("testuser", "pass123", "test@example.com");
        db.createUser(user);

        assertThrows(DataAccessException.class, () -> db.createUser(user));
    }

    @Test
    void getUser_Negative_NotFound() throws DataAccessException {
        assertNull(db.getUser("nonexistent"));
    }
}