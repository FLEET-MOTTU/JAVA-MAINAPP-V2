package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.web.OnboardingRequest;
import br.com.mottu.fleet.application.dto.web.PateoViewModel;
import br.com.mottu.fleet.application.dto.web.UsuarioAdminUpdateRequest;
import br.com.mottu.fleet.application.dto.web.AdminComPateoViewModel;
import br.com.mottu.fleet.domain.service.OnboardingService;
import br.com.mottu.fleet.domain.service.PateoService;
import br.com.mottu.fleet.domain.service.UsuarioAdminService;
import br.com.mottu.fleet.domain.service.MagicLinkService;
import br.com.mottu.fleet.domain.repository.FuncionarioRepository;
import br.com.mottu.fleet.domain.enums.Status;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.locationtech.jts.io.WKTWriter;

import java.util.UUID;

/**
 * Controller para todas as operações do painel web do Super Admin.
 * Todas as rotas sob "/admin" são protegidas e exigem a role 'SUPER_ADMIN'.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private final WKTWriter wktWriter = new WKTWriter();

    private final OnboardingService onboardingService;
    private final UsuarioAdminService usuarioAdminService;
    private final PateoService pateoService;
    private final MagicLinkService magicLinkService;
    private final FuncionarioRepository funcionarioRepository;


    public AdminController(OnboardingService onboardingService,
                           UsuarioAdminService usuarioAdminService,
                           PateoService pateoService,
                           MagicLinkService magicLinkService,
                           FuncionarioRepository funcionarioRepository) {
        this.onboardingService = onboardingService;
        this.usuarioAdminService = usuarioAdminService;
        this.pateoService = pateoService;
        this.magicLinkService = magicLinkService;
        this.funcionarioRepository = funcionarioRepository;
    }


    /**
     * Exibe o dashboard principal, que contém a lista de pátios ativos.
     * @param model O Model para adicionar atributos para a view.
     * @param request A requisição HTTP, usada para determinar a URI atual para o layout.
     * @return O nome da view do Thymeleaf "admin/dashboard".
     */
    @GetMapping("/dashboard")
    public String exibirDashboard(Model model, HttpServletRequest request) {
        model.addAttribute("pateos", pateoService.listarTodosAtivos());
        model.addAttribute("requestURI", request.getRequestURI());
        return "admin/dashboard";
    }


    /**
     * Exibe o formulário para cadastrar (onboarding) uma nova unidade Mottu.
     * @param model O Model para adicionar o objeto de requisição vazio para o form binding.
     * @param request A requisição HTTP para determinar a URI.
     * @return O nome da view "admin/form-onboarding".
     */
    @GetMapping("/onboarding/novo")
    public String exibirFormularioOnboarding(Model model, HttpServletRequest request) {
        model.addAttribute("request", new OnboardingRequest());
        model.addAttribute("requestURI", request.getRequestURI());
        return "admin/form-onboarding";
    }


    /**
     * Processa os dados submetidos pelo formulário, valida e cria uma nova unidade (Pátio + Admin).
     * @param request O objeto DTO populado com os dados do formulário.
     * @param bindingResult O resultado da validação, para checar por erros.
     * @param redirectAttributes Usado para passar mensagens de sucesso/erro após o redirecionamento.
     * @return Uma string de redirecionamento para o dashboard (sucess) / nome da view do formulário (error).
     */
    @PostMapping("/onboarding")
    public String processarOnboarding(@Valid @ModelAttribute("request") OnboardingRequest request,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes,
                                      Model model,
                                      HttpServletRequest httpRequest) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("requestURI", httpRequest.getRequestURI());
            return "admin/form-onboarding";
        }
        onboardingService.executar(request);
        redirectAttributes.addFlashAttribute("sucessoMessage", "Nova unidade cadastrada com sucesso!");
        return "redirect:/admin/dashboard";
    }


    /**
     * Exibe a lista paginada e filtrável de todos os Administradores de Pátio.
     * @param status O status para filtrar a lista (ATIVO, REMOVIDO, SUSPENSO). Opcional.
     * @param page O número da página solicitada (deafult: 0).
     * @param size O tamanho da página (deafult: 10).
     * @return O nome da view "admin/lista-usuarios".
     */
    @GetMapping("/usuarios")
    public String listarUsuarios(
            @RequestParam(required = false) Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model, HttpServletRequest request) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AdminComPateoViewModel> adminsPage = usuarioAdminService.listarAdminsDePateoPaginado(status, pageable);

        model.addAttribute("adminsPage", adminsPage);
        model.addAttribute("filtroStatus", status == null ? "ATIVO" : status.name());
        model.addAttribute("requestURI", request.getRequestURI());
        return "admin/lista-usuarios";
    }


     /**
     * Exibe o formulário de edição para um administrador de pátio específico.
     * @param id O UUID do administrador a ser editado.
     * @return O nome da view "admin/form-edit-usuario" (sucess) / redirect (error).
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
     * Processa a submissão do formulário de edição de um administrador de pátio.
     * @param id O UUID do usuário sendo editado.
     * @param request O objeto DTO populado com os dados do formulário.
     * @return String de redirecionamento para a lista de usuários (sucess) / nome da view do formulário (error).
     */
    @PostMapping("/usuarios/{id}/editar")
    public String processarEdicao(@PathVariable UUID id,
                                @Valid @ModelAttribute("usuarioRequest") UsuarioAdminUpdateRequest request,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model, HttpServletRequest httpRequest) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("requestURI", httpRequest.getRequestURI());
            return "admin/form-edit-usuario";
        }

        request.setId(id);
        usuarioAdminService.atualizar(request);
        redirectAttributes.addFlashAttribute("sucessoMessage", "Usuário atualizado com sucesso!");
        return "redirect:/admin/usuarios";
    }


    /**
     * Mapeia a requisição POST para desativar (soft delete) um administrador e seus pátios associados.
     * @param id O UUID do administrador a ser desativado.
     * @return Uma string de redirecionamento para a lista de usuários.
     */
    @PostMapping("/usuarios/{id}/desativar")
    public String desativarUsuario(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        usuarioAdminService.desativarPorId(id);
        redirectAttributes.addFlashAttribute("sucessoMessage", "Usuário e pátios associados foram desativados!");
        return "redirect:/admin/usuarios";
    }


    /**
     * Exibe a página de detalhes de um pátio específico, incluindo sua planta, zonas e funcionários.
     * @param pateoId O UUID do pátio a ser visualizado.
     * @return Nome da view "admin/detalhes-pateo" (sucess) / redirect para o dashboard (error).
     */
    @GetMapping("/pateos/{pateoId}")
    public String exibirDetalhesPateo(@PathVariable UUID pateoId, Model model, HttpServletRequest request) {
        model.addAttribute("requestURI", request.getRequestURI());
        
        PateoViewModel viewModel = pateoService.prepararViewModelDeDetalhes(pateoId);
        
        model.addAttribute("viewModel", viewModel);
        model.addAttribute("wktWriter", wktWriter);
        
        return "admin/detalhes-pateo";
    }


    /**
     * Gera um novo Magic Link para um funcionário e retorna à página de detalhes do pátio.
     * @param pateoId O ID do pátio (usado para o redirecionamento de volta).
     * @param id O ID do funcionário para o qual o link será gerado.
     * @return Uma string de redirecionamento para a página de detalhes do pátio.
     */
    @PostMapping("/pateos/{pateoId}/funcionarios/{id}/gerar-link")
    public String gerarNovoMagicLink(
        @PathVariable UUID pateoId,
        @PathVariable UUID id,
        RedirectAttributes redirectAttributes) {

        String link = magicLinkService.gerarLink(id);
        redirectAttributes.addFlashAttribute("sucessoMessage", "Novo Magic Link gerado com sucesso!");
        redirectAttributes.addFlashAttribute("generatedLink", link);
        redirectAttributes.addFlashAttribute("generatedForFuncionarioNome", funcionarioRepository.findById(id).get().getNome());
        return "redirect:/admin/pateos/" + pateoId;
    }


    /**
     * Mapeia a requisição POST para reativar um administrador e seus pátios associados.
     * @param id O UUID do administrador a ser reativado.
     * @return Uma string de redirecionamento para a lista de usuários.
     */
    @PostMapping("/usuarios/{id}/reativar")
    public String reativarUsuario(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        usuarioAdminService.reativarPorId(id);
        redirectAttributes.addFlashAttribute("sucessoMessage", "Usuário reativado com sucesso!");
        return "redirect:/admin/usuarios";
    }
}