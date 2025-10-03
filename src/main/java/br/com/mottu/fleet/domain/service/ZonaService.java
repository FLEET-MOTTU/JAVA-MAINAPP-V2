package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.api.ZonaRequest;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.entity.Zona;

import java.util.UUID;

public interface ZonaService {
    Zona criar(ZonaRequest request, UUID pateoId, UsuarioAdmin adminLogado);
    Zona atualizar(UUID pateoId, UUID zonaId, ZonaRequest request, UsuarioAdmin adminLogado);
    void deletar(UUID pateoId, UUID zonaId, UsuarioAdmin adminLogado);
}