package br.com.archbase.security.adapter;

import br.com.archbase.ddd.domain.contracts.FindDataWithFilterQuery;
import br.com.archbase.query.rsql.jpa.SortUtils;
import br.com.archbase.security.adapter.port.ActionPersistencePort;
import br.com.archbase.security.domain.dto.ActionDto;
import br.com.archbase.security.domain.dto.ApiTokenDto;
import br.com.archbase.security.persistence.ActionEntity;
import br.com.archbase.security.persistence.ApiTokenEntity;
import br.com.archbase.security.persistence.QActionEntity;
import br.com.archbase.security.repository.ActionJpaRepository;
import br.com.archbase.security.repository.ApiTokenRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ApiTokenPersistenceAdapter implements FindDataWithFilterQuery<String, ApiTokenDto> {

    private final ApiTokenRepository repository;

    @Autowired
    public ApiTokenPersistenceAdapter(ApiTokenRepository repository) {
        this.repository = repository;
    }


    @Override
    public ApiTokenDto findById(String id) {
        Optional<ApiTokenEntity> byId = repository.findById(id);
        return byId.map(ApiTokenEntity::toDto).orElse(null);
    }

    @Override
    public Page<ApiTokenDto> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ApiTokenEntity> result = repository.findAll(pageable);
        List<ApiTokenDto> list = result.stream().map(ApiTokenEntity::toDto).toList();
        return new ApiTokenPersistenceAdapter.PageApiToken(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<ApiTokenDto> findAll(int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<ApiTokenEntity> result = repository.findAll(pageable);
        List<ApiTokenDto> list = result.stream().map(ApiTokenEntity::toDto).toList();
        return new ApiTokenPersistenceAdapter.PageApiToken(list, pageable, result.getTotalElements());
    }

    @Override
    public List<ApiTokenDto> findAll(List<String> ids) {
        List<ApiTokenEntity> result = repository.findAllById(ids);
        return result.stream().map(ApiTokenEntity::toDto).toList();
    }

    @Override
    public Page<ApiTokenDto> findWithFilter(String filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ApiTokenEntity> result = repository.findAll(filter, pageable);
        List<ApiTokenDto> list = result.stream().map(ApiTokenEntity::toDto).toList();
        return new ApiTokenPersistenceAdapter.PageApiToken(list, pageable, result.getTotalElements());
    }

    @Override
    public Page<ApiTokenDto> findWithFilter(String filter, int page, int size, String[] sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(SortUtils.convertSortToJpa(sort)));
        Page<ApiTokenEntity> result = repository.findAll(filter, pageable);
        List<ApiTokenDto> list = result.stream().map(ApiTokenEntity::toDto).toList();
        return new ApiTokenPersistenceAdapter.PageApiToken(list, pageable, result.getTotalElements());
    }

    static class PageApiToken extends PageImpl<ApiTokenDto> {
        public PageApiToken(List<ApiTokenDto> content) {
            super(content);
        }

        public PageApiToken(List<ApiTokenDto> content, Pageable pageable, long total) {
            super(content, pageable, total);
        }
    }

    static class ListApiToken extends ArrayList<ApiTokenDto> {
        public ListApiToken(Collection<? extends ApiTokenDto> c) {
            super(c);
        }
    }
}
