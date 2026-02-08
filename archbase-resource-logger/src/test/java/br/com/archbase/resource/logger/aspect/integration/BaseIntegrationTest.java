package br.com.archbase.resource.logger.aspect.integration;

import br.com.archbase.resource.logger.aspect.integration.spring_boot_application.ApiSecurityConfig;
import br.com.archbase.resource.logger.aspect.integration.spring_boot_application.BeanConfig;
import br.com.archbase.resource.logger.aspect.integration.spring_boot_application.ControllerLoggerConsumerApplication;
import br.com.archbase.resource.logger.aspect.integration.spring_boot_application.UserResource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = {
                ApiSecurityConfig.class,
                UserResource.class,
                BeanConfig.class,
                ControllerLoggerConsumerApplication.class
        })
@AutoConfigureMockMvc
@EnableConfigurationProperties
abstract class BaseIntegrationTest {
}


