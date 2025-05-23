package service;

import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ClearServiceTest {
    @Test
    public void testClearSuccess() {
        var db = new MemoryDataAccess();
        var service = new ClearService(db);

        assertDoesNotThrow(service::clear);
    }
}