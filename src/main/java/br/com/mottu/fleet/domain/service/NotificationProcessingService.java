package br.com.mottu.fleet.domain.service;

public interface NotificationProcessingService {
    void processarStatusDaMensagem(String messageSid, String messageStatus);
}