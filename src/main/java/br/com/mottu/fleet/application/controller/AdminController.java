package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.OnboardingRequest;
import br.com.mottu.fleet.application.dto.UsuarioAdminUpdateRequest;
import br.com.mottu.fleet.domain.service.OnboardingService;
import br.com.mottu.fleet.domain.service.PateoService;
import br.com.mottu.fleet.domain.service.UsuarioAdminService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * Controller para todas as operações do painel do Super Admin.
 * Protegido para ser acessível apenas por usuários com a role 'SUPER_ADMIN'.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private final OnboardingService onboardingService;
    private final UsuarioAdminService usuarioAdminService;
    private final PateoService pateoService;

    public AdminController(OnboardingService onboardingService,
                           UsuarioAdminService usuarioAdminService,
                           PateoService pateoService) {
        this.onboardingService = onboardingService;
        this.usuarioAdminService = usuarioAdminService;
        this.pateoService = pateoService;
    }

    /**
     * Exibe o dashboard principal com a lista de pátios ativos.
     */
    @GetMapping("/dashboard")
    public String exibirDashboard(Model model, HttpServletRequest request) {
        model.addAttribute("pateos", pateoService.listarTodosAtivos());
        model.addAttribute("requestURI", request.getRequestURI());
        return "admin/dashboard";
    }

    /**
     * Exibe o formulário para cadastrar (onboarding) uma nova unidade Mottu.
     */
    @GetMapping("/onboarding/novo")
    public String exibirFormularioOnboarding(Model model, HttpServletRequest request) {
        model.addAttribute("request", new OnboardingRequest());
        model.addAttribute("requestURI", request.getRequestURI());
        return "admin/form-onboarding";
    }

    /**
     * Processa a submissão do formulário de onboarding de uma nova unidade.
     */
    @PostMapping("/onboarding")
    public String processarOnboarding(@Valid @ModelAttribute("request") OnboardingRequest request,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/form-onboarding";
        }
        onboardingService.executar(request);
        redirectAttributes.addFlashAttribute("sucessoMessage", "Nova unidade cadastrada com sucesso!");
        return "redirect:/admin/dashboard"; // Redireciona para o dashboard após sucesso
    }

    /**
     * Exibe a lista de todos os administradores de pátio ativos.
     */
    @GetMapping("/usuarios")
    public String listarUsuarios(Model model, HttpServletRequest request) {
        model.addAttribute("usuarios", usuarioAdminService.listarAdminsDePateo());
        model.addAttribute("requestURI", request.getRequestURI());
        return "admin/lista-usuarios";
    }

    /**
     * Exibe o formulário de edição para um administrador de pátio específico.
     */
    @GetMapping("/usuarios/{id}/editar")
    public String exibirFormularioEdicao(@PathVariable UUID id, Model model, HttpServletRequest request) {
        model.addAttribute("requestURI", request.getRequestURI());
        return usuarioAdminService.buscarPorId(id).map(usuario -> {
            model.addAttribute("usuarioRequest", UsuarioAdminUpdateRequest.fromEntity(usuario));
            return "admin/form-edit-usuario";
        }).orElse("redirect:/admin/usuarios");
    }

    /**
     * Processa a atualização dos dados de um administrador de pátio.
     */
    @PostMapping("/usuarios/editar")
    public String processarEdicao(@Valid @ModelAttribute("usuarioRequest") UsuarioAdminUpdateRequest request,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/form-edit-usuario";
        }
        usuarioAdminService.atualizar(request);
        redirectAttributes.addFlashAttribute("sucessoMessage", "Usuário atualizado com sucesso!");
        return "redirect:/admin/usuarios";
    }

    /**
     * Desativa (soft delete) um administrador e seus pátios associados.
     */
    @PostMapping("/usuarios/{id}/desativar")
    public String desativarUsuario(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        usuarioAdminService.desativarPorId(id);
        redirectAttributes.addFlashAttribute("sucessoMessage", "Usuário e pátios associados foram desativados!");
        return "redirect:/admin/usuarios";
    }
}