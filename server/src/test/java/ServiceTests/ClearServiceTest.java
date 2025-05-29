package ServiceTests;

import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.Test;
import service.game.ClearService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ClearServiceTest {
    @Test
    public void testClearPositive() {
        var db = new MemoryDataAccess();
        var service = new ClearService(db);

        assertDoesNotThrow(service::clear);
    }

    @Test
    public void testClearNegative() {
        assertThrows(NullPointerException.class, () -> {
            ClearService service = new ClearService(null);
            service.clear();
        });
    }
}