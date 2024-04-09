package br.com.archbase.resource.logger.aspect;

import br.com.archbase.resource.logger.utils.JsonUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author edsonmartins
 */
public interface ArchbaseResourceAspect {

    /**
     * Este ponto de corte é para o ponto de junção correspondente a todos os métodos públicos no controlador
     */
    void allPublicControllerMethodsPointcut();

    void methodOrClassLoggingEnabledPointcut();

    void methodLoggingNotDisabledPointcut();

    @Nullable
    Object log(@Nonnull ProceedingJoinPoint proceedingJoinPoint);

    /**
     * Registra os dados seguintes no nível INFO sobre o método de execução -
     * <ul>
     * <li>Nome do método</li>
     * <li>Par de nome-valor de argumento de método</li>
     * <li>Solicite detalhes, incluindo referenciador, método HTTP, URI e nome de usuário</li>
     * </ul>
     *
     * @param proceedingJoinPoint o objeto joinpoint que representa o método alvo
     */
    void logPreExecutionData(
            @Nonnull ProceedingJoinPoint proceedingJoinPoint,
            @Nullable RequestMapping methodRequestMapping);

    /**
     * Registra os dados seguintes no nível INFO sobre o método executado -
     * <ul>
     * <li>Tempo de execução do método em milissegundos</li>
     * </ul>
     * <p>
     * Registra os dados seguintes no nível DEBUG sobre o método executado -
     * <ul>
     * <li>Representação JSON do objeto retornado pelo método</li>
     * </ul>
     *
     * @param proceedingJoinPoint o ponto comum denotando o método executado
     * @param timer               {@link StopWatch} objeto contendo o tempo de execução do método
     * @param result              o objeto retornado pelo método executado
     * @param returnType          nome da classe do objeto retornado pelo método executado
     */
    void logPostExecutionData(
            @Nonnull ProceedingJoinPoint proceedingJoinPoint,
            @Nonnull StopWatch timer,
            @Nullable Object result,
            @Nonnull String returnType,
            @Nullable RequestMapping methodRequestMapping,
            @Nullable RequestMapping classRequestMapping);

    /**
     * Registra qualquer exceção lançada por método. Este aspecto é executado <b>APÓS</b> a exceção ter sido lançada, então
     * não posso engoli-lo aqui.
     */
    void onException(@Nonnull JoinPoint joinPoint, @Nonnull Throwable t);

    /**
     * Converte determinado objeto em sua representação JSON por meio de {@link JsonUtil}. O JSON serializado para, em seguida, anexado a
     * instância {@link StringBuilder} passada.
     *
     * <p>
     * Alguns casos excepcionais -
     * <ol>
     * <li>Para objetos do tipo de arquivo, o tamanho do arquivo em bytes é impresso.</li>
     * <li>Objetos simulados não são serializados. Em vez disso, uma mensagem é impressa indicando que o objeto é uma simulação
     * objeto. Objetos simulados são detectados pela presença de substring 'mock' em seu nome de classe.</li>
     * </ol>
     *
     * @param object       o objeto para serializar
     * @param objClassName nome da classe do objeto.
     * @param logMessage   {@link StringBuilder} instância para anexar JSON serializado.
     */
    void serialize(@Nullable Object object, @Nonnull String objClassName, @Nonnull StringBuilder logMessage);

}
