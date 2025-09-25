package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.FuncionarioCreateRequest;
import br.com.mottu.fleet.application.dto.FuncionarioResponse;
import br.com.mottu.fleet.application.dto.FuncionarioUpdateRequest;
import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.enums.Cargo;
import br.com.mottu.fleet.domain.enums.Status;
import br.com.mottu.fleet.domain.service.FuncionarioService;
import br.com.mottu.fleet.application.dto.MagicLinkResponse;
import br.com.mottu.fleet.domain.service.MagicLinkService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/funcionarios")
@Tag(name = "Funcionários", description = "Endpoints para gerenciamento de funcionários por Administradores de Pátio")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PATEO_ADMIN')")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;
    private final MagicLinkService magicLinkService;

    public FuncionarioController(FuncionarioService funcionarioService, MagicLinkService magicLinkService) {
        this.funcionarioService = funcionarioService;
        this.magicLinkService = magicLinkService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo funcionário e gera seu Magic Link")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Funcionário criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos")
    })
    public ResponseEntity<FuncionarioResponse> criarFuncionario(
            @Valid @RequestBody FuncionarioCreateRequest request,
            @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        FuncionarioService.FuncionarioCriado resultado = funcionarioService.criar(request, adminLogado);

        FuncionarioResponse response = toFuncionarioResponse(resultado.funcionario(), resultado.magicLink());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(resultado.funcionario().getId()).toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Lista funcionários de um pátio com filtros opcionais (padrão: apenas ATIVOS)")
    public ResponseEntity<List<FuncionarioResponse>> listarFuncionarios(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Cargo cargo,
            @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        List<Funcionario> funcionarios = funcionarioService.listarPorAdminEfiltros(adminLogado, status, cargo);
        List<FuncionarioResponse> response = funcionarios.stream()
                .map(f -> toFuncionarioResponse(f, null))
                .toList();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de um funcionário")
    public ResponseEntity<FuncionarioResponse> atualizarFuncionario(
            @PathVariable UUID id,
            @Valid @RequestBody FuncionarioUpdateRequest request,
            @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        Funcionario funcionarioAtualizado = funcionarioService.atualizar(id, request, adminLogado);
        FuncionarioResponse response = toFuncionarioResponse(funcionarioAtualizado, null);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativa (soft delete) um funcionário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Funcionário desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Funcionário não encontrado")
    })
    public ResponseEntity<Void> desativarFuncionario(
            @PathVariable UUID id,
            @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        funcionarioService.desativar(id, adminLogado);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/regenerar-link")
    @Operation(summary = "Gera um novo Magic Link para um funcionário existente")
    public ResponseEntity<MagicLinkResponse> gerarNovoMagicLink(
        @PathVariable UUID id,
        @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        String novoLink = magicLinkService.regenerarLink(id, adminLogado);
        return ResponseEntity.ok(new MagicLinkResponse(novoLink));
    }

    /**
     * Método auxiliar pra converter a entidade Funcionario em uma DTO de resposta.
     */
    private FuncionarioResponse toFuncionarioResponse(Funcionario funcionario, String magicLinkUrl) {
        return new FuncionarioResponse(
                funcionario.getId(),
                funcionario.getNome(),
                funcionario.getTelefone(),
                magicLinkUrl
        );
    }
}