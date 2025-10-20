package br.com.mottu.fleet.domain.entity;

import br.com.mottu.fleet.domain.enums.Role;
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
 * Entidade que representa um Usuário Administrador (SUPER_ADMIN ou PATEO_ADMIN).
 * Esta entidade implementa UserDetails para se integrar ao fluxo de autenticação
 * padrão do Spring Security (login com email e senha).
 */
@Entity
@Table(name = "usuario_admin")
public class UsuarioAdmin implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UsuarioAdmin() {}


    /**
     * Define as Roles do administrador (SUPER_ADMIN ou PATEO_ADMIN).
     * @return Uma coleção de autoridades (ex: "ROLE_SUPER_ADMIN").
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }


    /**
     * Retorna a senha do usuário (hash) para o Spring Security comparar.
     * @return A senha armazenada no banco.
     */
    @Override
    public String getPassword() {
        return this.senha;
    }


    /**
     * Define o "username" do administrador. Usamos o email para login.
     * @return O email do administrador.
     */
    @Override
    public String getUsername() {
        return this.email;
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
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public Role getRole() { return role; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(UUID id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setEmail(String email) { this.email = email; }
    public void setSenha(String senha) { this.senha = senha; }
    public void setRole(Role role) { this.role = role; }
    public void setStatus(Status status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

}