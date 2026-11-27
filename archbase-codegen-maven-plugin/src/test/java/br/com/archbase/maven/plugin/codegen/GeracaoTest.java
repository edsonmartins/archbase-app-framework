package br.com.archbase.maven.plugin.codegen;

import br.com.archbase.maven.plugin.codegen.support.maker.ResourceStructure;
import br.com.archbase.maven.plugin.codegen.support.maker.ServiceStructure;
import br.com.archbase.maven.plugin.codegen.util.BuildUtils;
import br.com.archbase.maven.plugin.codegen.util.GeneratorUtils;
import jakarta.persistence.Id;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("archbase-codegen-maven-plugin: código gerado")
class GeracaoTest {

    /** Entidade de exemplo com id primitivo. */
    public static class Produto {
        @Id
        private long id;
    }

    @AfterEach
    void limpar() {
        GeneratorUtils.setBaseDir(null);
    }

    @Test
    @DisplayName("diamante com argumentos iguais separa por vírgula (antes, <X,LongLong>)")
    void diamante() {
        assertThat(BuildUtils.builDiamond("Repository", "Produto", "Long", "Long"))
                .isEqualTo("Repository<Produto, Long, Long>");
    }

    @Test
    @DisplayName("controller gerado usa a anotação do OpenAPI 3 e caminho /api/vN; id primitivo é aceito")
    void controller() {
        String codigo = new ResourceStructure("com.acme.resource", "Produto", Produto.class.getName(), "Resource",
                "com.acme.service", "Service", null, "", "1", "java.lang.Long").build().left();

        assertThat(codigo).as("id long: antes o mapa de primitivos do resource estava vazio e nada era gerado")
                .isNotNull();
        assertThat(codigo).contains("import io.swagger.v3.oas.annotations.tags.Tag;")
                .contains("@Tag(name=\"Produto\")")
                .doesNotContain("io.swagger.annotations")
                .contains("@RequestMapping(value=\"/api/v1/produto\")")
                .contains("CommonArchbaseService<Produto,Long,Long>");
    }

    @Test
    @DisplayName("service com id primitivo é gerado (antes, recusado)")
    void service() {
        String codigo = new ServiceStructure("com.acme.service", "Produto", Produto.class.getName(), "Service",
                "com.acme.repository", "Repository", null, "", "1", "java.lang.Long").build().left();
        assertThat(codigo).isNotNull().contains("class ProdutoService");
    }

    @Test
    @DisplayName("os arquivos vão para o basedir do projeto do goal, não para o diretório de onde o Maven foi chamado")
    void diretorio() throws Exception {
        File modulo = new File("target/um-modulo").getCanonicalFile();
        GeneratorUtils.setBaseDir(modulo);
        assertThat(GeneratorUtils.getAbsolutePath()).isEqualTo(modulo.getPath() + "/src/main/java/");
    }
}
