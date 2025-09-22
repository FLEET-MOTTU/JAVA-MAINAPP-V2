package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.repository.UsuarioAdminRepository;
import br.com.mottu.fleet.domain.repository.PateoRepository;
import br.com.mottu.fleet.application.dto.UsuarioAdminUpdateRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
class UsuarioAdminServiceImpl implements UsuarioAdminService {

    @Autowired
    private UsuarioAdminRepository usuarioAdminRepository;

    @Autowired
    private PateoRepository pateoRepository;

    @Override
    public List<UsuarioAdmin> listarAdminsDePateo() {
        return usuarioAdminRepository.findAllByRoleAndStatus("PATEO_ADMIN", "ATIVO");
    }

    @Override
    @Transactional
    public void desativarPorId(UUID id) {
        // Busca o admin ou lança uma exceção se não encontrar
        UsuarioAdmin admin = usuarioAdminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Desativa o usuário
        admin.setStatus("INATIVO");
        usuarioAdminRepository.save(admin);

        // Busca e desativa todos os pátios gerenciados por ele
        List<Pateo> pateos = pateoRepository.findAllByGerenciadoPorId(id);
        pateos.forEach(pateo -> pateo.setStatus("INATIVO"));
        pateoRepository.saveAll(pateos);
    }

    @Override
    public Optional<UsuarioAdmin> buscarPorId(UUID id) {
        return usuarioAdminRepository.findById(id);
    }

    @Override
    @Transactional
    public void atualizar(UsuarioAdminUpdateRequest request) {
        // Busca o admin existente no banco ou lança exceção
        UsuarioAdmin adminExistente = usuarioAdminRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado para atualização"));

        // Atualiza apenas os campos permitidos
        adminExistente.setNome(request.getNome());

        // Salva as alterações
        usuarioAdminRepository.save(adminExistente);
    }    
}