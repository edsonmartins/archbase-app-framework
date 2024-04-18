
# Archbase-Starter-Multitenancy

O `archbase-starter-multitenancy` oferece uma solução pronta para aplicações que necessitam suportar múltiplos inquilinos, gerenciando isolamento de dados e contextos de tenancy de forma eficiente. Este starter é automaticamente carregado se o `archbase-starter` estiver incluído no projeto.

## Funcionalidades Incluídas
- Configuração automática para isolamento de tenancy.
- Suporte a estratégias de isolamento como banco de dados separados, esquemas separados ou filtragem por coluna.

## Configuração e Uso Básico

Para ativar o multitenancy em seu projeto, adicione o starter:

```xml
<dependency>
    <groupId>com.archbase</groupId>
    <artifactId>archbase-starter-multitenancy</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Configurando o Identificador de Tenancy

Você pode configurar como o tenant é identificado em cada requisição:

```java
@Bean
public TenantResolver tenantResolver() {
    return new HeaderTenantResolver();
}
```
