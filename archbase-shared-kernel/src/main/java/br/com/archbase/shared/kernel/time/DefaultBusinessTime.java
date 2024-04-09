package br.com.archbase.shared.kernel.time;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Uma implementação mutável de {@link BusinessTime} para registrar {@link Duration} s para calcular o horário comercial atual
 * acumulando-os.
 */
@Service
class DefaultBusinessTime implements BusinessTime {

    private Duration duration = Duration.ZERO;

    /*
     * (non-Javadoc)
     * @see br.com.archbase.shared.kernel.time.BusinessTime#getTime()
     */
    @Override
    public LocalDateTime getTime() {
        return LocalDateTime.now().plus(duration);
    }

    /*
     * (non-Javadoc)
     * @see br.com.archbase.shared.kernel.time.BusinessTime#forward(java.time.Duration)
     */
    @Override
    public void forward(Duration duration) {
        this.duration = this.duration.plus(duration);
    }

    /*
     * (non-Javadoc)
     * @see br.com.archbase.shared.kernel.time.BusinessTime#getOffset()
     */
    @Override
    public Duration getOffset() {
        return this.duration;
    }

    /*
     * (non-Javadoc)
     * @see br.com.archbase.shared.kernel.time.BusinessTime#reset()
     */
    @Override
    public void reset() {
        this.duration = Duration.ZERO;
    }
}
