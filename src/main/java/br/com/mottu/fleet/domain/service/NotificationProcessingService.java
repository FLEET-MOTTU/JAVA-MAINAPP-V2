package br.com.mottu.fleet.domain.service;

import java.util.UUID;

public interface NotificationProcessingService {
    void processarStatusDaMensagem(String messageSid, String messageStatus);
}