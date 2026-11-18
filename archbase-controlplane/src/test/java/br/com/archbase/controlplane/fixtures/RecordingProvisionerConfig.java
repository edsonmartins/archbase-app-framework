package br.com.archbase.controlplane.fixtures;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class RecordingProvisionerConfig {

    @Bean
    public RecordingProvisioner recordingProvisioner() {
        return new RecordingProvisioner();
    }
}
