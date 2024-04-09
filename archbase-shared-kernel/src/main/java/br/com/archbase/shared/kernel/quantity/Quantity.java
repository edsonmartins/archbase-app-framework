package br.com.archbase.shared.kernel.quantity;

import lombok.*;
import org.springframework.util.Assert;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;


/**
 * Um objeto de valor para representar uma quantidade.
 */
@Embeddable
@EqualsAndHashCode
@Access(AccessType.PROPERTY)
public class Quantity {

    public Quantity(@NonNull BigDecimal amount, @NonNull Metric metric) {
        this.amount = amount;
        this.metric = metric;
    }

    public static final Quantity NONE = Quantity.of(0);
    private static final String INCOMPATIBLE = "Quantidade %s é incompatível com quantidade %s!";

    /**
     * O valor da Quantidade. Defina explicitamente um nome de coluna com prefixo para evitar conflitos de nome.
     */
    @Getter
    @Column(name = "quantity_amount")
    private @NonNull BigDecimal amount;

    /**
     * A métrica da Quantidade. Defina explicitamente um nome de coluna com prefixo para evitar conflitos de nome.
     */
    @Getter
    @Column(name = "quantity_metric")
    private @NonNull Metric metric;

    /**
     * Cria uma nova {@link Quantity} do valor fornecido. Padroniza a métrica para {@value Metric#UNIT}.
     *
     * @param amount não deve ser {@literal null}.
     * @return
     */
    public static Quantity of(long amount) {
        return of(amount, Metric.UNIT);
    }

    /**
     * Cria uma nova {@link Quantity} do valor fornecido. Padroniza a métrica para {@value Metric#UNIT}.
     *
     * @param amount não deve ser {@literal null}.
     * @return
     */
    public static Quantity of(double amount) {
        return of(amount, Metric.UNIT);
    }

    /**
     * Cria uma nova {@link Quantity} do valor e {@link Metric} fornecidos.
     *
     * @param amount não deve ser {@literal null}.
     * @param metric não deve ser {@literal null}.
     * @return
     */
    public static Quantity of(long amount, Metric metric) {
        return new Quantity(BigDecimal.valueOf(amount), metric);
    }

    /**
     * Cria uma nova {@link Quantity} do valor e {@link Metric} fornecidos.
     *
     * @param amount não deve ser {@literal null}.
     * @param metric não deve ser {@literal null}.
     * @return
     */
    public static Quantity of(double amount, Metric metric) {
        return new Quantity(BigDecimal.valueOf(amount), metric);
    }

    /**
     * Cria uma nova {@link Quantity} do valor e {@link Metric} fornecidos.
     *
     * @param amount não deve ser {@literal null}.
     * @param metric não deve ser {@literal null}.
     * @return
     */
    static Quantity of(BigDecimal amount, Metric metric) {
        return new Quantity(amount, metric);
    }

    /**
     * Retorna se a {@link Quantity} é compatível com a {@link Metric} fornecida.
     *
     * @param metric não deve ser {@literal null}.
     * @return
     */
    public boolean isCompatibleWith(Metric metric) {

        if (this == Quantity.NONE) {
            return true;
        }

        Assert.notNull(metric, "A métrica não deve ser nula!");
        return this.metric.isCompatibleWith(metric);
    }

    /**
     * Adiciona a {@link Quantity} fornecida à atual.
     *
     * @param other a {@link Quantity} a ser adicionada. A {@link Metric} dada {@link Quantity} deve ser compatível com a
     *              atual.
     * @return
     * @see #isCompatibleWith(Metric)
     */
    public Quantity add(Quantity other) {

        if (this == NONE) {
            return other;
        }

        if (other == NONE) {
            return this;
        }

        assertCompatibility(other);

        return new Quantity(this.amount.add(other.amount), this.metric);
    }

    /**
     * Subtrai a quantidade fornecida da atual.
     *
     * @param other a {@link Quantity} a ser adicionada. A {@link Metric} dada {@link Quantity} deve ser compatível com a
     *              atual.
     * @return
     * @see #isCompatibleWith(Metric)
     */
    public Quantity subtract(Quantity other) {

        if (this == NONE) {
            return other;
        }

        if (other == NONE) {
            return this;
        }

        assertCompatibility(other);

        return new Quantity(this.amount.subtract(other.amount), this.metric);
    }

    /**
     * Retorna se a {@link Quantity} fornecida é menor que a atual.
     *
     * @param other não deve ser {@literal null}. A {@link Metric} dada {@link Quantity} deve ser compatível com a
     *              atual.
     * @return
     * @see #isCompatibleWith(Metric)
     */
    public boolean isLessThan(Quantity other) {

        assertCompatibility(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * Cria um novo {@link Quantity} do atual multiplicado pelo int fornecido.
     *
     * @param multiplier
     * @return nunca será {@literal null}.
     */
    public Quantity times(int multiplier) {
        return times((long) multiplier);
    }

    /**
     * Cria um novo {@link Quantity} do atual multiplicado pelo long fornecido.
     *
     * @param multiplier
     * @return nunca será {@literal null}.
     */
    public Quantity times(long multiplier) {
        return new Quantity(amount.multiply(BigDecimal.valueOf(multiplier)), metric);
    }

    /**
     * Retorna se a {@link Quantity} atual é igual à fornecida, negando diferenças potenciais em
     * precisão do valor subjacente. Ou seja um valor de 1 é considerado igual a um valor de 1,0.
     *
     * @param other não deve ser {@literal null}.
     * @return
     */
    public boolean isEqualTo(Quantity other) {

        Assert.notNull(other, "A quantidade não deve ser nula!");

        return metric.isCompatibleWith(other.metric) //
                && this.amount.compareTo(other.amount) == 0;
    }

    /**
     * Retorna se a {@link Quantity} fornecida é maior que a atual.
     *
     * @param other não deve ser {@literal null}. A {@link Metric} dada {@link Quantity} deve ser compatível com a
     *              atual.
     * @return
     * @see #isCompatibleWith(Metric)
     */
    public boolean isGreaterThan(Quantity other) {

        assertCompatibility(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * Retorna se a {@link Quantity} fornecida é maior ou igual à atual.
     *
     * @param other não deve ser {@literal null}. A {@link Metric} dada {@link Quantity} deve ser compatível com a
     *              atual.
     * @return
     * @see #isCompatibleWith(Metric)
     */
    public boolean isGreaterThanOrEqualTo(Quantity other) {

        assertCompatibility(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    /**
     * Retorna se a {@link Quantity} atual for negativa.
     *
     * @return
     */
    @Transient
    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Retorna se a {@link Quantity} atual é zero ou negativa.
     *
     * @return
     */
    @Transient
    public boolean isZeroOrNegative() {
        return !isGreaterThan(toZero());
    }

    /**
     * Retorna um novo {@link Quantity} de zero com a {@link Metric} do atual.
     *
     * @return nunca será {@literal null}.
     */
    public Quantity toZero() {
        return Quantity.of(0, metric);
    }

    private void assertCompatibility(Quantity quantity) {

        Assert.notNull(quantity, "A quantidade não deve ser nula!");

        if (this == NONE || quantity == NONE) {
            return;
        }

        if (!isCompatibleWith(quantity.metric)) {
            throw new MetricMismatchException(String.format(INCOMPATIBLE, this, quantity), metric, quantity.metric);
        }
    }

    // Ajustes para suportar adequadamente Quantidades baseadas em long para métrica UNIT

    void setMetric(Metric metric) {

        this.metric = metric;

        if (amount != null && Metric.UNIT == metric) {
            this.amount = BigDecimal.valueOf(amount.longValue());
        }
    }

    void setAmount(BigDecimal amount) {

        this.amount = amount;

        if (Metric.UNIT == this.metric) {
            this.amount = BigDecimal.valueOf(amount.longValue());
        }
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        DecimalFormat format = new DecimalFormat();
        format.setMinimumFractionDigits(this.amount.scale());

        return format.format(this.amount).concat(this.metric.getAbbreviation());
    }
}
