package edu.eci.arsw.blueprints.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlueprintModelTests {

    @Test
    void testBlueprintCreation() {
        Point p1 = new Point(0, 0);
        Point p2 = new Point(10, 10);
        Blueprint bp = new Blueprint("john", "house", List.of(p1, p2));

        assertEquals("john", bp.getAuthor());
        assertEquals("house", bp.getName());
        assertEquals(2, bp.getPoints().size());
    }

    @Test
    void testBlueprintAddPoint() {
        Blueprint bp = new Blueprint("john", "house", List.of(new Point(0, 0)));
        bp.addPoint(new Point(5, 5));

        assertEquals(2, bp.getPoints().size());
    }

    @Test
    void testBlueprintPointsUnmodifiable() {
        Blueprint bp = new Blueprint("john", "house", List.of(new Point(0, 0)));
        assertThrows(UnsupportedOperationException.class, () -> {
            bp.getPoints().add(new Point(1, 1));
        });
    }

    @Test
    void testBlueprintEquality() {
        Blueprint bp1 = new Blueprint("john", "house", List.of(new Point(0, 0)));
        Blueprint bp2 = new Blueprint("john", "house", List.of(new Point(5, 5)));
        Blueprint bp3 = new Blueprint("john", "garage", List.of(new Point(0, 0)));

        // Equality based on author and name only
        assertEquals(bp1, bp2);
        assertNotEquals(bp1, bp3);
    }

    @Test
    void testBlueprintHashCode() {
        Blueprint bp1 = new Blueprint("john", "house", List.of(new Point(0, 0)));
        Blueprint bp2 = new Blueprint("john", "house", List.of(new Point(5, 5)));

        assertEquals(bp1.hashCode(), bp2.hashCode());
    }

    @Test
    void testPointRecord() {
        Point p = new Point(10, 20);

        assertEquals(10, p.x());
        assertEquals(20, p.y());
    }

    @Test
    void testBlueprintEmptyPoints() {
        Blueprint bp = new Blueprint("alice", "empty", List.of());

        assertEquals(0, bp.getPoints().size());
        assertNotNull(bp.getPoints());
    }
}
