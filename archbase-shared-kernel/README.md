# archbase-shared-kernel

Utilitários e tipos comuns usados por outros módulos do Archbase: conversores JPA (`AttributeConverter`), tipos de
quantidade e tempo, constantes monetárias, utilitários de reflexão e validação e um validador de regras de negócio.
É só biblioteca: não tem auto-configuração nem propriedade `archbase.*`.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-shared-kernel</artifactId>
    <version>3.5.0</version>
</dependency>
```

Raramente é preciso declará-lo: vem transitivamente por `archbase-multitenancy`, `archbase-security`,
`archbase-validation` e `archbase-architecture-rules` (e, por eles, pelos starters).

## O que traz

`archbase-domain-driven-design-spec`, `jakarta.persistence-api`, `jakarta.validation-api`, `hibernate-envers`,
`money-api` + `moneta-core` (JSR 354), `commons-lang3`, `commons-io`, `spring-boot`, `spring-context`,
`spring-data-commons` e três artefatos do JavaParser (`javaparser-core`, `javaparser-symbol-solver-core`,
`javaparser-core-serialization`). Nenhuma classe do módulo usa o JavaParser.

## O que entra no contexto do Spring

Nada, por conta própria. Há quatro classes anotadas como componente, mas nenhum starter varre
`br.com.archbase.shared`. Elas só viram bean se a aplicação incluir o pacote na varredura (por exemplo,
`archbase.app.component.scan=br.com.archbase.shared.kernel`, do `archbase-starter-core`, ou `@ComponentScan`):

| Classe | Anotação | Observação |
|---|---|---|
| `quantity.QuantityFormatter` | `@Component` | `Formatter<Quantity>` (pacote `quantity`) |
| `converters.StringToQuantityConverter` | `@Component`, `@ConfigurationPropertiesBinding` | Usa o `QuantityFormatter`; classe não pública |
| `time.DefaultBusinessTime` | `@Service` | Implementa `BusinessTime`; classe não pública |
| `converters.AttributeEncryptor` | `@Component` | Lê `aes.encryption.key` com `@Value` sem padrão: varrido sem a propriedade, a aplicação não sobe |

## Conversores JPA

Pacote `br.com.archbase.shared.kernel.converters`. Os marcados com `autoApply = true` só valem automaticamente se o
pacote estiver no *entity scan* da unidade de persistência (`@EntityScan` ou `archbase.app.jpa.entities`); os
starters não o incluem. Fora disso, use `@Convert(converter = ...)` no campo.

| Conversor | Tipo Java → coluna | `autoApply` |
|---|---|---|
| `BooleanToSNConverter` | `Boolean` → `"S"`/`"N"` (`null` vira `"N"`) | não |
| `DayOfWeekConverter` | `DayOfWeek` → `Integer` (1–7) | sim |
| `MonthConverter` | `java.time.Month` → `Integer` | sim |
| `YearConverter` | `java.time.Year` → `Integer` | sim |
| `YearMonthConverter` | `YearMonth` → `String` (ISO) | sim |
| `PeriodConverter` | `Period` → `String` (ISO) | sim |
| `DurationToStringConverter` | `Duration` → `String` (ISO) | sim |
| `DurationToNanoConverter` | `Duration` → `Long` (nanos) | sem `@Converter`; só com `@Convert` |
| `InstantConverter` | `Instant` → `Timestamp` | sim |
| `ZonedDateTimeConverter` | `ZonedDateTime` → `Timestamp`; na leitura usa o fuso da JVM | sim |
| `ZoneIdConverter` | `ZoneId` → `String` | sim |
| `LocalTimeStringConverter` | `LocalTime` → `String` `HH:mm` | sim |
| `MonetaryAmountAttributeConverter` | `MonetaryAmount` → `String` (`toString()` do Moneta) | sim |
| `DescriptionAttributeConverter` | `types.Description` → `String` | sim |
| `QuantityAttributeConverter` | `types.Quantity` → `Double` | sim |
| `AttributeEncryptor` | `String` → AES, Base64 | não (não tem `@Converter`) |
| `StringEncryptDecryptConverter` | `String` → AES/CBC, Base64 | não |
| `LocalDateEncryptDecryptConverter` | `LocalDate` → AES/CBC, Base64 | não |

Comportamentos que podem surpreender:

- `LocalTimeStringConverter` grava só hora e minuto (segundos se perdem) e lança `NullPointerException` ao gravar
  `null`. Na leitura, valor que não se converte vira `null`, sem erro.
- Com `autoApply`, `InstantConverter` e `ZonedDateTimeConverter` substituem o mapeamento nativo do Hibernate para
  esses tipos em todas as entidades da unidade de persistência.

> **Não use os conversores de criptografia para dado sensível.** `StringEncryptDecryptConverter` e
> `LocalDateEncryptDecryptConverter` cifram com uma chave fixa no código-fonte (`"MySuperSecretKey"`) e IV zerado:
> qualquer um com acesso ao código decifra a coluna. `AttributeEncryptor` lê a chave de `aes.encryption.key`
> (os bytes da string, que precisa ter 16, 24 ou 32 caracteres), usa `Cipher.getInstance("AES")` — modo ECB — e
> compartilha um único `Cipher` entre threads. Para segredo em repouso, use outra solução.

## Tipos

### `br.com.archbase.shared.kernel.quantity`

`Quantity` (`@Embeddable`, valor `BigDecimal` + `Metric`): `of(long)`, `of(double)` (métrica `UNIT`),
`of(..., Metric)`, `add`, `subtract`, `times`, comparações (`isLessThan`, `isGreaterThan`, ...). Operar métricas
incompatíveis lança `MetricMismatchException`. `Metric`: `SQUARE_METER` (`m²`, `m2`), `METER`, `KILOGRAM`, `LITER`,
`UNIT`. `QuantityFormatter` imprime e lê número seguido da abreviatura da métrica, com o número no formato do `Locale` (`2,5kg` em pt-BR).

### `br.com.archbase.shared.kernel.time`

- `Interval`: `Interval.from(inicio).to(fim)` ou `.withLength(duracao)`; `contains`, `overlaps`, `getDuration`.
- `Intervals.divide(interval, duracao)`: divide em fatias (`Streamable<Interval>`).
- `BusinessTime`: relógio que pode ser adiantado (`forward`, `reset`) para teste e simulação. A implementação
  (`DefaultBusinessTime`) guarda o deslocamento num campo comum, compartilhado por todo o contexto.

### `br.com.archbase.shared.kernel.money`

`Currencies.REAL`, `Currencies.DOLLAR`, `Currencies.ZERO_REAL`, `Currencies.ZERO_DOLLAR` (JSR 354).

### `br.com.archbase.shared.kernel.types`

Tipos antigos, mantidos por compatibilidade. Implementam `br.com.archbase.ddd.domain.contracts.ValueObject` e
levam `@DomainValueObject`, mas **não** são `@Embeddable` e não têm conversor (salvo `Quantity` e `Description`).

| Tipo | O que realmente valida |
|---|---|
| `CPF` | `of(s)` confere os dígitos verificadores e lança `IllegalArgumentException` se forem inválidos; `isValido()` confere sem lançar. `instance(s)` e `setInternalCpf` só tiram `.`, `-` e espaço, sem conferir |
| `CNPJ` | `of(s)` confere os dígitos verificadores (numérico ou alfanumérico); `isValido()` confere sem lançar. `instance(s)` e `setInternalCnpj` tiram a máscara, passam letras para maiúsculas e exigem 14 caracteres, sem conferir os dígitos |
| `Email` | Nada |
| `CreditCard` | Nada |
| `PhoneNumber` | Aceita dígitos, espaço, `(`, `)`, `-` e `.`; exige ao menos um dígito |
| `Quantity` | — (`BigDecimal`; `add`/`subtract` passam por `double` e perdem precisão; sem `equals`) |
| `Description` | — (`Description.of(texto)`) |
| `CardType` | Enum de bandeiras com `detect(numero)` e `isLuhnValid(numero)` |

`DayOfMonth`, `Month`, `Year`, `WeekOfMonth` e `WeekOfYear` são interfaces vazias.

`instance` não passou a conferir os dígitos para não recusar valores que já eram aceitos. Até a 3.5.0,
`CPF.instance` guardava o texto com máscara (e `getCpfFormadado()` devolvia `null`), e `CNPJ.instance` recusava CNPJ
formatado.

Para e-mail e telefone com validação, e CPF/CNPJ como `@Embeddable`, veja o
[`archbase-value-objects`](../archbase-value-objects/README.md) ou os validadores do
[`archbase-validation`](../archbase-validation/README.md).

### Validação de regras de negócio (`br.com.archbase.shared.ddd`)

```java
import br.com.archbase.shared.ddd.validation.BusinessRuleValidator;
import br.com.archbase.shared.ddd.validation.ValidationResult;

ValidationResult r = BusinessRuleValidator.create()
        .rule(pedido.getItens().size() > 0, "Pedido sem itens")
        .rule(() -> pedido.getTotal().signum() >= 0, "Total negativo")
        .validate();

BusinessRuleValidator.create()
        .rule(cliente.isAtivo(), "Cliente inativo")
        .validateOrThrow();   // BusinessRuleValidator.ValidationException com a lista de erros
```

- As condições são avaliadas na hora em que `rule(...)` é chamado, inclusive a versão com `BooleanSupplier`.
- `ruleOrThrow(condicao, supplier)` lança a exceção do supplier imediatamente.
- `ValidationResult` (`success`, `failure`, `combine`, `withError`) deste pacote **não** é o
  `br.com.archbase.ddd.domain.contracts.ValidationResult` que `DomainEntityBase.validate()` devolve.
- `BaseValueObject` é uma classe abstrata com `validate()` (padrão: sucesso), `isValid()` e `validateOrThrow()`.
- `@ValidateBusinessRules` não tem efeito: nenhum aspecto ou processador do framework lê essa anotação.

## Utilitários (`br.com.archbase.shared.kernel.utils` e `bean.metadata`)

Usados internamente pelo `archbase-validation` e por outros módulos; a API é pública, mas não é estável.

- `ReflectionUtils`, `ObjectUtils`, `ArrayUtils`, `ArchbaseAssert`, `Annotations`.
- `EMailValidationUtils.isValid(valor)`, `InetAddressValidator`, `RegexValidator`.
- `ModUtil`: `calculateLuhnMod10Check`, `calculateMod10Check`, `calculateMod11Check`.
- `bean.metadata`: cópia de `java.beans` (`Introspector`, `BeanInfo`, `PropertyDescriptor`, ...).
- `annotations.Label`: rótulo de campo, lido pelo `ArchbaseMessageInterpolator` do `archbase-validation`.

## Limitações

- O módulo não tem testes automatizados.
- Os tipos de `types` não validam o que o nome promete (ver tabela acima).
- Os conversores de criptografia não protegem o dado (ver nota acima).

## Referência

- Objetos de valor com validação: [archbase-value-objects](../archbase-value-objects/README.md)
- Contratos DDD: [archbase-domain-driven-design-spec](../archbase-domain-driven-design-spec/README.md)
- Varredura e entity scan dos starters: [archbase-starter-core](../archbase-starter-core/README.md#a-varredura-de-pacotes)
- Validação com `@Label`: [archbase-validation](../archbase-validation/README.md)
