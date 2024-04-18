
# Archbase-Starter-Core

O `archbase-starter-core` é projetado para fornecer todas as configurações essenciais e infraestrutura comuns para projetos baseados em Spring Boot. Ele inclui internacionalização, configurações de serialização JSON, e suporte extensivo para JPA.

## Funcionalidades Incluídas
- **RSQL**: Suporte para RSQL, permitindo filtragem e busca avançada através de URLs.
- **Spring MVC**: Configuração básica do Spring MVC com serialização e deserialização personalizada.
- **Bean Validation**: Integração com Hibernate Validator para validação de beans.
- **Locale**: Configuração padrão do locale para `pt-BR`, facilitando a internacionalização.
- **Jackson Configuration**: Customizações no ObjectMapper para melhor tratamento de JSON, incluindo módulos como Hibernate5Module para lidar com entidades Hibernate.
- **Swagger**: Configuração pronta para uso do Swagger para documentação da API.
- **TaskExecutor**: Configuração de um `TaskExecutor` para gerenciamento eficiente de tarefas assíncronas.

## Personalização e Exclusão de Módulos

Os starters da Archbase foram projetados para serem flexíveis, permitindo aos desenvolvedores excluir módulos específicos ou desabilitar funcionalidades conforme necessário.

### Desabilitando Módulos via Propriedades

Para desabilitar um módulo específico, você pode utilizar a propriedade `enabled` no seu arquivo `application.properties` ou `application.yml`. Por exemplo, para desabilitar o módulo de multitenancy:

```properties
archbase.multitenancy.enabled=false
```

### Sobrepor Configurações

Se você deseja sobrepor qualquer configuração padrão ou bean fornecido pelo starter, você pode declarar seu próprio bean com a mesma assinatura no seu contexto de aplicação. Spring Boot usará sua declaração de bean em vez da fornecida pelo starter, graças ao mecanismo de `@ConditionalOnMissingBean`.

#### Exemplo de Sobreposição

Para substituir o `LocaleResolver` padrão que define o locale como `pt-BR`, você pode definir o seguinte bean:

```java
@Bean
public LocaleResolver localeResolver() {
    SessionLocaleResolver resolver = new SessionLocaleResolver();
    resolver.setDefaultLocale(new Locale("en", "US")); // Definir para inglês americano
    return resolver;
}
```

## Configuração e Uso Básico

Inclua o `archbase-starter-core` no seu projeto para ativar automaticamente as configurações:

```xml
<dependency>
    <groupId>com.archbase</groupId>
    <artifactId>archbase-starter-core</artifactId>
    <version>1.0.0</version>
</dependency>
```
Configurações adicionais podem ser feitas através de `application.properties` para ajustar o comportamento padrão das funcionalidades incluídas.

### Configuração do Swagger

Para customizar a configuração do Swagger, ajuste as seguintes propriedades:

```properties
archbase.swagger.title=Minha API
archbase.swagger.description=Descrição detalhada da API
archbase.swagger.version=v1
archbase.swagger.terms-of-service-url=http://examplo.com/terms
archbase.swagger.contact.name=Suporte Técnico
archbase.swagger.contact.url=http://examplo.com/contact
archbase.swagger.contact.email=suporte@examplo.com
```

Estas propriedades permitem definir os detalhes da sua API que serão exibidos na interface do Swagger.
