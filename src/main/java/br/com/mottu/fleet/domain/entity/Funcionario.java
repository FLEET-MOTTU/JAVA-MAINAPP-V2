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


/**
 * Entidade que representa um Funcionário (Operador, Admin, etc.).
 * Esta entidade tem uma responsabilidade dupla:
 * 1. Mapear os dados do funcionário na tabela 'funcionario'.
 * 2. Implementar a interface 'UserDetails' do Spring Security, permitindo que
 * o funcionário seja tratado como um principal autenticável no sistema
 * (para a validação do JWT).
 */
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

    @Column(nullable = false, unique = true)
    private String email;

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


    /**
     * Define as Roles do funcionário para o Spring Security.
     * A autoridade é derivada diretamente do enum Cargo.
     * @return Uma coleção de autoridades (ex: "ROLE_OPERACIONAL").
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.cargo.name()));
    }


    /**
     * Override no Bean do Spring pra retornar uma senha nula.
     * Como o funcionário loga via Magic Link, ele não tem senha.
     * @return null.
     */
    @Override
    public String getPassword() {
        return null;
    }


    /**
     * Define o "username" do funcionário para o Spring Security.
     * Usamos o telefone como identificador principal no JWT.
     * @return O número de telefone do funcionário.
     */
    @Override
    public String getUsername() {
        return this.telefone;
    }


    /**
     * Verifica se a conta do usuário está habilitada.
     * @return true se o status for 'ATIVO', false caso contrário.
     */
    @Override
    public boolean isEnabled() {
        return Status.ATIVO.equals(this.status);
    }

    // Métodos padrão do UserDetails, não são relevantes para o fluxo
    // mas são incializados para cumprir o contrato da interface
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    
    public UUID getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
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
    public void setEmail(String email) { this.email = email; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }
    public void setStatus(Status status) { this.status = status; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public void setUltimoLogin(Instant ultimoLogin) { this.ultimoLogin = ultimoLogin; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setPateo(Pateo pateo) { this.pateo = pateo; }

}