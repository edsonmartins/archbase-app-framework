package br.com.archbase.event.driven.bus.autoscan;


import br.com.archbase.ddd.domain.annotations.DomainCommand;
import br.com.archbase.event.driven.spec.annotations.CommandMapping;
import br.com.archbase.event.driven.spec.annotations.CommandMappings;
import br.com.archbase.event.driven.spec.annotations.QueryMapping;
import br.com.archbase.event.driven.spec.annotations.QueryMappings;
import br.com.archbase.event.driven.spec.command.contracts.Command;
import br.com.archbase.event.driven.spec.command.contracts.CommandHandler;
import br.com.archbase.event.driven.spec.command.contracts.CommandHandlerFactory;
import br.com.archbase.event.driven.spec.query.contracts.Query;
import br.com.archbase.event.driven.spec.query.contracts.QueryHandler;
import br.com.archbase.event.driven.spec.query.contracts.QueryHandlerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings("all")
public class AutoScanHandlerFactory implements QueryHandlerFactory, CommandHandlerFactory {
    private static final Log log = LogFactory.getLog(AutoScanHandlerFactory.class);

    private Map<String, Class<? extends QueryHandler>> handlerClassByQueryNameMap = new HashMap<>();
    private Map<String, Class<? extends CommandHandler>> handlerClassByCommandNameMap = new HashMap<>();

    private BeanFactory beanFactory;

    public AutoScanHandlerFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public void scanAndRegisterHandlers(String packageToScan) {
        log.info("Verificando manipuladores de consulta e comando no pacote: " + packageToScan);
        Reflections reflections = new Reflections(packageToScan);
        Set<Class<? extends QueryHandler>> queryClasses = reflections.getSubTypesOf(QueryHandler.class);
        Set<Class<? extends CommandHandler>> commandClasses = reflections.getSubTypesOf(CommandHandler.class);

        queryClasses.forEach(queryClass -> {
            QueryMappings multiMappingAnnotation = queryClass.getAnnotation(QueryMappings.class);
            QueryMapping mappingAnnotation = queryClass.getAnnotation(QueryMapping.class);
            if (multiMappingAnnotation != null) {
                List<QueryMapping> queryMappings = Arrays.asList(multiMappingAnnotation.value());
                queryMappings.forEach(queryMapping -> {
                    log.info(String.format("Registrando o manipulador %s para lidar com a consulta %s",
                            queryClass.getSimpleName(), queryMapping.value().getName()));
                    handlerClassByQueryNameMap.put(queryMapping.value().getName(), queryClass);
                });
            } else if (mappingAnnotation != null) {
                log.info(String.format("\n" +
                                "Registrando o manipulador %s para lidar com a consulta %s",
                        queryClass.getSimpleName(), mappingAnnotation.value().getName()));
                handlerClassByQueryNameMap.put(mappingAnnotation.value().getName(), queryClass);
            }
        });

        commandClasses.forEach(commandClass -> {
            CommandMappings multiMappingAnnotation = commandClass.getAnnotation(CommandMappings.class);
            CommandMapping mappingAnnotation = commandClass.getAnnotation(CommandMapping.class);
            if (multiMappingAnnotation != null) {
                List<CommandMapping> commandMappings = Arrays.asList(multiMappingAnnotation.value());
                commandMappings.forEach(commandMapping -> {
                    log.info(String.format("Registrando o manipulador %s para manipular o comando %s",
                            commandClass.getSimpleName(), commandMapping.value().getName()));

                    handlerClassByCommandNameMap.put(commandMapping.value().getName(), commandClass);
                });
            } else if (mappingAnnotation != null) {
                log.info(String.format("Registrando o manipulador %s para manipular o comando %s",
                        commandClass.getSimpleName(), mappingAnnotation.value().getName()));

                handlerClassByCommandNameMap.put(mappingAnnotation.value().getName(), commandClass);
            }
            for (Method method : commandClass.getMethods()) {
                if (method.getParameterTypes().length > 0) {
                    Class<?> commandType = method.getParameterTypes()[0];
                    if (commandType.isAnnotationPresent(DomainCommand.class)) {
                        handlerClassByCommandNameMap.put(commandType.getName(), commandClass);
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> QueryHandler<Query<R>, R> createQueryHandler(String queryName) {
        Class<? extends QueryHandler> handlerClass = handlerClassByQueryNameMap.get(queryName);
        if (handlerClass == null) {
            return null;
        }

        return beanFactory.createBean(handlerClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> CommandHandler<Command<R>, R> createCommandHandler(String commandName) {
        Class<? extends CommandHandler> handlerClass = handlerClassByCommandNameMap.get(commandName);
        if (handlerClass == null) {
            return null;
        }

        return beanFactory.createBean(handlerClass);
    }
}
