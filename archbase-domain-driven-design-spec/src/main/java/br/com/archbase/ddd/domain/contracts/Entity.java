package br.com.archbase.ddd.domain.contracts;

/**
 * Identifica uma {@link Entity}. Entidades representam um fio de continuidade e identidade, passando por um ciclo de vida,
 * embora seus atributos possam mudar. Os meios de identificação podem vir de fora ou podem ser arbitrários
 * identificadores criados por e para o sistema, mas deve corresponder às distinções de identidade no modelo. O modelo
 * deve definir o que significa ser a mesma coisa.
 * Ela também está vinculado a um {@link AggregateRoot}. Isso pode parecer contra-intuitivo à primeira vista,
 * mas permite verificar que uma Entity não é acidentalmente referido a partir de um agregado diferente.
 * Usando essas interfaces, podemos configurar ferramentas de análise de código estático para verificar a
 * estrutura do nosso modelo.
 *
 * @author edsonmartins
 * @see <a href="https://domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf">Domain-Driven Design
 * Reference (Evans) - Entities</a>
 */
public interface Entity<T extends AggregateRoot<T, ?>, ID> extends Identifiable<ID> {

}
