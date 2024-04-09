package br.com.archbase.plugin.manager;


public class ExtensionDescriptor {

    public final int ordinal;
    public final Class<?> extensionClass;

    public ExtensionDescriptor(int ordinal, Class<?> extensionClass) {
        this.ordinal = ordinal;
        this.extensionClass = extensionClass;
    }

}
