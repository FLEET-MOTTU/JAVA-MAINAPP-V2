package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.web.OnboardingRequest;
import br.com.mottu.fleet.application.dto.web.PateoViewModel;
import br.com.mottu.fleet.application.dto.web.UsuarioAdminUpdateRequest;
import br.com.mottu.fleet.application.dto.web.AdminComPateoViewModel;
import br.com.mottu.fleet.domain.entity.Funcionario; // Importe a entidade
import br.com.mottu.fleet.domain.service.OnboardingService;
import br.com.mottu.fleet.domain.service.PateoService;
import br.com.mottu.fleet.domain.service.UsuarioAdminService;
import br.com.mottu.fleet.domain.service.MagicLinkService;
import br.com.mottu.fleet.domain.service.QueueMonitoringService;
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

import java.util.List; // Importe o List
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
    private final QueueMonitoringService queueMonitoringService;

    public AdminController(OnboardingService onboardingService,
                           UsuarioAdminService usuarioAdminService,
                           PateoService pateoService,
                           MagicLinkService magicLinkService,
                           FuncionarioRepository funcionarioRepository,
                           QueueMonitoringService queueMonitoringService) {
        this.onboardingService = onboardingService;
        this.usuarioAdminService = usuarioAdminService;
        this.pateoService = pateoService;
        this.magicLinkService = magicLinkService;
        this.funcionarioRepository = funcionarioRepository;
        this.queueMonitoringService = queueMonitoringService;
    }

    // --- SEÇÃO: DASHBOARD E ONBOARDING ---

    /**
     * Exibe o dashboard principal, que contém a lista de pátios ativos.
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
     * Processa os dados submetidos pelo formulário de onboarding.
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

    // --- SEÇÃO: GERENCIAMENTO DE ADMINS DE PÁTIO ---

    /**
     * Exibe a lista paginada e filtrável de todos os Administradores de Pátio.
     */
    @GetMapping("/usuarios")
    public String listarUsuarios(@RequestParam(required = false) Status status,
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
     * Mapeia a requisição POST para desativar (soft delete) um administrador.
     */
    @PostMapping("/usuarios/{id}/desativar")
    public String desativarUsuario(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        usuarioAdminService.desativarPorId(id);
        redirectAttributes.addFlashAttribute("sucessoMessage", "Usuário e pátios associados foram desativados!");
        return "redirect:/admin/usuarios";
    }

    /**
     * Mapeia a requisição POST para reativar um administrador.
     */
    @PostMapping("/usuarios/{id}/reativar")
    public String reativarUsuario(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        usuarioAdminService.reativarPorId(id);
        redirectAttributes.addFlashAttribute("sucessoMessage", "Usuário reativado com sucesso!");
        return "redirect:/admin/usuarios";
    }

    // --- SEÇÃO: GERENCIAMENTO DE PÁTIOS (VISUALIZAÇÃO) ---

    /**
     * Exibe a página de detalhes de um pátio específico (planta, zonas, funcionários).
     */
    @GetMapping("/pateos/{pateoId}")
    public String exibirDetalhesPateo(@PathVariable UUID pateoId, Model model, HttpServletRequest request) {
        model.addAttribute("requestURI", request.getRequestURI());
        PateoViewModel viewModel = pateoService.prepararViewModelDeDetalhes(pateoId);
        model.addAttribute("viewModel", viewModel);
        model.addAttribute("wktWriter", wktWriter);
        return "admin/detalhes-pateo";
    }

    // --- SEÇÃO: GERENCIAMENTO MESTRE DE FUNCIONÁRIOS (SUPER ADMIN) ---

    /**
     * Exibe a lista mestre de TODOS os funcionários de todos os pátios (Visão do Super Admin).
     * Permite filtrar por um pátio específico.
     * @param pateoId O UUID do pátio para filtrar (opcional).
     * @param model O Model para adicionar atributos para a view.
     * @param request A requisição HTTP para a URI.
     * @return O nome da view "admin/lista-funcionarios-mestre".
     */
    @GetMapping("/funcionarios")
    public String listarTodosFuncionarios(@RequestParam(required = false) UUID pateoId, Model model, HttpServletRequest request) {
        List<Funcionario> funcionarios;
        if (pateoId != null) {
            // Chama o serviço do Admin, que tem a permissão de listar
            funcionarios = usuarioAdminService.listarTodosFuncionariosPorPateoId(pateoId);
        } else {
            // Chama o serviço do Admin
            funcionarios = usuarioAdminService.listarTodosFuncionariosComPateo();
        }
        
        model.addAttribute("funcionarios", funcionarios);
        model.addAttribute("pateos", pateoService.listarTodosAtivos()); // Para o <select> do filtro
        model.addAttribute("filtroPateoId", pateoId);
        model.addAttribute("requestURI", request.getRequestURI());
        return "admin/lista-funcionarios-mestre";
    }

    /**
     * Processa o HARD DELETE (exclusão permanente) de um funcionário.
     * Esta é uma operação de Super Admin e não pode ser desfeita.
     * @param id O UUID do funcionário a ser deletado.
     * @return Um redirecionamento para a lista mestre de funcionários.
     */
    @PostMapping("/funcionarios/{id}/delete-hard")
    public String deletarFuncionarioHard(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        // Chama o serviço do Admin, que tem a lógica de hard delete
        usuarioAdminService.deletarFuncionarioPermanentemente(id);
        redirectAttributes.addFlashAttribute("sucessoMessage", "Funcionário DELETADO PERMANENTEMENTE do banco de dados.");
        return "redirect:/admin/funcionarios";
    }

    /**
     * Gera um novo Magic Link para um funcionário (ferramenta de teste do Super Admin).
     */
    @PostMapping("/pateos/{pateoId}/funcionarios/{id}/gerar-link")
    public String gerarNovoMagicLink(
            @PathVariable UUID pateoId,
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {

        String link = magicLinkService.gerarLink(id);
        redirectAttributes.addFlashAttribute("sucessoMessage", "Novo Magic Link gerado com sucesso!");
        redirectAttributes.addFlashAttribute("generatedLink", link);
        // O uso do FuncionarioRepository aqui é um pequeno "atalho"
        // O ideal seria o magicLinkService retornar o nome, mas isso é aceitável.
        funcionarioRepository.findById(id).ifPresent(funcionario -> 
            redirectAttributes.addFlashAttribute("generatedForFuncionarioNome", funcionario.getNome())
        );
        return "redirect:/admin/pateos/" + pateoId;
    }


    /**
     * Exibe a página de monitoramento de status das filas do Azure Service Bus.
     */
    @GetMapping("/filas")
    public String monitorarFilas(Model model, HttpServletRequest request) {
        model.addAttribute("queueStats", queueMonitoringService.getQueueStats());
        model.addAttribute("requestURI", request.getRequestURI());
        return "admin/monitor-filas";
    }
    
}