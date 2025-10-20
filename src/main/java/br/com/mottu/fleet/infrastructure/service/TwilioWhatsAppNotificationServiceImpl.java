package br.com.mottu.fleet.infrastructure.service;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.TokenAcesso;
import br.com.mottu.fleet.domain.repository.TokenAcessoRepository;
import br.com.mottu.fleet.domain.service.NotificationService;
import br.com.mottu.fleet.domain.service.TokenUpdateService;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementação do NotificationService que utiliza a API do Twilio para enviar mensagens via WhatsApp.
 * Esta é a implementação primária (@Primary), sendo a primeira tentativa de notificação.
 * Responsável por:
 * 1. Formatar a mensagem e o número de telefone.
 * 2. Enviar a mensagem pela API do Twilio.
 * 3. Rastrear o ID da mensagem (MessageSID) no TokenAcesso correspondente
 * para permitir o monitoramento de status de entrega (via listener).
 */
@Service
@Primary
public class TwilioWhatsAppNotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppNotificationServiceImpl.class);
    
    private final String twilioWhatsappNumber;
    private final TokenAcessoRepository tokenAcessoRepository;
    private final TokenUpdateService tokenUpdateService;

    public TwilioWhatsAppNotificationServiceImpl(@Value("${twilio.whatsapp-number}") String twilioWhatsappNumber,
                                                 TokenAcessoRepository tokenAcessoRepository,
                                                 TokenUpdateService tokenUpdateService) { // ADICIONE NO CONSTRUTOR
        this.twilioWhatsappNumber = twilioWhatsappNumber;
        this.tokenAcessoRepository = tokenAcessoRepository;
        this.tokenUpdateService = tokenUpdateService; // INICIALIZE
    }


    /**
     * Envia uma mensagem de WhatsApp para um funcionário contendo seu Magic Link.
     * Este método também extrai o token da URL para encontrar e atualizar
     * o registro TokenAcesso com o MessageSID retornado pelo Twilio.
     *
     * @param funcionario O objeto do funcionário que receberá a mensagem.
     * @param magicLinkUrl A URL completa do Magic Link a ser enviada.
     */
    @Override
    public void enviarMagicLink(Funcionario funcionario, String magicLinkUrl) {
        try {
            // 1. Encontra o TokenAcesso no banco usando o UUID da URL
            String tokenUuid = extrairTokenDaUrl(magicLinkUrl);
            TokenAcesso tokenAcesso = tokenAcessoRepository.findByToken(tokenUuid)
                    .orElseThrow(() -> new IllegalStateException("TokenAcesso não encontrado para a URL do Magic Link: " + magicLinkUrl));

            // 2. Formata a mensagem
            String numeroDestino = formatarParaE164(funcionario.getTelefone());
            String corpoMensagem = String.format(
                "Olá %s, bem-vindo ao F.L.E.E.T.! Para seu primeiro acesso, use o link a seguir. Ele é válido por 24 horas: %s",
                funcionario.getNome().split(" ")[0],
                magicLinkUrl
            );

            PhoneNumber to = new PhoneNumber("whatsapp:" + numeroDestino);
            PhoneNumber from = new PhoneNumber(this.twilioWhatsappNumber);

            // 3. Envia a mensagem via API Twilio
            Message message = Message.creator(to, from, corpoMensagem).create();   
            
            // 4. Captura e salva o SID da mensagem no token
            String messageSid = message.getSid();
            tokenUpdateService.atualizarMessageSid(tokenAcesso.getId(), messageSid);
            
            log.info("Magic Link enviado via WhatsApp para {}. MessageSID: {}", funcionario.getNome(), messageSid);

        } catch (Exception e) {
            log.error("Falha ao enviar WhatsApp para o funcionário ID {}: {}", funcionario.getId(), e.getMessage());
        }
    }


    /**
     * Método auxiliar para extrair o UUID do token do final da URL do Magic Link.
     */
    private String extrairTokenDaUrl(String magicLinkUrl) {
        return magicLinkUrl.substring(magicLinkUrl.lastIndexOf("=") + 1);
    }   


    /**
     * Método auxiliar para garantir que o número de telefone esteja no formato E.164 (+55119...).
     */
    private String formatarParaE164(String telefone) {
        String apenasNumeros = telefone.replaceAll("[^0-9]", "");
        
        if (apenasNumeros.startsWith("55")) {
            return "+" + apenasNumeros;
        }

        return "+55" + apenasNumeros;
    }
}