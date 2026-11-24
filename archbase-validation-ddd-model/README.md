# archbase-validation-ddd-model

Regras ArchUnit para o modelo de domínio, na classe `br.com.archbase.validation.ddd.model.ArchbaseRules`: entidade só
dentro do agregado que a declara e referência entre agregados só por id ou associação. É só biblioteca, sem
auto-configuração, bean ou propriedade.

> **Não use este módulo.** Na 3.5.0 as regras não funcionam (ver [Limitações](#limitações)): não selecionam
> campo nenhum e, com a configuração padrão do ArchUnit, falham sempre com "failed to check any classes". A versão
> que funciona é `ArchbaseAggregateRules`, no módulo `archbase-architecture-rules`
> ([fonte](../archbase-architecture-rules/src/main/java/br/com/archbase/architecture/rules/ddd/ArchbaseAggregateRules.java)).
> O módulo continua no build e é descrito aqui para quem já depende dele.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-validation-ddd-model</artifactId>
    <version>3.5.0</version>
    <scope>test</scope>
</dependency>
```

Traz `archbase-domain-driven-design-spec` e `com.tngtech.archunit:archunit` na versão fixada no pom raiz (1.0.1).

## API

| Método | Regra |
|---|---|
| `ArchbaseRules.all()` | As duas abaixo, compostas (`CompositeArchRule`) |
| `ArchbaseRules.entitiesShouldBeDeclaredForUseInSameAggregate()` | Campo cujo tipo é entidade (e não raiz de agregado) deve pertencer ao agregado da classe que o declara |
| `ArchbaseRules.aggregateReferencesShouldBeViaIdOrAssociation()` | Campo cujo tipo é `AggregateRoot` ou anotado com `@DomainAggregateRoot` é violação: use o id ou uma associação |

Uso pretendido, num teste:

```java
JavaClasses classes = new ClassFileImporter().importPackages("com.minhaempresa.dominio");
ArchbaseRules.all().check(classes);
```

## Limitações

Verificado executando as regras contra as classes da `archbase-domain-driven-design-spec`:

1. **Os predicados nunca selecionam nada.** `IsAssignableTypeField.test` e `FieldTypeIsAnnotatedWith.test` devolvem
   `false` fixo; a lógica real ficou num `apply()` que o ArchUnit 1.x não chama. Nenhum campo chega à condição.
2. **Por isso a regra falha sempre.** Com `that()` vazio, o ArchUnit (`archRule.failOnEmptyShould=true`, o padrão)
   lança `AssertionError: ... failed to check any classes`. Com `allowEmptyShould(true)` ou
   `archRule.failOnEmptyShould=false` em `archunit.properties`, passa sempre, sem examinar nada. Em nenhum dos casos
   verifica o modelo.
3. **"Entidade" é a anotação JPA.** A primeira regra testa atribuição a `jakarta.persistence.Entity` (anotação), não
   à interface `br.com.archbase.ddd.domain.contracts.Entity`. Mesmo com os predicados consertados, nenhum campo seria
   atribuível a ela.
4. **ArchUnit 1.0.1 não lê classes de Java 25.** Com a versão que vem do pom raiz, `ClassFileImporter` importa zero
   classes compiladas para Java 25. O `archbase-architecture-rules` declara ArchUnit 1.4.2 por conta própria.

## Referência

- Regras DDD que funcionam: `archbase-architecture-rules`, classes `ArchbaseAggregateRules` e `ArchbaseDddRules`
  ([pacote](../archbase-architecture-rules/src/main/java/br/com/archbase/architecture/rules/ddd/))
- Contratos verificados (`AggregateRoot`, `@DomainAggregateRoot`): [archbase-domain-driven-design-spec](../archbase-domain-driven-design-spec/README.md)
