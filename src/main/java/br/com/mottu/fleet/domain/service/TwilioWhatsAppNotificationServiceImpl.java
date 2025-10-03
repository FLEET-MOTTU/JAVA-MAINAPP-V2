package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.entity.Funcionario;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementação do NotificationService que utiliza a API do Twilio para enviar mensagens via WhatsApp.
 * Esta classe é responsável por formatar os números, construir a mensagem e se comunicar com o serviço externo.
 */
@Service
public class TwilioWhatsAppNotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppNotificationServiceImpl.class);
    private final String twilioWhatsappNumber;
    
    public TwilioWhatsAppNotificationServiceImpl(@Value("${twilio.whatsapp-number}") String twilioWhatsappNumber) {
        this.twilioWhatsappNumber = twilioWhatsappNumber;
    }


    /**
     * Envia uma mensagem de WhatsApp para um funcionário contendo seu Magic Link de primeiro acesso.
     *
     * @param funcionario O objeto do funcionário que receberá a mensagem. Seu telefone será usado como destino.
     * @param magicLinkUrl A URL completa do Magic Link a ser enviada.
     */
    @Override
    public void enviarMagicLinkPorWhatsapp(Funcionario funcionario, String magicLinkUrl) {
        String numeroDestino = formatarParaE14(funcionario.getTelefone());

        String corpoMensagem = String.format(
            "Olá %s, bem-vindo ao F.L.E.E.T.! Para seu primeiro acesso, use o link a seguir. Ele é válido por 24 horas: %s",
            funcionario.getNome().split(" ")[0],
            magicLinkUrl
        );

        try {
            PhoneNumber to = new PhoneNumber("whatsapp:" + numeroDestino);
            PhoneNumber from = new PhoneNumber(this.twilioWhatsappNumber);

            Message.creator(to, from, corpoMensagem).create();

            log.info("Magic Link enviado com sucesso via WhatsApp para o funcionário: {}", funcionario.getNome());

        } catch (Exception e) {
            log.error("Falha ao enviar WhatsApp para o funcionário ID {}: {}", funcionario.getId(), e.getMessage());
        }
    }
    

    /**
     * Método auxiliar para garantir que o número de telefone esteja no formato E.164 (+55119...).
     * Remove caracteres não numéricos e adiciona o código do país (+55).
     * @param telefone O número de telefone em qualquer formato.
     * @return O número de telefone formatado no padrão E.164.
     */
    private String formatarParaE14(String telefone) {
        String apenasNumeros = telefone.replaceAll("[^0-9]", "");
        
        if (apenasNumeros.startsWith("55")) {
            return "+" + apenasNumeros;
        }
        return "+55" + apenasNumeros;
    }
}