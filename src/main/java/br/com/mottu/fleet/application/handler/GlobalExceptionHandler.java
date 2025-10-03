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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Handler global para exceções. Centraliza o tratamento de erros, decidindo
 * o formato da resposta (JSON para API, Redirect para Web) com base na
 * origem da requisição.
 */
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
     * Handler específico para o fluxo de onboarding via Thymeleaf.
     * Captura o erro de email duplicado e redireciona para o formulário.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public String handleEmailAlreadyExistsForWeb(EmailAlreadyExistsException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/admin/onboarding/novo";
    }
    
    /**
     * Handler unificado para exceções de regra de negócio, validação ou recursos não encontrados.
     * Ele inspeciona a URI da requisição para decidir a resposta apropriada.
     */
    @ExceptionHandler({ResourceNotFoundException.class, InvalidTokenException.class, BusinessException.class, SecurityException.class})
    public Object handleContextAwareExceptions(RuntimeException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String requestUri = request.getRequestURI();

        // Erro no fluxo de validação do Magic Link eedireciona para o deep link do app
        if ("/auth/validar-token".equals(requestUri)) {
            String encodedMessage = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
            String errorUrl = UriComponentsBuilder.fromUriString(deepLinkBaseUrl + deepLinkErrorPath)
                    .queryParam("message", encodedMessage)
                    .toUriString();
            return new RedirectView(errorUrl);
        }

        // Erro no Painel Web (qualquer rota que NÃO seja /api/) redireciona para a página anterior
        if (!requestUri.startsWith("/api/")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro: " + ex.getMessage());
            String referer = request.getHeader("Referer");
            return "redirect:" + (referer != null ? referer : "/admin/dashboard");
        }
        
        // Erro na API REST retorna uma resposta JSON
        HttpStatus status;
        String error;

        if (ex instanceof ResourceNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            error = "Recurso não encontrado";
        } else if (ex instanceof SecurityException) {
            status = HttpStatus.FORBIDDEN;
            error = "Acesso Negado";
        } else { // BusinessException e InvalidTokenException
            status = HttpStatus.BAD_REQUEST;
            error = "Requisição inválida";
        }

        return buildErrorResponse(ex, status, error, request);
    }
    
    /**
     * Handler específico para falhas de autenticação na API (usuário/senha inválidos).
     * Retorna um status 401 Unauthorized com uma mensagem clara.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(HttpServletRequest request) {
        String mensagem = "Usuário inexistente ou senha inválida";
        return buildErrorResponse(new RuntimeException(mensagem), HttpStatus.UNAUTHORIZED, "Não Autorizado", request);
    }

    /**
     * Handler para erros inesperados que ocorrem na API.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllApiExceptions(Exception ex, HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/")) {
            log.error("Erro inesperado na API na rota {}: {}", request.getRequestURI(), ex.getMessage(), ex);
            return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Erro Interno do Servidor", request);
        }
        // Se não for uma rota da API, relança a exceção para o Spring tratar e mostrar a Whitelabel Error Page
        throw new RuntimeException(ex);
    }

    /**
     * Handler para exceções de validação de DTOs (@Valid).
     * Captura os erros, formata uma mensagem clara e retorna um status 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        String mensagem = "Erro de validação: " + errors;
        return buildErrorResponse(new RuntimeException(mensagem), HttpStatus.BAD_REQUEST, "Requisição Inválida", request);
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