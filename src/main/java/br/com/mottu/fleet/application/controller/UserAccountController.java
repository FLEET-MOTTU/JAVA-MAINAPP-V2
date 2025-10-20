package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.ErrorResponse;
import br.com.mottu.fleet.application.dto.api.PasswordChangeRequest;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.service.UsuarioAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Controller REST que gerencia a conta do usuário (Admin de Pátio)
 * atualmente autenticado. Permite que o usuário altere seus próprios dados, como a senha.
 */
@RestController
@RequestMapping("/api/account")
@Tag(name = "Conta do Usuário", description = "Endpoints para o usuário logado gerenciar a própria conta")
@SecurityRequirement(name = "bearerAuth")
public class UserAccountController {

    private final UsuarioAdminService usuarioAdminService;

    public UserAccountController(UsuarioAdminService usuarioAdminService) {
        this.usuarioAdminService = usuarioAdminService;
    }


    /**
     * Altera a senha do administrador de pátio que está logado.
     * @param adminLogado O principal do usuário autenticado (injetado pelo Spring Security).
     * @param request DTO contendo a senha atual e a nova senha.
     * @return ResponseEntity 204 No Content em caso de sucesso.
     * @throws br.com.mottu.fleet.domain.exception.BusinessException Se a senha atual estiver incorreta.
     */
    @PutMapping("/change-password")
    @PreAuthorize("hasRole('PATEO_ADMIN')")
    @Operation(summary = "Altera a senha do administrador de pátio logado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (ex: nova senha curta) ou senha atual incorreta",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Falha na autenticação (token inválido ou expirado)"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (usuário não tem a permissão 'PATEO_ADMIN')")
    })
    public ResponseEntity<Void> alterarSenha(
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAdmin adminLogado,
            @Valid @RequestBody PasswordChangeRequest request) {

        usuarioAdminService.alterarSenha(adminLogado, request);
        return ResponseEntity.noContent().build();
    }
}