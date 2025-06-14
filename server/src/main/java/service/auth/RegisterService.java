package service.auth;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.data.AuthData;
import model.RegisterRequest;
import model.RegisterResult;
import model.data.UserData;

import java.util.UUID;

public class RegisterService {
    private final DataAccess db;
    public RegisterService(DataAccess db){
        this.db = db;
    }

    public RegisterResult register(RegisterRequest request) throws DataAccessException {
        // Validate input
        if (request.username() == null || request.username().isBlank() ||
                request.password() == null || request.password().isBlank() ||
                request.email() == null || request.email().isBlank()) {
            throw new DataAccessException("bad request");
        }
        //Check to see if user already exists
        if(db.getUser(request.username()) != null) {
            throw new DataAccessException("this user is already taken");
        }
        var user = new UserData(request.username(), request.password(), request.email());
        db.createUser(user);

        String token = UUID.randomUUID().toString();
        var auth = new AuthData(token, request.username());
        db.createAuth(auth);

        return new RegisterResult(request.username(), token);
    }
}