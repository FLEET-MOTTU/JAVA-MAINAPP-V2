package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.api.FuncionarioCreateRequest;
import br.com.mottu.fleet.application.dto.api.FuncionarioUpdateRequest;
import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.enums.Cargo;
import br.com.mottu.fleet.domain.enums.Status;
import br.com.mottu.fleet.domain.exception.BusinessException;
import br.com.mottu.fleet.domain.exception.ResourceNotFoundException;
import br.com.mottu.fleet.domain.repository.FuncionarioRepository;
import br.com.mottu.fleet.domain.repository.PateoRepository;
import br.com.mottu.fleet.domain.repository.specification.FuncionarioSpecification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FuncionarioServiceImpl implements FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final PateoRepository pateoRepository;
    private final MagicLinkService magicLinkService;

    @Autowired
    public FuncionarioServiceImpl(FuncionarioRepository funcionarioRepository, PateoRepository pateoRepository, MagicLinkService magicLinkService) {
        this.funcionarioRepository = funcionarioRepository;
        this.pateoRepository = pateoRepository;
        this.magicLinkService = magicLinkService;
    }

    @Override
    @Transactional
    public FuncionarioCriado criar(FuncionarioCreateRequest request, UsuarioAdmin adminLogado) {
        Pateo pateo = getPateoDoAdmin(adminLogado);

        Funcionario novoFuncionario = new Funcionario();
        novoFuncionario.setNome(request.getNome());
        novoFuncionario.setTelefone(request.getTelefone());
        novoFuncionario.setPateo(pateo);
        novoFuncionario.setCargo(parseCargo(request.getCargo()));
        novoFuncionario.setStatus(Status.ATIVO);
        novoFuncionario.setCodigo("FUNC-" + request.getTelefone());

        Funcionario funcionarioSalvo = funcionarioRepository.save(novoFuncionario);
        String link = magicLinkService.gerarLink(funcionarioSalvo);

        return new FuncionarioCriado(funcionarioSalvo, link);
    }

    @Override
    public List<Funcionario> listarPorAdminEfiltros(UsuarioAdmin adminLogado, Status status, Cargo cargo) {
        Pateo pateo = getPateoDoAdmin(adminLogado);
        Status statusFiltrar = (status == null) ? Status.ATIVO : status;
        Specification<Funcionario> spec = FuncionarioSpecification.comFiltros(pateo.getId(), statusFiltrar, cargo);
        return funcionarioRepository.findAll(spec);
    }

    @Override
    @Transactional
    public Funcionario atualizar(UUID id, FuncionarioUpdateRequest request, UsuarioAdmin adminLogado) {
        Pateo pateoDoAdmin = getPateoDoAdmin(adminLogado);
        Funcionario funcionario = findFuncionarioByIdAndCheckPateo(id, pateoDoAdmin.getId());

        if (funcionario.getStatus() == Status.REMOVIDO) {
            throw new BusinessException("Não é possível alterar um funcionário que já foi removido.");
        }

        funcionario.setNome(request.getNome());
        funcionario.setTelefone(request.getTelefone());
        funcionario.setCargo(parseCargo(request.getCargo()));
        funcionario.setStatus(parseStatusUpdate(request.getStatus()));

        return funcionarioRepository.save(funcionario);
    }

    @Override
    @Transactional
    public void desativar(UUID id, UsuarioAdmin adminLogado) {
        Pateo pateoDoAdmin = getPateoDoAdmin(adminLogado);
        Funcionario funcionario = findFuncionarioByIdAndCheckPateo(id, pateoDoAdmin.getId());

        funcionario.setStatus(Status.REMOVIDO);
        funcionarioRepository.save(funcionario);
    }

    
    // Métodos aux.

    private Pateo getPateoDoAdmin(UsuarioAdmin adminLogado) {
        return pateoRepository.findAllByGerenciadoPorId(adminLogado.getId())
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException("Admin não está associado a nenhum pátio."));
    }

    private Funcionario findFuncionarioByIdAndCheckPateo(UUID funcionarioId, UUID pateoId) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário com ID " + funcionarioId + " não encontrado."));

        if (!funcionario.getPateo().getId().equals(pateoId)) {
            throw new SecurityException("Acesso negado: este funcionário não pertence ao seu pátio.");
        }
        return funcionario;
    }

    private Cargo parseCargo(String cargoStr) {
        try {
            return Cargo.valueOf(cargoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Cargo inválido: " + cargoStr);
        }
    }

    private Status parseStatusUpdate(String statusStr) {
        try {
            Status novoStatus = Status.valueOf(statusStr.toUpperCase());
            if (novoStatus == Status.REMOVIDO) {
                throw new BusinessException("Para remover um funcionário, utilize o endpoint DELETE.");
            }
            return novoStatus;
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Status inválido: " + statusStr);
        }
    }
    
}