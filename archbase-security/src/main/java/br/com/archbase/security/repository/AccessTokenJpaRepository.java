package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.AccessTokenEntity;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccessTokenJpaRepository extends ArchbaseCommonJpaRepository<AccessTokenEntity, String, Long> {

  @Query(value = """
      select t from AccessTokenEntity t inner join UserEntity u\s
      on t.user.id = u.id\s
      where u.id = :id and (t.expired = false or t.revoked = false)\s
      """)
  List<AccessTokenEntity> findAllValidTokenByUser(String id);

  Optional<AccessTokenEntity> findByToken(String token);
}
