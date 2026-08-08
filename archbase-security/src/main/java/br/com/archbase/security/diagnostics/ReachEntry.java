package br.com.archbase.security.diagnostics;

/**
 * Uma pessoa que alcança uma capacidade, e por qual via.
 *
 * <p><b>A pergunta que hoje não tem resposta.</b> "Quem consegue aprovar custo?" só se responde
 * abrindo grupo por grupo no admin e cruzando na cabeça — e o resultado depende de quem cruzou.
 * Esta é a consulta reversa: dada a capacidade, quem chega nela.
 *
 * @param via     por onde ela chega: o nome do grupo, do perfil, ou "concessão direta"
 * @param kind    a classe da origem — {@code GRUPO}, {@code PERFIL}, {@code USUARIO} ou
 *                {@code ADMINISTRADOR}
 * @param situation se aquela via de fato produz efeito hoje
 */
public record ReachEntry(
        String userId,
        String userName,
        String email,
        String via,
        String kind,
        String situation) {

    /**
     * A origem que não é concessão nenhuma.
     *
     * <p>Administrador alcança tudo sem passar pelo catálogo. Omiti-lo da resposta daria uma
     * resposta <b>errada</b> à pergunta "quem pode" — e é justamente a resposta que mais importa
     * numa auditoria.
     */
    public static final String KIND_ADMIN = "ADMINISTRADOR";
}
