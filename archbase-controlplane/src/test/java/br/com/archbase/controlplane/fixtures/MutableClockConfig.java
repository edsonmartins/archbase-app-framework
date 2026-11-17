package br.com.archbase.controlplane.fixtures;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Instant;

/**
 * Fora do pacote {@code it}, que a aplicação de teste varre: só entra em quem fizer {@code @Import} dele.
 */
@TestConfiguration
public class MutableClockConfig {

    @Bean
    public MutableClock controlPlaneClock() {
        return new MutableClock(Instant.now());
    }
}
