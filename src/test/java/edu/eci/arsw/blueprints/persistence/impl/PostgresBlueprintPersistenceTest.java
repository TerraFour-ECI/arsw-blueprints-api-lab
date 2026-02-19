package edu.eci.arsw.blueprints.persistence.impl;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.persistence.entity.BlueprintEntity;
import edu.eci.arsw.blueprints.persistence.jpa.BlueprintJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostgresBlueprintPersistenceTest {

    @Mock
    private BlueprintJpaRepository repo;

    private PostgresBlueprintPersistence persistence;

    // Helper: build a BlueprintEntity with points for mocking
    private BlueprintEntity entityWith(String author, String name, List<int[]> pts) {
        BlueprintEntity e = new BlueprintEntity(author, name);
        pts.forEach(c -> e.addPoint(new edu.eci.arsw.blueprints.persistence.entity.PointEntity(c[0], c[1])));
        return e;
    }

    @BeforeEach
    void setUp() {
        persistence = new PostgresBlueprintPersistence(repo);
    }

    // ── saveBlueprint ────────────────────────────────────────────────────────

    @Test
    void testSaveBlueprintSuccess() throws BlueprintPersistenceException {
        when(repo.findByAuthorAndName("john", "house")).thenReturn(Optional.empty());
        Blueprint bp = new Blueprint("john", "house", List.of(new Point(1, 2)));

        persistence.saveBlueprint(bp);

        verify(repo, times(1)).save(any(BlueprintEntity.class));
    }

    @Test
    void testSaveBlueprintDuplicateThrows() {
        BlueprintEntity existing = entityWith("john", "house", List.of());
        when(repo.findByAuthorAndName("john", "house")).thenReturn(Optional.of(existing));

        Blueprint bp = new Blueprint("john", "house", List.of());
        assertThrows(BlueprintPersistenceException.class, () -> persistence.saveBlueprint(bp));
        verify(repo, never()).save(any());
    }

    // ── getBlueprint ─────────────────────────────────────────────────────────

    @Test
    void testGetBlueprintSuccess() throws BlueprintNotFoundException {
        BlueprintEntity e = entityWith("john", "house", List.of(new int[]{0, 0}, new int[]{10, 10}));
        when(repo.findByAuthorAndName("john", "house")).thenReturn(Optional.of(e));

        Blueprint bp = persistence.getBlueprint("john", "house");

        assertEquals("john", bp.getAuthor());
        assertEquals("house", bp.getName());
        assertEquals(2, bp.getPoints().size());
    }

    @Test
    void testGetBlueprintNotFoundThrows() {
        when(repo.findByAuthorAndName("x", "y")).thenReturn(Optional.empty());
        assertThrows(BlueprintNotFoundException.class, () -> persistence.getBlueprint("x", "y"));
    }

    // ── getBlueprintsByAuthor ─────────────────────────────────────────────────

    @Test
    void testGetBlueprintsByAuthorSuccess() throws BlueprintNotFoundException {
        BlueprintEntity e1 = entityWith("john", "house", List.of());
        BlueprintEntity e2 = entityWith("john", "garage", List.of());
        when(repo.findByAuthor("john")).thenReturn(List.of(e1, e2));

        Set<Blueprint> result = persistence.getBlueprintsByAuthor("john");

        assertEquals(2, result.size());
    }

    @Test
    void testGetBlueprintsByAuthorEmptyThrows() {
        when(repo.findByAuthor("ghost")).thenReturn(List.of());
        assertThrows(BlueprintNotFoundException.class, () -> persistence.getBlueprintsByAuthor("ghost"));
    }

    // ── getAllBlueprints ──────────────────────────────────────────────────────

    @Test
    void testGetAllBlueprints() {
        BlueprintEntity e1 = entityWith("john", "house", List.of());
        BlueprintEntity e2 = entityWith("jane", "garden", List.of());
        when(repo.findAll()).thenReturn(List.of(e1, e2));

        Set<Blueprint> result = persistence.getAllBlueprints();

        assertEquals(2, result.size());
    }

    @Test
    void testGetAllBlueprintsEmpty() {
        when(repo.findAll()).thenReturn(List.of());
        Set<Blueprint> result = persistence.getAllBlueprints();
        assertTrue(result.isEmpty());
    }

    // ── addPoint ─────────────────────────────────────────────────────────────

    @Test
    void testAddPointSuccess() throws BlueprintNotFoundException {
        BlueprintEntity e = entityWith("john", "house", List.of());
        when(repo.findByAuthorAndName("john", "house")).thenReturn(Optional.of(e));

        persistence.addPoint("john", "house", 5, 10);

        assertEquals(1, e.getPoints().size());
        verify(repo, times(1)).save(e);
    }

    @Test
    void testAddPointBlueprintNotFoundThrows() {
        when(repo.findByAuthorAndName("x", "y")).thenReturn(Optional.empty());
        assertThrows(BlueprintNotFoundException.class, () -> persistence.addPoint("x", "y", 1, 1));
        verify(repo, never()).save(any());
    }

    @Test
    void testAddPointCorrectCoordinates() throws BlueprintNotFoundException {
        BlueprintEntity e = entityWith("john", "house", List.of());
        when(repo.findByAuthorAndName("john", "house")).thenReturn(Optional.of(e));

        persistence.addPoint("john", "house", 99, 77);

        assertEquals(99, e.getPoints().get(0).getX());
        assertEquals(77, e.getPoints().get(0).getY());
    }

    // ── toDomain mapping ─────────────────────────────────────────────────────

    @Test
    void testToDomainMapsPointsCorrectly() throws BlueprintNotFoundException {
        BlueprintEntity e = entityWith("jane", "garden",
                List.of(new int[]{2, 3}, new int[]{7, 8}));
        when(repo.findByAuthorAndName("jane", "garden")).thenReturn(Optional.of(e));

        Blueprint bp = persistence.getBlueprint("jane", "garden");

        assertEquals(2, bp.getPoints().size());
        assertEquals(2, bp.getPoints().get(0).x());
        assertEquals(3, bp.getPoints().get(0).y());
        assertEquals(7, bp.getPoints().get(1).x());
        assertEquals(8, bp.getPoints().get(1).y());
    }

    @Test
    void testToDomainWithNoPoints() throws BlueprintNotFoundException {
        BlueprintEntity e = entityWith("bob", "empty", List.of());
        when(repo.findByAuthorAndName("bob", "empty")).thenReturn(Optional.of(e));

        Blueprint bp = persistence.getBlueprint("bob", "empty");

        assertNotNull(bp.getPoints());
        assertTrue(bp.getPoints().isEmpty());
    }

    // ── toEntity mapping (via saveBlueprint) ─────────────────────────────────

    @Test
    void testToEntityMapsPointsOnSave() throws BlueprintPersistenceException {
        when(repo.findByAuthorAndName("alice", "loft")).thenReturn(Optional.empty());

        Blueprint bp = new Blueprint("alice", "loft",
                List.of(new Point(10, 20), new Point(30, 40)));
        persistence.saveBlueprint(bp);

        // Capture what was saved and verify point mapping
        var captor = org.mockito.ArgumentCaptor.forClass(BlueprintEntity.class);
        verify(repo).save(captor.capture());
        BlueprintEntity saved = captor.getValue();

        assertEquals("alice", saved.getAuthor());
        assertEquals("loft",  saved.getName());
        assertEquals(2, saved.getPoints().size());
        assertEquals(10, saved.getPoints().get(0).getX());
        assertEquals(20, saved.getPoints().get(0).getY());
        assertEquals(30, saved.getPoints().get(1).getX());
        assertEquals(40, saved.getPoints().get(1).getY());
    }

    @Test
    void testGetAllBlueprintsMapsCorrectly() {
        BlueprintEntity e = entityWith("john", "house", List.of(new int[]{1, 2}));
        when(repo.findAll()).thenReturn(List.of(e));

        Set<Blueprint> result = persistence.getAllBlueprints();

        assertEquals(1, result.size());
        Blueprint bp = result.iterator().next();
        assertEquals("john", bp.getAuthor());
        assertEquals(1, bp.getPoints().size());
        assertEquals(1, bp.getPoints().get(0).x());
        assertEquals(2, bp.getPoints().get(0).y());
    }
}