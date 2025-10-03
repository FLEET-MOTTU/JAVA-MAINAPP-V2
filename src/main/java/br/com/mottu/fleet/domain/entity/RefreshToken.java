package br.com.mottu.fleet.domain.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public String getToken() { return token; }
    public Funcionario getFuncionario() { return funcionario; }
    public Instant getExpiraEm() { return expiraEm; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(UUID id) { this.id = id; }
    public void setToken(String token) { this.token = token; }
    public void setFuncionario(Funcionario funcionario) { this.funcionario = funcionario; }
    public void setExpiraEm(Instant expiraEm) { this.expiraEm = expiraEm; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

}