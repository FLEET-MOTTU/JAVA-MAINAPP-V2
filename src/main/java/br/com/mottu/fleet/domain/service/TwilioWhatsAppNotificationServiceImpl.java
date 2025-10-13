package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.TokenAcesso;
import br.com.mottu.fleet.domain.repository.TokenAcessoRepository;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Implementação do NotificationService que utiliza a API do Twilio para enviar mensagens via WhatsApp.
 * Esta classe é responsável por formatar os números, construir a mensagem, se comunicar com o serviço externo
 * e rastrear o ID da mensagem (MessageSID) para consultas de status.
 */
@Service
@Primary
public class TwilioWhatsAppNotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppNotificationServiceImpl.class);
    private final String twilioWhatsappNumber;
    private final TokenAcessoRepository tokenAcessoRepository;

    public TwilioWhatsAppNotificationServiceImpl(@Value("${twilio.whatsapp-number}") String twilioWhatsappNumber,
                                                TokenAcessoRepository tokenAcessoRepository) {
        this.twilioWhatsappNumber = twilioWhatsappNumber;
        this.tokenAcessoRepository = tokenAcessoRepository;
    }


    /**
     * Envia uma mensagem de WhatsApp para um funcionário contendo seu Magic Link de primeiro acesso.
     *
     * @param funcionario O objeto do funcionário que receberá a mensagem. Seu telefone será usado como destino.
     * @param magicLinkUrl A URL completa do Magic Link a ser enviada.
     */
    @Override
    public void enviarMagicLink(Funcionario funcionario, String magicLinkUrl) {
        try {
            // Extrair o token UUID da URL para encontrar o registro no banco
            String tokenUuid = extrairTokenDaUrl(magicLinkUrl);
            TokenAcesso tokenAcesso = tokenAcessoRepository.findByToken(tokenUuid)
                    .orElseThrow(() -> new IllegalStateException("TokenAcesso não encontrado para a URL do Magic Link: " + magicLinkUrl));

            // Formatar e preparar a mensagem
            String numeroDestino = formatarParaE164(funcionario.getTelefone());
            String corpoMensagem = String.format(
                "Olá %s, bem-vindo ao F.L.E.E.T.! Para seu primeiro acesso, use o link a seguir. Ele é válido por 24 horas: %s",
                funcionario.getNome().split(" ")[0],
                magicLinkUrl
            );

            PhoneNumber to = new PhoneNumber("whatsapp:" + numeroDestino);
            PhoneNumber from = new PhoneNumber(this.twilioWhatsappNumber);

            // Enviar a mensagem e capturar o resultado
            Message message = Message.creator(to, from, corpoMensagem).create();
            
            // Salvar o MessageSID no registro do TokenAcesso
            String messageSid = message.getSid();
            tokenAcesso.setTwilioMessageSid(messageSid);
            tokenAcessoRepository.save(tokenAcesso);

            log.info("Magic Link enviado via WhatsApp para {}. MessageSID: {}", funcionario.getNome(), messageSid);

        } catch (Exception e) {
            log.error("Falha ao enviar WhatsApp para o funcionário ID {}: {}", funcionario.getId(), e.getMessage());
        }
    }


    /**
     * Método auxiliar para extrair o UUID do token do final da URL do Magic Link.
     * @param magicLinkUrl para validação da integridade do token
     * @return uuid extraído do corpo do token
     */
    private String extrairTokenDaUrl(String magicLinkUrl) {
        return magicLinkUrl.substring(magicLinkUrl.lastIndexOf("=") + 1);
    }    


    /**
     * Método auxiliar para garantir que o número de telefone esteja no formato E.164 (+55119...).
     * Remove caracteres não numéricos e adiciona o código do país (+55).
     * @param telefone O número de telefone em qualquer formato.
     * @return O número de telefone formatado no padrão E.164.
     */
    private String formatarParaE164(String telefone) {
        String apenasNumeros = telefone.replaceAll("[^0-9]", "");
        
        if (apenasNumeros.startsWith("55")) {
            return "+" + apenasNumeros;
        }
        return "+55" + apenasNumeros;
    }
}