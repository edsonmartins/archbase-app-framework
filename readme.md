<div align="center">

![Archbase Logo](docs/docusaurus/static/img/logo.png)

# Archbase Java

**Framework Java para aplicações corporativas com DDD**

[![Maven Central](https://img.shields.io/badge/Maven%20Central-archbase-blue)](https://central.sonatype.com/artifact/br.com.archbase/archbase-parent)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green.svg)](https://spring.io/projects/spring-boot)

[Documentação](https://archbase-framework.github.io/archbase-app-framework/) •
[Guia de Início](https://archbase-framework.github.io/archbase-app-framework/docs/category/começando) •
[Exemplos](https://github.com/archbase-framework/archbase-java-boilerplate)

</div>

---

## Sobre o Archbase

**Archbase** é um framework Java que facilita o desenvolvimento de aplicações corporativas utilizando os conceitos de **Domain-Driven Design (DDD)**. Os módulos permitem tanto aplicações simples com modelos anêmicos e CRUDs, quanto aplicações complexas com arquitetura DDD bem estruturada.

### Características Principais

| Recurso | Descrição |
|---------|-----------|
| **DDD First** | Construído com padrões Domain-Driven Design desde o início |
| **Multi-tenancy** | Suporte nativo para aplicações multi-tenant |
| **Segurança** | Autenticação JWT com autorização baseada em permissões |
| **Event-Driven** | Arquitetura orientada a eventos com CQRS |
| **Queries Dinâmicas** | Suporte a RSQL para queries flexíveis |
| **Auto-configuração** | Integração transparente com Spring Boot |

### Arquitetura

O framework segue a **Arquitetura Hexagonal** (Ports & Adapters):

![Arquitetura Hexagonal](docs/docusaurus/static/img/hexagonal-architecture.svg)

---

## Quick Start

### 1. Adicione a dependência

**Maven:**
```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter</artifactId>
    <version>${archbase.version}</version>
</dependency>
```

**Gradle:**
```groovy
implementation 'br.com.archbase:archbase-starter:${archbase.version}'
```

### 2. Crie uma Entidade

```java
@Entity
@DomainEntity
public class Cliente extends PersistenceEntityBase<Cliente, UUID> {

    private String nome;
    private String email;

    // getters e setters
}
```

### 3. Crie um Repository

```java
@DomainRepository
public interface ClienteRepository extends Repository<Cliente, UUID, Long> {
    // Queries automáticas disponíveis
}
```

### 4. Configure a aplicação

```yaml
archbase:
  multitenancy:
    enabled: true
  security:
    jwt:
      secret: sua-chave-secreta
      expiration: 86400000
```

---

## Módulos

| Módulo | Descrição |
|--------|-----------|
| `archbase-starter` | Starter completo com todos os módulos |
| `archbase-domain-driven-design` | Base para entidades DDD |
| `archbase-event-driven` | CQRS com Command/Event/Query Bus |
| `archbase-security` | Autenticação JWT e autorização |
| `archbase-multitenancy` | Suporte multi-tenant |
| `archbase-query-rsql` | Queries com RSQL e filtros dinâmicos |
| `archbase-validation` | Validação fluente de regras |
| `archbase-test-utils` | Utilitários para testes |
| `archbase-shared-kernel` | Value Objects compartilhados |
| `archbase-error-handling` | Tratamento centralizado de erros |
| `archbase-logging` | Logging estruturado |
| `archbase-mapper` | Mapeamento entre objetos |

> Para documentação completa de cada módulo, visite a [documentação oficial](https://archbase-framework.github.io/archbase-app-framework/docs/category/módulos).

---

## Começando com Boilerplate

Para facilitar ainda mais o início de um projeto com Archbase, utilize o **archbase-java-boilerplate**:

```bash
# Clone o boilerplate
git clone https://github.com/archbase-framework/archbase-java-boilerplate.git meu-projeto

# Entre na pasta
cd meu-projeto

# Configure suas propriedades
# Editar application.yml com suas configurações

# Execute!
mvn spring-boot:run
```

O boilerplate já vem com:
- ✅ Estrutura de pastas organizada para DDD
- ✅ Exemplos de Entidades, Repositories e Services
- ✅ Configurações de segurança e multi-tenancy
- ✅ Testes de exemplo
- ✅ Docker Compose para banco de dados

---

## Quando usar Archbase?

### Use Archbase para:
- Aplicações corporativas com regras de negócio complexas
- Sistemas que requerem multi-tenancy
- Projetos que precisam de auditoria e segurança avançada
- Equipes que praticam DDD

### Considere alternativas para:
- APIs CRUD simples sem regras de negócio
- Protótipos rápidos ou MVPs
- Microserviços simples

---

## Documentação

A documentação completa do Archbase está disponível em:

**[https://archbase-framework.github.io/archbase-app-framework/](https://archbase-framework.github.io/archbase-app-framework/)**

### Principais Seções

- [Instalação](https://archbase-framework.github.io/archbase-app-framework/docs/getting-started/installation)
- [Quick Start](https://archbase-framework.github.io/archbase-app-framework/docs/getting-started/quick-start)
- [Conceitos DDD](https://archbase-framework.github.io/archbase-app-framework/docs/category/conceitos-ddd)
- [Guias](https://archbase-framework.github.io/archbase-app-framework/docs/category/guias)
- [Módulos](https://archbase-framework.github.io/archbase-app-framework/docs/category/módulos)

---

## Contribuindo

Contribuições são bem-vindas! Por favor, leia nosso [Guia de Contribuição](CONTRIBUTING.md) antes de abrir pull requests.

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/NovaFuncionalidade`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/NovaFuncionalidade`)
5. Abra um Pull Request

---

## Licença

Este projeto está licenciado sob a Licença Apache 2.0 - veja o arquivo [LICENSE](LICENSE) para detalhes.

---

<div align="center">

**[Documentação](https://archbase-framework.github.io/archbase-app-framework/)** •
[GitHub](https://github.com/archbase-framework/archbase-app-framework) •
[Issues](https://github.com/archbase-framework/archbase-app-framework/issues) •
[Discussions](https://github.com/archbase-framework/archbase-app-framework/discussions)

</div>
