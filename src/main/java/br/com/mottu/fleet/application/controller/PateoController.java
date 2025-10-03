package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.api.PateoDetailResponse;
import br.com.mottu.fleet.application.dto.api.ZonaResponse;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.service.PateoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.locationtech.jts.io.WKTWriter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/pateos")
@Tag(name = "Pátios", description = "Endpoints para visualização e gerenciamento de pátios")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PATEO_ADMIN')")
public class PateoController {

    private final PateoService pateoService;
    private final WKTWriter wktWriter = new WKTWriter();

    public PateoController(PateoService pateoService) {
        this.pateoService = pateoService;
    }

    @GetMapping("/{pateoId}")
    @Operation(summary = "Busca os detalhes completos de um pátio, incluindo suas zonas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dados do pátio retornados com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado se o pátio não pertencer ao admin logado"),
            @ApiResponse(responseCode = "404", description = "Pátio não encontrado")
    })
    public ResponseEntity<PateoDetailResponse> buscarDetalhes(
            @PathVariable UUID pateoId,
            @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        Pateo pateo = pateoService.buscarDetalhesDoPateo(pateoId, adminLogado);

        List<ZonaResponse> zonasResponse = pateo.getZonas().stream()
                .map(zona -> new ZonaResponse(
                        zona.getId(),
                        zona.getNome(),
                        wktWriter.write(zona.getCoordenadas())
                )).toList();

        PateoDetailResponse response = new PateoDetailResponse(
                pateo.getId(),
                pateo.getNome(),
                pateo.getPlantaBaixaUrl(),
                pateo.getPlantaLargura(),
                pateo.getPlantaAltura(),
                zonasResponse
        );

        return ResponseEntity.ok(response);
    }
}