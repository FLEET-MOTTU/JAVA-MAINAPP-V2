package br.com.mottu.fleet.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Polygon;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "zona")
public class Zona {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pateo_id", nullable = false)
    private Pateo pateo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id", nullable = false)
    private UsuarioAdmin criadoPor;

    @Column(columnDefinition = "GEOMETRY NOT NULL SRID 0")
    private Polygon coordenadas;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // --- Getters e Setters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Pateo getPateo() {
        return pateo;
    }

    public void setPateo(Pateo pateo) {
        this.pateo = pateo;
    }

    public UsuarioAdmin getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(UsuarioAdmin criadoPor) {
        this.criadoPor = criadoPor;
    }

    public Polygon getCoordenadas() {
        return coordenadas;
    }

    public void setCoordenadas(Polygon coordenadas) {
        this.coordenadas = coordenadas;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}