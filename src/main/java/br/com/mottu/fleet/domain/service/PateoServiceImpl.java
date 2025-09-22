package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.repository.PateoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
class PateoServiceImpl implements PateoService {

    @Autowired
    private PateoRepository pateoRepository;

    @Override
    public List<Pateo> listarTodosAtivos() {
        return pateoRepository.findAllByStatus("ATIVO");
    }
}