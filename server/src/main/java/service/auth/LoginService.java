package service.auth;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.data.AuthData;
import model.LoginRequest;
import model.LoginResult;
import model.data.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

public class LoginService {
    private final DataAccess db;

    public LoginService(DataAccess db) {
        this.db = db;
    }

    public LoginResult login(LoginRequest request) throws DataAccessException {
        if (request.username() == null || request.username().isBlank() ||
                request.password() == null || request.password().isBlank()) {
            throw new DataAccessException("bad request");
        }

        UserData user = db.getUser(request.username());
        if (user == null || !BCrypt.checkpw(request.password(), user.password())) {
            throw new DataAccessException("unauthorized");
        }

        String token = UUID.randomUUID().toString();
        AuthData auth = new AuthData(token, request.username());
        db.createAuth(auth);

        return new LoginResult(request.username(), token);
    }
}