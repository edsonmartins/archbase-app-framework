package br.com.archbase.workflow.process.contracts;

import java.util.UUID;

/**
 * Esta interface representa uma unidade de trabalho. As implementações desta interface devem:
 *
 * <ul>
 *      <li> capturar todas as exceções marcadas ou não e retornar um {@link WorkReport}
 *           da instância com um status de {@link WorkStatus#FAILED} e uma referência à exceção </li>
 *      <li> certifique-se de que o trabalho seja concluído em um período de tempo finito </li>
 * </ul>
 * <p>
 * O nome do trabalho deve ser exclusivo em uma definição de fluxo de trabalho.
 *
 * @author edson martins
 */
public interface Work {

    /**
     * O nome da unidade de trabalho. O nome deve ser exclusivo em uma definição de fluxo de trabalho.
     *
     * @return nome da unidade de trabalho.
     */
    default String getName() {
        return UUID.randomUUID().toString();
    }

    /**
     * Execute a unidade de trabalho e devolva seu relatório. Implementações são necessárias
     * para capturar qualquer exceção marcada ou não verificada e retornar uma instância {@link WorkReport}
     * com um status de {@link WorkStatus#FAILED} e uma referência à exceção.
     *
     * @param workContext contexto no qual esta unidade de trabalho está sendo executada
     * @return o relatório de execução
     */
    WorkReport execute(WorkContext workContext);
}
