package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.OnboardingRequest;

public interface OnboardingService {
    void executar(OnboardingRequest request);
}