package za.co.fnb.dcre.platform.persistence;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BaseEntityTest {

    static class TestEntity extends BaseEntity { }

    @Test
    void idAssignmentIsIdempotent() {
        TestEntity e = new TestEntity();
        e.assignIdIfMissing();
        UUID first = e.getId();
        assertNotNull(first);
        e.assignIdIfMissing();
        assertEquals(first, e.getId());
    }
}
