package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;

public interface NotificationService {
    void enviarMagicLinkPorWhatsapp(Funcionario funcionario, String magicLinkUrl);
}