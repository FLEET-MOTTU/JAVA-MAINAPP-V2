package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.repository.TokenAcessoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


/**
 * Serviço de domínio com uma responsabilidade única: atualizar um TokenAcesso
 * em uma thread async e permitir persistencia de dados entre threads antes que a thread
 * principal finalize a transação com a camada de repository.
 */
@Service
public class TokenUpdateServiceImpl implements TokenUpdateService {

    private final TokenAcessoRepository tokenAcessoRepository;

    public TokenUpdateServiceImpl(TokenAcessoRepository tokenAcessoRepository) {
        this.tokenAcessoRepository = tokenAcessoRepository;
    }


    /**
     * Atualiza o campo twilioMessageSid de um TokenAcesso existente.
     * Força o Spring a criar uma transação nova e dedicada para esta operação.
     * @param tokenId O ID do TokenAcesso a ser atualizado.
     * @param messageSid O MessageSID retornado pelo Twilio a ser salvo.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void atualizarMessageSid(UUID tokenId, String messageSid) {
        tokenAcessoRepository.findById(tokenId).ifPresent(token -> {
            token.setTwilioMessageSid(messageSid);
            tokenAcessoRepository.save(token);
        });
    }
}