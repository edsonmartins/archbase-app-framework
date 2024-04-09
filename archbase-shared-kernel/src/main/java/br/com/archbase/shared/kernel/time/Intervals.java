package br.com.archbase.shared.kernel.time;

import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.*;

/**
 * Objeto de valor para representar uma lista de {@link Interval}s.
 */
public class Intervals implements Streamable<Interval> {

    private final Iterable<Interval> intervalsList;

    /**
     * Cria uma nova instância de {@link Intervals} com todos os {@link Intervals}s de determinada duração entre o início dado
     * e data de término.
     *
     * @param start não deve ser {@literal null}.
     * @param end   não deve ser {@literal null}.
     *              A duração de @param não deve ser {@literal null}.
     */
    private Intervals(LocalDateTime start, LocalDateTime end, TemporalAmount duration) {

        Assert.notNull(start, "A data de início não deve ser nula!");
        Assert.notNull(end, "Data de término não deve ser nula!");
        Assert.notNull(duration, "A duração não deve ser nula!");

        this.intervalsList = getIntervals(start, end, duration);
    }

    /**
     * Divide o determinado {@link Interval} em intervalos menores de determinada duração.
     *
     * @param interval não deve ser {@literal null}.
     *                 A duração de @param não deve ser {@literal null}.
     * @return
     */
    public static Intervals divide(Interval interval, TemporalAmount duration) {

        Assert.notNull(interval, "O intervalo não deve ser nulo!");
        Assert.notNull(duration, "A duração não deve ser nula!");

        return new Intervals(interval.getStart(), interval.getEnd(), duration);
    }

    /**
     * Cria recursivamente todos os {@link Interval}s de determinada duração entre as datas de início e término fornecidas.
     *
     * @param start não deve ser {@literal null}.
     * @param end   não deve ser {@literal null}.
     *              A duração de @param não deve ser {@literal null}.
     * @return
     */
    private static Collection<Interval> getIntervals(LocalDateTime start, LocalDateTime end, TemporalAmount duration) {

        LocalDateTime target = start.plus(duration);
        if (!target.isBefore(end)) {
            return Collections.singleton(Interval.from(start).to(end));
        }
        List<Interval> intervals = new ArrayList<>();
        intervals.add(Interval.from(start).to(target));
        intervals.addAll(getIntervals(target, end, duration));
        return Collections.unmodifiableList(intervals);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Interval> iterator() {
        return intervalsList.iterator();
    }
}
