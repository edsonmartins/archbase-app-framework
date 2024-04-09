package br.com.archbase.resource.logger.aspect;

import br.com.archbase.resource.logger.annotation.Logging;
import br.com.archbase.resource.logger.annotation.NoLogging;
import lombok.SneakyThrows;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.lang.annotation.Annotation;

import static br.com.archbase.resource.logger.utils.JsonUtil.toJson;
import static br.com.archbase.resource.logger.utils.RequestUtil.getRequestContext;

//@formatter:off

/**
 * Esta classe é responsável por realizar o login nos métodos do Controlador.
 * Ele usou Spring AspectJ (não Spring CGLib AOP nativo) para entrelaçar a lógica de registro em métodos de controlador correspondentes.
 *
 * <p>Este aspecto usa duas anotações - {@link Logging} e {@link NoLogging} para obter controle de granulação fina sobre
 * comportamento de registro do método.
 *
 * @author edsonmartins
 * @see <a href="http://docs.spring.io/spring/docs/current/spring-framework-reference/html/aop.html">
 * Documentação do Spring sobre programação orientada a aspectos com o Spring
 * </a>
 */
//@formatter:on

@Aspect
public class SimpleArchbaseResourceAspect extends ArchbaseLoggerAspect implements ArchbaseResourceAspect {

    @Nonnull
    private Logger log;

    public SimpleArchbaseResourceAspect() {
        this(
                org.slf4j.LoggerFactory.getLogger(String.class)
        );
    }

    public SimpleArchbaseResourceAspect(
            @Nonnull Logger log) {
        this.log = log;
    }

    @Pointcut("@annotation(br.com.archbase.resource.logger.annotation.Logging) " +
            "|| @target(br.com.archbase.resource.logger.annotation.Logging)")
    public void methodOrClassLoggingEnabledPointcut() {
        //
    }

    @Pointcut("!@annotation(br.com.archbase.resource.logger.annotation.NoLogging)")
    public void methodLoggingNotDisabledPointcut() {
        //
    }

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *) ||" +
            "within(@org.springframework.stereotype.Controller *)")
    public void allPublicControllerMethodsPointcut() {
        //
    }


    @SneakyThrows
    @Around("allPublicControllerMethodsPointcut() "
            + "&& methodLoggingNotDisabledPointcut() "
            + "&& methodOrClassLoggingEnabledPointcut()")
    @Nullable
    public Object log(@Nonnull ProceedingJoinPoint proceedingJoinPoint) {
        Object result = null;
        String returnType = null;
        RequestMapping methodRequestMapping = null;
        RequestMapping classRequestMapping = null;

        try {
            MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
            methodRequestMapping = methodSignature.getMethod().getAnnotation(RequestMapping.class);
            classRequestMapping = proceedingJoinPoint.getTarget().getClass().getAnnotation(RequestMapping.class);

            // isso é necessário para distinguir entre um valor retornado de nulo e nenhum valor de retorno, como no caso de
            // tipo de retorno nulo.
            returnType = methodSignature.getReturnType().getName();

            logPreExecutionData(proceedingJoinPoint, methodRequestMapping);
        } catch (Exception e) {
            log.error("Ocorreu uma exceção na lógica de pré-procedimento", e);
        }

        StopWatch timer = new StopWatch();
        try {
            timer.start();
            result = proceedingJoinPoint.proceed();
        } finally {
            timer.stop();
            if (returnType != null) {
                logPostExecutionData(
                        proceedingJoinPoint, timer, result, returnType, methodRequestMapping, classRequestMapping
                );
            }
        }

        return result;
    }

    public void logPreExecutionData(
            @Nonnull ProceedingJoinPoint proceedingJoinPoint,
            @Nullable RequestMapping methodRequestMapping) {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();

        String methodName = methodSignature.getName() + "()";
        Object[] argValues = proceedingJoinPoint.getArgs();
        String[] argNames = methodSignature.getParameterNames();
        String requestContext = getRequestContext().toString();
        Annotation[][] annotations = methodSignature.getMethod().getParameterAnnotations();

        StringBuilder preMessage = new StringBuilder().append(methodName);

        if (argValues.length > 0) {
            logFunctionArguments(argNames, argValues, preMessage, annotations, methodRequestMapping);
        }

        String message = preMessage.append(" chamado via ").append(requestContext).toString();
        log.info(message);
    }

    @SuppressWarnings("java:S3776")
    public void logPostExecutionData(
            @Nonnull ProceedingJoinPoint proceedingJoinPoint,
            @Nonnull StopWatch timer,
            @Nullable Object result,
            @Nonnull String returnType,
            @Nullable RequestMapping methodRequestMapping,
            @Nullable RequestMapping classRequestMapping) {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        String methodName = methodSignature.getName() + "()";
        String message = methodName.concat(" levou [").concat(timer.getTotalTimeMillis() + "").concat(" ms] para concluir");
        log.info(message);

        boolean needsSerialization = false;

        String[] produces = methodRequestMapping != null ? methodRequestMapping.produces() : new String[0];
        for (String produce : produces) {
            if (produce.equals(MediaType.APPLICATION_JSON_VALUE)) {
                needsSerialization = true;
                break;
            }
        }

        if (!needsSerialization) {
            produces = classRequestMapping != null ? classRequestMapping.produces() : new String[0];
            for (String produce : produces) {
                if (produce.equals(MediaType.APPLICATION_JSON_VALUE)) {
                    needsSerialization = true;
                    break;
                }
            }
        }

        StringBuilder postMessage = new StringBuilder().append(methodName).append(" retornou: [");

        if (needsSerialization) {
            String resultClassName = result == null ? "null" : result.getClass().getName();
            resultClassName = returnType.equals("java.lang.Void") ? returnType : resultClassName;

            // TODO talvez possamos tentar usar toString() quando a serialização falhar?
            serialize(result, resultClassName, postMessage);
        } else {
            postMessage.append(result);
        }
        postMessage.append("]");
        String res = postMessage.toString();
        log.info(res);
    }

    @AfterThrowing(
            pointcut = "allPublicControllerMethodsPointcut() && "
                    + "methodLoggingNotDisabledPointcut() && "
                    + "methodOrClassLoggingEnabledPointcut()",
            throwing = "t")
    public void onException(@Nonnull JoinPoint joinPoint, @Nonnull Throwable t) {
        String methodName = joinPoint.getSignature().getName() + "()";
        String message = methodName.concat(" lançou exceção: [").concat(t + "]");
        log.info(message);
    }

    public void serialize(@Nullable Object object, @Nonnull String objClassName, @Nonnull StringBuilder logMessage) {
        boolean serializedSuccessfully = false;
        Exception exception = null;

        // isso serve para distinguir entre métodos que retornam valor nulo e métodos que retornam void.
        // Object arg é nulo em ambos os casos, mas objClassName não é.
        if (objClassName.equals("java.lang.Void")) {
            logMessage.append("void");
            serializedSuccessfully = true;
        }

        // tente serializar assumindo um objeto perfeitamente serializável.
        if (!serializedSuccessfully) {
            try {
                logMessage.append(toJson(object));
                serializedSuccessfully = true;
            } catch (Exception e) {
                exception = e;
            }
        }

        // tente obter o tamanho do arquivo assumindo que o objeto é um objeto do tipo de arquivo
        if (!serializedSuccessfully) {
            long fileSize = -1;

            if (object instanceof ByteArrayResource) {
                fileSize = ((ByteArrayResource) object).contentLength();
            } else if (object instanceof MultipartFile) {
                fileSize = ((MultipartFile) object).getSize();
            }

            if (fileSize != -1) {
                logMessage.append("tamanho do arquivo:[").append(fileSize).append(" B]");
                serializedSuccessfully = true;
            }
        }

        // detect if its a mock object.
        if (!serializedSuccessfully && objClassName.toLowerCase().contains("mock")) {
            logMessage.append("Mock Object");
            serializedSuccessfully = true;
        }

        if (!serializedSuccessfully) {
            String message = "Incapaz de serializar o objeto do tipo [".concat(objClassName).concat("] para registro");
            log.warn(message, exception);
        }
    }

    /**
     * Par nome-valor gerado de argumentos formais do método. Acrescenta a string gerada no StringBuilder fornecido
     *
     * @param argNames      String[] contendo os nomes dos argumentos formais do método A ordem dos nomes deve corresponder à ordem no arg
     *                      valores em argValues.
     * @param argValues     String[] contendo os valores dos argumentos formais do método. A ordem dos valores deve corresponder à ordem em
     *                      nomes de arg em argNames.
     * @param stringBuilder o StringBuilder ao qual vai anexar dados de argumento.
     */
    @SuppressWarnings("java:S3776")
    private void logFunctionArguments(
            @Nonnull String[] argNames,
            @Nonnull Object[] argValues,
            @Nonnull StringBuilder stringBuilder,
            @Nonnull Annotation[][] annotations,
            @Nullable RequestMapping methodRequestMapping) {
        boolean someArgNeedsSerialization = false;

        if (methodRequestMapping != null) {
            for (String consumes : methodRequestMapping.consumes()) {
                if (consumes.equals(MediaType.APPLICATION_JSON_VALUE)) {
                    someArgNeedsSerialization = true;
                    break;
                }
            }
        }

        stringBuilder.append(" chamado com argumentos: ");

        for (int i = 0, length = argNames.length; i < length; ++i) {
            boolean needsSerialization = false;

            if (argValues[i] instanceof ByteArrayResource || argValues[i] instanceof MultipartFile) {
                needsSerialization = true;
            } else {
                if (someArgNeedsSerialization) {
                    // Só precisamos serializar um parâmetro se a anotação @RequestBody for encontrada.
                    for (Annotation annotation : annotations[i]) {
                        if (annotation instanceof RequestBody) {
                            needsSerialization = true;
                            break;
                        }
                    }
                }
            }

            stringBuilder.append(argNames[i]).append(": [");
            if (needsSerialization) {
                String argClassName = argValues[i] == null ? "NULL" : argValues[i].getClass().getName();
                serialize(argValues[i], argClassName, stringBuilder);
            } else {
                stringBuilder.append(getScrubbedValue(argNames[i], argValues[i]));
            }
            stringBuilder.append("]").append(i == (length - 1) ? "" : ", ");
        }
    }

    /**
     * Retorna o valor apagado para um determinado valor de nome de argumento. O valor arg original é retornado
     * se a depuração de dados estiver desativada.
     *
     * @param argName  nome do parâmetro formal
     * @param argValue o valor do parâmetro
     * @return valor depurado de argValue ou valor original se a depuração de dados estiver desativada
     */
    private Object getScrubbedValue(@Nonnull String argName, @Nullable Object argValue) {
        Object argValueToUse = argValue;

        if (enableDataScrubbing && (paramBlacklist.contains(argName.toLowerCase())
                || (paramBlacklistRegex != null && paramBlacklistRegex.matcher(argName).matches()))) {
            argValueToUse = scrubbedValue;
        }

        return argValueToUse;
    }

    public void setLog(@Nonnull Logger log) {
        this.log = log;
    }
}
