package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.ErrorResponse; // Importe
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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

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
import java.util.Set;
import java.util.UUID;
import java.io.IOException;


/**
 * Controller REST para o gerenciamento completo do ciclo de vida de Funcionários.
 * Todos os endpoints são protegidos e exigem a role 'PATEO_ADMIN'.
 * A segurança de acesso (garantindo que um admin só veja funcionários do seu pátio)
 * é delegada para a camada de serviço (FuncionarioService).
 */
@RestController
@RequestMapping("/api/funcionarios")
@Tag(name = "Funcionários", description = "Endpoints para gerenciamento de funcionários por Administradores de Pátio")
@SecurityRequirement(name = "bearerAuth") // Exige o cadeado de autenticação em todos os endpoints
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


    /**
     * Cadastra um novo funcionário. Este endpoint espera uma requisição 'multipart/form-data'
     * contendo uma parte 'dados' (JSON) e uma parte 'foto' (arquivo opcional).
     *
     * @param dadosJson Uma string JSON contendo os dados do FuncionarioCreateRequest.
     * @param foto O arquivo de imagem (MultipartFile) opcional.
     * @param adminLogado O usuário admin autenticado, injetado pelo Spring Security.
     * @return ResponseEntity 201 Created com a localização e o corpo do funcionário criado.
     * @throws IOException Se houver erro na leitura do JSON ou no upload do arquivo.
     * @throws ConstraintViolationException Se o JSON de 'dados' falhar na validação.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cadastra um novo funcionário com foto opcional")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Funcionário criado com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FuncionarioResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados de validação inválidos ou arquivo inválido",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Conflito de dados (ex: e-mail ou telefone já existem)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FuncionarioResponse> criarFuncionario(
            @Valid @RequestPart("dados") @Schema(implementation = FuncionarioCreateRequest.class) String dadosJson,
            @RequestPart(value = "foto", required = false) MultipartFile foto,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAdmin adminLogado) throws IOException {

        // 1. Converte manualmente a string JSON para a DTO
        FuncionarioCreateRequest request = objectMapper.readValue(dadosJson, FuncionarioCreateRequest.class);

        // 2. Valida a DTO
        Set<ConstraintViolation<FuncionarioCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        // 3. Delega para o serviço
        Funcionario funcionarioCriado = funcionarioService.criar(request, foto, adminLogado);
        FuncionarioResponse response = toFuncionarioResponse(funcionarioCriado);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(funcionarioCriado.getId()).toUri();

        return ResponseEntity.created(location).body(response);
    }


    /**
     * Lista os funcionários do pátio do admin logado.
     * Permite filtrar por status (ATIVO, SUSPENSO, REMOVIDO) e cargo.
     *
     * @param status Filtro opcional por status. Se não fornecido, o serviço assume 'ATIVO'.
     * @param cargo Filtro opcional por cargo.
     * @param adminLogado O usuário admin autenticado.
     * @return ResponseEntity 200 OK com a lista de funcionários.
     */
    @GetMapping
    @Operation(summary = "Lista funcionários de um pátio com filtros opcionais (padrão: apenas ATIVOS)")
    @ApiResponse(responseCode = "200", description = "Lista de funcionários recuperada com sucesso")
    public ResponseEntity<List<FuncionarioResponse>> listarFuncionarios(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Cargo cargo,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        List<Funcionario> funcionarios = funcionarioService.listarPorAdminEfiltros(adminLogado, status, cargo);
        List<FuncionarioResponse> response = funcionarios.stream()
            .map(this::toFuncionarioResponse)
            .toList();

        return ResponseEntity.ok(response);
    }


    /**
     * Atualiza os dados textuais de um funcionário (nome, e-mail, status, etc.).
     * Para atualizar a foto, use o endpoint POST /{id}/photo.
     *
     * @param id O UUID do funcionário a ser atualizado.
     * @param request DTO com os novos dados.
     * @param adminLogado O usuário admin autenticado.
     * @return ResponseEntity 200 OK com os dados atualizados do funcionário.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados (nome, email, status, etc.) de um funcionário")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Funcionário atualizado com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FuncionarioResponse.class))),
        @ApiResponse(responseCode = "400", description = "Dados de validação inválidos",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Funcionário não encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FuncionarioResponse> atualizarFuncionario(
            @PathVariable UUID id,
            @Valid @RequestBody FuncionarioUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        Funcionario funcionarioAtualizado = funcionarioService.atualizar(id, request, adminLogado);
        FuncionarioResponse response = toFuncionarioResponse(funcionarioAtualizado);
        return ResponseEntity.ok(response);
    }


    /**
     * Faz o upload ou a substituição da foto de um funcionário.
     *
     * @param id O UUID do funcionário.
     * @param foto O arquivo da imagem enviado como multipart/form-data.
     * @param adminLogado O admin autenticado.
     * @return ResponseEntity 200 OK com os dados atualizados do funcionário (incluindo a nova fotoUrl).
     */
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Faz o upload ou atualiza a foto de um funcionário")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Foto atualizada com sucesso",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FuncionarioResponse.class))),
        @ApiResponse(responseCode = "400", description = "Arquivo de foto inválido ou ausente",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Funcionário não encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FuncionarioResponse> uploadFoto(
            @PathVariable UUID id,
            @RequestPart("foto") MultipartFile foto,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAdmin adminLogado) throws IOException {

        Funcionario funcionarioAtualizado = funcionarioService.atualizarFoto(id, foto, adminLogado);
        return ResponseEntity.ok(toFuncionarioResponse(funcionarioAtualizado));
    }


    /**
     * Desativa (soft delete) um funcionário, mudando seu status para REMOVIDO.
     *
     * @param id O UUID do funcionário a ser desativado.
     * @param adminLogado O usuário admin autenticado.
     * @return ResponseEntity 204 No Content.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Desativa (soft delete) um funcionário")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Funcionário desativado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Funcionário não encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acesso negado (funcionário não pertence ao pátio)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> desativarFuncionario(
        @PathVariable UUID id,
        @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        funcionarioService.desativar(id, adminLogado);
        return ResponseEntity.noContent().build();
    }


    /**
     * Reativa um funcionário que foi desativado (status REMOVIDO -> ATIVO).
     *
     * @param id O UUID do funcionário a ser reativado.
     * @param adminLogado O usuário admin autenticado.
     * @return ResponseEntity 204 No Content.
     */
    @PostMapping("/{id}/reativar")
    @Operation(summary = "Reativa um funcionário que foi desativado (soft delete)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Funcionário reativado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Funcionário não está desativado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Funcionário não encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acesso negado (funcionário não pertence ao pátio)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> reativarFuncionario(
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        funcionarioService.reativar(id, adminLogado);
        return ResponseEntity.noContent().build();
    }


    /**
     * Gera e envia um novo Magic Link para um funcionário (ex: se o original expirou).
     *
     * @param id O UUID do funcionário.
     * @param adminLogado O usuário admin autenticado.
     * @return ResponseEntity 200 OK com a nova URL do Magic Link.
     */
    @PostMapping("/{id}/regenerar-link")
    @Operation(summary = "Gera e envia um novo Magic Link para um funcionário existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Novo link gerado e enviado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Não é possível gerar link (ex: funcionário removido)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Funcionário não encontrado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MagicLinkResponse> gerarNovoMagicLink(
        @PathVariable UUID id,
        @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        String novoLink = magicLinkService.regenerarLink(id, adminLogado);
        return ResponseEntity.ok(new MagicLinkResponse(novoLink));
    }


    /**
     * Método auxiliar privado para converter a entidade Funcionario em uma DTO de resposta.
     * @param funcionario A entidade vinda do serviço.
     * @return A DTO de resposta da API.
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