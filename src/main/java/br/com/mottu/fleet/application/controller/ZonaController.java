package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.api.ZonaRequest;
import br.com.mottu.fleet.application.dto.api.ZonaResponse;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.entity.Zona;
import br.com.mottu.fleet.domain.service.ZonaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.locationtech.jts.io.WKTWriter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

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

    @PostMapping
    @Operation(summary = "Cria uma nova zona em um pátio específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Zona criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou WKT malformado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao pátio")
    })
    public ResponseEntity<ZonaResponse> criarZona(
            @PathVariable UUID pateoId,
            @Valid @RequestBody ZonaRequest request,
            @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        Zona novaZona = zonaService.criar(request, pateoId, adminLogado);
        ZonaResponse response = toZonaResponse(novaZona);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(novaZona.getId()).toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{zonaId}")
    @Operation(summary = "Atualiza uma zona existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Zona atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou WKT malformado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao pátio ou zona"),
            @ApiResponse(responseCode = "404", description = "Zona não encontrada")
    })
    public ResponseEntity<ZonaResponse> atualizarZona(
            @PathVariable UUID pateoId,
            @PathVariable UUID zonaId,
            @Valid @RequestBody ZonaRequest request,
            @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        Zona zonaAtualizada = zonaService.atualizar(pateoId, zonaId, request, adminLogado);
        ZonaResponse response = toZonaResponse(zonaAtualizada);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{zonaId}")
    @Operation(summary = "Deleta uma zona existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Zona deletada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao pátio ou zona"),
            @ApiResponse(responseCode = "404", description = "Zona não encontrada")
    })
    public ResponseEntity<Void> deletarZona(
            @PathVariable UUID pateoId,
            @PathVariable UUID zonaId,
            @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        zonaService.deletar(pateoId, zonaId, adminLogado);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Método privado para converter a entidade Zona em uma DTO de resposta.
     * Centraliza a lógica de mapeamento em um único lugar.
     */
    private ZonaResponse toZonaResponse(Zona zona) {
        return new ZonaResponse(
                zona.getId(),
                zona.getNome(),
                wktWriter.write(zona.getCoordenadas())
        );
    }
}