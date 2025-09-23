package br.com.mottu.fleet.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class AuthenticationResponse {

    @Schema(description = "Token JWT gerado para a sessão do usuário")
    private String token;

    public AuthenticationResponse() {
    }

    public AuthenticationResponse(String token) {
        this.token = token;
    }

    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }
}