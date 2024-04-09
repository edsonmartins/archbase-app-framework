package br.com.archbase.workflow.process.contracts;

/**
 * Enumeração do status de execução do trabalho.
 *
 * @author edsonmartins
 */
public enum WorkStatus {


    /**
     * A unidade de trabalho iniciou.
     */
    STARTED("WORK STARTED"),

    /**
     * A unidade de trabalho falhou.
     */
    FAILED("WORK FAILED"),

    /**
     * A unidade de trabalho foi concluída com sucesso
     */
    COMPLETED("WORK COMPLETED");


    private final String value;

    WorkStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
