package edu.eci.arsw.blueprints.persistence.impl;

import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistence;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.persistence.entity.BlueprintEntity;
import edu.eci.arsw.blueprints.persistence.entity.PointEntity;
import edu.eci.arsw.blueprints.persistence.jpa.BlueprintJpaRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Primary                         // ← overrides InMemoryBlueprintPersistence
@Profile("postgres")             // ← only active with the "postgres" Spring profile
public class PostgresBlueprintPersistence implements BlueprintPersistence {

    private final BlueprintJpaRepository repo;

    public PostgresBlueprintPersistence(BlueprintJpaRepository repo) {
        this.repo = repo;
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private Blueprint toDomain(BlueprintEntity e) {
        List<Point> pts = e.getPoints().stream()
                .map(p -> new Point(p.getX(), p.getY()))
                .collect(Collectors.toList());
        return new Blueprint(e.getAuthor(), e.getName(), pts);
    }

    private BlueprintEntity toEntity(Blueprint bp) {
        BlueprintEntity e = new BlueprintEntity(bp.getAuthor(), bp.getName());
        bp.getPoints().forEach(p -> e.addPoint(new PointEntity(p.x(), p.y())));
        return e;
    }

    // ── Interface methods ────────────────────────────────────────────────────

    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        if (repo.findByAuthorAndName(bp.getAuthor(), bp.getName()).isPresent()) {
            throw new BlueprintPersistenceException(
                "Blueprint already exists: %s/%s".formatted(bp.getAuthor(), bp.getName()));
        }
        repo.save(toEntity(bp));
    }

    @Override
    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        return repo.findByAuthorAndName(author, name)
                .map(this::toDomain)
                .orElseThrow(() -> new BlueprintNotFoundException(
                    "Blueprint not found: %s/%s".formatted(author, name)));
    }

    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {
        List<BlueprintEntity> entities = repo.findByAuthor(author);
        if (entities.isEmpty())
            throw new BlueprintNotFoundException("No blueprints for author: " + author);
        return entities.stream().map(this::toDomain).collect(Collectors.toSet());
    }

    @Override
    public Set<Blueprint> getAllBlueprints() {
        return repo.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toSet());
    }

    @Override
    public void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException {
        BlueprintEntity e = repo.findByAuthorAndName(author, name)
                .orElseThrow(() -> new BlueprintNotFoundException(
                    "Blueprint not found: %s/%s".formatted(author, name)));
        e.addPoint(new PointEntity(x, y));
        repo.save(e);
    }
}