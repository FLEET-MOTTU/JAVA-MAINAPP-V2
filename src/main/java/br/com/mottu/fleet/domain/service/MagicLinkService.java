package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.api.TokenResponse;
import br.com.mottu.fleet.domain.entity.AuthCode;
import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;

import java.util.UUID;

public interface MagicLinkService {
    String gerarLink(Funcionario funcionario);
    AuthCode validarMagicLinkEGerarAuthCode(String valorToken);
    String regenerarLink(UUID funcionarioId, UsuarioAdmin adminLogado);
    String gerarLink(UUID funcionarioId);
    TokenResponse trocarAuthCodePorTokens(String authCode);
    TokenResponse renovarTokens(String refreshToken);  
}