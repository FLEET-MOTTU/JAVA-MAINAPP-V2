package br.com.mottu.fleet.application.handler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
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

import br.com.mottu.fleet.application.dto.ErrorResponse;
import br.com.mottu.fleet.domain.exception.BusinessException;
import br.com.mottu.fleet.domain.exception.EmailAlreadyExistsException;
import br.com.mottu.fleet.domain.exception.InvalidTokenException;
import br.com.mottu.fleet.domain.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

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

    /**
     * Construtor que injeta as propriedades de configuração para o fluxo de deep link.
     * @param deepLinkBaseUrl URL base do deep link do app mobile (ex: fleetapp://).
     * @param deepLinkErrorPath Caminho para a tela de erro no deep link (ex: login-error).
     */
    public GlobalExceptionHandler(
            @Value("${application.deeplink.login-success-path}") String deepLinkBaseUrl,
            @Value("${application.deeplink.login-error-path}") String deepLinkErrorPath) {
        this.deepLinkBaseUrl = deepLinkBaseUrl;
        this.deepLinkErrorPath = deepLinkErrorPath;
    }


    /**
     * Handler para o fluxo de onboarding do Super Admin (Thymeleaf).
     * Captura o erro de email duplicado e redireciona o usuário de volta ao formulário
     * com uma mensagem de erro.
     *
     * @param ex A exceção capturada.
     * @param redirectAttributes Usado para passar a mensagem de erro para a página de redirect.
     * @return Uma string de redirecionamento para a view do formulário de onboarding.
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public String handleEmailAlreadyExistsForWeb(EmailAlreadyExistsException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/admin/onboarding/novo";
    }
    

    /**
     * Handler unificado e ciente do contexto para exceções comuns de negócio e segurança.
     * Inspeciona a URI da requisição para decidir a resposta apropriada,
     * tratando 3 contextos diferentes: Magic Link, Painel Web e API REST.
     *
     * @param ex A exceção de runtime capturada.
     * @param request A requisição HTTP para determinar a rota.
     * @param redirectAttributes Usado para redirecionamentos no painel web.
     * @return Um objeto de resposta (RedirectView, String de redirect, or ResponseEntity).
     */
    @ExceptionHandler({ResourceNotFoundException.class, InvalidTokenException.class, BusinessException.class, SecurityException.class})
    public Object handleContextAwareExceptions(RuntimeException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        String requestUri = request.getRequestURI();

        // Contexto 1: Falha no fluxo de validação do Magic Link (app mobile).
        // Resposta: Redireciona o navegador para o deep link de erro do app.
        if ("/auth/validar-token".equals(requestUri)) {
            String encodedMessage = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
            String errorUrl = UriComponentsBuilder.fromUriString(deepLinkBaseUrl + deepLinkErrorPath)
                    .queryParam("message", encodedMessage)
                    .toUriString();
            return new RedirectView(errorUrl);
        }

        // Contexto 2: Erro no Painel Web do Super Admin (Thymeleaf).
        // Resposta: Redireciona o usuário para a página anterior com uma mensagem de erro.
        if (!requestUri.startsWith("/api/")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro: " + ex.getMessage());
            String referer = request.getHeader("Referer");
            return "redirect:" + (referer != null ? referer : "/admin/dashboard");
        }
        
        // Contexto 3: Erro na API REST (chamada pelo app do Admin de Pátio).
        // Resposta: Monta um JSON de erro padronizado (ResponseEntity).
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
     * Handler para falhas de autenticação na API (login e senha inválidos).
     * Retorna um status 401 Unauthorized com uma mensagem genérica por segurança.
     *
     * @param request A requisição HTTP.
     * @return Um ResponseEntity com status 401 e corpo de erro padronizado.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(HttpServletRequest request) {
        String mensagem = "Verifique as credenciais e tente novamente.";
        return buildErrorResponse(new RuntimeException(mensagem), HttpStatus.UNAUTHORIZED, "Não Autorizado", request);
    }


    /**
     * Handler "pega-tudo" para exceções inesperadas (ex: NullPointerException).
     * Diferencia entre erros na API e erros no Painel Web.
     *
     * @param ex A exceção genérica capturada.
     * @param request A requisição HTTP.
     * @return Um ResponseEntity (API) ou relança a exceção (Web).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllApiExceptions(Exception ex, HttpServletRequest request) {

        // Se o erro ocorreu em uma rota da API, loga o stack trace completo
        // e retorna um JSON de erro 500.
        if (request.getRequestURI().startsWith("/api/")) {
            log.error("Erro inesperado na API na rota {}: {}", request.getRequestURI(), ex.getMessage(), ex);
            return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Erro Interno do Servidor", request);
        }

        // Se o erro foi no painel web, relança a exceção.
        // Spring Boot captura e mostra "Whitelabel Error Page" padrão.
        throw new RuntimeException(ex);
    }


    /**
     * Handler para exceções de validação de DTOs anotadas com @Valid (ex: @RequestBody).
     * Formata os erros de campo em uma string e retorna 400 Bad Request.
     *
     * @param ex A exceção capturada.
     * @param request A requisição HTTP.
     * @return Um ResponseEntity com status 400 e os erros de validação.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        String mensagem = "Erro de validação: " + errors;
        return buildErrorResponse(new RuntimeException(mensagem), HttpStatus.BAD_REQUEST, "Requisição Inválida", request);
    }


    /**
     * Handler para erros de integridade do banco de dados (ex: violação de constraint 'UNIQUE').
     * Retorna um status 409 Conflict com uma mensagem genérica para não expor detalhes do schema.
     *
     * @param ex A exceção capturada.
     * @param request A requisição HTTP.
     * @return Um ResponseEntity com status 409.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        String mensagem = "Erro de integridade dos dados. Provavelmente um registro duplicado (ex: email ou telefone já cadastrado).";
        return buildErrorResponse(new RuntimeException(mensagem, ex), HttpStatus.CONFLICT, "Conflito de Dados", request);
    }


    /**
     * Handler para exceções de validação lançadas manualmente (ex: no controller com @RequestPart).
     * Formata os erros de campo e retorna 400 Bad Request.
     *
     * @param ex A exceção capturada.
     * @param request A requisição HTTP.
     * @return Um ResponseEntity com status 400.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String errors = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining(", "));

        String mensagem = "Erro de validação: " + errors;
        return buildErrorResponse(new RuntimeException(mensagem, ex), HttpStatus.BAD_REQUEST, "Requisição Inválida", request);
    }


    /**
     * Método auxiliar para construir o DTO de resposta de erro padronizado.
     * @param ex A exceção original.
     * @param status O HttpStatus da resposta.
     * @param error O título do erro (ex: "Não Autorizado").
     * @param request A requisição original para obter o path.
     * @return Um ResponseEntity preenchido.
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