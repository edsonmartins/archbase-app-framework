
# Archbase-Starter

O `archbase-starter` é o módulo base que facilita a integração e configuração de todos os módulos básicos necessários para uma aplicação Spring Boot seguindo as práticas recomendadas da Archbase. Este starter inclui configurações essenciais que são comuns a todas as aplicações.

## Funcionalidades Incluídas
- Configurações básicas de Spring Boot.
- Integração com `archbase-starter-core`, `archbase-starter-security`, e `archbase-starter-multitenancy`.

## Configuração

Este starter não requer configurações específicas, pois serve principalmente como um agregador de outros starters. Ele automaticamente configura seu projeto para usar os módulos Core, Security e Multitenancy, caso sejam incluídos no classpath.

## Uso Básico

Inclua o `archbase-starter` em seu arquivo `pom.xml` para começar a usar:

```xml
<dependency>
    <groupId>com.archbase</groupId>
    <artifactId>archbase-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```
Este starter não necessita de configurações adicionais para uso básico.
