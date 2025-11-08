package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.api.ZonaRequest;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.exception.BusinessException;
import br.com.mottu.fleet.domain.repository.PateoRepository;
import br.com.mottu.fleet.domain.repository.ZonaRepository;
import br.com.mottu.fleet.infrastructure.publisher.InterServiceEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;


/**
 * Testes unitários para regras de negócio da `ZonaServiceImpl`.
 */
class ZonaServiceUnitTest {

    private ZonaRepository zonaRepository;
    private PateoRepository pateoRepository;
    private InterServiceEventPublisher eventPublisher;
    private ZonaServiceImpl zonaService;

    @BeforeEach
    void setup() {
        zonaRepository = Mockito.mock(ZonaRepository.class);
        pateoRepository = Mockito.mock(PateoRepository.class);
        eventPublisher = Mockito.mock(InterServiceEventPublisher.class);

        zonaService = new ZonaServiceImpl(zonaRepository, pateoRepository, eventPublisher);
    }

    @Test
    @DisplayName("criar: WKT inválido deve lançar BusinessException")
    void criar_comWKTInvalido_deveLancarBusinessException() {
        UUID pateoId = UUID.randomUUID();
        UsuarioAdmin admin = new UsuarioAdmin();
        admin.setId(UUID.randomUUID());

        Pateo pateo = new Pateo();
        pateo.setId(pateoId);
        pateo.setGerenciadoPor(admin);

        when(pateoRepository.findById(pateoId)).thenReturn(Optional.of(pateo));

        ZonaRequest request = new ZonaRequest("Minha Zona", "INVALID_WKT_TEXT");

        assertThatThrownBy(() -> zonaService.criar(request, pateoId, admin))
                .isInstanceOf(BusinessException.class);
    }
}
