package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.OnboardingRequest;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.enums.Status;
import br.com.mottu.fleet.domain.repository.PateoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class PateoServiceImpl implements PateoService {

    private final PateoRepository pateoRepository;

    @Autowired
    public PateoServiceImpl(PateoRepository pateoRepository) {
        this.pateoRepository = pateoRepository;
    }

    @Override
    public List<Pateo> listarTodosAtivos() {
        return pateoRepository.findAllByStatus(Status.ATIVO);
    }

    @Override
    @Transactional
    public Pateo criarPateo(OnboardingRequest request, UsuarioAdmin adminResponsavel) {
        Pateo novoPateo = new Pateo();
        novoPateo.setNome(request.getNomePateo());
        novoPateo.setGerenciadoPor(adminResponsavel);
        novoPateo.setStatus(Status.ATIVO);

        return pateoRepository.save(novoPateo);
    }
        
}