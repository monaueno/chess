package servicetests;

import dataaccess.DataAccessException;
import dataaccess.MySqlDataAccess;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import model.RegisterRequest;
import model.RegisterResult;
import service.auth.RegisterService;

import static org.junit.jupiter.api.Assertions.*;

class RegisterServiceTest {
    private MySqlDataAccess db;
    private RegisterService service;

    @BeforeEach
    void setUp() throws Exception {
        db = new MySqlDataAccess();
        db.clear();
        service = new RegisterService(db);
    }

    @Test
    void registerPositive() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser", "pass", "newuser@example.com");
        RegisterResult result = service.register(request);

        assertEquals("newuser", result.username());
        assertNotNull(result.authToken());
        assertNotNull(db.getUser("newuser"));
        AuthData auth = db.getAuth(result.authToken());
        assertEquals("newuser", auth.username());
    }

    @Test
    void registerNegativeMissingField() {
        RegisterRequest request = new RegisterRequest(null, "pass", "email@example.com");
        assertThrows(DataAccessException.class, () -> service.register(request));
    }

    @Test
    void registerNegativeDuplicateUser() throws Exception {
        db.createUser(new UserData("existing", "pass", "email"));
        RegisterRequest request = new RegisterRequest("existing", "pass", "email");
        assertThrows(DataAccessException.class, () -> service.register(request));
    }
}