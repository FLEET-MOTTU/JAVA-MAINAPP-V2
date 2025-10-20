package br.com.mottu.fleet.application.controller;

import br.com.mottu.fleet.application.dto.web.*;
import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.service.OnboardingService;
import br.com.mottu.fleet.domain.service.PateoService;
import br.com.mottu.fleet.domain.service.UsuarioAdminService;
import br.com.mottu.fleet.infrastructure.service.QueueMonitoringService;
import br.com.mottu.fleet.domain.service.MagicLinkService;
import br.com.mottu.fleet.domain.repository.FuncionarioRepository;
import br.com.mottu.fleet.domain.enums.Status;
import br.com.mottu.fleet.domain.service.StorageService;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import org.locationtech.jts.io.WKTWriter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Controller MVC para todas as operações do painel web do Super Admin.
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
    private final StorageService storageService;

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    public AdminController(OnboardingService onboardingService,
                           UsuarioAdminService usuarioAdminService,
                           PateoService pateoService,
                           MagicLinkService magicLinkService,
                           FuncionarioRepository funcionarioRepository,
                           QueueMonitoringService queueMonitoringService,
                           StorageService storageService) {
        this.onboardingService = onboardingService;
        this.usuarioAdminService = usuarioAdminService;
        this.pateoService = pateoService;
        this.magicLinkService = magicLinkService;
        this.funcionarioRepository = funcionarioRepository;
        this.queueMonitoringService = queueMonitoringService;
        this.storageService = storageService;
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
     * @return Uma string de redirecionamento para o dashboard (sucesso) / nome da view do formulário (erro).
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
     * @param page O número da página solicitada (default: 0).
     * @param size O tamanho da página (default: 10).
     * @return O nome da view "admin/lista-usuarios".
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
     * @param id O UUID do administrador a ser editado.
     * @return O nome da view "admin/form-edit-usuario" (sucesso) / redirect (erro).
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
     * @return String de redirecionamento para a lista de usuários (sucesso) / nome da view do formulário (erro).
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
     * Mapeia a requisição POST para reativar um administrador.
     * @param id O UUID do administrador a ser reativado.
     * @return Uma string de redirecionamento para a lista de usuários.
     */
    @PostMapping("/usuarios/{id}/reativar")
    public String reativarUsuario(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        usuarioAdminService.reativarPorId(id);
        redirectAttributes.addFlashAttribute("sucessoMessage", "Usuário reativado com sucesso!");
        return "redirect:/admin/usuarios";
    }


    /**
     * Exibe a página de detalhes de um pátio específico (planta, zonas, funcionários).
     * @param pateoId O UUID do pátio a ser visualizado.
     * @return Nome da view "admin/detalhes-pateo" (sucesso) / redirect para o dashboard (erro).
     */
    @GetMapping("/pateos/{pateoId}")
    public String exibirDetalhesPateo(@PathVariable UUID pateoId, Model model, HttpServletRequest request) {
        model.addAttribute("requestURI", request.getRequestURI());
        
        PateoViewModel viewModel = pateoService.prepararViewModelDeDetalhes(pateoId);
        Pateo pateo = viewModel.pateo();

        String urlPlantaAcessivel = pateo.getPlantaBaixaUrl(); // Pega a URL base
        
        if (!"dev".equals(activeProfile) && urlPlantaAcessivel != null && !urlPlantaAcessivel.isBlank()) {
            String blobName = urlPlantaAcessivel.substring(urlPlantaAcessivel.lastIndexOf("/") + 1);
            urlPlantaAcessivel = storageService.gerarUrlAcessoTemporario("plantas", blobName);            
            pateo.setPlantaBaixaUrl(urlPlantaAcessivel);
        }
        
        model.addAttribute("viewModel", viewModel);
        model.addAttribute("wktWriter", wktWriter);

        return "admin/detalhes-pateo";
    }


    /**
     * Exibe a lista mestre de TODOS os funcionários de todos os pátios (Visão do Super Admin).
     * Permite filtrar por um pátio específico.
     * @param pateoId O UUID do pátio para filtrar (opcional).
     * @return O nome da view "admin/lista-funcionarios-mestre".
     */
    @GetMapping("/funcionarios")
    public String listarTodosFuncionarios(@RequestParam(required = false) UUID pateoId, Model model, HttpServletRequest request) {

        List<Funcionario> funcionarios;
        if (pateoId != null) {
            funcionarios = usuarioAdminService.listarTodosFuncionariosPorPateoId(pateoId);
        } else {
            funcionarios = usuarioAdminService.listarTodosFuncionariosComPateo();
        }
        
        List<FuncionarioMestreViewModel> funcionariosVM = funcionarios.stream()
            .map(func -> {
                String urlFotoAcessivel = func.getFotoUrl();

                if (!"dev".equals(activeProfile) && urlFotoAcessivel != null && !urlFotoAcessivel.isBlank()) {
                    String blobName = urlFotoAcessivel.substring(urlFotoAcessivel.lastIndexOf("/") + 1);
                    urlFotoAcessivel = storageService.gerarUrlAcessoTemporario("fotos", blobName);
                }

                return new FuncionarioMestreViewModel(func, urlFotoAcessivel);
            }).collect(Collectors.toList());

        model.addAttribute("funcionariosVM", funcionariosVM);
        model.addAttribute("pateos", pateoService.listarTodosAtivos());
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
        usuarioAdminService.deletarFuncionarioPermanentemente(id);
        redirectAttributes.addFlashAttribute("sucessoMessage", "Funcionário DELETADO PERMANENTEMENTE do banco de dados.");
        return "redirect:/admin/funcionarios";
    }


    /**
     * Gera um novo Magic Link para um funcionário (ferramenta de teste do Super Admin).
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

        funcionarioRepository.findById(id).ifPresent(funcionario -> 
            redirectAttributes.addFlashAttribute("generatedForFuncionarioNome", funcionario.getNome())
        );

        return "redirect:/admin/pateos/" + pateoId;
    }


    /**
     * Exibe a página de monitoramento de status das filas do Azure Service Bus.
     * @param model O Model para adicionar atributos para a view.
     * @param request A requisição HTTP para a URI.
     * @return O nome da view "admin/monitor-filas".
     */
    @GetMapping("/filas")
    public String monitorarFilas(Model model, HttpServletRequest request) {
        model.addAttribute("queueStats", queueMonitoringService.getQueueStats());
        model.addAttribute("requestURI", request.getRequestURI());
        return "admin/monitor-filas";
    }


    /**
     * Processa o upload de uma nova imagem de planta baixa para um pátio.
     * @param pateoId O ID do pátio que receberá a nova planta.
     * @param arquivoPlanta O arquivo de imagem enviado via formulário.
     * @param redirectAttributes Para exibir a mensagem de sucesso/erro.
     * @return Redireciona de volta para a página de detalhes do pátio.
     */
    @PostMapping("/pateos/{pateoId}/upload-planta")
    public String processarUploadPlanta(@PathVariable UUID pateoId,
                                        @RequestParam("planta") MultipartFile arquivoPlanta,
                                        @RequestParam("plantaLargura") Integer plantaLargura,
                                        @RequestParam("plantaAltura") Integer plantaAltura,
                                        RedirectAttributes redirectAttributes) {
        try {
            pateoService.atualizarPlantaBaixa(pateoId, arquivoPlanta, plantaLargura, plantaAltura);
            redirectAttributes.addFlashAttribute("sucessoMessage", "Planta baixa do pátio atualizada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar planta: " + e.getMessage());
        }
        
        return "redirect:/admin/pateos/" + pateoId;
    }
    
}