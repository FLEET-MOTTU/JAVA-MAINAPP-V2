package br.com.mottu.fleet.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;


/**
 * Entidade que representa o Token de Acesso de uso único (Magic Link).
 * Este é o "convite" inicial enviado ao funcionário. Ele é validado e
 * queimado (marcado como 'usado') na primeira etapa do login.
 */
@Entity
@Table(name = "token_acesso")
public class TokenAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private boolean usado;

    @Column(name = "twilio_message_sid", unique = true)
    private String twilioMessageSid;

    @Column(name = "usado_em")
    private Instant usadoEm;
    
    public TokenAcesso() {}
    

    public UUID getId() { return id; }
    public String getToken() { return token; }
    public Funcionario getFuncionario() { return funcionario; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getExpiraEm() { return expiraEm; }
    public boolean isUsado() { return usado; }
    public String getTwilioMessageSid() { return twilioMessageSid; }
    public Instant getUsadoEm() { return usadoEm; }

    public void setId(UUID id) { this.id = id; }
    public void setToken(String token) { this.token = token; }
    public void setFuncionario(Funcionario funcionario) { this.funcionario = funcionario; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    public void setExpiraEm(Instant expiraEm) { this.expiraEm = expiraEm; }
    public void setUsado(boolean usado) { this.usado = usado; }
    public void setTwilioMessageSid(String twilioMessageSid) { this.twilioMessageSid = twilioMessageSid; }
    public void setUsadoEm(Instant usadoEm) { this.usadoEm = usadoEm; }

}