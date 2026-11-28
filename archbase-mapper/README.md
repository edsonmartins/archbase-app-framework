# archbase-mapper

Contrato `ArchbaseMapper<S, T>` para conversão entre entidade (origem) e DTO (destino), com métodos `default` para
listas, conjuntos, coleções e páginas do Spring Data. Não faz o mapeamento em si: a aplicação implementa `toDto` e
`toEntity`, à mão ou com MapStruct.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-mapper</artifactId>
    <version>3.5.0</version>
</dependency>
```

Nenhum starter do Archbase traz este módulo; declare-o diretamente.

## O que traz

| Dependência | Escopo | Observação |
|---|---|---|
| `spring-boot-starter`, `spring-data-commons` | compile | `Page`, `PageImpl`, `Pageable` |
| `mapstruct` 1.5.5.Final | `optional` | Não chega à aplicação; declare `mapstruct` e o `mapstruct-processor` no seu pom se for usar |
| `modelmapper` | `optional` | Não é usado por nenhuma classe do módulo |

## Auto-configuração

`ArchbaseMapperAutoConfiguration` está registrada no `AutoConfiguration.imports`, sem condição, e registra só o
`MapperConfiguration` (`@ConditionalOnMissingBean`), ponto de extensão sem configuração própria. Não procura mappers.
Até a 3.5.0 não registrava nada: o método não tinha `@Bean`, e um `@ComponentScan` do próprio pacote não encontrava
nada.

Não há propriedades. Mappers da aplicação entram no contexto pelos meios normais (`@Component`, ou
`@Mapper(componentModel = "spring")` do MapStruct).

## Uso

| Tipo | Papel |
|---|---|
| `ArchbaseMapper<S, T>` | `S` é a origem (entidade), `T` o destino (DTO). Abstratos: `T toDto(S)`, `S toEntity(T)` |
| `MappingException` | `RuntimeException` para a aplicação sinalizar erro de mapeamento; o módulo não a lança |

Métodos `default`:

| Método | Retorno |
|---|---|
| `toDtoList(List<S>)`, `toEntityList(List<T>)` | `List` |
| `toDtoSet(Set<S>)`, `toEntitySet(Set<T>)` | `Set` |
| `toDtoCollection(Collection<S>)`, `toEntityCollection(Collection<T>)` | `List` |
| `toDtoPage(Page<S>)` | `PageImpl` com o mesmo `Pageable` e total da página de origem |
| `toEntityPage(Page<T>, Pageable)` | `PageImpl` com o `Pageable` informado |
| `updateFromDto(T dto, S entity)` | `null` se `dto` é `null`; senão `toEntity(dto)` — ver limitações |
| `createDto()`, `createEntity()` | Instância nova por construtor sem argumentos, via `getTargetClass()` / `getSourceClass()` |

Implementação manual:

```java
@Component
public class ClienteMapper implements ArchbaseMapper<Cliente, ClienteDto> {

    @Override
    public ClienteDto toDto(Cliente source) {
        return new ClienteDto(source.getId(), source.getNome());
    }

    @Override
    public Cliente toEntity(ClienteDto dto) {
        return new Cliente(dto.id(), dto.nome());
    }
}

// no serviço
Page<ClienteDto> pagina = clienteMapper.toDtoPage(repository.findAll(pageable));
```

Com MapStruct (dependências declaradas pela aplicação):

```java
@Mapper(componentModel = "spring")
public interface ClienteMapper extends ArchbaseMapper<Cliente, ClienteDto> {
}
```

O MapStruct gera `toDto` e `toEntity`; os métodos `default` são herdados.

## Limitações

- `updateFromDto` ignora a entidade recebida e devolve `toEntity(dto)`, um objeto novo. Para atualizar uma entidade
  gerenciada, sobrescreva o método (no MapStruct, com `@MappingTarget`).
- `getTargetClass()` / `getSourceClass()` resolvem `S` e `T` na hierarquia inteira, inclusive na implementação
  gerada pelo MapStruct (`ClienteMapperImpl implements ClienteMapper`). Até a 3.5.0 olhavam só a primeira interface
  da classe e, nesse caso, `createDto()` / `createEntity()` falhavam sempre.
- Os métodos de coleção não tratam `null`: passar lista nula lança `NullPointerException`.

## Referência

- Módulos do framework: [CLAUDE.md](../CLAUDE.md)
