package br.com.archbase.shared.kernel.time;

import lombok.*;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;

/**
 * Objeto de valor simples para representar intervalos de tempo. Observe que se os terminais estão incluídos
 * ou não pode variar entre os métodos oferecidos.
 */
@Value
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public final class Interval {

    /**
     * A data de início do {@link Interval}.
     */
    LocalDateTime start;

    /**
     * A data de término do {@link Interval}.
     */
    LocalDateTime end;

    /**
     * Cria um novo {@link Interval} entre o início e o fim fornecidos.
     *
     * @param start não deve ser {@literal null}.
     * @param end   não deve ser {@literal null}.
     */
    private Interval(LocalDateTime start, LocalDateTime end) {

        Assert.notNull(start, "O início não deve ser nulo!");
        Assert.notNull(end, "O fim não deve ser nulo!");
        Assert.isTrue(start.isBefore(end) || start.isEqual(end), "O início deve ser anterior ou igual ao final!");

        this.start = start;
        this.end = end;
    }

    /**
     * Começa a construir um novo {@link Interval} com o horário de início fornecido.
     *
     * @param start não deve ser {@literal null}.
     * @return nunca será {@literal null}.
     */
    public static IntervalBuilder from(LocalDateTime start) {
        return new IntervalBuilder(start);
    }

    /**
     * Retorna a duração do intervalo, com o final excluído.
     *
     * @return nunca será {@literal null}.
     */
    public Duration getDuration() {
        return Duration.between(start, end);
    }

    /**
     * Retorna se o {@link LocalDateTime} fornecido está contido no {@link Interval} atual.
     * A comparação inclui início e fim, ou seja, o método trata esse intervalo como fechado.
     *
     * @param reference não deve ser {@literal null}.
     * @return
     */
    public boolean contains(LocalDateTime reference) {

        Assert.notNull(reference, "A referência não deve ser nula!");

        boolean afterOrOnStart = start.isBefore(reference) || start.isEqual(reference);
        boolean beforeOrOnEnd = end.isAfter(reference) || end.isEqual(reference);

        return afterOrOnStart && beforeOrOnEnd;
    }

    /**
     * Retorna se o {@link Interval} atual se sobrepõe ao fornecido. o
     * a comparação exclui o início e o fim, ou seja, o método trata ambos os intervalos como abertos.
     *
     * @param reference não deve ser {@literal null}.
     * @return
     */
    public boolean overlaps(Interval reference) {

        Assert.notNull(reference, "A referência não deve ser nula!");

        boolean endsAfterOtherStarts = getEnd().isAfter(reference.getStart());
        boolean startsBeforeOtherEnds = getStart().isBefore(reference.getEnd());

        return startsBeforeOtherEnds && endsAfterOtherStarts;
    }

    /**
     * Retorna a {@link Duration} representada por determinado {@link Interval}, com o
     * fim excluído.
     *
     * @return
     */
    public Duration toDuration() {
        return Duration.between(start, end);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("Intervalo de %s até %s", start, end);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class IntervalBuilder {
        private final @NonNull LocalDateTime from;

        /**
         * Cria um {@link Interval} da hora de início atual até a hora de término fornecida.
         *
         * @param end não deve ser {@literal null} e posterior à hora de início atual ou igual a ela.
         * @return nunca será {@literal null}.
         */
        public Interval to(LocalDateTime end) {
            return new Interval(from, end);
        }

        /**
         * Cria um novo {@link Interval} a partir do horário de início atual adicionando o {@link TemporalAmount} fornecido a ele.
         * <p>
         * valor @param não deve ser {@literal null}.
         *
         * @return nunca será {@literal null}.
         * @see Duration
         */
        public Interval withLength(TemporalAmount amount) {
            Assert.notNull(amount, "A quantidade temporal não deve ser nula!");
            return new Interval(from, from.plus(amount));
        }
    }
}
