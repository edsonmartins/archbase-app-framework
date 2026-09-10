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
 * @param minimumLevel  o piso que o portão LEVEL exige para esta capacidade. {@code null} quando a
 *                      ação não declara nenhum — a maioria, hoje. Sem este campo a tela não
 *                      consegue explicar por que uma concessão existente não vale: "concedida e
 *                      bloqueada" fica indistinguível de "concedida e valendo"
 * @param situation     se a concessão vale hoje, e quando não, por quê
 * @param unmetDependencies as capacidades que esta aqui declara precisar e que o sujeito
 *                      <b>não</b> alcança. Vem <b>vazia</b> de {@code ArchbaseCapabilityReader}:
 *                      calculá-la ali custaria uma consulta a mais em todo carregamento de tela,
 *                      e a tela não usa o campo. Quem preenche é o diagnóstico, que é onde a
 *                      pergunta é feita
 */
public record EffectiveCapability(
        String resource,
        String action,
        String grantedBy,
        String grantedByName,
        String grantedByType,
        boolean actionActive,
        boolean resourceActive,
        AccessLevel minimumLevel,
        Situation situation,
        java.util.List<String> unmetDependencies) {

    public EffectiveCapability {
        unmetDependencies = unmetDependencies == null
                ? java.util.List.of() : java.util.List.copyOf(unmetDependencies);
    }

    /**
     * A mesma capacidade, com as dependências não atendidas preenchidas.
     *
     * <p><b>A situação NÃO muda.</b> Uma capacidade com dependência faltando continua
     * {@code EFFECTIVE}, porque é isso que a decisão faz com ela: deixa passar. Introduzir um
     * quarto valor de {@link Situation} faria o diagnóstico afirmar algo que o avaliador não
     * sustenta — a divergência entre tela e decisão que o core existe para eliminar. Daí a
     * informação viver em campo separado.
     */
    public EffectiveCapability withUnmetDependencies(java.util.List<String> naoAtendidas) {
        return new EffectiveCapability(resource, action, grantedBy, grantedByName, grantedByType,
                actionActive, resourceActive, minimumLevel, situation, naoAtendidas);
    }

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
