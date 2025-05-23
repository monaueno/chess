package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;

public class LogoutService {
    private final DataAccess db;

    public LogoutService(DataAccess db) {
        this.db = db;
    }

    public void logout(LogoutRequest request) throws DataAccessException {
        AuthData auth = db.getAuth(request.authToken());

        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        db.deleteAuth(request.authToken());
    }
}