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

    String tenantId() default "";

    String companyId() default "";

    String projectId() default "";
}
