package br.com.mottu.fleet.domain.entity;

import br.com.mottu.fleet.domain.enums.Status;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;


/**
 * Entidade que representa um Pátio (unidade física) da Mottu.
 * Contém os dados do pátio e seus relacionamentos com o admin que o gerencia,
 * suas zonas e seus funcionários.
 */
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

    @Column(name = "planta_largura")
    private Integer plantaLargura;

    @Column(name = "planta_altura")
    private Integer plantaAltura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gerenciado_por_id", nullable = false)
    private UsuarioAdmin gerenciadoPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;


    // Deleção em cascata: Se um Pátio for deletado, todas as Zonas
    // associadas a ele também serão deletadas.
    @OneToMany(
            mappedBy = "pateo",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<Zona> zonas = new HashSet<>();

    @OneToMany(mappedBy = "pateo", fetch = FetchType.LAZY)
    private Set<Funcionario> funcionarios = new HashSet<>();

    public Pateo() {}    

    
    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getPlantaBaixaUrl() { return plantaBaixaUrl; }
    public Integer getPlantaLargura() { return plantaLargura; }
    public Integer getPlantaAltura() { return plantaAltura; }
    public UsuarioAdmin getGerenciadoPor() { return gerenciadoPor; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Set<Zona> getZonas() { return zonas; }
    public Set<Funcionario> getFuncionarios() { return funcionarios; }

    public void setId(UUID id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setPlantaBaixaUrl(String plantaBaixaUrl) { this.plantaBaixaUrl = plantaBaixaUrl; }
    public void setPlantaLargura(Integer plantaLargura) { this.plantaLargura = plantaLargura; }
    public void setPlantaAltura(Integer plantaAltura) { this.plantaAltura = plantaAltura; }
    public void setGerenciadoPor(UsuarioAdmin gerenciadoPor) { this.gerenciadoPor = gerenciadoPor; }
    public void setStatus(Status status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setZonas(Set<Zona> zonas) { this.zonas = zonas; }
    public void setFuncionarios(Set<Funcionario> funcionarios) { this.funcionarios = funcionarios; }
    
}