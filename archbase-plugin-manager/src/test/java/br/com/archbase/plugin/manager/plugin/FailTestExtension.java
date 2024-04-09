package br.com.archbase.plugin.manager.plugin;

import br.com.archbase.plugin.manager.Extension;


@Extension
public class FailTestExtension implements TestExtensionPoint {

    public FailTestExtension(String name) {

    }

    @Override
    public String saySomething() {
        return "Eu sou uma extensão de teste de falha";
    }

}
