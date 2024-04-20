package br.com.archbase.starter.core.auto.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


//@Configuration
//@EnableSwagger2
//@ConditionalOnProperty(name = "archbase.swagger.enabled", havingValue = "true", matchIfMissing = true)
public class ArchbaseSwaggerConfiguration {

    @Value("${archbase.app.swagger.contact.name}")
    private String contactName;

    @Value("${archbase.app.swagger.contact.email}")
    private String contactEmail;

    @Value("${archbase.app.swagger.contact.url}")
    private String contactUrl;

    @Value("${archbase.app.swagger.api.title}")
    private String apiTitle;

    @Value("${archbase.app.swagger.api.description}")
    private String apiDescription;

    @Value("${archbase.app.swagger.api.terms.url}")
    private String apiTermsUrl;

    @Value("${archbase.app.swagger.api.license}")
    private String apiLicense;

    @Value("${archbase.app.swagger.api.license.url}")
    private String apiLicenseUrl;

    @Value("${archbase.app.swagger.api.version}")
    private String apiVersion;

    @Bean
    public Docket postsApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(metaInfo());
    }


    private ApiInfo metaInfo() {
        Contact contact = new Contact(contactName, contactUrl, contactEmail);

        return new ApiInfoBuilder().title(apiTitle)
                .description(apiDescription)
                .termsOfServiceUrl(apiTermsUrl)
                .contact(contact).license(apiLicense)
                .licenseUrl(apiLicenseUrl).version(apiVersion).build();
    }
}