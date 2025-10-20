package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.ErrorResponse;
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

import org.springframework.http.MediaType;
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
 * Controller REST que agrupa todos os endpoints públicos de autenticação da API.
 * Responsável pelo login de Admins de Pátio e pelo fluxo de troca de tokens dos Funcionários.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints para autenticação de usuários da API (Admins e Funcionários)")
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
        

    /**
     * Endpoint para autenticação de Administradores de Pátio via e-mail e senha.
     *
     * @param request DTO contendo e-mail and senha.
     * @return Um ResponseEntity com um token JWT de uso único (AuthenticationResponse)
     * que deve ser trocado por um token de acesso final.
     */
    @Operation(summary = "Autentica um Admin de Pátio e retorna um token JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticação bem-sucedida",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthenticationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos (ex: email mal formatado)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas (usuário não existe ou senha incorreta)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody @Valid AuthenticationRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthenticationResponse(token));
    }


    /**
     * Endpoint para o app do funcionário trocar o AuthCode (recebido via deep link)
     * por um par de tokens (Access Token e Refresh Token).
     *
     * @param request DTO contendo o AuthCode de uso único.
     * @return Um ResponseEntity com o par de tokens (TokenResponse).
     */
    @Operation(summary = "Troca um código de autorização (AuthCode) por um par de tokens (Access e Refresh)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Troca bem-sucedida",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Código de autorização inválido, expirado ou já utilizado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/exchange-token")
    public ResponseEntity<TokenResponse> exchangeToken(@Valid @RequestBody AuthCodeRequest request) {
        TokenResponse tokens = magicLinkService.trocarAuthCodePorTokens(request.code());
        return ResponseEntity.ok(tokens);
    }


    /**
     * Endpoint para o app do funcionário renovar um Access Token expirado
     * usando um Refresh Token válido.
     *
     * @param request DTO contendo o Refresh Token.
     * @return Um ResponseEntity com um novo par de tokens (TokenResponse).
     */
    @PostMapping("/refresh-token")
    @Operation(summary = "Renova um Access Token expirado usando um Refresh Token válido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tokens renovados com sucesso",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Refresh Token inválido ou expirado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse novosTokens = magicLinkService.renovarTokens(request.refreshToken());
        return ResponseEntity.ok(novosTokens);
    }
}