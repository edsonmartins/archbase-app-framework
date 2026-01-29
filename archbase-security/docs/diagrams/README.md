[← Voltar ao Índice](../README.md)

---

# Diagramas do Sistema de Segurança

## Introdução

Esta seção contém diagramas visuais que ilustram a estrutura e os fluxos do módulo `archbase-security`. Os diagramas utilizam a sintaxe Mermaid e são renderizados automaticamente em visualizadores compatíveis (GitHub, GitLab, etc.).

---

## Diagramas Disponíveis

### 1. [Entity Relationship Diagram (ERD)](entity-relationships.md)
Diagrama do modelo de dados completo, mostrando todas as entidades de segurança e seus relacionamentos.

**Conteúdo**:
- Tabela `SEGURANCA` (User, Group, Profile)
- Tabela `SEGURANCA_PERMISSAO`
- Tabelas de Recursos e Ações
- Tokens (Access, API, Password Reset)
- Horários de Acesso
- Relacionamentos Many-to-Many e One-to-Many

**Quando Consultar**: Para entender a estrutura do banco de dados e relacionamentos entre entidades.

---

### 2. [Fluxos de Sistema](flows.md)
Diagramas de sequência mostrando os fluxos principais do sistema:
- **Fluxo de Avaliação de Permissões**: Como `@HasPermission` funciona internamente
- **Fluxo de Autenticação JWT**: Processo completo de login

**Conteúdo**:
- Diagrama de sequência de avaliação de permissões (9 passos)
- Diagrama de sequência de autenticação JWT (10 passos)
- Participantes e interações entre componentes

**Quando Consultar**: Para entender o fluxo de execução e debugging de problemas de autenticação/autorização.

---

### 3. [Modelo de Escopo de Permissões](permission-scopes.md)
Diagrama hierárquico mostrando os 4 níveis de escopo de permissões e a lógica de matching.

**Conteúdo**:
- Nível 1: Global
- Nível 2: Tenant-Scoped
- Nível 3: Company-Scoped
- Nível 4: Project-Scoped
- Fluxograma de lógica de matching

**Quando Consultar**: Para projetar permissões com granularidade apropriada.

---

## Como Visualizar os Diagramas

### GitHub / GitLab
Os diagramas são renderizados automaticamente ao visualizar os arquivos `.md`.

### VS Code
Instale a extensão **Markdown Preview Mermaid Support**:
```
ext install bierner.markdown-mermaid
```

### IntelliJ IDEA / WebStorm
Suporte nativo para diagramas Mermaid no preview de Markdown.

### Navegador
Use ferramentas online como:
- [Mermaid Live Editor](https://mermaid.live/)
- [Mermaid.js Playground](https://mermaid-js.github.io/mermaid-live-editor/)

### Exportar para Imagem
No Mermaid Live Editor, você pode exportar diagramas como PNG ou SVG.

---

## Navegação

- [Entity Relationship Diagram](entity-relationships.md)
- [Fluxos de Sistema](flows.md)
- [Modelo de Escopo de Permissões](permission-scopes.md)

---

[← Voltar ao Índice](../README.md)
