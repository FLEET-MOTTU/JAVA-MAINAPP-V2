package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.ErrorResponse;
import br.com.mottu.fleet.application.dto.api.ZonaRequest;
import br.com.mottu.fleet.application.dto.api.ZonaResponse;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.entity.Zona;
import br.com.mottu.fleet.domain.service.ZonaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.locationtech.jts.io.WKTWriter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;


/**
 * Controller REST para o gerenciamento de Zonas dentro de um Pátio específico.
 * Todas as operações são sub-recursos de um Pátio e exigem autenticação
 * de um Administrador de Pátio.
 */
@RestController
@RequestMapping("/api/pateos/{pateoId}/zonas")
@Tag(name = "Zonas", description = "Endpoints para gerenciamento de Zonas de um Pátio")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PATEO_ADMIN')")
public class ZonaController {

    private final ZonaService zonaService;
    private final WKTWriter wktWriter = new WKTWriter();

    public ZonaController(ZonaService zonaService) {
        this.zonaService = zonaService;
    }


    /**
     * Cria uma nova zona em um pátio específico.
     * A camada de serviço valida se o admin logado é o dono do pátio.
     *
     * @param pateoId O ID do pátio onde a zona será criada.
     * @param request DTO com o nome e as coordenadas WKT da nova zona.
     * @param adminLogado O usuário admin autenticado.
     * @return ResponseEntity 201 Created com a localização e os dados da nova zona.
     */
    @PostMapping
    @Operation(summary = "Cria uma nova zona em um pátio específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Zona criada com sucesso",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ZonaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou WKT malformado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado (pátio não pertence ao admin)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ZonaResponse> criarZona(
            @PathVariable UUID pateoId,
            @Valid @RequestBody ZonaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        Zona novaZona = zonaService.criar(request, pateoId, adminLogado);
        ZonaResponse response = toZonaResponse(novaZona);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(novaZona.getId()).toUri();

        return ResponseEntity.created(location).body(response);
    }


    /**
     * Atualiza uma zona existente.
     * A camada de serviço realiza uma dupla validação de segurança.
     *
     * @param pateoId O ID do pátio.
     * @param zonaId O ID da zona a ser atualizada.
     * @param request DTO com os novos dados.
     * @param adminLogado O usuário admin autenticado.
     * @return ResponseEntity 200 OK com os dados atualizados da zona.
     */
    @PutMapping("/{zonaId}")
    @Operation(summary = "Atualiza uma zona existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Zona atualizada com sucesso",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ZonaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou conflito (zona não pertence ao pátio)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao pátio",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Zona não encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ZonaResponse> atualizarZona(
            @PathVariable UUID pateoId,
            @PathVariable UUID zonaId,
            @Valid @RequestBody ZonaRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        Zona zonaAtualizada = zonaService.atualizar(pateoId, zonaId, request, adminLogado);
        ZonaResponse response = toZonaResponse(zonaAtualizada);
        return ResponseEntity.ok(response);
    }


    /**
     * Deleta uma zona existente.
     *
     * @param pateoId O ID do pátio.
     * @param zonaId O ID da zona a ser deletada.
     * @param adminLogado O usuário admin autenticado.
     * @return ResponseEntity 204 No Content.
     */
    @DeleteMapping("/{zonaId}")
    @Operation(summary = "Deleta uma zona existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Zona deletada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Conflito (zona não pertence ao pátio)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao pátio",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Zona não encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deletarZona(
            @PathVariable UUID pateoId,
            @PathVariable UUID zonaId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        zonaService.deletar(pateoId, zonaId, adminLogado);
        return ResponseEntity.noContent().build();
    }
    
    
    /**
     * Método auxiliar privado para converter a entidade Zona em uma DTO de resposta.
     * Centraliza a lógica de mapeamento e a conversão da geometria para WKT.
     * @param zona A entidade Zona vinda do serviço.
     * @return A DTO de resposta da API.
     */
    private ZonaResponse toZonaResponse(Zona zona) {
        return new ZonaResponse(
                zona.getId(),
                zona.getNome(),
                wktWriter.write(zona.getCoordenadas())
        );
    }
}