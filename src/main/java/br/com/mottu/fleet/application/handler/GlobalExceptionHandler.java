package br.com.mottu.fleet.application.handler;

import br.com.mottu.fleet.application.dto.ErrorResponse;
import br.com.mottu.fleet.domain.exception.BusinessException;
import br.com.mottu.fleet.domain.exception.EmailAlreadyExistsException;
import br.com.mottu.fleet.domain.exception.InvalidTokenException;
import br.com.mottu.fleet.domain.exception.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

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

/**
 * Handler global para exceções. Centraliza o tratamento de erros, decidindo
 * o formato da resposta (JSON para API, Redirect para Web) com base na
 * origem da requisição.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private final String deepLinkBaseUrl;
    private final String deepLinkErrorPath;

    public GlobalExceptionHandler(
            @Value("${application.deeplink.base-url}") String deepLinkBaseUrl,
            @Value("${application.deeplink.login-error-path}") String deepLinkErrorPath) {
        this.deepLinkBaseUrl = deepLinkBaseUrl;
        this.deepLinkErrorPath = deepLinkErrorPath;
    }

    /**
     * Handler para o fluxo de onboarding via Thymeleaf.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public String handleEmailAlreadyExistsForWeb(EmailAlreadyExistsException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/admin/onboarding/novo";
    }

    /**
     * Handler unificado para exceções da API e do fluxo de Magic Link.
     * Ele inspeciona a URI da requisição para decidir a resposta apropriada.
     */
    @ExceptionHandler({ResourceNotFoundException.class, InvalidTokenException.class, BusinessException.class})
    public Object handleApiAndMagicLinkExceptions(RuntimeException ex, HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        // Se o erro veio do endpoint de validação do token, redireciona para o app mobile.
        if ("/auth/validar-token".equals(requestUri)) {
            String encodedMessage = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
            String errorUrl = UriComponentsBuilder.fromUriString(deepLinkBaseUrl + deepLinkErrorPath)
                    .queryParam("message", encodedMessage)
                    .toUriString();
            return new RedirectView(errorUrl);
        }

        // Para todas as outras exceções (vindas de /api/**), retorna uma resposta JSON.
        HttpStatus status;
        String error;

        if (ex instanceof ResourceNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            error = "Recurso não encontrado";
        } else {
            status = HttpStatus.BAD_REQUEST;
            error = "Requisição inválida";
        }

        return buildErrorResponse(ex, status, error, request);
    }

    /**
     * Handler para exceções genéricas que ocorrem no fluxo web (não API).
     * Redireciona para uma página de referência com uma mensagem de erro.
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericExceptionForWeb(Exception ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (!request.getRequestURI().startsWith("/api/")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ocorreu um erro inesperado: " + ex.getMessage());
            String referer = request.getHeader("Referer");
            return "redirect:" + (referer != null ? referer : "/admin/dashboard");
        }
        // Se for uma rota da API, deixa outros handlers ou o Spring tratar
        return null; 
    }

    /**
     * Método auxiliar para construir a resposta de erro JSON padronizada.
     */
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