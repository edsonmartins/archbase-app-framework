package br.com.archbase.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interface base para mappers de objeto.
 * <p>
 * Fornece métodos utilitários para conversão entre DTOs, entidades e Value Objects.
 * <p>
 * Uso:
 * <pre>
 * {@code
 * public interface ClientMapper extends ArchbaseMapper<ClientEntity, ClientDto> {
 *     ClientMapper INSTANCE = Mappers.getMapper(ClientMapper.class);
 *
 *     @Mapping(target = "id", ignore = true)
 *     ClientDto toDto(ClientEntity entity);
 * }
 * }
 * </pre>
 *
 * @param <S> Tipo de origem (Source)
 * @param <T> Tipo de destino (Target)
 */
public interface ArchbaseMapper<S, T> {

    /**
     * Converte um objeto de origem para destino.
     *
     * @param source Objeto de origem
     * @return Objeto de destino
     */
    T toDto(S source);

    /**
     * Converte um objeto de destino para origem.
     *
     * @param dto Objeto de destino
     * @return Objeto de origem
     */
    S toEntity(T dto);

    /**
     * Converte uma lista de objetos.
     *
     * @param source Lista de origem
     * @return Lista de destino
     */
    default List<T> toDtoList(List<S> source) {
        return source.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Converte uma lista de objetos para entidades.
     *
     * @param dtos Lista de DTOs
     * @return Lista de entidades
     */
    default List<S> toEntityList(List<T> dtos) {
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * Converte um conjunto de objetos.
     *
     * @param source Conjunto de origem
     * @return Conjunto de destino
     */
    default Set<T> toDtoSet(Set<S> source) {
        return source.stream()
                .map(this::toDto)
                .collect(Collectors.toSet());
    }

    /**
     * Converte um conjunto de objetos para entidades.
     *
     * @param dtos Conjunto de DTOs
     * @return Conjunto de entidades
     */
    default Set<S> toEntitySet(Set<T> dtos) {
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toSet());
    }

    /**
     * Converte uma coleção de objetos.
     *
     * @param source Coleção de origem
     * @return Lista de destino
     */
    default List<T> toDtoCollection(Collection<S> source) {
        return source.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Converte uma coleção de objetos para entidades.
     *
     * @param dtos Coleção de DTOs
     * @return Lista de entidades
     */
    default List<S> toEntityCollection(Collection<T> dtos) {
        return dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * Converte uma página de objetos.
     *
     * @param source Página de origem
     * @return Página de destino
     */
    default Page<T> toDtoPage(Page<S> source) {
        return new PageImpl<>(
                toDtoList(source.getContent()),
                source.getPageable(),
                source.getTotalElements()
        );
    }

    /**
     * Converte uma página de objetos para entidades.
     *
     * @param dtos Página de DTOs
     * @param pageable Informações de paginação
     * @return Página de entidades
     */
    default Page<S> toEntityPage(Page<T> dtos, Pageable pageable) {
        return new PageImpl<>(
                toEntityList(dtos.getContent()),
                pageable,
                dtos.getTotalElements()
        );
    }

    /**
     * Atualiza uma entidade existente com dados do DTO.
     *
     * @param dto  DTO com dados atualizados
     * @param entity Entidade a ser atualizada
     * @return Entidade atualizada
     */
    default S updateFromDto(T dto, S entity) {
        if (dto == null) {
            return null;
        }
        if (entity == null) {
            return toEntity(dto);
        }
        // Implementação padrão - subclasses podem sobrescrever
        return toEntity(dto);
    }

    /**
     * Cria uma nova instância do tipo de destino.
     *
     * @return Nova instância
     */
    default T createDto() {
        try {
            // Tenta obter a classe genérica T por reflexão
            return getTargetClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new UnsupportedOperationException(
                    "Não foi possível criar instância de DTO. Implemente createDto() manualmente.", e);
        }
    }

    /**
     * Cria uma nova instância do tipo de origem.
     *
     * @return Nova instância
     */
    default S createEntity() {
        try {
            // Tenta obter a classe genérica S por reflexão
            return getSourceClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new UnsupportedOperationException(
                    "Não foi possível criar instância de Entidade. Implemente createEntity() manualmente.", e);
        }
    }

    /**
     * Obtém a classe de destino.
     * Implementação padrão que tenta inferir por reflexão.
     *
     * @return Classe de destino
     */
    @SuppressWarnings("unchecked")
    default Class<T> getTargetClass() {
        try {
            return (Class<T>) java.lang.reflect.ParameterizedType.class
                    .cast(getClass().getGenericInterfaces()[0])
                    .getActualTypeArguments()[1];
        } catch (Exception e) {
            throw new UnsupportedOperationException("Não foi possível inferir a classe de destino.", e);
        }
    }

    /**
     * Obtém a classe de origem.
     * Implementação padrão que tenta inferir por reflexão.
     *
     * @return Classe de origem
     */
    @SuppressWarnings("unchecked")
    default Class<S> getSourceClass() {
        try {
            return (Class<S>) java.lang.reflect.ParameterizedType.class
                    .cast(getClass().getGenericInterfaces()[0])
                    .getActualTypeArguments()[0];
        } catch (Exception e) {
            throw new UnsupportedOperationException("Não foi possível inferir a classe de origem.", e);
        }
    }
}
