package servicetests;

import dataaccess.DataAccessException;
import dataaccess.MySqlDataAccess;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.auth.LogoutRequest;
import service.auth.LogoutService;

import static org.junit.jupiter.api.Assertions.*;

class LogoutServiceTest {
    private MySqlDataAccess db;
    private LogoutService service;

    @BeforeEach
    void setUp() throws Exception {
        db = new MySqlDataAccess();
        db.clear();

        db.createUser(new UserData("tester", "pass", "tester@example.com"));
        db.createAuth(new AuthData("valid-token", "tester"));

        service = new LogoutService(db);
    }

    @Test
    void logoutPositive() throws Exception {
        LogoutRequest request = new LogoutRequest("valid-token");
        assertDoesNotThrow(() -> service.logout(request));
        assertNull(db.getAuth("valid-token"));
    }

    @Test
    void logoutNegativeInvalidToken() {
        LogoutRequest request = new LogoutRequest("invalid-token");
        assertThrows(DataAccessException.class, () -> service.logout(request));
    }
}