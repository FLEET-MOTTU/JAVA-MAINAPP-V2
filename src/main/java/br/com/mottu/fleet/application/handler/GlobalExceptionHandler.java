package br.com.mottu.fleet.application.handler;

import br.com.mottu.fleet.application.dto.ErrorResponse;
import br.com.mottu.fleet.domain.exception.BusinessException;
import br.com.mottu.fleet.domain.exception.EmailAlreadyExistsException;
import br.com.mottu.fleet.domain.exception.InvalidTokenException;
import br.com.mottu.fleet.domain.exception.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final String deepLinkBaseUrl;
    private final String deepLinkErrorPath;

    public GlobalExceptionHandler(
            @Value("${application.deeplink.base-url}") String deepLinkBaseUrl,
            @Value("${application.deeplink.login-error-path}") String deepLinkErrorPath) {
        this.deepLinkBaseUrl = deepLinkBaseUrl;
        this.deepLinkErrorPath = deepLinkErrorPath;
    }

    /**
     * Regra de Negócio: Se a criação de um admin pelo painel falhar por email duplicado,
     * o usuário deve ser redirecionado de volta ao formulário com uma mensagem de erro clara.
     * Este handler é específico para o fluxo web do Thymeleaf.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public String handleEmailAlreadyExistsForWeb(EmailAlreadyExistsException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/admin/onboarding/novo";
    }

    /**
     * Handler para exceções que podem ocorrer tanto na API quanto no fluxo de Magic Link.
     * Regra de Negócio:
     * 1. Se o erro ocorrer durante a validação do Magic Link (rota /auth/validar-token),
     * a resposta DEVE ser um redirecionamento para o deep link de erro do app mobile.
     * 2. Se o erro ocorrer em qualquer outra rota (API), a resposta DEVE ser um JSON padronizado.
     */
    @ExceptionHandler({ResourceNotFoundException.class, InvalidTokenException.class, BusinessException.class})
    public Object handleApiAndMagicLinkExceptions(RuntimeException ex, HttpServletRequest request) {
        // Verifica se a requisição veio do fluxo de validação do Magic Link
        if ("/auth/validar-token".equals(request.getRequestURI())) {
            String encodedMessage = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
            String errorUrl = UriComponentsBuilder.fromUriString(deepLinkBaseUrl + deepLinkErrorPath)
                    .queryParam("message", encodedMessage)
                    .toUriString();
            return new RedirectView(errorUrl);
        }

        // Caso contrário, trata como um erro de API e retorna JSON
        HttpStatus status;
        String error;

        if (ex instanceof ResourceNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            error = "Recurso não encontrado";
        } else { // BusinessException e InvalidTokenException
            status = HttpStatus.BAD_REQUEST;
            error = "Requisição inválida";
        }

        return buildErrorResponse(ex, status, error, request);
    }

    /**
     * Handler para exceções de segurança na API (acesso negado).
     * Retorna um status 403 Forbidden com um corpo de erro padronizado.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, "Acesso Negado", request);
    }

    /**
     * Handler "pega-tudo" para erros inesperados que ocorrem na API.
     * Garante que nenhuma exceção vaze sem tratamento, sempre retornando um JSON padronizado.
     * Retorna um status 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllApiExceptions(Exception ex, HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/")) {
            log.error("Erro inesperado na API na rota {}: {}", request.getRequestURI(), ex.getMessage(), ex);
            return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Erro Interno do Servidor", request);
        }
        // Se não for rota da API, relança a exceção para o Spring tratar (Whitelabel Error Page)
        throw new RuntimeException(ex);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, HttpStatus status, String error, HttpServletRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                status.value(),
                error,
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, status);
    }
}