package edu.eci.arsw.blueprints.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "points")
public class PointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int x;

    @Column(nullable = false)
    private int y;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_id", nullable = false)
    private BlueprintEntity blueprint;

    protected PointEntity() {}

    public PointEntity(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX()                          { return x; }
    public int getY()                          { return y; }
    public BlueprintEntity getBlueprint()      { return blueprint; }
    public void setBlueprint(BlueprintEntity b){ this.blueprint = b; }
}