package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementação de "último recurso" do NotificationService.
 * Acionada quando todos os outros métodos de notificação (WhatsApp, E-mail) falham.
 * Sua responsabilidade é registrar a falha de forma persistente ou alertar administradores.
 */
@Service("lastResortNotificationService")
public class LastResortNotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LastResortNotificationServiceImpl.class);

    @Override
    public void enviarMagicLink(Funcionario funcionario, String magicLinkUrl) {
        log.error("FALHA FINAL DE NOTIFICAÇÃO! Não foi possível contatar o funcionário ID: {}", funcionario.getId());
        log.error("O Magic Link que falhou em ser entregue é: {}", magicLinkUrl);

        // TODO: Ação final.
        // Opção A: Salvar um registro em uma tabela de "AlertasAdmin".
        // Opção B: Atualizar o status do próprio funcionário para "NOTIFICACAO_PENDENTE_MANUAL".
        // Por enquanto, o log de erro já é uma ação muito valiosa.
    }
}