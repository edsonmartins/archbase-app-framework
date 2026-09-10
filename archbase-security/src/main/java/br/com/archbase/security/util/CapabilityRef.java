package br.com.archbase.security.util;

/**
 * Uma referência a capacidade, no formato {@code recurso:acao}.
 *
 * <p>Existe para que haja <b>uma</b> interpretação dessa string no framework. A varredura de
 * {@code @HasPermission} e o registro de tela declaram dependências no mesmo formato, e duas
 * implementações da mesma leitura já produziram, neste módulo, o defeito de catalogar com um nome e
 * consultar com outro.
 *
 * <p>Duas formas aceitas:
 *
 * <ul>
 *   <li>{@code "view"} — a ação {@code view} do <b>mesmo recurso</b> de quem declara;</li>
 *   <li>{@code "tms.cliente:view"} — qualificada.</li>
 * </ul>
 */
public final class CapabilityRef {

    public static final char SEPARADOR = ':';

    private CapabilityRef() {
    }

    /**
     * Normaliza a declaração para a forma qualificada, ou devolve {@code null} quando não dá.
     *
     * <p>Devolve nulo — em vez de lançar — porque quem chama precisa <b>continuar</b>: uma
     * dependência mal declarada não pode impedir o catálogo inteiro de ser sincronizado, nem
     * derrubar a subida da aplicação. Quem chama registra o aviso apontando o método.
     *
     * @param declarada       o que veio na anotação ou no payload
     * @param recursoDeQuemDeclara recurso usado para completar a forma curta
     */
    public static String qualificar(String declarada, String recursoDeQuemDeclara) {
        if (declarada == null) {
            return null;
        }
        String limpa = declarada.trim();
        if (limpa.isEmpty()) {
            return null;
        }

        int separador = limpa.indexOf(SEPARADOR);
        if (separador < 0) {
            // Forma curta: a ação do próprio recurso.
            if (recursoDeQuemDeclara == null || recursoDeQuemDeclara.isBlank()) {
                return null;
            }
            return de(recursoDeQuemDeclara, limpa);
        }

        // Forma qualificada. Mais de um separador torna a leitura ambígua: não há como saber onde
        // termina o nome do recurso, e escolher uma das leituras catalogaria a aresta errada em
        // silêncio.
        if (limpa.indexOf(SEPARADOR, separador + 1) >= 0) {
            return null;
        }

        String recurso = limpa.substring(0, separador).trim();
        String acao = limpa.substring(separador + 1).trim();
        if (recurso.isEmpty() || acao.isEmpty()) {
            return null;
        }
        return de(recurso, acao);
    }

    /** A forma canônica que vai para a coluna e para a resposta da API. */
    public static String de(String recurso, String acao) {
        return recurso + SEPARADOR + acao;
    }

    /** O nome do recurso de uma referência já qualificada. */
    public static String recursoDe(String qualificada) {
        int separador = qualificada.indexOf(SEPARADOR);
        return separador < 0 ? null : qualificada.substring(0, separador);
    }

    /** O nome da ação de uma referência já qualificada. */
    public static String acaoDe(String qualificada) {
        int separador = qualificada.indexOf(SEPARADOR);
        return separador < 0 ? null : qualificada.substring(separador + 1);
    }
}
