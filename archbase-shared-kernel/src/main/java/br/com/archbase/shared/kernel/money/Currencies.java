package br.com.archbase.shared.kernel.money;

import org.javamoney.moneta.Money;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;

/**
 * Interface para conter constantes {@link CurrencyUnit}.
 */
public class Currencies {

    public static final CurrencyUnit REAL = Monetary.getCurrency("BRL");
    public static final CurrencyUnit DOLLAR = Monetary.getCurrency("USD");
    public static final MonetaryAmount ZERO_REAL = Money.of(0, REAL);
    public static final MonetaryAmount ZERO_DOLLAR = Money.of(0, DOLLAR);
    private Currencies() {
    }
}