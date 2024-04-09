package br.com.archbase.plugin.manager.plugin;

/**
 * Define a interface para classes que sabem fornecer dados de classe para um nome de classe.
 * A ideia é ter a possibilidade de recuperar os dados de uma classe de diferentes fontes:
 * <ul>
 * <li> Caminho da classe - a classe já está carregada pelo carregador de classes </li>
 * <li> String - a string (o código-fonte) é compilada dinamicamente através de {@link javax.tools.JavaCompiler} </>
 * <li> Gere o código-fonte programaticamente usando algo como {@code https://github.com/square/javapoet} </li>
 * </ul>
 */
public interface ClassDataProvider {

    byte[] getClassData(String className);

}
