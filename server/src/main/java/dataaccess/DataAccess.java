package dataaccess;
import model.UserData;
import model.AuthData;

public interface DataAccess {
    void clear();
    UserData getUser(String username) throws DataAccessException;

    void createUser(UserData username) throws DataAccessException;
    void createAuth(AuthData auth) throws DataAccessException;
}