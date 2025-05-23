package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;

public class ClearService {
    private final DataAccess db;

    public ClearService(DataAccess db) {
        this.db = db;
    }

    public void clear() throws DataAccessException {
        db.clear();
    }
}