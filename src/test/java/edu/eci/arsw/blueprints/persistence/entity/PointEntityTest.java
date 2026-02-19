package edu.eci.arsw.blueprints.persistence.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PointEntityTest {

    @Test
    void testConstructorAndGetters() {
        PointEntity p = new PointEntity(10, 20);
        assertEquals(10, p.getX());
        assertEquals(20, p.getY());
    }

    @Test
    void testSetBlueprint() {
        PointEntity p = new PointEntity(3, 7);
        BlueprintEntity bp = new BlueprintEntity("jane", "garden");
        p.setBlueprint(bp);

        assertSame(bp, p.getBlueprint());
    }

    @Test
    void testGetBlueprintNullByDefault() {
        PointEntity p = new PointEntity(0, 0);
        assertNull(p.getBlueprint());
    }

    @Test
    void testSetBlueprintOverwrite() {
        PointEntity p = new PointEntity(1, 1);
        BlueprintEntity bp1 = new BlueprintEntity("a", "b");
        BlueprintEntity bp2 = new BlueprintEntity("c", "d");

        p.setBlueprint(bp1);
        p.setBlueprint(bp2);

        assertSame(bp2, p.getBlueprint());
    }
}