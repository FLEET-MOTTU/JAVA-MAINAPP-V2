package br.com.mottu.fleet.application.dto.web;

import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.Zona;

import java.util.List;

/**
 * ViewModel que agrupa todas as informações necessárias para a renderização
 * da página de "Detalhes do Pátio" no painel do Super Admin.
 */
public record PateoViewModel(
    Pateo pateo,
    List<FuncionarioViewModel> funcionariosComLink,
    List<Zona> zonas
) {}