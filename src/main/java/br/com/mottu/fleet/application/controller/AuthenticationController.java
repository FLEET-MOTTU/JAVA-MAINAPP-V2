package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.api.AuthCodeRequest;
import br.com.mottu.fleet.application.dto.api.AuthenticationRequest;
import br.com.mottu.fleet.application.dto.api.AuthenticationResponse;
import br.com.mottu.fleet.application.dto.api.TokenResponse;
import br.com.mottu.fleet.application.dto.api.RefreshTokenRequest;
import br.com.mottu.fleet.config.JwtService;
import br.com.mottu.fleet.domain.service.MagicLinkService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de autenticação da API
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints para autenticação de usuários da API")
public class AuthenticationController {

        private final AuthenticationManager authenticationManager;
        private final JwtService jwtService;
        private final MagicLinkService magicLinkService;

        public AuthenticationController(AuthenticationManager authenticationManager,
                                        JwtService jwtService,
                                        MagicLinkService magicLinkService) {
                this.authenticationManager = authenticationManager;
                this.jwtService = jwtService;
                this.magicLinkService = magicLinkService;
        }
        

        @Operation(summary = "Autentica um usuário e retorna um token JWT")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Autenticação bem-sucedida", content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
        })
        @PostMapping("/login")
        public ResponseEntity<AuthenticationResponse> login(@RequestBody @Valid AuthenticationRequest request) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.email(), request.senha()));

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String token = jwtService.generateToken(userDetails);

                return ResponseEntity.ok(new AuthenticationResponse(token));
        }


        @Operation(summary = "Troca um código de autorização de uso único por um par de tokens (Access e Refresh)")
        @ApiResponses(value = @ApiResponse(responseCode = "200", description = "Troca bem-sucedida"))
        @PostMapping("/exchange-token")
        public ResponseEntity<TokenResponse> exchangeToken(@RequestBody AuthCodeRequest request) {
                TokenResponse tokens = magicLinkService.trocarAuthCodePorTokens(request.code());
                return ResponseEntity.ok(tokens);
        }


        @PostMapping("/refresh-token")
        @Operation(summary = "Renova um Access Token expirado usando um Refresh Token válido")
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Tokens renovados com sucesso"),
                @ApiResponse(responseCode = "400", description = "Refresh Token inválido ou expirado")
        })
        public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
                TokenResponse novosTokens = magicLinkService.renovarTokens(request.refreshToken());
                return ResponseEntity.ok(novosTokens);
        }
}