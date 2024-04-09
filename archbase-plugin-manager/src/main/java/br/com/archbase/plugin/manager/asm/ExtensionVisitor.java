package br.com.archbase.plugin.manager.asm;

import br.com.archbase.plugin.manager.Extension;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Este visitante extrai um {@link ExtensionInfo} de qualquer classe,
 * que contém uma anotação de {@link Extension}.
 * <p>
 * Os parâmetros de anotação são extraídos do código de bytes usando o
 * <a href="https://asm.ow2.io/"> Biblioteca ASM </a>. Isso torna possível
 * acesse os parâmetros de {@link Extension} sem carregar a classe em
 * o carregador de classes. Isso evita possíveis {@link NoClassDefFoundError}
 * para extensões, que não podem ser carregadas devido a dependências ausentes.
 */
class ExtensionVisitor extends ClassVisitor {

    private static final Logger log = LoggerFactory.getLogger(ExtensionVisitor.class);

    private static final int ASM_VERSION = Opcodes.ASM7;

    private final ExtensionInfo extensionInfo;

    ExtensionVisitor(ExtensionInfo extensionInfo) {
        super(ASM_VERSION);
        this.extensionInfo = extensionInfo;
    }

    @Override
    @SuppressWarnings("java:S3776")
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (!Type.getType(descriptor).getClassName().equals(Extension.class.getName())) {
            return super.visitAnnotation(descriptor, visible);
        }

        return new AnnotationVisitor(ASM_VERSION) {

            @Override
            public AnnotationVisitor visitArray(final String name) {
                if ("ordinal".equals(name) || "plugins".equals(name) || "points".equals(name)) {
                    return new AnnotationVisitor(ASM_VERSION, super.visitArray(name)) {

                        @Override
                        public void visit(String key, Object value) {
                            log.debug("Carregar atributo de anotação {} = {} ({})", name, value, value.getClass().getName());
                            if ("ordinal".equals(name)) {
                                extensionInfo.ordinal = Integer.parseInt(value.toString());
                            } else if ("plugins".equals(name)) {
                                if (value instanceof String) {
                                    log.debug("ArchbasePlugin encontrado {}", value);
                                    extensionInfo.plugins.add((String) value);
                                } else if (value instanceof String[]) {
                                    String parameters = Arrays.toString((String[]) value);
                                    log.debug("Plugins encontrados {}", parameters);
                                    extensionInfo.plugins.addAll(Arrays.asList((String[]) value));
                                } else {
                                    log.debug("ArchbasePlugin encontrado {}", value);
                                    extensionInfo.plugins.add(value.toString());
                                }
                            } else {
                                String pointClassName = ((Type) value).getClassName();
                                log.debug("Ponto encontrado {}", pointClassName);
                                extensionInfo.points.add(pointClassName);
                            }

                            super.visit(key, value);
                        }
                    };
                }

                return super.visitArray(name);
            }

        };
    }

}
