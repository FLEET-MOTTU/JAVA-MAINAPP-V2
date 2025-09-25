package br.com.mottu.fleet.application.dto;

import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.Zona;
import java.util.List;

public record PateoViewModel(
    Pateo pateo,
    List<FuncionarioViewModel> funcionariosComLink,
    List<Zona> zonas
) {}