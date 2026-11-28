package br.com.archbase.mapper;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuração do módulo Archbase Mapper.
 *
 * <p>Registra só o {@link MapperConfiguration}, ponto de extensão para configurações comuns aos mappers. Não procura
 * mappers: um mapper do MapStruct com {@code componentModel = "spring"} é um {@code @Component} e entra pela varredura
 * da própria aplicação.
 *
 * <p>Até a 3.5.0 o método abaixo não tinha {@code @Bean} (nada era registrado) e um {@code @ComponentScan} do próprio
 * pacote não encontrava nada além desta classe, apesar do javadoc dizer que escaneava os mappers.
 */
@AutoConfiguration
public class ArchbaseMapperAutoConfiguration {

    /**
     * Bean de configuração para mappers customizados.
     * Pode ser extendido por projetos para adicionar configurações específicas.
     */
    @Bean
    @ConditionalOnMissingBean
    public MapperConfiguration mapperConfiguration() {
        return new MapperConfiguration();
    }

    /**
     * Configuração base para mappers.
     */
    public static class MapperConfiguration {
        // Configurações podem ser adicionadas aqui
    }
}
