package br.com.mottu.fleet.domain.service;

import br.com.mottu.fleet.application.dto.FuncionarioCreateRequest;
import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.entity.Pateo;
import br.com.mottu.fleet.domain.entity.UsuarioAdmin;
import br.com.mottu.fleet.domain.enums.Cargo;
import br.com.mottu.fleet.domain.enums.Status;
import br.com.mottu.fleet.domain.repository.FuncionarioRepository;
import br.com.mottu.fleet.domain.repository.PateoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuncionarioServiceImpl implements FuncionarioService {

    @Autowired
    private FuncionarioRepository funcionarioRepository;
    @Autowired
    private PateoRepository pateoRepository;
    @Autowired
    private MagicLinkService magicLinkService;

    @Override
    @Transactional
    public FuncionarioCriado criar(FuncionarioCreateRequest request, UsuarioAdmin adminLogado) {
        Pateo pateo = pateoRepository.findAllByGerenciadoPorId(adminLogado.getId())
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Admin não está associado a nenhum pátio."));

        Funcionario novoFuncionario = new Funcionario();
        novoFuncionario.setNome(request.getNome());
        novoFuncionario.setTelefone(request.getTelefone());
        novoFuncionario.setPateo(pateo);

        // **CORREÇÃO 1: Convertendo a String do DTO para o Enum Cargo.**
        // O .toUpperCase() torna a conversão mais robusta (aceita "operacional" ou "OPERACIONAL")
        try {
            Cargo cargoEnum = Cargo.valueOf(request.getCargo().toUpperCase());
            novoFuncionario.setCargo(cargoEnum);
        } catch (IllegalArgumentException e) {
            // Se o front enviar um valor inválido (ex: "MOTORISTA"), lançamos um erro claro.
            // Nosso GlobalExceptionHandler poderia tratar isso no futuro.
            throw new RuntimeException("Cargo inválido: " + request.getCargo() + ". Valores válidos são: OPERACIONAL, ADMINISTRATIVO, TEMPORARIO.");
        }

        // **CORREÇÃO 2: Usando o Enum Status diretamente.**
        novoFuncionario.setStatus(Status.ATIVO);

        // O código pode ser um gerador mais inteligente no futuro
        novoFuncionario.setCodigo("FUNC-" + request.getTelefone());

        Funcionario funcionarioSalvo = funcionarioRepository.save(novoFuncionario);

        // Gera o link mágico para o funcionário recém-criado
        String link = magicLinkService.gerarLink(funcionarioSalvo);

        return new FuncionarioCriado(funcionarioSalvo, link);
    }
}