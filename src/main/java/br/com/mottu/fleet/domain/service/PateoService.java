package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Pateo;
import java.util.List;

public interface PateoService {
    List<Pateo> listarTodosAtivos();
}