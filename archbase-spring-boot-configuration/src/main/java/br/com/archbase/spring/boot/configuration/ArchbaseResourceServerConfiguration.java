package br.com.archbase.spring.boot.configuration;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;


@Configuration
@EnableResourceServer
@EntityScan(basePackages = {"#{'${archbase.app.scan.entities}'.split(',')}"})
@PropertySource(value = "classpath:application.properties")
public class ArchbaseResourceServerConfiguration extends ResourceServerConfigurerAdapter {

    @Value("${archbase.app.resource.id}")
    private String resourceId;

    @Value("${archbase.app.secured.pattern}")
    private String securedPattern;

    @Value("${archbase.app.url.endpoint.check.token}")
    private String urlEndpointCheckToken;

    @Value("${archbase.app.client.id}")
    private String clientIdCheckToken;

    @Value("${archbase.app.client.secret}")
    private String clientSecret;


    @Override
    public void configure(ResourceServerSecurityConfigurer resources) {
        ResourceServerTokenServices tokenServices = getResourceServerTokenServicesToAuthentication();
        if (tokenServices != null) {
            resources.tokenServices(tokenServices);
        }
        resources.resourceId(getResourceId());
    }


    public String getResourceId() {
        return resourceId;
    }


    public String getSecuredPattern() {
        return securedPattern;
    }


    public ResourceServerTokenServices getResourceServerTokenServicesToAuthentication() {
        final RemoteTokenServices tokenServices = new RemoteTokenServices();
        tokenServices.setCheckTokenEndpointUrl(urlEndpointCheckToken);
        tokenServices.setClientId(clientIdCheckToken);
        tokenServices.setClientSecret(clientSecret);
        return tokenServices;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        ((ExpressionUrlAuthorizationConfigurer.AuthorizedUrl)http.authorizeRequests().anyRequest()).authenticated();


    }

}