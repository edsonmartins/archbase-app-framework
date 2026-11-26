package br.com.archbase.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArchbaseMapper")
class ArchbaseMapperTest {

    public static class Cliente {
        public String nome;

        public Cliente() {
        }
    }

    public static class ClienteDto {
        public String nome;

        public ClienteDto() {
        }
    }

    public interface ClienteMapper extends ArchbaseMapper<Cliente, ClienteDto> {
    }

    /** Como o MapStruct gera: a classe implementa a interface do usuário, não a ArchbaseMapper diretamente. */
    public static class ClienteMapperImpl implements ClienteMapper {
        @Override
        public ClienteDto toDto(Cliente source) {
            ClienteDto dto = new ClienteDto();
            dto.nome = source.nome;
            return dto;
        }

        @Override
        public Cliente toEntity(ClienteDto dto) {
            Cliente c = new Cliente();
            c.nome = dto.nome;
            return c;
        }
    }

    @Test
    @DisplayName("classes de origem e destino resolvidas na implementação gerada (antes falhava sempre)")
    void tiposNaImplementacaoGerada() {
        ClienteMapper mapper = new ClienteMapperImpl();
        assertThat(mapper.getSourceClass()).isEqualTo(Cliente.class);
        assertThat(mapper.getTargetClass()).isEqualTo(ClienteDto.class);
        assertThat(mapper.createDto()).isInstanceOf(ClienteDto.class);
        assertThat(mapper.createEntity()).isInstanceOf(Cliente.class);
    }

    @Test
    @DisplayName("a auto-configuração registra o MapperConfiguration (antes não registrava nada)")
    void autoConfiguracao() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ArchbaseMapperAutoConfiguration.class))
                .run(ctx -> assertThat(ctx).hasSingleBean(ArchbaseMapperAutoConfiguration.MapperConfiguration.class));
    }
}
