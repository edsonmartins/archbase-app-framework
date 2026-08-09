package br.com.archbase.security.audit;

/**
 * Os acontecimentos que a trilha registra.
 *
 * <p>A lista é curta de propósito. Registrar tudo produz um volume em que ninguém encontra nada —
 * e o custo de uma trilha inútil não é o disco, é a falsa sensação de que se tem rastreabilidade.
 * Cada tipo aqui responde a uma pergunta que aparece de fato numa investigação.
 */
public enum SecurityEventType {

    /** Entrou. Responde "quando essa pessoa esteve no sistema". */
    LOGIN,

    /** Não entrou. Em sequência e do mesmo endereço, é a assinatura de uma tentativa de invasão. */
    LOGIN_FALHOU,

    /** Saiu — inclusive quando a saída foi provocada por troca de senha ou revogação. */
    LOGOUT,

    /**
     * Pediu algo e foi recusado.
     *
     * <p>É o evento mais útil dos cinco: em volume, aponta permissão faltando; isolado e repetido
     * na mesma capacidade, aponta alguém tentando o que não deveria.
     */
    ACESSO_NEGADO,

    /**
     * Alguém simulou um acesso na tela de diagnóstico.
     *
     * <p>Registrado porque a simulação revela a estrutura de permissões do tenant. Não é ataque,
     * mas é reconhecimento — e quem investiga um vazamento vai querer saber quem andou olhando.
     */
    SIMULACAO
}
