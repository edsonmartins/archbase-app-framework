package br.com.archbase.security.access;

/**
 * Uma capacidade que foi concedida a alguém — <b>com a origem junto</b>.
 *
 * <p>A origem é o que não existia em lugar nenhum antes do core: a consulta sempre devolveu qual
 * perfil ou grupo concedeu, e o {@code anyMatch} descartava. Sem ela, investigar um acesso obriga a
 * abrir grupo por grupo no admin.
 *
 * @param grantedByType {@code USUARIO}, {@code GRUPO} ou {@code PERFIL} — o discriminador da
 *                      entidade de segurança que recebeu a concessão
 * @param situation     se a concessão vale hoje, e quando não, por quê
 */
public record EffectiveCapability(
        String resource,
        String action,
        String grantedBy,
        String grantedByName,
        String grantedByType,
        boolean actionActive,
        boolean resourceActive,
        Situation situation) {

    public String capability() {
        return resource + ":" + action;
    }

    public enum Situation {

        /** Vale hoje e continuará valendo. */
        EFFECTIVE,

        /**
         * Há uma <b>negação explícita</b> alcançando esta capacidade.
         *
         * <p>A linha aparece na lista porque foi concedida em alguma origem — e não vale, porque
         * outra a nega. Omiti-la esconderia metade da explicação de quem investiga; contá-la como
         * efetiva faria o diagnóstico dizer o oposto da decisão.
         */
        DENIED,

        /**
         * A concessão existe, mas aponta para ação ou recurso inativo.
         *
         * <p>Hoje ela <b>conta</b> no caminho do {@code @HasPermission}, que não filtra
         * {@code active}, e <b>não conta</b> no caminho do frontend, que filtra. Deixará de contar
         * dos dois lados quando {@code archbase.security.permission.require-active} for ligado — e
         * é por isso que este relatório precisa ser lido antes de ligar.
         */
        INERT
    }
}
