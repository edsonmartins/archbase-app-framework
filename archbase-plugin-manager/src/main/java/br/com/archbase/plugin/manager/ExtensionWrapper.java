package br.com.archbase.plugin.manager;

import java.util.Objects;

/**
 * Um wrapper sobre instância de extensão.
 */
public class ExtensionWrapper<T> implements Comparable<ExtensionWrapper<T>> {

    private final ExtensionDescriptor descriptor;
    private final ExtensionFactory extensionFactory;
    private T extension; // cache

    public ExtensionWrapper(ExtensionDescriptor descriptor, ExtensionFactory extensionFactory) {
        this.descriptor = descriptor;
        this.extensionFactory = extensionFactory;
    }

    @SuppressWarnings("unchecked")
    public T getExtension() {
        if (extension == null) {
            extension = (T) extensionFactory.create(descriptor.extensionClass);
        }

        return extension;
    }

    public ExtensionDescriptor getDescriptor() {
        return descriptor;
    }

    public int getOrdinal() {
        return descriptor.ordinal;
    }

    @Override
    public int compareTo(ExtensionWrapper<T> o) {
        return (getOrdinal() - o.getOrdinal());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtensionWrapper<?> that = (ExtensionWrapper<?>) o;
        return Objects.equals(descriptor, that.descriptor) &&
                Objects.equals(extensionFactory, that.extensionFactory) &&
                Objects.equals(extension, that.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(descriptor, extensionFactory, extension);
    }
}
