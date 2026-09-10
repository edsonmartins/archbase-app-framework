package br.com.archbase.security.domain.entity;

/**
 * Quem declarou a aresta de dependência — e, por consequência, como ela é podada.
 *
 * <p>A distinção existe porque os dois lados sabem coisas diferentes. Ver
 * {@code CONTRATO_DEPENDENCIAS_DE_CAPACIDADE.md}, seção "Poda".
 */
public enum DependencySource {

    /**
     * Declarada em {@code @HasPermission(requires = ...)} e descoberta pela varredura de subida.
     *
     * <p>A varredura tem a <b>lista completa</b> do que o código declara, então a reconciliação é
     * integral: cria o que falta e remove o que não é mais declarado.
     */
    SCAN,

    /**
     * Declarada por uma tela, no {@code POST /api/v1/resource/register}.
     *
     * <p>Quem registra conhece uma parte — um recurso pode ser declarado por mais de uma tela, e
     * cada uma envia só as ações que usa. A poda é <b>escopada à ação presente no payload</b>; ação
     * ausente não é tocada.
     */
    REGISTER
}
