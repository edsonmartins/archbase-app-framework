package br.com.archbase.starter.security.auto.configuration;

import br.com.archbase.security.schema.ArchbaseSecuritySchemaConfiguration;
import br.com.archbase.security.schema.ArchbaseSecuritySchemaInitializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

/**
 * Faz a rotina de esquema de segurança chegar a quem depende do starter.
 *
 * <p><b>O defeito que isto corrige.</b> A rotina foi entregue na 3.1.12 como uma
 * {@code @Configuration} comum dentro de {@code br.com.archbase.security.schema} — e configuração
 * comum só existe se alguém a varrer. Na prática, ela só era registrada em aplicações cujo
 * {@code @ComponentScan} incluísse {@code br.com.archbase}, o que é escolha de cada projeto e não
 * algo que o framework possa presumir. Onde não incluía, a rotina simplesmente não existia: nenhum
 * erro, nenhuma linha de log, e a promessa de que "o framework cuida do próprio esquema" valia
 * apenas para parte dos consumidores.
 *
 * <p>Isso não apareceu em teste algum porque a aplicação de teste do módulo varre
 * {@code br.com.archbase.security} inteiro — o arranjo em que o problema não existe. Apareceu ao
 * conferir a configuração de uma aplicação real, que varre apenas os próprios pacotes.
 *
 * <p>Como auto-configuração, o registro passa a depender de estar no classpath, e não de como cada
 * projeto escreveu a varredura.
 *
 * <p><b>Sobre a convivência com a varredura.</b> Em quem varre {@code br.com.archbase}, a
 * configuração continua sendo encontrada pelos dois caminhos, e não há duplicata: o Spring
 * identifica classes de configuração pela própria classe, então importar uma que a varredura já
 * registrou não cria um segundo registro. O bean ainda traz
 * {@code @ConditionalOnMissingBean(ArchbaseSecuritySchemaInitializer.class)}, que preserva o
 * inicializador declarado à mão por quem tem mais de um banco.
 */
@AutoConfiguration
@ConditionalOnClass({ArchbaseSecuritySchemaInitializer.class, DataSource.class})
@ConditionalOnProperty(prefix = "archbase.security", name = "enabled", matchIfMissing = true)
@Import(ArchbaseSecuritySchemaConfiguration.class)
public class ArchbaseSecuritySchemaAutoConfiguration {
}
