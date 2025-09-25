package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.PasswordChangeRequest;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.service.UsuarioAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@Tag(name = "Conta do Usuário", description = "Endpoints para o usuário logado gerenciar a própria conta")
@SecurityRequirement(name = "bearerAuth")
public class UserAccountController {

    private final UsuarioAdminService usuarioAdminService;

    public UserAccountController(UsuarioAdminService usuarioAdminService) {
        this.usuarioAdminService = usuarioAdminService;
    }

    @PutMapping("/change-password")
    @PreAuthorize("hasRole('PATEO_ADMIN')")
    @Operation(summary = "Altera a senha do administrador de pátio logado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou senha atual incorreta"),
            @ApiResponse(responseCode = "401", description = "Falha na autenticação (token inválido ou expirado)"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não tem a permissão 'PATEO_ADMIN')")
    })
    public ResponseEntity<Void> alterarSenha(
            @AuthenticationPrincipal UsuarioAdmin adminLogado,
            @Valid @RequestBody PasswordChangeRequest request) {

        usuarioAdminService.alterarSenha(adminLogado, request);
        return ResponseEntity.noContent().build();
    }
}