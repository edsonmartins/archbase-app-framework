package br.com.archbase.security.repository;

import br.com.archbase.ddd.infraestructure.persistence.jpa.repository.ArchbaseCommonJpaRepository;
import br.com.archbase.security.persistence.AccessTokenEntity;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccessTokenJpaRepository extends ArchbaseCommonJpaRepository<AccessTokenEntity, String, Long> {

  @Query(value = """
      select t from TokenAcessoEntity t inner join UsuarioEntity u\s
      on t.usuario.id = u.id\s
      where u.id = :id and (t.expirado = false or t.revogado = false)\s
      """)
  List<AccessTokenEntity> findAllValidTokenByUser(String id);

  Optional<AccessTokenEntity> findByToken(String token);
}
