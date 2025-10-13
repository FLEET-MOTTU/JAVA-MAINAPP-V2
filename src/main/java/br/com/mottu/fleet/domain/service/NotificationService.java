package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;

public interface NotificationService {
    void enviarMagicLink(Funcionario funcionario, String magicLinkUrl);
}