package br.com.archbase.security.persistence;

import br.com.archbase.security.audit.SecurityEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Um acontecimento de segurança que <b>não altera tabela nenhuma</b>.
 *
 * <p>É a metade que a trilha do Envers não cobre, e não por limitação dela: o Envers registra
 * mudanças de estado, e entrar no sistema, errar a senha ou ter um acesso negado não mudam estado.
 * São exatamente os eventos que respondem "quem tentou o quê", enquanto a trilha responde "quem
 * mudou o quê" — e uma investigação séria precisa das duas.
 *
 * <p>Tabela própria, e não junto das revisões: o volume é de outra ordem — uma tentativa de login
 * por pessoa por dia contra uma alteração de permissão por mês — e misturá-los faria a purga de uma
 * arrastar a outra.
 */
@Entity
@Table(name = "SEGURANCA_EVENTO", indexes = {
        @Index(name = "IDX_SEGURANCA_EVENTO_DH", columnList = "DH_EVENTO"),
        @Index(name = "IDX_SEGURANCA_EVENTO_TENANT", columnList = "TENANT_ID"),
        // A pergunta mais comum na investigação é sobre uma pessoa: "o que aconteceu com o fulano".
        @Index(name = "IDX_SEGURANCA_EVENTO_USUARIO", columnList = "USUARIO")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEventEntity {

    @Id
    @Column(name = "ID_EVENTO", length = 40)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "TP_EVENTO", nullable = false, length = 40)
    private SecurityEventType tipo;

    @Column(name = "DH_EVENTO", nullable = false)
    private LocalDateTime dataHora;

    /**
     * Quem. Pode ser um e-mail que não existe — numa tentativa de login com usuário inexistente é
     * justamente esse valor que interessa.
     */
    @Column(name = "USUARIO", length = 255)
    private String usuario;

    @Column(name = "TENANT_ID", length = 255)
    private String tenantId;

    @Column(name = "ORIGEM", length = 100)
    private String origem;

    /** O recurso e a ação, quando o evento é sobre uma capacidade. */
    @Column(name = "RECURSO", length = 255)
    private String recurso;

    @Column(name = "ACAO", length = 255)
    private String acao;

    /**
     * O motivo, em texto.
     *
     * <p>Para uma negação, é o portão que recusou — a informação que transforma o registro em algo
     * acionável, em vez de um "negado" que não diz o que ajustar.
     */
    @Column(name = "DETALHE", length = 500)
    private String detalhe;

    /** Se o que se tentou fazer aconteceu. Falso numa senha errada, num acesso negado. */
    @Column(name = "BO_SUCESSO", nullable = false)
    private boolean sucesso;
}
