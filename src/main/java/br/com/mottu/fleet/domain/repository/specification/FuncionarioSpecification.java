package br.com.mottu.fleet.domain.repository.specification;

import br.com.mottu.fleet.domain.entity.Funcionario;
import br.com.mottu.fleet.domain.enums.Cargo;
import br.com.mottu.fleet.domain.enums.Status;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Specification para a entidade Funcionario. (Filtros)
 */
public class FuncionarioSpecification {

    /**
     * Filtra obrigatoriamente por pateoId e opcionalmente por status e cargo.
     *
     * @param pateoId O ID do pátio (obrigatório).
     * @param status O status para filtrar (opcional).
     * @param cargo O cargo para filtrar (opcional).
     * @return Uma Specification<Funcionario> pronta para ser usada no repositório.
     */
    public static Specification<Funcionario> comFiltros(UUID pateoId, Status status, Cargo cargo) {

        // lambda para implementar a interface Specification
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("pateo").get("id"), pateoId));

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (cargo != null) {
                predicates.add(criteriaBuilder.equal(root.get("cargo"), cargo));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
}