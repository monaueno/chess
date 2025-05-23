package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;

import java.util.UUID;

public class RegisterService {
    private final DataAccess db;
    public RegisterService(DataAccess db){
        this.db = db;
    }

    public RegisterResult register(RegisterRequest request) throws DataAccessException {
        // Validate input
        if (request.username() == null || request.password() == null || request.email() == null) {
            throw new DataAccessException("bad request");
        }
        //Chec to see if user already exists
        if(db.getUser(request.username()) != null) {
            throw new DataAccessException("already taken");
        }
        var user = new UserData(request.username(), request.password(), request.email());
        db.createUser(user);

        String token = UUID.randomUUID().toString();
        var auth = new AuthData(token, request.username());
        db.createAuth(auth);

        return new RegisterResult(request.username(), token);
    }
}