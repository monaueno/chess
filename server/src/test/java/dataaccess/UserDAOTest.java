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
    void createUserPositive() throws Exception {
        UserData user = new UserData("testuser", "pass123", "test@example.com");
        db.createUser(user);

        UserData retrieved = db.getUser("testuser");
        assertEquals(user.username(), retrieved.username());
    }

    @Test
    void createUserNegativeDuplicate() throws Exception {
        UserData user = new UserData("testuser", "pass123", "test@example.com");
        db.createUser(user);

        assertThrows(DataAccessException.class, () -> db.createUser(user));
    }

    @Test
    void getUserNegativeNotFound() throws DataAccessException {
        assertNull(db.getUser("nonexistent"));
    }
}