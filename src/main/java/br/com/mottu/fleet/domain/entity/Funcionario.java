package br.com.mottu.fleet.domain.entity;

import br.com.mottu.fleet.domain.enums.Cargo;
import br.com.mottu.fleet.domain.enums.Status;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "funcionario")
public class Funcionario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Cargo cargo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "ultimo_login")
    private Instant ultimoLogin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pateo_id", nullable = false)
    private Pateo pateo;

    public Funcionario() {}

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.cargo.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.telefone;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return Status.ATIVO.equals(this.status);
    }

    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public Cargo getCargo() { return cargo; }
    public Status getStatus() { return status; }
    public String getFotoUrl() { return fotoUrl; }
    public Instant getUltimoLogin() { return ultimoLogin; }
    public Instant getCreatedAt() { return createdAt; }
    public Pateo getPateo() { return pateo; }

    public void setId(UUID id) { this.id = id; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setNome(String nome) { this.nome = nome; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }
    public void setStatus(Status status) { this.status = status; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public void setUltimoLogin(Instant ultimoLogin) { this.ultimoLogin = ultimoLogin; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setPateo(Pateo pateo) { this.pateo = pateo; }

}