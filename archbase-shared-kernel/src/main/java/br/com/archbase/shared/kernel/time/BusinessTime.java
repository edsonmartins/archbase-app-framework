package br.com.archbase.shared.kernel.time;

import java.time.Duration;
import java.time.LocalDateTime;


/**
 * Componente para permitir acesso ao horário comercial atual. Geralmente retorna a hora atual do sistema, mas permite
 * encaminhá-lo manualmente por uma determinada {@link Duration} para fins de teste e simulação.
 */
public interface BusinessTime {

    /**
     * Retorna o horário comercial atual. Esta será a hora do sistema em que o aplicativo está sendo executado por padrão, mas
     * pode ser ajustado chamando {@link #forward (Duration)}.
     *
     * @return
     */
    LocalDateTime getTime();

    /**
     * Avança a hora atual com a {@link Duration} fornecida. Chamar o método várias vezes irá se acumular
     * durações.
     *
     * @param duration
     */
    void forward(Duration duration);

    /**
     * Retorna o deslocamento atual entre o tempo real e o virtual criado pela chamada de {@link #forward (Duration)}.
     *
     * @return
     */
    Duration getOffset();

    /**
     * Desfaz qualquer encaminhamento. Depois disso, qualquer chamada para {@link #getTime()}
     * será novamente equivalente ao horário do sistema.
     */
    void reset();
}
