package edu.eci.arsw.blueprints.services;

import edu.eci.arsw.blueprints.filters.IdentityFilter;
import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.persistence.InMemoryBlueprintPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlueprintServiceTests {

    private BlueprintsServices services;
    private InMemoryBlueprintPersistence persistence;
    private IdentityFilter filter;

    @BeforeEach
    void setUp() {
        persistence = new InMemoryBlueprintPersistence();
        filter = new IdentityFilter();
        services = new BlueprintsServices(persistence, filter);
    }

    @Test
    void testGetAllBlueprints() {
        Set<Blueprint> blueprints = services.getAllBlueprints();
        assertNotNull(blueprints);
        assertEquals(3, blueprints.size()); 
    }

    @Test
    void testGetBlueprintsByAuthor() throws BlueprintNotFoundException {
        Set<Blueprint> blueprints = services.getBlueprintsByAuthor("john");
        assertNotNull(blueprints);
        assertEquals(2, blueprints.size());
        assertTrue(blueprints.stream().allMatch(bp -> bp.getAuthor().equals("john")));
    }

    @Test
    void testGetBlueprintsByAuthorNotFound() {
        assertThrows(BlueprintNotFoundException.class, () -> {
            services.getBlueprintsByAuthor("nonexistent");
        });
    }

    @Test
    void testGetBlueprint() throws BlueprintNotFoundException {
        Blueprint bp = services.getBlueprint("john", "house");
        assertNotNull(bp);
        assertEquals("john", bp.getAuthor());
        assertEquals("house", bp.getName());
        assertEquals(4, bp.getPoints().size());
    }

    @Test
    void testGetBlueprintNotFound() {
        assertThrows(BlueprintNotFoundException.class, () -> {
            services.getBlueprint("nonexistent", "blueprint");
        });
    }

    @Test
    void testAddNewBlueprint() throws BlueprintPersistenceException, BlueprintNotFoundException {
        Blueprint newBp = new Blueprint("alice", "painting", 
            List.of(new Point(1, 1), new Point(2, 2)));
        services.addNewBlueprint(newBp);
        
        Blueprint retrieved = services.getBlueprint("alice", "painting");
        assertEquals("alice", retrieved.getAuthor());
        assertEquals("painting", retrieved.getName());
    }

    @Test
    void testAddNewBlueprintDuplicate() throws BlueprintPersistenceException {
        Blueprint newBp = new Blueprint("john", "house", List.of(new Point(5, 5)));
        assertThrows(BlueprintPersistenceException.class, () -> {
            services.addNewBlueprint(newBp);
        });
    }

    @Test
    void testAddPoint() throws BlueprintNotFoundException {
        services.addPoint("john", "house", 99, 99);
        Blueprint bp = services.getBlueprint("john", "house");
        assertEquals(5, bp.getPoints().size()); 
        Point lastPoint = bp.getPoints().get(4);
        assertEquals(99, lastPoint.x());
        assertEquals(99, lastPoint.y());
    }

    @Test
    void testAddPointBlueprintNotFound() {
        assertThrows(BlueprintNotFoundException.class, () -> {
            services.addPoint("nonexistent", "blueprint", 1, 1);
        });
    }
}
