package br.com.archbase.plugin.manager.plugin;

import br.com.archbase.plugin.manager.Extension;


@Extension
public class TestExtension implements TestExtensionPoint {


    @Override
    public String saySomething() {
        return "Eu sou uma extensão de teste";
    }

}
