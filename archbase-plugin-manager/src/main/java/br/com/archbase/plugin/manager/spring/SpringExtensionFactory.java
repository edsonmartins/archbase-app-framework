package br.com.archbase.plugin.manager.spring;


import br.com.archbase.plugin.manager.ArchbasePlugin;
import br.com.archbase.plugin.manager.ArchbasePluginManager;
import br.com.archbase.plugin.manager.ExtensionFactory;
import br.com.archbase.plugin.manager.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

/**
 * Implementação básica de uma fábrica de extensões.
 * <p> <p>
 * Usa Springs {@link AutowireCapableBeanFactory} para instanciar uma determinada classe de extensão. Todos os tipos de
 * {@link Autowired} são suportados (veja o exemplo abaixo). Se nenhum {@link ApplicationContext} estiver disponível (este é o caso
 * se o plug-in relacionado não for um {@link SpringPlugin} ou o gerenciador de plug-in fornecido não for um
 * {@link SpringArchbasePluginManager}), a reflexão Java padrão será usada para instanciar uma extensão.
 * <p> <p>
 * Cria uma nova instância de extensão toda vez que uma solicitação é feita.
 * <p> <p>
 * Exemplo de modos autowire suportados:
 * <pre>{@code
 *     @Extension
 *     public class Foo implements ExtensionPoint {
 *
 *         private final Bar bar;       // Injeção de construtor
 *         private Baz baz;             // Injeção setter
 *         @Autowired
 *         private Qux qux;             // Injeção de campo
 *
 *         @Autowired
 *         public Foo(final Bar bar) {
 *             this.bar = bar;
 *         }
 *
 *         @Autowired
 *         public void setBaz(final Baz baz) {
 *             this.baz = baz;
 *         }
 *     }
 * }</pre>
 */
public class SpringExtensionFactory implements ExtensionFactory {

    public static final boolean AUTOWIRE_BY_DEFAULT = true;
    private static final Logger log = LoggerFactory.getLogger(SpringExtensionFactory.class);
    private static final int AUTOWIRE_CONSTRUCTOR = AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR;

    /**
     * O gerenciador de plugins é usado para recuperar um plugin de uma determinada classe de extensão
     * e como um fornecedor substituto de um contexto de aplicativo.
     */
    protected final ArchbasePluginManager archbasePluginManager;
    /**
     * Indica se as possibilidades de fiação automática das molas devem ser usadas.
     */
    protected final boolean autowire;

    public SpringExtensionFactory(final ArchbasePluginManager archbasePluginManager) {
        this(archbasePluginManager, AUTOWIRE_BY_DEFAULT);
    }

    public SpringExtensionFactory(final ArchbasePluginManager archbasePluginManager, final boolean autowire) {
        this.archbasePluginManager = archbasePluginManager;
        this.autowire = autowire;
        if (!autowire) {
            log.warn("A fiação automática está desativada, embora a única razão para a existência desta fábrica especial seja " +
                    " apoiando o Spring e seu contexto de aplicação.");
        }
    }

    /**
     * Cria uma instância de {@code extensionClass} fornecida. Se {@link #autowire} for definido como {@code true} este método
     * tentará usar as possibilidades de fiação automática das molas.
     *
     * @param extensionClass A classe anotada com {@code @} {@ link Extension}.
     * @param <T>            O tipo para o qual uma instância deve ser criada.
     * @return uma instância da {@code extensionClass} solicitada.
     * @see #getApplicationContextBy(Class)
     */
    @Override
    public <T> T create(final Class<T> extensionClass) {
        if (!this.autowire) {
            String message = "Criar instância de '" + nameOf(extensionClass) + "' sem usar possibilidades de molas como" +
                    " autowiring está desabilitado.";
            log.warn(message);
            return createWithoutSpring(extensionClass);
        }

        return getApplicationContextBy(extensionClass)
                .map(applicationContext -> createWithSpring(extensionClass, applicationContext))
                .orElseGet(() -> createWithoutSpring(extensionClass));
    }

    /**
     * Cria uma instância do {@code extensionClass} fornecido usando o {@link AutowireCapableBeanFactory} do dado
     * {@code applicationContext}. Todos os tipos de autowiring são aplicados:
     * <ol>
     *     <li>Constructor injection</li>
     *     <li>Setter injection</li>
     *     <li>Field injection</li>
     * </ol>
     *
     * @param extensionClass     A classe anotada com {@code @} {@ link Extension}.
     * @param <T>                O tipo para o qual uma instância deve ser criada.
     * @param applicationContext O contexto a ser usado para autowiring.
     * @return uma instância de extensão autowired.
     */
    @SuppressWarnings("unchecked")
    protected <T> T createWithSpring(final Class<T> extensionClass, final ApplicationContext applicationContext) {
        final AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();

        String message = "Instancie a classe de extensão '" + nameOf(extensionClass) + "' usando construtor autowiring.";
        log.debug(message);
        // Autowire pelo construtor. Isso não inclui os outros tipos de injeção (setters e / ou campos).
        final Object autowiredExtension = beanFactory.autowire(extensionClass, AUTOWIRE_CONSTRUCTOR,
                // O valor do parâmetro 'dependencyCheck' é realmente irrelevante como o construtor de 'RootBeanDefinition'
                // pula a ação quando o modo autowire é definido como 'AUTOWIRE_CONSTRUCTOR'. Embora o valor padrão em
                // 'AbstractBeanDefinition' é 'DEPENDENCY_CHECK_NONE', portanto, também é definido como falso aqui.
                false);
        log.trace("Instância de extensão criada por injeção de construtor: {}", autowiredExtension);

        log.debug("Concluindo autowiring de extensão: {}", autowiredExtension);
        // Autowire usando os tipos restantes de injeção (por exemplo, setters e / ou campos).
        beanFactory.autowireBean(autowiredExtension);
        log.trace("Autowiring foi concluída para a extensão:: {}", autowiredExtension);

        return (T) autowiredExtension;
    }

    /**
     * Recupera molas {@link ApplicationContext} do plug-in de extensões ou do {@link #archbasePluginManager}.
     * <p>
     * O pedido de cheques é:
     * <ol>
     *      <li> Se a {@code extensionClass} fornecida pertencer a um plug-in que é um {@link SpringPlugin}, o contexto de plug-ins será retornado. </li>
     *      <li> Caso contrário, se o {@link #archbasePluginManager} fornecido desta instância for um {@link SpringArchbasePluginManager}, o contexto do gerenciador será retornado. </li>
     *      <li> Se nenhuma dessas verificações se encaixar, {@code null} será retornado. </li>
     * </ol>
     *
     * @param extensionClass A classe anotada com {@code @} {@ link Extension}.
     * @param <T>            O tipo de extensão para o qual um {@link ApplicationContext} é solicitado.
     * @return o contexto de melhor ajuste ou {@code null}.
     */
    protected <T> Optional<ApplicationContext> getApplicationContextBy(final Class<T> extensionClass) {
        final ArchbasePlugin archbasePlugin = Optional.ofNullable(this.archbasePluginManager.whichPlugin(extensionClass))
                .map(PluginWrapper::getPlugin)
                .orElse(null);

        final ApplicationContext applicationContext;

        if (archbasePlugin instanceof SpringPlugin) {
            String message = "  Classe de extensão ' " + nameOf(extensionClass) + "' pertence ao archbasePlugin spring '" + nameOf(archbasePlugin)
                    + "' e será autowired usando seu contexto de aplicativo.";
            log.debug(message);
            applicationContext = ((SpringPlugin) archbasePlugin).getApplicationContext();
        } else if (this.archbasePluginManager instanceof SpringArchbasePluginManager) {
            String message = "Extension class '" + nameOf(extensionClass) + "' pertence a um archbasePlugin não spring (ou aplicativo principal)" +
                    "'" + nameOf(archbasePlugin) + ", mas o gerenciador de plug-in Archbase usado é um gerenciador de plug-in spring. Portanto" +
                    "a classe de extensão será autowired usando os contextos do aplicativo de gerenciadores";
            log.debug(message);
            applicationContext = ((SpringArchbasePluginManager) this.archbasePluginManager).getApplicationContext();
        } else {
            String message = "Nenhum contexto de aplicativo pode ser usado para instanciar a classe de extensão '" + nameOf(extensionClass) + "'."
                    + "Esta extensão não pertence a um archbasePlugin spring Archbase (id: '" + nameOf(archbasePlugin) + "') nem é usado" +
                    "o gerenciador de plug-ins ou um gerenciador de plug-in spring (gerenciador usado: '" + nameOf(this.archbasePluginManager.getClass()) + "')." +
                    "Na perspectiva do Archbase, isso parece altamente incomum em combinação com uma fábrica cuja única razão de existência" +
                    "está usando o Spring (e seu contexto de aplicação) e deve pelo menos ser revisado. Na verdade, nenhuma injeção automática pode ser" +
                    "aplicado embora o sinalizador autowire tenha sido definido como 'true'. A instanciação voltará para a reflexão Java padrão.";
            log.warn(message);
            applicationContext = null;
        }

        return Optional.ofNullable(applicationContext);
    }

    /**
     * Cria uma instância do objeto de classe fornecido usando reflexão Java padrão.
     *
     * @param extensionClass A classe anotada com {@code @} {@ link Extension}.
     * @param <T>            O tipo para o qual uma instância deve ser criada.
     * @return uma extensão instanciada.
     * @throws IllegalArgumentException se o objeto de classe fornecido não tiver um construtor público.
     * @throws RuntimeException         se o construtor chamado não puder ser instanciado com os parâmetros {@code null}.
     */
    @SuppressWarnings("unchecked")
    protected <T> T createWithoutSpring(final Class<T> extensionClass) {
        final Constructor<?> constructor = getPublicConstructorWithShortestParameterList(extensionClass)
                // Uma classe de extensão é necessária para ter pelo menos um construtor público.
                .orElseThrow(() -> new IllegalArgumentException("Classe de extensão '" + nameOf(extensionClass)
                        + "' deve ter pelo menos um construtor público."));
        try {
            String message = "Instanciar '" + nameOf(extensionClass) + "' chamando '" + constructor + "' com reflexão Java padrão.";
            log.debug(message);
            // Criação da instância chamando o construtor com parâmetros nulos (se houver).
            return (T) constructor.newInstance(nullParameters(constructor));
        } catch (final InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            // Se uma dessas exceções for lançada, provavelmente devido ao NPE dentro do construtor chamado e
            // não a chamada reflexiva em si, pois procuramos precisamente por um construtor de ajuste.
            throw new SpringExtensionFactoryException("Provavelmente esta exceção foi lançada porque o construtor chamado (" + constructor + ")" +
                    "não pode manipular parâmetros 'nulos'. A mensagem original era:"
                    + ex.getMessage(), ex);
        }
    }

    private Optional<Constructor<?>> getPublicConstructorWithShortestParameterList(final Class<?> extensionClass) {
        return Stream.of(extensionClass.getConstructors())
                .min(Comparator.comparing(Constructor::getParameterCount));
    }

    private Object[] nullParameters(final Constructor<?> constructor) {
        return new Object[constructor.getParameterCount()];
    }

    private String nameOf(final ArchbasePlugin archbasePlugin) {
        return nonNull(archbasePlugin)
                ? archbasePlugin.getWrapper().getPluginId()
                : "system";
    }

    private <T> String nameOf(final Class<T> clazz) {
        return clazz.getName();
    }
}
