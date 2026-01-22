# Documentação do Módulo archbase-security

## Visão Geral

O módulo `archbase-security` é um componente fundamental do Archbase Framework, fornecendo uma infraestrutura completa e robusta para autenticação, autorização e gerenciamento de segurança em aplicações empresariais. Este módulo implementa padrões modernos de segurança, incluindo autenticação baseada em JWT, controle de acesso baseado em permissões (RBAC), e suporte completo para arquiteturas multi-tenant.

### Conceitos Fundamentais

#### Domain-Driven Design (DDD)
Todas as entidades de segurança seguem os princípios de Domain-Driven Design, com clara separação entre:
- **Entidades de Persistência**: Classes que representam o modelo de dados (package `persistence`)
- **Entidades de Domínio**: Classes que representam o modelo de negócio (package `domain.entity`)
- **DTOs**: Objetos de transferência de dados para comunicação com clientes (package `domain.dto`)

#### Multi-Tenancy
O sistema é projetado desde o início para suportar múltiplas organizações (tenants) compartilhando a mesma infraestrutura, com isolamento completo de dados. Três níveis de granularidade são suportados:
- **Tenant**: Organização principal
- **Company**: Sub-organização dentro de um tenant
- **Project**: Projeto específico dentro de uma empresa

#### RBAC (Role-Based Access Control)
O controle de acesso é baseado em permissões que conectam entidades de segurança (usuários, grupos, perfis) a ações específicas sobre recursos, com suporte para escopos contextuais.

### Filosofia da Arquitetura

A arquitetura do módulo de segurança segue os seguintes princípios:

1. **Segurança por Padrão**: Todos os endpoints são protegidos por padrão; acesso público deve ser explicitamente declarado
2. **Flexibilidade Contextual**: Permissões podem ser globais ou restritas a contextos específicos (tenant/company/project)
3. **Extensibilidade**: Suporte para customizações via delegates e enrichers
4. **Separação de Responsabilidades**: Clara divisão entre autenticação (quem você é) e autorização (o que você pode fazer)
5. **Auditabilidade**: Todas as entidades mantêm informações de criação e modificação

---

## 📚 Guias de Referência

### Entidades de Segurança

Documentação completa sobre o modelo de dados e entidades do sistema de segurança.

- [Visão Geral das Entidades](entities/overview.md) - Introdução à hierarquia de entidades e estratégia de herança
- [Entidades Core (User, Group, Profile)](entities/core-entities.md) - Usuários, grupos, perfis e controle de acesso temporal
- [Sistema de Permissões (Permission, Resource, Action)](entities/permission-entities.md) - Entidades que implementam o RBAC
- [Gerenciamento de Tokens](entities/token-entities.md) - Access tokens, API tokens e redefinição de senha

### Arquitetura do Sistema

Documentação sobre os fluxos e padrões arquiteturais do módulo de segurança.

- [Sistema de Permissões](architecture/permissions-system.md) - Fluxo de avaliação e modelo de escopos
- [Sistema de Autenticação](architecture/authentication-system.md) - JWT, API tokens e redefinição de senha
- [Multi-Tenancy](architecture/multi-tenancy.md) - Propagação de contexto e isolamento multi-nível

### Guias de Uso

Documentação prática sobre como utilizar o módulo em suas aplicações.

- [Configuração](guides/configuration.md) - Propriedades e configuração do módulo
- [Anotações e Uso Básico](guides/annotations-and-usage.md) - @HasPermission e outras anotações de segurança
- [Exemplos de Código](guides/code-examples.md) - Exemplos completos e casos de uso
- [Melhores Práticas](guides/best-practices.md) - Recomendações de design e segurança
- [Troubleshooting](guides/troubleshooting.md) - Solução de problemas comuns

### Análises Comparativas

Documentação comparativa entre o Archbase e outras soluções de segurança.

- [Archbase vs Keycloak](guides/archbase-vs-keycloak.md) - Comparação detalhada entre os modelos de segurança do Archbase e Keycloak, incluindo diagramas de entidades, equivalências conceituais e estratégias de migração

### Diagramas

Diagramas visuais para entender a estrutura e fluxos do sistema.

- [Índice de Diagramas](diagrams/README.md) - Lista completa de diagramas disponíveis
- [Relacionamentos de Entidades (ERD)](diagrams/entity-relationships.md) - Diagrama do modelo de dados
- [Fluxos de Sistema](diagrams/flows.md) - Diagramas de sequência para autenticação e permissões
- [Escopos de Permissões](diagrams/permission-scopes.md) - Hierarquia de escopos contextuais

---

## 🚀 Início Rápido

### 1. Adicionar Dependência

Para aplicações básicas:
```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter-core</artifactId>
</dependency>
```

Para aplicações com segurança e multi-tenancy:
```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-starter</artifactId>
</dependency>
```

### 2. Configurar Propriedades

```properties
# Habilitar segurança
archbase.security.enabled=true
archbase.security.jwt.secret=your-secret-key-here
archbase.security.jwt.expiration=86400000

# Habilitar multi-tenancy (opcional)
archbase.multitenancy.enabled=true
```

### 3. Proteger Endpoints

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    @HasPermission(action = "VIEW", resource = "USER")
    public UserDto getUser(@PathVariable String id) {
        // Implementação
    }

    @PostMapping
    @HasPermission(action = "CREATE", resource = "USER")
    public UserDto createUser(@RequestBody UserDto user) {
        // Implementação
    }
}
```

### 4. Criar Usuário e Permissões

```java
// Criar recurso
ResourceEntity resource = new ResourceEntity();
resource.setName("USER");
resource.setType(TipoRecurso.API);
resourceRepository.save(resource);

// Criar ação
ActionEntity action = new ActionEntity();
action.setName("VIEW");
action.setResource(resource);
actionRepository.save(action);

// Criar usuário
UserEntity user = new UserEntity();
user.setUserName("john.doe");
user.setEmail("john@example.com");
user.setPassword(passwordEncoder.encode("password"));
userRepository.save(user);

// Criar permissão
PermissionEntity permission = new PermissionEntity();
permission.setSecurity(user);
permission.setAction(action);
// Permissão global (todos os tenants/companies/projects)
permissionRepository.save(permission);
```

---

## 📖 Leitura Adicional

- [Documentação Geral do Framework](../readme-security.md)
- [CLAUDE.md](../../CLAUDE.md) - Guia para desenvolvimento com Claude Code

---

## 🔄 Fluxo de Leitura Recomendado

Para novos desenvolvedores, recomendamos a seguinte ordem de leitura:

1. **Começar aqui** - Este README para visão geral
2. [Entidades Core](entities/core-entities.md) - Entender User, Group, Profile
3. [Sistema de Permissões](entities/permission-entities.md) - Como funcionam as permissões
4. [Fluxo de Autenticação](architecture/authentication-system.md) - Como usuários fazem login
5. [Fluxo de Avaliação de Permissões](architecture/permissions-system.md) - Como permissões são verificadas
6. [Anotações e Uso](guides/annotations-and-usage.md) - Como usar @HasPermission
7. [Exemplos de Código](guides/code-examples.md) - Ver implementações práticas
8. [Melhores Práticas](guides/best-practices.md) - Aprender recomendações

---

## 📝 Nota sobre Migração

Este documento foi reorganizado a partir de `security-entities-and-permissions.md` para melhorar a navegabilidade e manutenibilidade. O documento original foi arquivado em `archive/` para referência.
