

package servicetests;

import dataaccess.DataAccessException;
import dataaccess.MySqlDataAccess;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import model.LoginRequest;
import model.LoginResult;
import service.auth.LoginService;

import static org.junit.jupiter.api.Assertions.*;

class LoginServiceTest {
    private MySqlDataAccess db;
    private LoginService service;

    @BeforeEach
    void setUp() throws Exception {
        db = new MySqlDataAccess();
        db.clear();

        String rawPassword = "correctpassword";
        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        UserData user = new UserData("testuser", "correctpassword", "test@example.com");
        db.createUser(user);

        service = new LoginService(db);
    }

    @Test
    void loginPositive() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "correctpassword");
        LoginResult result = service.login(request);

        assertEquals("testuser", result.username());
        assertNotNull(result.authToken());

        AuthData auth = db.getAuth(result.authToken());
        assertEquals("testuser", auth.username());
    }

    @Test
    void loginNegativeWrongPassword() {
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");
        assertThrows(DataAccessException.class, () -> service.login(request));
    }

    @Test
    void loginNegativeNoSuchUser() {
        LoginRequest request = new LoginRequest("nouser", "any");
        assertThrows(DataAccessException.class, () -> service.login(request));
    }

    @Test
    void loginNegativeMissingField() {
        LoginRequest request = new LoginRequest(null, "pass");
        assertThrows(DataAccessException.class, () -> service.login(request));
    }
}