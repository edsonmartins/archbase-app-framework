package br.com.archbase.ddd.infraestructure.persistence.jpa.specification;

/**
 * Fornece lógica de construção para instâncias de {@link SpecificationTranslator}. o
 * construção de um tradutor pode ser bastante difícil, pois há vários conversores
 * a serem registrados. Ao fornecer uma fábrica, os usuários podem permanecer alheios a esta construção.
 *
 * @author edsonmartins
 */
public interface SpecificationTranslatorFactory {

    /**
     * Construa um novo {@link SpecificationTranslator} com conversores padrão.
     *
     * @return nova instância do tradutor com todos os conversores padrão
     */
    public default SpecificationTranslator createWithDefaultConverters() {
        SpecificationTranslator translator = new SpecificationTranslatorImpl();
        translator.registerConverter(new EqualityConverter());
        translator.registerConverter(new GreaterThanConverter());
        translator.registerConverter(new LessThanConverter());
        translator.registerConverter(new NotConverter(translator));
        translator.registerConverter(new AndConverter(translator));
        translator.registerConverter(new OrConverter(translator));
        translator.registerConverter(new LessThanOrEqualConverter());
        translator.registerConverter(new GreaterThanOrEqualConverter());
        translator.registerConverter(new BetweenConverter());
        translator.registerConverter(new InConverter());
        translator.registerConverter(new NotInConverter());
        translator.registerConverter(new LikeConverter());
        translator.registerConverter(new NotLikeConverter());
        return translator;
    }

    /**
     * Construa um novo {@link SpecificationTranslator} com conversores padrão e
     * instâncias de conversor anotadas. Use este método para registrar rapidamente todos os
     * conversores, minimizando o processo de configuração do nosso tradutor.
     *
     * @param basePackage o pacote básico de nossos conversores personalizados
     * @return instância de Translator com todos os conversores padrão e anotados
     */
    public SpecificationTranslator createWithAnnotatedConverters(String basePackage);

}
