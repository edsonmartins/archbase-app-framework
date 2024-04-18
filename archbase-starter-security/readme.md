
# Archbase-Starter-Security

O `archbase-starter-security` facilita a implementação de robustas funcionalidades de segurança em aplicações Spring Boot, fornecendo configurações pré-definidas para autenticação, autorização e controle de acesso. Este starter é automaticamente carregado se o `archbase-starter` estiver incluído no projeto.

## Funcionalidades Incluídas
- Configurações pré-definidas para uso de JWT para autenticação.
- Suporte a autorizações complexas baseadas em roles e permissões.
- Integração fácil com diferentes fontes de dados de usuários.

## Configuração e Uso Básico

Para utilizar o `archbase-starter-security`, adicione-o ao seu projeto:

```xml
<dependency>
    <groupId>com.archbase</groupId>
    <artifactId>archbase-starter-security</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Customização de Segurança

O starter permite extensa customização para se adaptar às necessidades específicas do seu projeto.

### Exemplo de Customização de Autenticação

Para customizar a autenticação, você pode fornecer sua própria implementação do `AuthenticationProvider`:

```java
@Bean
public AuthenticationProvider customAuthenticationProvider() {
    return new CustomAuthenticationProvider();
}
```

### Desabilitando Segurança

Para desabilitar a segurança em desenvolvimento, você pode usar a propriedade:

```properties
archbase.security.enabled=false
```
