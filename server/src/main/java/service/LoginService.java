package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;

import java.util.UUID;

public class LoginService {
    private final DataAccess db;

    public LoginService(DataAccess db) {
        this.db = db;
    }

    public LoginResult login(LoginRequest request) throws DataAccessException {
        if (request.username() == null || request.password() == null) {
            throw new DataAccessException("bad request");
        }

        UserData user = db.getUser(request.username());
        if (user == null || !user.password().equals(request.password())) {
            throw new DataAccessException("unauthorized");
        }

        String token = UUID.randomUUID().toString();
        AuthData auth = new AuthData(token, request.username());
        db.createAuth(auth);

        return new LoginResult(request.username(), token);
    }
}