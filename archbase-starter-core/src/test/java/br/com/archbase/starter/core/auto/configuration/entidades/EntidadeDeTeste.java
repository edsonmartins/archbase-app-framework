package br.com.archbase.starter.core.auto.configuration.entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Entidade só para o teste do registrar de pacotes.
 *
 * <p>O {@code equals}/{@code hashCode} sobre os próprios campos é de propósito: é o padrão comum em
 * entidades JPA e é exatamente o que estourava quando o container tentava instanciá-la como bean.
 */
@Entity
@Table(name = "entidade_de_teste")
public class EntidadeDeTeste {

    @Id
    private Long id;

    private String nome;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntidadeDeTeste outra)) {
            return false;
        }
        return id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id.hashCode());
    }
}
