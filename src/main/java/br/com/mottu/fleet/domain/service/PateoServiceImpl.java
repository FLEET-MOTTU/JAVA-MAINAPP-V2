package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.OnboardingRequest;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.enums.Status;
import br.com.mottu.fleet.domain.exception.BusinessException;
import br.com.mottu.fleet.domain.exception.ResourceNotFoundException;
import br.com.mottu.fleet.domain.repository.PateoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PateoServiceImpl implements PateoService {

    private final PateoRepository pateoRepository;

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

    /**
     * Busca os detalhes de um pátio, incluindo suas zonas, e valida se o admin logado
     * tem permissão de acesso.
     * REGRA DE NEGÓCIO: Um admin só pode acessar detalhes do pátio que ele gerencia.
     */
    @Override
    public Pateo buscarDetalhesDoPateo(UUID pateoId, UsuarioAdmin adminLogado) {
        // Valida se o pátio pertence ao admin que está fazendo a requisição
        Pateo pateoDoAdmin = getPateoDoAdmin(adminLogado);
        if (!pateoDoAdmin.getId().equals(pateoId)) {
            throw new SecurityException("Acesso negado: este pátio não pertence a você.");
        }

        // Usa a query otimizada para buscar o pátio e suas zonas em uma única consulta
        return pateoRepository.findPateoWithZonasById(pateoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pátio com ID " + pateoId + " não encontrado."));
    }

    /**
     * Método para buscar o pátio associado a um admin.
     * Centraliza a regra de negócio de que um admin deve ter um pátio.
     */
    private Pateo getPateoDoAdmin(UsuarioAdmin adminLogado) {
        return pateoRepository.findAllByGerenciadoPorId(adminLogado.getId())
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException("Admin não está associado a nenhum pátio."));
    }
}