package br.com.archbase.security.diagnostics;

/**
 * Um item por trás de um número do panorama.
 *
 * <p>A forma é deliberadamente uniforme para os seis cards. Cada métrica devolve coisas diferentes —
 * permissões, usuários, ações, recursos —, mas quem abre o detalhe quer sempre a mesma coisa: que é,
 * onde está e por que apareceu nesta lista. Uma forma só significa uma tela só no cliente, em vez de
 * seis listas que divergem com o tempo.
 *
 * @param id     identificador da linha de origem, para quem quiser navegar até ela
 * @param label  o nome pelo qual a pessoa reconhece o item
 * @param detail onde ele vive — o recurso da ação, o e-mail do usuário, o destinatário da concessão
 * @param reason por que ele está nesta lista. É o campo que transforma número em explicação: sem
 *               ele, ver "ação X" numa lista de inativas não diz se o problema é a ação ou o recurso
 */
public record OverviewItem(String id, String label, String detail, String reason) {
}
