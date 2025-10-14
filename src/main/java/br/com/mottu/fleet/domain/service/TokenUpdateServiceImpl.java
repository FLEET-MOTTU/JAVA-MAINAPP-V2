package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.domain.repository.TokenAcessoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TokenUpdateServiceImpl implements TokenUpdateService {

    private final TokenAcessoRepository tokenAcessoRepository;

    public TokenUpdateServiceImpl(TokenAcessoRepository tokenAcessoRepository) {
        this.tokenAcessoRepository = tokenAcessoRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void atualizarMessageSid(UUID tokenId, String messageSid) {
        // Esta anotação GARANTE que este método SEMPRE rodará em uma
        // transação nova e independente, que será commitada ao final.
        tokenAcessoRepository.findById(tokenId).ifPresent(token -> {
            token.setTwilioMessageSid(messageSid);
            tokenAcessoRepository.save(token);
        });
    }
}