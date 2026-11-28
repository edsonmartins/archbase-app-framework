package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

/**
 * Tradutor com os conversores padrão (igualdade, comparação, {@code like}, {@code in}, {@code between} e os
 * combinadores {@code and}/{@code or}/{@code not}), compartilhado pelos repositórios.
 *
 * <p>Até a 3.5.0 os repositórios só recebiam um tradutor por {@code setTranslator}, que nada no framework chamava:
 * {@code matching}, {@code howMany} e {@code containsAny} lançavam {@code NullPointerException} sempre. O tradutor não
 * guarda estado de consulta, só o mapa de conversores, montado uma vez.
 */
public final class TradutorPadrao {

    private TradutorPadrao() {
    }

    public static SpecificationTranslator instancia() {
        return Holder.INSTANCIA;
    }

    private static final class Holder {
        private static final SpecificationTranslator INSTANCIA = new SpecificationTranslatorFactory() {
            @Override
            public SpecificationTranslator createWithAnnotatedConverters(String basePackage) {
                throw new UnsupportedOperationException();
            }
        }.createWithDefaultConverters();
    }
}
