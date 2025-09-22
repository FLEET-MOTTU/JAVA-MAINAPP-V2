package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.OnboardingRequest;
import br.com.mottu.fleet.application.dto.UsuarioAdminUpdateRequest;
import br.com.mottu.fleet.domain.service.OnboardingService;
import br.com.mottu.fleet.domain.service.UsuarioAdminService;
import br.com.mottu.fleet.domain.service.PateoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;

import jakarta.validation.Valid;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    @Autowired
    private OnboardingService onboardingService;

    @Autowired
    private UsuarioAdminService usuarioAdminService;

    @Autowired
    private PateoService pateoService;

    /**
     * Método para EXIBIR a página com o formulário de cadastro.
     */
    @GetMapping("/onboarding/novo")
    public String exibirFormularioOnboarding(Model model) {
        // Adiciona um objeto DTO vazio ao modelo para que o Thymeleaf possa se vincular a ele.
        model.addAttribute("request", new OnboardingRequest());
        // Retorna o nome do arquivo HTML que está em 'resources/templates/admin/'
        return "admin/form-onboarding";
    }


    /**
     * Método para PROCESSAR os dados enviados pelo formulário.
     */
    @PostMapping("/onboarding")
    public String processarOnboarding(@Valid @ModelAttribute("request") OnboardingRequest request, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "admin/form-onboarding";
        }

        onboardingService.executar(request);
        return "redirect:/admin/onboarding/novo?sucesso";
    }
  

    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioAdminService.listarAdminsDePateo());
        return "admin/lista-usuarios";
    }


    @PostMapping("/usuarios/{id}/desativar")
    public String desativarUsuario(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            usuarioAdminService.desativarPorId(id);
            redirectAttributes.addFlashAttribute("sucessoMessage", "Usuário e pátios associados foram desativados!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    /**
     * GET: Exibe o formulário de edição para um usuário específico.
     */
    @GetMapping("/usuarios/{id}/editar")
    public String exibirFormularioEdicao(@PathVariable UUID id, Model model) {
        return usuarioAdminService.buscarPorId(id).map(usuario -> {
            
            UsuarioAdminUpdateRequest request = new UsuarioAdminUpdateRequest();
            request.setId(usuario.getId());
            request.setNome(usuario.getNome());
            request.setEmail(usuario.getEmail());
            
            model.addAttribute("usuarioRequest", request);
            return "admin/form-edit-usuario";

        }).orElse("redirect:/admin/usuarios");
    }

    /**
     * POST: Processa a atualização do usuário.
     */
    @PostMapping("/usuarios/editar")
    public String processarEdicao(@Valid @ModelAttribute("usuarioRequest") UsuarioAdminUpdateRequest request, BindingResult bindingResult) {
        
        if (bindingResult.hasErrors()) {
            // Se a validação falhar (ex: nome em branco), retorna ao formulário
            return "admin/form-edit-usuario";
        }

        usuarioAdminService.atualizar(request);
        return "redirect:/admin/usuarios";
    }

    /**
     * GET: Exibe o dashboard principal com a lista de pátios.
     */
    @GetMapping("/dashboard")
    public String exibirDashboard(Model model) {
        model.addAttribute("pateos", pateoService.listarTodosAtivos());
        return "admin/dashboard";
    }
    
}