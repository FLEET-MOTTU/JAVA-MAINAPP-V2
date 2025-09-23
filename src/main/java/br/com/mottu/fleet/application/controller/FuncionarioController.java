package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.FuncionarioCreateRequest;
import br.com.mottu.fleet.application.dto.FuncionarioResponse;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.service.FuncionarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/api/funcionarios")
@Tag(name = "Funcionários", description = "Endpoints para gerenciamento de funcionários")
@SecurityRequirement(name = "bearerAuth")
public class FuncionarioController {

    @Autowired
    private FuncionarioService funcionarioService;

    @PostMapping
    @PreAuthorize("hasRole('PATEO_ADMIN')")
    @Operation(summary = "Cadastra um novo funcionário e gera seu Magic Link")
    public ResponseEntity<FuncionarioResponse> criarFuncionario(
            @Valid @RequestBody FuncionarioCreateRequest request,
            @AuthenticationPrincipal UsuarioAdmin adminLogado) {

        FuncionarioService.FuncionarioCriado resultado = funcionarioService.criar(request, adminLogado);

        FuncionarioResponse response = new FuncionarioResponse();
        response.setId(resultado.funcionario().getId());
        response.setNome(resultado.funcionario().getNome());
        response.setTelefone(resultado.funcionario().getTelefone());
        response.setMagicLinkUrl(resultado.magicLink());
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(resultado.funcionario().getId()).toUri();

        return ResponseEntity.created(location).body(response);
    }
}
