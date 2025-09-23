package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;

public interface MagicLinkService {
    String gerarLink(Funcionario funcionario);
    String validarTokenEGerarJwt(String valorToken);
}