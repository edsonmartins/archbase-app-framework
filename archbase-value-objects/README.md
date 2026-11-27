# archbase-value-objects

Cinco objetos de valor brasileiros prontos para usar como `@Embeddable` em entidades JPA: `CPF`, `CNPJ`,
`EmailAddress`, `Telefone` e `Endereco`. É só biblioteca: não tem auto-configuração, bean nem propriedade.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-value-objects</artifactId>
    <version>3.5.0</version>
</dependency>
```

Nenhum starter traz este módulo; declare-o diretamente. Ele traz `archbase-domain-driven-design-spec`,
`jakarta.validation-api`, `jakarta.persistence-api` e o Lombok em escopo `compile` (nenhuma classe do módulo usa
Lombok nem a spec).

Não confunda com os tipos `CPF`, `CNPJ`, `Email` e `PhoneNumber` do [`archbase-shared-kernel`](../archbase-shared-kernel/README.md),
que são outras classes, em outro pacote, com outro comportamento.

## Tipos

Todos implementam `br.com.archbase.valueobject.ValueObject`, interface própria deste módulo que só redeclara
`equals`/`hashCode`. Ela **não** é o `br.com.archbase.ddd.domain.contracts.ValueObject` da spec e não estende
`Serializable`.

| Classe | Pacote | Colunas (`@Column`) | Criação | Igualdade |
|---|---|---|---|---|
| `CPF` | `br.com.archbase.valueobject.document` | `cpf` (14) | `new CPF(s)`, `CPF.of(s)`, `CPF.ofSemValidacao(s)` | número só com dígitos |
| `CNPJ` | `br.com.archbase.valueobject.document` | `cnpj` (18) | `new CNPJ(s)`, `CNPJ.of(s)`, `CNPJ.ofSemValidacao(s)` | número sem máscara (numérico ou alfanumérico) |
| `EmailAddress` | `br.com.archbase.valueobject.contact` | `email` (255) | `new EmailAddress(s)`, `EmailAddress.of(s)`, `EmailAddress.ofSemValidacao(s)` | endereço em minúsculas |
| `Telefone` | `br.com.archbase.valueobject.contact` | `tipo_telefone`, `numero`, `ddd` | construtores, `of(numero, tipo)`, `celular(numero)`, `fixo(numero)` | DDD + número |
| `Endereco` | `br.com.archbase.valueobject.address` | `logradouro`, `numero`, `complemento`, `bairro`, `cidade`, `uf`, `cep`, `pais` | `Endereco.builder()...build()`, `Endereco.deCep(cep)` | todos os campos menos `pais` |

Os construtores protegidos sem argumento existem para o JPA.

### CPF e CNPJ

- O construtor público tira a máscara e exige o formato; senão, `IllegalArgumentException`. Não confere dígito
  verificador.
  - CPF: 11 dígitos (tudo que não é dígito é descartado).
  - CNPJ: 12 posições de letra ou dígito seguidas de 2 dígitos. Tudo que não é letra nem dígito é descartado, e as
    letras passam para maiúsculas.
- `of(s)` faz o mesmo e depois chama `valido()`; se der `false`, lança `IllegalArgumentException`.
- `ofSemValidacao(s)` é igual ao construtor.
- `valido()` confere os dois dígitos verificadores (módulo 11) e recusa sequências repetidas (`111.111.111-11`).
- `numero()` e `toString()` devolvem formatado (`XXX.XXX.XXX-XX`, `XX.XXX.XXX/XXXX-XX`); `numeroLimpo()` sem
  máscara, que é o que vai para o banco.
- **CNPJ alfanumérico** (emitido pela Receita Federal desde julho de 2026) é aceito: `CNPJ.of("12.ABC.345/01DE-35")`.
  Cada caractere vale o código ASCII menos 48 no cálculo; o CNPJ numérico continua igual.

Até a 3.5.0, `valido()` calculava o primeiro dígito com os pesos do segundo e `of(s)` recusava todo documento
válido, e o CNPJ alfanumérico era recusado no construtor. Corrigido; os testes conferem o cálculo contra o
validador do Caelum Stella (o mesmo do `archbase-validation`).

### EmailAddress

O construtor apara, passa para minúsculas e confere contra `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`
(não cobre toda a RFC 5322). `ofSemValidacao` só apara e passa para minúsculas. Métodos: `endereco()`, `usuario()`,
`dominio()`, `isDeDominio(d)`, `isCorporativo()` (falso para gmail, outlook, hotmail, yahoo, icloud e live). É
`Comparable`.

### Telefone

- `new Telefone(numero, tipo)`: número com DDD, 10 ou 11 dígitos depois de tirar o que não é dígito.
- `new Telefone(ddd, numero, tipo)`: DDD de 2 dígitos e número de 8 ou 9.
- `tipo` nulo vira `CELULAR`. Tipos: `CELULAR`, `FIXO`, `COMERCIAL`, `RESIDENCIAL`, `RECADO`, `OUTRO`.
- Não confere se o DDD existe.
- `formatado()` / `toString()`: `(XX) XXXXX-XXXX` ou `(XX) XXXX-XXXX`. `numeroCompleto()`: DDD + número.
- `isCelular()` é verdadeiro se o tipo é `CELULAR` **ou** se o número tem 9 dígitos.
- O campo `tipo` não tem `@Enumerated`: o JPA grava o ordinal do enum, não o nome. Reordenar o enum muda o
  significado do que já está gravado.

### Endereco

Sem validação nenhuma: o builder aceita qualquer combinação, inclusive tudo nulo. `pais` vale `"Brasil"` se não
informado. Métodos: `linhaUnica()`, `cidadeUf()` (`cidade-uf`; com só um dos dois, só ele — até a 3.5.0 saía `null` no texto),
`isCompleto()` (logradouro, número, bairro, cidade, UF e CEP não vazios). O CEP é gravado como veio, sem
normalização.

## Uso

```java
import br.com.archbase.ddd.domain.base.TenantPersistenceEntityBase;
import br.com.archbase.valueobject.address.Endereco;
import br.com.archbase.valueobject.contact.EmailAddress;
import br.com.archbase.valueobject.contact.Telefone;
import br.com.archbase.valueobject.document.CPF;
import jakarta.persistence.*;

@Entity
@Table(name = "CLIENTE")
public class Cliente extends TenantPersistenceEntityBase {

    @Embedded
    private CPF cpf;

    @Embedded
    private EmailAddress email;

    @Embedded
    private Telefone telefone;

    @Embedded
    @AttributeOverride(name = "numero", column = @Column(name = "endereco_numero", length = 20))
    private Endereco endereco;

    protected Cliente() {
    }

    public Cliente(String cpf, String email, String celular, Endereco endereco) {
        this.cpf = CPF.of(cpf);
        this.email = EmailAddress.of(email);
        this.telefone = Telefone.celular(celular);
        this.endereco = endereco;
    }
}
```

`Telefone` e `Endereco` usam a mesma coluna `numero`: na mesma entidade, um dos dois precisa de
`@AttributeOverride`, como acima. O mesmo vale para dois embutidos do mesmo tipo (por exemplo, dois `Telefone`).

```java
Endereco e = Endereco.builder()
        .logradouro("Rua das Flores").numero("100").bairro("Centro")
        .cidade("Curitiba").uf("PR").cep("80000-000")
        .build();
e.linhaUnica();   // "Rua das Flores, 100, Centro"
e.cidadeUf();     // "Curitiba-PR"
```

## Limitações

- Sem teste automatizado no módulo.
- Sem conversor Jackson nem Bean Validation próprio (`@CPF`, `@CNPJ`): a validação está nos construtores e
  fábricas. As anotações `@NotNull`/`@NotBlank`/`@Size` nos campos valem só quando a entidade for validada.

## Referência

- Tipos equivalentes com outro comportamento: [archbase-shared-kernel](../archbase-shared-kernel/README.md)
- Contratos DDD (`ValueObject`, `AggregateRoot`): [archbase-domain-driven-design-spec](../archbase-domain-driven-design-spec/README.md)
- Visão geral do framework: [CLAUDE.md](../CLAUDE.md)
