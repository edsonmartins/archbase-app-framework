package br.com.archbase.security.access;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Lê a coluna {@code EFFECT} pelo {@link PermissionEffect#parse}, e não por
 * {@code @Enumerated(EnumType.STRING)}.
 *
 * <p><b>Por quê.</b> {@code @Enumerated} lança {@code IllegalArgumentException} diante de um valor
 * que não corresponda exatamente a uma constante. Numa coluna de segurança isso é desproporcional:
 * um {@code UPDATE ... SET effect = 'Deny'} feito à mão faria toda decisão que tocasse aquela linha
 * estourar — e o {@code CustomAuthorizationManager} converte exceção em <b>403 para todo mundo que
 * tem aquela permissão</b>, enquanto o diagnóstico responde 500.
 *
 * <p>{@code parse} degrada para {@code GRANT} e tolera caixa e espaço. Isso não afrouxa segurança:
 * o valor desconhecido vira concessão, nunca negação — um erro de digitação não pode transformar
 * uma concessão legítima numa negação que ninguém pediu.
 */
@Converter(autoApply = false)
public class PermissionEffectConverter implements AttributeConverter<PermissionEffect, String> {

    @Override
    public String convertToDatabaseColumn(PermissionEffect atributo) {
        return atributo == null ? null : atributo.name();
    }

    @Override
    public PermissionEffect convertToEntityAttribute(String coluna) {
        return coluna == null ? null : PermissionEffect.parse(coluna);
    }
}
