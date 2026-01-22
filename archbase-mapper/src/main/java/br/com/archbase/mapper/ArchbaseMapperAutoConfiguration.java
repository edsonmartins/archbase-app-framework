package br.com.archbase.mapper;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuração para o módulo Archbase Mapper.
 * <p>
 * Escaneia automaticamente interfaces que estendem {@link ArchbaseMapper}.
 */
@AutoConfiguration
@ComponentScan(basePackages = "br.com.archbase.mapper")
public class ArchbaseMapperAutoConfiguration {

    /**
     * Bean de configuração para mappers customizados.
     * Pode ser extendido por projetos para adicionar configurações específicas.
     */
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
