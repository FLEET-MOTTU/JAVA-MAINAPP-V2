package br.com.mottu.fleet.domain.service;

import java.util.UUID;

public interface TokenUpdateService {
    void atualizarMessageSid(UUID tokenId, String messageSid);
}