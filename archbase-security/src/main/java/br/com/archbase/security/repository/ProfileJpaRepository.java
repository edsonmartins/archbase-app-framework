package br.com.archbase.security.repository;


import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.ProfileEntity;
import org.springframework.stereotype.Repository;


@Repository
public interface ProfileJpaRepository extends ArchbaseCommonJpaRepository<ProfileEntity, String, Long> {
}
