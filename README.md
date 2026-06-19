# Archbase Framework

[![Maven Central](https://img.shields.io/maven-central/v/br.com.archbase/archbase-starter.svg?style=flat-square)](https://central.sonatype.com/search?q=g:br.com.archbase)
[![Java](https://img.shields.io/badge/Java-25-orange.svg?style=flat-square)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen.svg?style=flat-square)](https://spring.io/projects/spring-boot)

**Archbase** é um framework Java para aplicações empresariais construído sobre Spring Boot 4.1.0 e Java 25. Implementa Domain-Driven Design (DDD) com Onion Architecture, oferecendo uma base sólida e modular para aplicações complexas.

> **Versão 3.0.0** - Veja as [notas de release](CHANGELOG.md) para informações sobre atualizações.

## Características

- **Domain-Driven Design** - Suporte completo a entidades, agregados, value objects e repositórios
- **Multi-tenancy** - Isolamento de tenant com múltiplas estratégias de resolução
- **Segurança** - JWT com controle de permissões baseado em anotações
- **Event-Driven** - CQRS com buses separados para Commands, Queries e Events
- **Query Flexível** - Suporte a RSQL para filtros dinâmicos
- **Validação** - Framework de validação com fluent interface
- **Workflow Engine** - Orquestração de processos de negócio
- **Plugin System** - Carregamento dinâmico de extensões

## Quick Start

### Adicione a dependência

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter</artifactId>
    <version>3.0.0</version>
</dependency>
```

Ou use módulos específicos:

```xml
<!-- Para apenas DDD básico -->
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter-core</artifactId>
    <version>3.0.0</version>
</dependency>

<!-- Para segurança -->
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter-security</artifactId>
    <version>3.0.0</version>
</dependency>

<!-- Para multi-tenancy -->
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter-multitenancy</artifactId>
    <version>3.0.0</version>
</dependency>
```

### Configure a aplicação

```yaml
archbase:
  security:
    enabled: true
    jwt:
      secret-key: sua-chave-base64-minimo-32-bytes
      token-expiration: 86400000  # 24 horas
      refresh-expiration: 604800000  # 7 dias

  multitenancy:
    enabled: true
    scan-packages: com.suaempresa

  rsql:
    enabled: true
    case-insensitive: true
```

### Crie uma entidade

```java
@Entity
@DomainEntity
public class Cliente extends PersistenceEntityBase<Cliente, UUID> {

    private String nome;
    private String email;

    @Override
    public ValidationResult validate() {
        return new Cliente.Validator().validate(this);
    }

    static class Validator extends AbstractArchbaseValidator<Cliente> {
        @Override
        public void rules() {
            ruleFor(Cliente::getNome)
                .must(not(stringEmptyOrNull()))
                .withMessage("Nome é obrigatório");

            ruleFor(Cliente::getEmail)
                .must(emailAddress())
                .withMessage("Email inválido");
        }
    }
}
```

### Crie um repositório

```java
@DomainRepository
public interface ClienteRepository
    extends Repository<Cliente, UUID, Long> {
    // CRUD automático disponível
}
```

### Crie um controller

```java
@RestController
@RequestMapping("/api/clientes")
@HasPermission(action = "VIEW", resource = "CLIENTE")
public class ClienteController {

    @Autowired
    private ClienteRepository repository;

    @GetMapping
    public Page<Cliente> buscar(
            @RequestParam(required = false) String filter,
            Pageable pageable) {

        ArchbaseSpecification<Cliente> spec = Specification.where(null);
        if (filter != null) {
            spec = ArchbaseRSQLJPASupport.toSpecification(filter, Cliente.class);
        }

        return repository.findAll(spec, pageable);
    }

    @PostMapping
    @HasPermission(action = "CREATE", resource = "CLIENTE")
    public Cliente criar(@RequestBody @Valid Cliente cliente) {
        return repository.save(cliente);
    }
}
```

## Módulos

| Módulo | Descrição |
|--------|-----------|
| `archbase-starter` | Starter completo com todos os módulos |
| `archbase-starter-core` | Configuração core (RSQL, Jackson, Swagger) |
| `archbase-starter-security` | Autenticação JWT e controle de permissões |
| `archbase-starter-multitenancy` | Suporte a multi-tenancy |
| `archbase-domain-driven-design` | Implementação base de DDD |
| `archbase-domain-driven-design-spec` | Padrão Specification para queries |
| `archbase-query` | Query handling e suporte RSQL |
| `archbase-security` | `@HasPermission` e autenticação programática |
| `archbase-multitenancy` | Isolamento de tenant e contexto |
| `archbase-event-driven` | CQRS com Command/Query/Event buses |
| `archbase-workflow-process` | Motor de workflow |
| `archbase-plugin-manager` | Carregamento dinâmico de plugins |
| `archbase-validation` | Framework de validação |
| `archbase-error-handling` | Tratamento centralizado de erros |

## Arquitetura

O framework segue **Hexagonal Architecture** (Ports & Adapters) com separação clara de responsabilidades:

![Arquitetura Hexagonal](docs/hexagonal-architecture.svg)

## Recursos Avançados

### Multi-tenancy

```java
@Entity
@DomainEntity
public class Produto extends TenantPersistenceEntityBase<Produto, UUID> {
    // Tenant ID é adicionado automaticamente
}
```

### CQRS com Event Bus

```java
@Configuration
@HandlerScan(basePackages = "com.minhaempresa.handlers")
public class HandlerConfig {
}

@Component
public class CriarProdutoHandler {
    @CommandHandler
    public UUID handle(CriarProdutoCommand command) {
        // Lógica do comando
        return id;
    }

    @EventHandler
    public void on(ProdutoCriadoEvent event) {
        // Reage ao evento
    }
}
```

### Autenticação Programática

```java
@Autowired
private SystemUserContext systemUserContext;

public void processarJob() {
    systemUserContext.runAsSystemUser("system@empresa.com", () -> {
        // Executa como usuário do sistema
    });
}
```

### Workflow Engine

```java
Workflow workflow = Workflow.builder()
    .id("pedido-workflow")
    .step("validar")
    .step("processar-pagamento")
    .step("enviar-confirmacao")
    .build();

WorkflowEngine executor = new WorkflowEngine(workflow);
executor.execute(context);
```

### Console de Desenvolvimento (BootUI)

Todo projeto que usa o Archbase já inclui automaticamente o [BootUI](https://github.com/jdubois/boot-ui),
um console de desenvolvimento embarcado. Ele vem transitivamente via `archbase-starter-core`, portanto
**não é necessário declarar nenhuma dependência adicional**.

O console expõe mais de 20 painéis (health, métricas, beans, configurações, loggers, tasks agendadas,
repositórios, cache, segurança, traces, log streaming etc.) acessíveis em:

```
http://localhost:8080/bootui
```

Por segurança, o BootUI:

- ativa-se apenas em modo `AUTO` — com os profiles `dev`/`local` ativos ou com o Spring Boot DevTools no
  classpath. Em `prod`/`production` ele se desativa automaticamente;
- rejeita requisições que não venham de `localhost` (loopback);
- mascara valores sensíveis de configuração.

Em aplicações com `archbase-security`, o caminho `/bootui/**` já está liberado na whitelist padrão.

Para rodar com o console habilitado:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Controle explícito via propriedades (opcional):

```properties
bootui.enabled=AUTO            # AUTO | ON | OFF
bootui.enabled-profiles=dev,local
bootui.disabled-profiles=prod,production
bootui.expose-values=MASKED    # MASKED | METADATA_ONLY | FULL
bootui.read-only=false
```

## Documentação

Para documentação completa e detalhada, visite:

- [Guia do Usuário](https://docs.archbase.com.br)
- [API Reference](https://javadoc.io/doc/br.com.archbase/archbase-starter)
- [Exemplos](https://github.com/edsonmartins/archbase-examples)

## Requisitos

- Java 17 ou superior
- Spring Boot 3.5.6
- Maven 3.6+

## Licença

Copyright © 2024 Relevant Solutions

## Links Úteis

- [Site Oficial](https://www.archbase.com.br)
- [Documentação](https://docs.archbase.com.br)
- [Maven Central](https://central.sonatype.com/search?q=g:br.com.archbase)
- [GitHub](https://github.com/edsonmartins/archbase-app-framework)

## Versão 1.x

Para a versão 1.x do framework, use a branch [V1](https://github.com/edsonmartins/archbase-app-framework/tree/V1).
