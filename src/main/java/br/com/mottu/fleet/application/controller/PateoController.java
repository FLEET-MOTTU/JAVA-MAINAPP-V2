package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.ErrorResponse;
import br.com.mottu.fleet.application.dto.api.PateoDetailResponse;
import br.com.mottu.fleet.application.dto.api.ZonaResponse;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.service.PateoService;
import br.com.mottu.fleet.domain.service.StorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.locationtech.jts.io.WKTWriter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;


/**
 * Controller REST para o gerenciamento de Pátios pelos Admins de Pátio.
 * Fornece os dados necessários para o app mobile renderizar o mapa do pátio e suas zonas.
 */
@RestController
@RequestMapping("/api/pateos")
@Tag(name = "Pátios", description = "Endpoints para visualização e gerenciamento de pátios")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PATEO_ADMIN')")
public class PateoController {

    private final PateoService pateoService;
    private final StorageService storageService;

    // WKTWriter pra converter objeto JTS Polygon em String
    private final WKTWriter wktWriter = new WKTWriter();

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    public PateoController(PateoService pateoService, StorageService storageService) {
        this.pateoService = pateoService;
        this.storageService = storageService;
    }

    
    /**
     * Busca os detalhes completos de um pátio, incluindo sua planta e todas as zonas cadastradas.
     * A segurança é garantida pela camada de serviço, que valida se o admin logado
     * é o gerente do pátio solicitado.
     * @param pateoId O UUID do pátio a ser buscado.
     * @param adminLogado O usuário admin autenticado, injetado pelo Spring Security.
     * @return Um ResponseEntity 200 OK com o PateoDetailResponse.
     * @throws br.com.mottu.fleet.domain.exception.ResourceNotFoundException Se o pátio não for encontrado.
     * @throws SecurityException Se o pátio não pertencer ao admin logado.
     */
    @GetMapping("/{pateoId}")
    @Operation(summary = "Busca os detalhes completos de um pátio, incluindo suas zonas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dados do pátio retornados com sucesso",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PateoDetailResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acesso negado (pátio não pertence ao admin logado)",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Pátio não encontrado",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PateoDetailResponse> buscarDetalhes(
            @PathVariable UUID pateoId,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        // 1. Delega a busca e a validação de segurança para o serviço
        Pateo pateo = pateoService.buscarDetalhesDoPateo(pateoId, adminLogado);

        // 2. Transforma as entidades Zona em DTOs ZonaResponse
        List<ZonaResponse> zonasResponse = pateo.getZonas().stream()
                .map(zona -> new ZonaResponse(
                        zona.getId(),
                        zona.getNome(),
                        wktWriter.write(zona.getCoordenadas())
                )).toList();

        // 3. LÓGICA DE PERFIL (PROD vs DEV) PARA A URL DA PLANTA
        String urlPlantaAcessivel = pateo.getPlantaBaixaUrl(); // Pega a URL base do banco
        
        if (!"dev".equals(activeProfile) && urlPlantaAcessivel != null && !urlPlantaAcessivel.isBlank()) {            
            String blobName = urlPlantaAcessivel.substring(urlPlantaAcessivel.lastIndexOf("/") + 1);            
            urlPlantaAcessivel = storageService.gerarUrlAcessoTemporario("plantas", blobName);
        }

        // 4. Monta a DTO de resposta final
        PateoDetailResponse response = new PateoDetailResponse(
                pateo.getId(),
                pateo.getNome(),
                urlPlantaAcessivel,
                pateo.getPlantaLargura(),
                pateo.getPlantaAltura(),
                zonasResponse
        );

        return ResponseEntity.ok(response);
    }
}