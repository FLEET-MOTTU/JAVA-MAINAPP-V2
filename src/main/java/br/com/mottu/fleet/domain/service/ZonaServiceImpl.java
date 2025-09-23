package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.ZonaRequest;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.entity.Zona;
import br.com.mottu.fleet.domain.exception.BusinessException;
import br.com.mottu.fleet.domain.exception.ResourceNotFoundException;
import br.com.mottu.fleet.domain.repository.PateoRepository;
import br.com.mottu.fleet.domain.repository.ZonaRepository;

import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ZonaServiceImpl implements ZonaService {

    private final ZonaRepository zonaRepository;
    private final PateoRepository pateoRepository;
    private final WKTReader wktReader = new WKTReader();

    public ZonaServiceImpl(ZonaRepository zonaRepository, PateoRepository pateoRepository) {
        this.zonaRepository = zonaRepository;
        this.pateoRepository = pateoRepository;
    }

    @Override
    @Transactional
    public Zona criar(ZonaRequest request, UUID pateoId, UsuarioAdmin adminLogado) {
        Pateo pateo = findPateoAndVerifyOwnership(pateoId, adminLogado);
        Polygon polygon = parseWKT(request.coordenadasWKT());

        Zona novaZona = new Zona();
        novaZona.setNome(request.nome());
        novaZona.setCoordenadas(polygon);
        novaZona.setPateo(pateo);
        novaZona.setCriadoPor(adminLogado);

        return zonaRepository.save(novaZona);
    }

    @Override
    @Transactional
    public Zona atualizar(UUID pateoId, UUID zonaId, ZonaRequest request, UsuarioAdmin adminLogado) {
        findPateoAndVerifyOwnership(pateoId, adminLogado); // Garante que o pátio pertence ao admin
        
        Zona zonaExistente = zonaRepository.findById(zonaId)
            .orElseThrow(() -> new ResourceNotFoundException("Zona com ID " + zonaId + " não encontrada."));

        // Valida se a zona realmente pertence ao pátio informado na URL
        if (!zonaExistente.getPateo().getId().equals(pateoId)) {
            throw new BusinessException("Conflito: A zona informada não pertence ao pátio especificado.");
        }

        Polygon polygon = parseWKT(request.coordenadasWKT());
        zonaExistente.setNome(request.nome());
        zonaExistente.setCoordenadas(polygon);

        return zonaRepository.save(zonaExistente);
    }

    @Override
    @Transactional
    public void deletar(UUID pateoId, UUID zonaId, UsuarioAdmin adminLogado) {
        findPateoAndVerifyOwnership(pateoId, adminLogado);
        
        Zona zonaExistente = zonaRepository.findById(zonaId)
            .orElseThrow(() -> new ResourceNotFoundException("Zona com ID " + zonaId + " não encontrada."));
        
        if (!zonaExistente.getPateo().getId().equals(pateoId)) {
             throw new BusinessException("Conflito: A zona informada não pertence ao pátio especificado.");
        }

        zonaRepository.delete(zonaExistente);
    }

    /**
     * Método que busca um pátio e verifica se ele é gerenciado pelo admin logado.
     * Centraliza a principal regra de segurança de acesso.
     */
    private Pateo findPateoAndVerifyOwnership(UUID pateoId, UsuarioAdmin adminLogado) {
        Pateo pateo = pateoRepository.findById(pateoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pátio com ID " + pateoId + " não encontrado."));

        if (!pateo.getGerenciadoPor().getId().equals(adminLogado.getId())) {
            throw new SecurityException("Acesso negado: você não pode acessar recursos de um pátio que não gerencia.");
        }
        return pateo;
    }

    /**
     * Método para converter uma String WKT em um objeto Polygon.
     * Centraliza o tratamento de erro de parsing.
     */
    private Polygon parseWKT(String wkt) {
        try {
            return (Polygon) wktReader.read(wkt);
        } catch (ParseException e) {
            throw new BusinessException("Formato de coordenadas WKT inválido: " + e.getMessage());
        }
    }
}
