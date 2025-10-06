package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.api.FuncionarioCreateRequest;
import br.com.mottu.fleet.application.dto.api.FuncionarioResponse;
import br.com.mottu.fleet.application.dto.api.FuncionarioUpdateRequest;
import br.com.mottu.fleet.application.dto.api.MagicLinkResponse;
import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.enums.Cargo;
import br.com.mottu.fleet.domain.enums.Status;
import br.com.mottu.fleet.domain.service.FuncionarioService;
import br.com.mottu.fleet.domain.service.MagicLinkService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.io.IOException;

@RestController
@RequestMapping("/api/funcionarios")
@Tag(name = "Funcionários", description = "Endpoints para gerenciamento de funcionários por Administradores de Pátio")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PATEO_ADMIN')")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;
    private final MagicLinkService magicLinkService;
    private final Validator validator;
    private final ObjectMapper objectMapper;

    public FuncionarioController(FuncionarioService funcionarioService,
                                 MagicLinkService magicLinkService,
                                 Validator validator,
                                 ObjectMapper objectMapper) {
        this.funcionarioService = funcionarioService;
        this.magicLinkService = magicLinkService;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cadastra um novo funcionário com foto opcional")
    public ResponseEntity<FuncionarioResponse> criarFuncionario(
            @Valid @RequestPart("dados") @Schema(implementation = FuncionarioCreateRequest.class) String dadosJson,
            @RequestPart(value = "foto", required = false) MultipartFile foto,
            @AuthenticationPrincipal UsuarioAdmin adminLogado) throws IOException {

        FuncionarioCreateRequest request = objectMapper.readValue(dadosJson, FuncionarioCreateRequest.class);

        Set<ConstraintViolation<FuncionarioCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        Funcionario funcionarioCriado = funcionarioService.criar(request, foto, adminLogado);
        FuncionarioResponse response = toFuncionarioResponse(funcionarioCriado);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(funcionarioCriado.getId()).toUri();

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
                .map(this::toFuncionarioResponse)
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
        FuncionarioResponse response = toFuncionarioResponse(funcionarioAtualizado);
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


    @PostMapping("/{id}/reativar")
    @Operation(summary = "Reativa um funcionário que foi desativado (soft delete)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Funcionário reativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Funcionário não encontrado"),
            @ApiResponse(responseCode = "403", description = "Funcionário não pertence ao pátio do admin")
    })
    public ResponseEntity<Void> reativarFuncionario(
            @PathVariable UUID id,
            @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        funcionarioService.reativar(id, adminLogado);
        return ResponseEntity.noContent().build();
    }


    /**
     * Endpoint para fazer a substituição da foto de um funcionário.
     * @param id O UUID do funcionário.
     * @param foto O arquivo da imagem enviado como multipart/form-data.
     * @param adminLogado O admin autenticado.
     * @return Os dados atualizados do funcionário, incluindo a nova URL da foto.
     */
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Faz o upload ou atualiza a foto de um funcionário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Foto atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Funcionário não encontrado")
    })
    public ResponseEntity<FuncionarioResponse> uploadFoto(
            @PathVariable UUID id,
            @RequestPart("foto") MultipartFile foto,
            @AuthenticationPrincipal UsuarioAdmin adminLogado) throws IOException {

        // Reutilizamos o mesmo método de serviço que criamos antes!
        Funcionario funcionarioAtualizado = funcionarioService.atualizarFoto(id, foto, adminLogado);
        return ResponseEntity.ok(toFuncionarioResponse(funcionarioAtualizado));
    }

    
    /**
     * Método auxiliar pra converter a entidade Funcionario em uma DTO de resposta.
     */
    private FuncionarioResponse toFuncionarioResponse(Funcionario funcionario) {
        return new FuncionarioResponse(
                funcionario.getId(),
                funcionario.getNome(),
                funcionario.getTelefone(),
                funcionario.getEmail(),
                funcionario.getFotoUrl()
        );
    }
}