package br.com.archbase.security.annotation;

import br.com.archbase.security.access.AccessLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declara a capacidade que este endpoint exige.
 *
 * <p>É a única anotação que <b>concede</b> — {@code @RequireRole}, {@code @RequireProfile} e
 * {@code @RequirePersona} só sabem negar. Ver {@link br.com.archbase.security.access.Gate} para a
 * hierarquia dos portões.
 *
 * <p>Além de proteger, ela <b>alimenta o catálogo</b>: a varredura de subida cria o recurso e a ação
 * correspondentes, e é isso que faz o endpoint aparecer na tela de segurança para receber
 * permissão. Por isso {@code description} é obrigatória — é o texto que quem administra vai ler na
 * hora de conceder.
 *
 * <p>Só em método, de propósito. Uma capacidade é <i>recurso + ação</i>, e a ação é sempre por
 * método; para não repetir o recurso, declare {@link ArchbaseResource} na classe.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HasPermission {

    String action();

    /** Texto que aparece no admin. Obrigatório: quem concede a permissão precisa saber o que é. */
    String description();

    /** Vazio herda o recurso de {@link ArchbaseResource} declarado na classe. */
    String resource() default "";

    /**
     * O rótulo curto que aparece na lista do admin — "Aprovar custo".
     *
     * <p>Existe porque {@code description} vinha fazendo três trabalhos ao mesmo tempo: identificar
     * a linha, explicar a ação e — via {@code ->} embutido — agrupar. O resultado é um catálogo em
     * que centenas de linhas se chamam "Criar X", "Editar X", "Listar X", geradas em massa, e quem
     * administra não consegue distinguir uma da outra.
     *
     * <p>Vazio significa <b>use a descrição</b>, que é como todo catálogo existente continua se
     * comportando. Não há reescrita em massa: as descrições atuais funcionam como rótulo hoje, e
     * trocá-las por conta própria substituiria um texto que alguém conhece por outro que ninguém
     * pediu.
     */
    String label() default "";

    /**
     * O agrupamento das capacidades dentro do recurso — "Custos", "Faturamento".
     *
     * <p>Substitui o {@code ->} embutido na descrição, que o cliente quebra na exibição para
     * simular hierarquia. Vazio não agrupa, e a lista fica como está.
     */
    String category() default "";

    /**
     * O nível mínimo que esta capacidade exige.
     *
     * <p>É apenas a <b>semente</b>: o valor é gravado em {@code SEGURANCA_ACAO.MINIMUM_LEVEL} no
     * primeiro registro da ação, e a partir daí quem manda é o admin — igual já acontece com a
     * descrição. Assim o desenvolvedor declara o piso que conhece, e a operação ajusta o que ela
     * conhece melhor, sem precisar de deploy.
     *
     * <p>{@link AccessLevel#NONE} — o padrão — significa sem piso. E o portão só é avaliado com
     * {@code archbase.security.access-level.enabled=true}.
     *
     * <p><b>Piso, não substituto.</b> O nível não concede nada: o acesso continua dependendo de
     * permissão concedida no catálogo. Ele apenas impede que uma concessão indevida valha.
     */
    AccessLevel minimumLevel() default AccessLevel.NONE;

    /**
     * As capacidades sem as quais esta aqui não serve para nada na prática.
     *
     * <p>Duas formas: {@code "view"} é a ação do <b>mesmo recurso</b>; {@code "tms.cliente:view"} é
     * qualificada. Nome de recurso que contenha {@code :} torna a forma qualificada ambígua — a
     * aresta não é catalogada e a varredura registra o método no log, sem derrubar a subida.
     *
     * <p><b>Informativo. Nunca é portão.</b> A decisão de acesso <b>não</b> lê estas arestas, e não
     * há flag que a faça ler. Se declarar {@code requires} passasse a exigir a dependência na
     * autorização, toda instalação existente perderia acesso na primeira subida após a atualização —
     * em silêncio, porque ninguém declarou pensando em autorização. O que elas alimentam é a tela de
     * concessão (que passa a oferecer as dependências junto, e a avisar ao revogar) e o relatório de
     * efetivo.
     *
     * <p>Ao contrário de {@code description} e {@code minimumLevel}, que são <b>semente</b> e depois
     * pertencem ao administrador, a aresta é propriedade do código: ela é refeita a cada subida.
     */
    String[] requires() default {};

    String tenantId() default "";

    String companyId() default "";

    String projectId() default "";
}
