package br.com.archbase.security.access;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os conversores que leem {@code EFFECT}, {@code MINIMUM_LEVEL} e {@code ACCESS_LEVEL}.
 *
 * <p><b>Por que não são {@code @Enumerated(EnumType.STRING)}.</b> Aquele mapeamento lança
 * {@code IllegalArgumentException} diante de um valor que não corresponda exatamente a uma
 * constante. Numa coluna de segurança isso é desproporcional: um {@code UPDATE} feito à mão faria
 * toda decisão que tocasse a linha estourar — e o {@code CustomAuthorizationManager} converte
 * exceção em <b>403 para todo mundo que tem aquela permissão</b>, enquanto os endpoints de
 * diagnóstico respondem 500.
 *
 * <p>O cenário é real em produção: o schema entregue pela migration cria as três como
 * {@code varchar} puro, <b>sem</b> check constraint. Só num schema gerado por {@code ddl-auto} o
 * Hibernate acrescenta a checagem — e é por isso que este teste exercita os conversores
 * diretamente, em vez de tentar gravar o valor inválido pelo banco.
 */
@DisplayName("Conversores das colunas do core")
class ConversoresDeColunaTest {

    private final PermissionEffectConverter efeito = new PermissionEffectConverter();
    private final AccessLevelConverter nivel = new AccessLevelConverter();

    @Test
    @DisplayName("efeito desconhecido degrada para GRANT, e nunca para DENY")
    void efeitoDesconhecido() {
        // Um erro de digitação não pode transformar uma concessão legítima numa negação que
        // ninguém pediu — nem derrubar a autorização de quem depende daquela linha.
        assertThat(efeito.convertToEntityAttribute("Deny")).isEqualTo(PermissionEffect.DENY);
        assertThat(efeito.convertToEntityAttribute("  deny  ")).isEqualTo(PermissionEffect.DENY);
        assertThat(efeito.convertToEntityAttribute("NEGAR")).isEqualTo(PermissionEffect.GRANT);
        assertThat(efeito.convertToEntityAttribute("")).isEqualTo(PermissionEffect.GRANT);
        assertThat(efeito.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("nível desconhecido vira ausência de piso, e não um degrau qualquer")
    void nivelDesconhecido() {
        // Nem o mais alto, que abriria acesso, nem o mais baixo, que o fecharia em silêncio.
        assertThat(nivel.convertToEntityAttribute("SUPERVISOR")).isEqualTo(AccessLevel.SUPERVISOR);
        assertThat(nivel.convertToEntityAttribute("  supervisor ")).isEqualTo(AccessLevel.SUPERVISOR);
        assertThat(nivel.convertToEntityAttribute("GERENTE_SENIOR")).isNull();
        assertThat(nivel.convertToEntityAttribute("NONE")).isNull();
        assertThat(nivel.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("a gravação continua canônica — só a leitura é tolerante")
    void gravacaoCanonica() {
        assertThat(efeito.convertToDatabaseColumn(PermissionEffect.DENY)).isEqualTo("DENY");
        assertThat(nivel.convertToDatabaseColumn(AccessLevel.SUPERVISOR)).isEqualTo("SUPERVISOR");
        assertThat(efeito.convertToDatabaseColumn(null)).isNull();
    }
}
