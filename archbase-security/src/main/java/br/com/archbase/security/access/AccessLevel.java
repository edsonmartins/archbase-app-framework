package br.com.archbase.security.access;

/**
 * O nível de acesso — o piso que uma capacidade exige e a altura que uma pessoa alcança.
 *
 * <p><b>Piso, não substituto.</b> Alcançar o nível não concede nada: o acesso continua dependendo de
 * permissão concedida no catálogo. O nível apenas <b>impede</b> que uma concessão indevida valha —
 * é a resposta ao risco de alguém atribuir uma capacidade sensível a quem não deveria tê-la.
 *
 * <p><b>Deliberadamente curta.</b> Quatro degraus, e a ordem é o que o core usa; os rótulos exibidos
 * são do cliente. Se aparecer vontade de ter oito níveis, é sinal de estar tentando expressar no
 * nível o que pertence à capacidade — {@code tms.ordemservico:aprovar_custo} já distingue aprovar
 * custo de iniciar execução, e não precisa de um degrau próprio para isso.
 *
 * <p>De onde a pessoa tira o seu nível: do <b>perfil</b>, que é um por usuário e portanto
 * univalorado. Grupo não carrega nível — um usuário está em vários, e escolher entre o maior e o
 * menor seria arbitrário nos dois sentidos. Quem modela senioridade fora do perfil implementa
 * {@link ArchbaseAccessLevelResolver}.
 */
public enum AccessLevel {

    /** Consulta: listar, ver, exportar, monitorar. */
    READER,

    /** Operação diária: criar, editar, iniciar, executar, atribuir, registrar. */
    OPERATOR,

    /** Decisão e irreversível: aprovar, cancelar, excluir, fechar, bloquear, configurar. */
    SUPERVISOR,

    /** Segurança e identidade: conceder, revogar, resetar senha, ver auditoria. */
    TENANT_ADMIN;

    /** {@code true} se este nível alcança o mínimo exigido. */
    public boolean reaches(AccessLevel minimum) {
        return minimum == null || this.ordinal() >= minimum.ordinal();
    }

    /**
     * Converte o texto gravado na coluna, tolerando espaço e caixa.
     *
     * <p>Devolve {@code null} para valor ausente <b>ou desconhecido</b>. Um rótulo que o framework
     * não reconhece não pode virar um degrau qualquer — nem o mais alto, que abriria acesso, nem o
     * mais baixo, que o fecharia em silêncio. Ausência de nível é tratada pela política de
     * {@code archbase.security.access-level.default}.
     */
    public static AccessLevel parse(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return AccessLevel.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException naoReconhecido) {
            return null;
        }
    }
}
