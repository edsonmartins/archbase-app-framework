package teste.starter;

import br.com.archbase.ddd.context.ArchbaseTenantContext;
import br.com.archbase.ddd.domain.specification.basic.EqualityArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.basic.GreaterThanArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.basic.GreaterThanOrEqualArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.basic.InArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.basic.LessThanArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.basic.LessThanOrEqualArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.basic.LikeArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.basic.NotEqualityArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.basic.NotInArchbaseSpecification;
import br.com.archbase.ddd.domain.specification.basic.NotLikeArchbaseSpecification;
import br.com.archbase.security.persistence.UserEntity;
import br.com.archbase.security.repository.UserJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code matching}, {@code howMany} e {@code containsAny} num repositório de verdade (o de usuários, cuja base é
 * {@code CommonArchbaseJpaRepository}). Até a 3.5.0 os três lançavam {@code NullPointerException}: o tradutor de
 * specification só era preenchido por um {@code setTranslator} que ninguém chamava. Por trás disso, a tradução estava
 * errada para quase todo operador — ver os comentários de cada caso.
 */
@DisplayName("ArchbaseSpecification no repositório")
class SpecificationNoRepositorioTest {

    private static final LocalDateTime T1 = LocalDateTime.of(2026, 1, 1, 10, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2026, 2, 1, 10, 0);
    private static final LocalDateTime T3 = LocalDateTime.of(2026, 3, 1, 10, 0);

    @Test
    @DisplayName("matching, howMany e containsAny traduzem cada operador para o SQL certo")
    void consulta() {
        try (ConfigurableApplicationContext ctx = SubidaDoStarterCompletoTest.subir(SubidaDoStarterCompletoTest.AppPadrao.class,
                "--archbase.app.tenant.default.id=tenant-teste")) {
            UserJpaRepository r = ctx.getBean(UserJpaRepository.class);
            ArchbaseTenantContext.setTenantId("tenant-teste");
            try {
                r.save(usuario("u1", "ana@teste.com", "Ana", T1));
                r.save(usuario("u2", "bruno@teste.com", "Bruno", T2));
                r.save(usuario("u3", "anabela@teste.com", "Anabela", T3));

                var ana = new EqualityArchbaseSpecification<UserEntity>("email", "ana@teste.com");
                // Igualdade virava "not like" (o NotLikeConverter declarava o tipo da igualdade).
                assertThat(ids(r.matching(ana))).containsExactly("u1");
                assertThat(ids(r.matching(new NotEqualityArchbaseSpecification<>("email", "ana@teste.com"))))
                        .containsExactlyInAnyOrder("u2", "u3");
                assertThat(r.howMany(new LikeArchbaseSpecification<>("name", "Ana%"))).isEqualTo(2L);
                assertThat(ids(r.matching(new NotLikeArchbaseSpecification<>("name", "Ana%")))).containsExactly("u2");

                List<Object> lista = List.of("u1", "u3");
                // "in" virava "not in"; e o "in" em si montava um predicado aninhado inválido.
                assertThat(ids(r.matching(new InArchbaseSpecification<>("id", lista)))).containsExactlyInAnyOrder("u1", "u3");
                assertThat(ids(r.matching(new NotInArchbaseSpecification<>("id", lista)))).containsExactly("u2");

                // ">" virava ">=" e "<" virava "<="; o valor era comparado como texto.
                assertThat(ids(r.matching(new GreaterThanArchbaseSpecification<>("createEntityDate", T2)))).containsExactly("u3");
                assertThat(ids(r.matching(new GreaterThanOrEqualArchbaseSpecification<>("createEntityDate", T2))))
                        .containsExactlyInAnyOrder("u2", "u3");
                assertThat(ids(r.matching(new LessThanArchbaseSpecification<>("createEntityDate", T2)))).containsExactly("u1");
                assertThat(ids(r.matching(new LessThanOrEqualArchbaseSpecification<>("createEntityDate", T2))))
                        .containsExactlyInAnyOrder("u1", "u2");

                assertThat(r.containsAny(ana)).isTrue();
                assertThat(r.containsAny(new EqualityArchbaseSpecification<>("email", "ninguem@teste.com"))).isFalse();
                assertThat(ids(r.matching(ana.or(new EqualityArchbaseSpecification<>("email", "bruno@teste.com")))))
                        .containsExactlyInAnyOrder("u1", "u2");
                assertThat(r.howMany(new LikeArchbaseSpecification<UserEntity>("name", "Ana%").and(ana.not()))).isEqualTo(1L);
            } finally {
                ArchbaseTenantContext.clear();
            }
        }
    }

    private static List<String> ids(List<UserEntity> usuarios) {
        return usuarios.stream().map(UserEntity::getId).toList();
    }

    private static UserEntity usuario(String id, String email, String nome, LocalDateTime criacao) {
        UserEntity u = UserEntity.builder()
                .id(id).createEntityDate(criacao)
                .name(nome).description(nome).email(email).userName(email).password("x")
                .isAdministrator(false).accountDeactivated(false).accountLocked(false)
                .changePasswordOnNextLogin(false).passwordNeverExpires(true).allowPasswordChange(true)
                .allowMultipleLogins(true).unlimitedAccessHours(true)
                .build();
        u.setTenantId("tenant-teste");
        return u;
    }
}
