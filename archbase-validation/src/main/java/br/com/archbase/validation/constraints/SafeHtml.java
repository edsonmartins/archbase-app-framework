package br.com.archbase.validation.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Valide um valor de rich text fornecido pelo usuário para garantir que não contenha nenhum código malicioso, como incorporado
 * & lt; script & gt; elementos
 * <p>
 * Observe que esta restrição assume que você deseja validar a entrada que representa um fragmento do corpo de um documento HTML. E se
 * em vez disso, você deseja validar a entrada que representa um documento HTML completo, adicione o {@code html}, {@code head} e
 * tags {@code body} para a lista de permissões usada, conforme necessário.
 */
@Documented
@Constraint(validatedBy = {})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
public @interface SafeHtml {

    String message() default "{br.com.archbase.bean.validation.constraints.SafeHtml.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * @return O tipo de lista de permissões integrado que será aplicado ao valor de rich text
     */
    WhiteListType whitelistType() default WhiteListType.RELAXED;

    /**
     * @return Tags adicionais da lista de permissões que são permitidas no topo das tags especificadas pelo
     * {@link #whitelistType ()}.
     */
    String[] additionalTags() default {};

    /**
     * @return Permite especificar tags de lista branca adicionais com atributos opcionais.
     */
    Tag[] additionalTagsWithAttributes() default {};

    /**
     * Define implementações de lista branca padrão.
     */
    public enum WhiteListType {
        /**
         * Esta lista de permissões permite apenas nós de texto: todo o HTML será removido.
         */
        NONE,

        /**
         * Esta lista branca permite apenas formatação de texto simples: <code> b, em, i, strong, u </code>. Todos os outros HTML (tags e
         * atributos) serão removidos.
         */
        SIMPLE_TEXT,

        /**
         * Esta lista de permissões permite uma gama mais ampla de nós de texto:
         * <code> a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li, ol, p, pre, q, small, strike, strong, sub, sup, u, ul < / code>
         * e atributos apropriados.
         * <p>
         * Links (elementos <code> a </code>) podem apontar para <code> http, https, ftp, mailto </code>, e ter um imposto
         * Atributo <code> rel = nofollow </code>.
         * </p>
         * Não permite imagens.
         */
        BASIC,

        /**
         * Esta lista de permissões permite as mesmas tags de texto que {@link WhiteListType # BASIC} e também permite <code> img </code>
         * Tag,
         * com
         * atributos apropriados, com <code> src </code> apontando para <code> http </code> ou <code> https </code>.
         */
        BASIC_WITH_IMAGES,

        /**
         * Esta lista de permissões permite uma gama completa de texto e HTML de corpo estrutural:
         * <code> a, b, blockquote, br, caption, cite, code, col, colgroup, dd, dl, dt, em, h1, h2, h3, h4, h5, h6, i, img, li,
         * ol, p, pre, q, small, strike, strong, sub, sup, table, tbody, td, tfoot, th, thead, tr, u, ul </code>
         * <p>
         * Os links não têm um atributo <code> rel = nofollow </code> imposto, mas você pode adicioná-lo se desejar.
         * </p>
         */
        RELAXED
    }

    /**
     * Permite especificar tags de lista branca com atributos opcionais especificados. Adicionar uma tag com um determinado atributo também
     * coloca a própria tag na lista de permissões sem qualquer atributo.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
    @Retention(RUNTIME)
    @Documented
    public @interface Tag {
        /**
         * @return o nome da tab para lista de permissões.
         */
        String name();

        /**
         * @return lista de atributos de tag que estão na lista de permissões.
         */
        String[] attributes() default {};
    }

    /**
     * Define várias anotações {@code @SafeHtml} no mesmo elemento.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
    @Retention(RUNTIME)
    @Documented
    public @interface List {
        SafeHtml[] value();
    }
}
