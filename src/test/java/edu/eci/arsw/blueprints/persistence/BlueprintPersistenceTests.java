package edu.eci.arsw.blueprints.persistence;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlueprintPersistenceTests {

    private BlueprintPersistence persistence;

    @BeforeEach
    void setUp() {
        persistence = new InMemoryBlueprintPersistence();
    }

    @Test
    void testSaveBlueprint() throws BlueprintPersistenceException, BlueprintNotFoundException {
        Blueprint bp = new Blueprint("testauthor", "testblueprint", 
            List.of(new Point(1, 1)));
        persistence.saveBlueprint(bp);

        Blueprint retrieved = persistence.getBlueprint("testauthor", "testblueprint");
        assertEquals("testauthor", retrieved.getAuthor());
        assertEquals("testblueprint", retrieved.getName());
    }

    @Test
    void testSaveDuplicateBlueprint() throws BlueprintPersistenceException {
        Blueprint bp = new Blueprint("john", "house", List.of(new Point(0, 0)));
        assertThrows(BlueprintPersistenceException.class, () -> {
            persistence.saveBlueprint(bp);
        });
    }

    @Test
    void testGetBlueprintNotFound() {
        assertThrows(BlueprintNotFoundException.class, () -> {
            persistence.getBlueprint("nonexistent", "blueprint");
        });
    }

    @Test
    void testGetBlueprintsByAuthor() throws BlueprintNotFoundException {
        Set<Blueprint> blueprints = persistence.getBlueprintsByAuthor("john");
        assertNotNull(blueprints);
        assertEquals(2, blueprints.size());
    }

    @Test
    void testGetBlueprintsByAuthorNotFound() {
        assertThrows(BlueprintNotFoundException.class, () -> {
            persistence.getBlueprintsByAuthor("nonexistent");
        });
    }

    @Test
    void testGetAllBlueprints() {
        Set<Blueprint> blueprints = persistence.getAllBlueprints();
        assertNotNull(blueprints);
        assertEquals(3, blueprints.size());
    }

    @Test
    void testAddPoint() throws BlueprintNotFoundException {
        persistence.addPoint("john", "house", 50, 50);
        Blueprint bp = persistence.getBlueprint("john", "house");
        assertEquals(5, bp.getPoints().size());
    }

    @Test
    void testAddPointNotFound() {
        assertThrows(BlueprintNotFoundException.class, () -> {
            persistence.addPoint("nonexistent", "blueprint", 1, 1);
        });
    }
}
