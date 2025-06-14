package service.auth;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.LogoutRequest;

public class LogoutService {
    private final DataAccess db;

    public LogoutService(DataAccess db) {
        this.db = db;
    }

    public void logout(LogoutRequest request) throws DataAccessException {

        String token = request.authToken();
        AuthData auth = db.getAuth(token);

        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        db.deleteAuth(token);
    }
}