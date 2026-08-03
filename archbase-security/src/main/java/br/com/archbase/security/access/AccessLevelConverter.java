package br.com.archbase.security.access;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Lê as colunas de nível pelo {@link AccessLevel#parse}, e não por
 * {@code @Enumerated(EnumType.STRING)}.
 *
 * <p>Mesma razão de {@link PermissionEffectConverter}: um rótulo inesperado na coluna não pode
 * derrubar a autorização de quem depende daquela linha. {@code parse} devolve {@code null} para o
 * desconhecido — ou seja, <b>sem piso</b> para uma capacidade e "não declarado" para um perfil,
 * que cai no padrão configurado. Nunca um degrau arbitrário: nem o mais alto, que abriria acesso,
 * nem o mais baixo, que o fecharia em silêncio.
 */
@Converter(autoApply = false)
public class AccessLevelConverter implements AttributeConverter<AccessLevel, String> {

    @Override
    public String convertToDatabaseColumn(AccessLevel atributo) {
        return atributo == null ? null : atributo.name();
    }

    @Override
    public AccessLevel convertToEntityAttribute(String coluna) {
        return coluna == null ? null : AccessLevel.parse(coluna);
    }
}
