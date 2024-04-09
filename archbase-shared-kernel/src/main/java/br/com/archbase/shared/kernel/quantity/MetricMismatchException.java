package br.com.archbase.shared.kernel.quantity;

/**
 * @author edsonmartins
 */
@SuppressWarnings("serial")
public class MetricMismatchException extends RuntimeException {

    public MetricMismatchException(String text) {
        super(text);
    }

    public MetricMismatchException(Metric m1, Metric m2) {
        super("Métrica 1 (" + m1.name() + ") não corresponde à métrica 2 (" + m2.name() + ")");
    }

    public MetricMismatchException(String text, Metric m1, Metric m2) {
        super(text + "\n" + "Métrica 1 (" + m1.name() + ") não corresponde à métrica 2 (" + m2.name() + ")");
    }
}
