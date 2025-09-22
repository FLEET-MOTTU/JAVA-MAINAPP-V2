package br.com.mottu.fleet.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pateo")
public class Pateo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "planta_baixa_url")
    private String plantaBaixaUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerenciado_por_id", nullable = false)
    private UsuarioAdmin gerenciadoPor;

    @Column(nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getPlantaBaixaUrl() { return plantaBaixaUrl; }
    public UsuarioAdmin getGerenciadoPor() { return gerenciadoPor; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(UUID id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setPlantaBaixaUrl(String plantaBaixaUrl) { this.plantaBaixaUrl = plantaBaixaUrl; }
    public void setGerenciadoPor(UsuarioAdmin gerenciadoPor) { this.gerenciadoPor = gerenciadoPor; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setStatus(String status) { this.status = status; }
    
}