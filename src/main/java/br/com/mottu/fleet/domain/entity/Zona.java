package br.com.mottu.fleet.domain.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Polygon;

import java.time.Instant;
import java.util.UUID;


/**
 * Entidade que representa uma Zona de depósito das motos dentro de um Pátio.
 * Armazena o nome da zona e sua definição geométrica (Polígono).
 */
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

    // Define a coluna como do tipo GEOMETRY, sem projeção espacial (SRID 0)
    @Column(columnDefinition = "GEOMETRY NOT NULL SRID 0")
    private Polygon coordenadas;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Zona() {}
    

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public Pateo getPateo() { return pateo; }
    public UsuarioAdmin getCriadoPor() { return criadoPor; }
    public Polygon getCoordenadas() { return coordenadas; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(UUID id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setPateo(Pateo pateo) { this.pateo = pateo; }
    public void setCriadoPor(UsuarioAdmin criadoPor) { this.criadoPor = criadoPor; }
    public void setCoordenadas(Polygon coordenadas) { this.coordenadas = coordenadas; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
}