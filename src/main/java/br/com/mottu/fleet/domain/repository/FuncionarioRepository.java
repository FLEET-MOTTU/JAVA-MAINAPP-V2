package br.com.mottu.fleet.domain.repository;

import br.com.mottu.fleet.domain.entity.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID>, JpaSpecificationExecutor<Funcionario> {
}