package edu.eci.arsw.blueprints.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlueprintEntityTest {

    @Test
    void testConstructorAndGetters() {
        BlueprintEntity e = new BlueprintEntity("john", "house");
        assertEquals("john", e.getAuthor());
        assertEquals("house", e.getName());
        assertNotNull(e.getPoints());
        assertTrue(e.getPoints().isEmpty());
    }

    @Test
    void testAddPoint() {
        BlueprintEntity e = new BlueprintEntity("john", "house");
        PointEntity p = new PointEntity(5, 10);
        e.addPoint(p);

        assertEquals(1, e.getPoints().size());
        assertSame(e, p.getBlueprint()); // setBlueprint was called
    }

    @Test
    void testAddMultiplePoints() {
        BlueprintEntity e = new BlueprintEntity("alice", "plan");
        e.addPoint(new PointEntity(1, 2));
        e.addPoint(new PointEntity(3, 4));
        e.addPoint(new PointEntity(5, 6));

        assertEquals(3, e.getPoints().size());
    }

    @Test
    void testGetIdIsNullBeforePersistence() {
        // ID is only set by JPA on save; null before that
        BlueprintEntity e = new BlueprintEntity("bob", "sketch");
        assertNull(e.getId());
    }
}