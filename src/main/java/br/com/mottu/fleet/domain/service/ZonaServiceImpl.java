package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.api.ZonaRequest;
import br.com.mottu.fleet.application.dto.integration.ZonaSyncPayload;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.entity.Zona;
import br.com.mottu.fleet.domain.exception.BusinessException;
import br.com.mottu.fleet.domain.exception.ResourceNotFoundException;
import br.com.mottu.fleet.domain.repository.PateoRepository;
import br.com.mottu.fleet.domain.repository.ZonaRepository;
import br.com.mottu.fleet.infrastructure.publisher.InterServiceEventPublisher;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


/**
 * Implementação do serviço de domínio que contém as regras de negócio
 * para o gerenciamento de Zonas de trabalho dentro de um Pátio.
 * Todas as operações são validadas contra o admin de pátio autenticado.
 */
@Service
public class ZonaServiceImpl implements ZonaService {

    private final ZonaRepository zonaRepository;
    private final PateoRepository pateoRepository;
    private final InterServiceEventPublisher eventPublisher;
    private final WKTReader wktReader = new WKTReader();

    public ZonaServiceImpl(ZonaRepository zonaRepository, 
                           PateoRepository pateoRepository,
                           InterServiceEventPublisher eventPublisher) {
        this.zonaRepository = zonaRepository;
        this.pateoRepository = pateoRepository;
        this.eventPublisher = eventPublisher;
    }


    /**
     * Cria uma nova Zona e a associa a um Pátio.
     * A segurança é validada pelo método auxiliar findPateoAndVerifyOwnership.
     *
     * @param request O DTO contendo o nome e as coordenadas WKT da nova zona.
     * @param pateoId O ID do pátio onde a zona será criada.
     * @param adminLogado O admin autenticado que está realizando a operação.
     * @return A entidade Zona recém-criada e salva.
     * @throws BusinessException Se as coordenadas WKT forem inválidas.
     * @throws SecurityException Se o admin não for o dono do pátio.
     */
    @Override
    @Transactional
    public Zona criar(ZonaRequest request, UUID pateoId, UsuarioAdmin adminLogado) {
        Pateo pateo = findPateoAndVerifyOwnership(pateoId, adminLogado);

        // Converte a string WKT em um objeto Polygon
        Polygon polygon = parseWKT(request.coordenadasWKT());

        Zona novaZona = new Zona();
        novaZona.setNome(request.nome());
        novaZona.setCoordenadas(polygon);
        novaZona.setPateo(pateo);
        novaZona.setCriadoPor(adminLogado);

        Zona zonaSalva = zonaRepository.save(novaZona);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ZonaSyncPayload payload = new ZonaSyncPayload(zonaSalva);
                eventPublisher.publishEvent(payload, "ZONA_CRIADA");
            }
        });

        return zonaSalva;
    }


    /**
     * Atualiza o nome e as coordenadas de uma Zona existente.
     * Realiza uma verificação de segurança dupla:
     * 1. O admin é dono do pátio?
     * 2. A zona a ser editada realmente pertence a este pátio?
     *
     * @param pateoId O ID do pátio (para verificação de propriedade).
     * @param zonaId O ID da zona a ser atualizada.
     * @param request O DTO com os novos dados.
     * @param adminLogado O admin autenticado.
     * @return A entidade Zona atualizada.
     * @throws SecurityException Se o admin não for o dono do pátio.
     * @throws ResourceNotFoundException Se a zona não for encontrada.
     * @throws BusinessException Se a zona não pertencer ao pátio ou o WKT for inválido.
     */
    @Override
    @Transactional
    public Zona atualizar(UUID pateoId, UUID zonaId, ZonaRequest request, UsuarioAdmin adminLogado) {
        findPateoAndVerifyOwnership(pateoId, adminLogado);
        
        Zona zonaExistente = zonaRepository.findById(zonaId)
            .orElseThrow(() -> new ResourceNotFoundException("Zona com ID " + zonaId + " não encontrada."));

        if (!zonaExistente.getPateo().getId().equals(pateoId)) {
            throw new BusinessException("Conflito: A zona informada não pertence ao pátio especificado.");
        }

        Polygon polygon = parseWKT(request.coordenadasWKT());
        zonaExistente.setNome(request.nome());
        zonaExistente.setCoordenadas(polygon);

        Zona zonaAtualizada = zonaRepository.save(zonaExistente);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ZonaSyncPayload payload = new ZonaSyncPayload(zonaAtualizada);
                eventPublisher.publishEvent(payload, "ZONA_ATUALIZADA");
            }
        });

        return zonaAtualizada;
    }


    /**
     * Deleta uma Zona de um Pátio.
     * Realiza a mesma verificação de segurança dupla do método 'atualizar'.
     *
     * @param pateoId O ID do pátio (para verificação de propriedade).
     * @param zonaId O ID da zona a ser deletada.
     * @param adminLogado O admin autenticado.
     * @throws SecurityException Se o admin não for o dono do pátio.
     * @throws ResourceNotFoundException Se a zona não for encontrada.
     * @throws BusinessException Se a zona não pertencer ao pátio.
     */
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

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ZonaSyncPayload payload = new ZonaSyncPayload(zonaExistente);
                eventPublisher.publishEvent(payload, "ZONA_DELETADA");
            }
        });
    }


    // Métodos Auxiliares

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
