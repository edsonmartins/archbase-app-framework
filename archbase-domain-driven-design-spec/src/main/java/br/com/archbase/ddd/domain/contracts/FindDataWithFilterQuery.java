package br.com.archbase.ddd.domain.contracts;


import org.springframework.data.domain.Page;

import java.util.List;

public interface FindDataWithFilterQuery<ID,R> {
    public R findById(ID id);
    public Page<R> findAll(int page, int size);
    public Page<R> findAll(int page, int size, String[] sort);
    public List<R> findAll(List<ID> ids);
    public Page<R> findWithFilter(String filter, int page, int size);
    public Page<R> findWithFilter(String filter, int page, int size, String[] sort);
}
