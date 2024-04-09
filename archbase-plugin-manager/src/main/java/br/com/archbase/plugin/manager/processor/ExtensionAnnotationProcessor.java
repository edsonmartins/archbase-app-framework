                package br.com.archbase.plugin.manager.processor;

import br.com.archbase.plugin.manager.Extension;
import br.com.archbase.plugin.manager.ExtensionPoint;
import br.com.archbase.plugin.manager.util.ClassUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Processa anotações de {@link Extension} e gera um {@link ExtensionStorage}.
 * Você pode especificar o {@link ExtensionStorage} concreto por meio das opções de ambiente do processador
 * ({@link ProcessingEnvironment #getOptions()}) ou propriedade do sistema.
 * Em ambas as variantes, o nome da opção / propriedade é {@code archbase.storageClassName}.
 */

public class ExtensionAnnotationProcessor extends AbstractProcessor {

    private static final String STORAGE_CLASS_NAME = "archbase.storageClassName";
    private static final String IGNORE_EXTENSION_POINT = "archbase.ignoreExtensionPoint";

    private Map<String, Set<String>> extensions = new HashMap<>(); // a chave é o ponto de extensão
    private Map<String, Set<String>> oldExtensions = new HashMap<>(); // a chave é o ponto de extensão

    private ExtensionStorage storage;
    private boolean ignoreExtensionPoint;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        info("%s inicializado", ExtensionAnnotationProcessor.class.getName());
        info("Opçoes %s", processingEnv.getOptions());

        initStorage();
        initIgnoreExtensionPoint();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> options = new HashSet<>();
        options.add(STORAGE_CLASS_NAME);
        options.add(IGNORE_EXTENSION_POINT);

        return options;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        info("Processando @%s", Extension.class.getName());
        for (Element element : roundEnv.getElementsAnnotatedWith(Extension.class)) {
            if (element.getKind() != ElementKind.ANNOTATION_TYPE) {
                processExtensionElement(element);
            }
        }

        // coletar anotações de extensão aninhadas
        List<TypeElement> extensionAnnotations = new ArrayList<>();
        for (TypeElement annotation : annotations) {
            if (ClassUtils.getAnnotationMirror(annotation, Extension.class) != null) {
                extensionAnnotations.add(annotation);
            }
        }

        // processar anotações de extensão aninhadas
        for (TypeElement te : extensionAnnotations) {
            info("Processando @%s", te);
            for (Element element : roundEnv.getElementsAnnotatedWith(te)) {
                processExtensionElement(element);
            }
        }

        //leia extensões antigas
        oldExtensions = storage.read();
        for (Map.Entry<String, Set<String>> entry : oldExtensions.entrySet()) {
            String extensionPoint = entry.getKey();
            if (extensions.containsKey(extensionPoint)) {
                extensions.get(extensionPoint).addAll(entry.getValue());
            } else {
                extensions.put(extensionPoint, entry.getValue());
            }
        }

        // escrever extensões
        storage.write(extensions);

        return false;
    }

    public ProcessingEnvironment getProcessingEnvironment() {
        return processingEnv;
    }

    public void error(String message, Object... args) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(message, args));
    }

    public void error(Element element, String message, Object... args) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(message, args), element);
    }

    public void info(String message, Object... args) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format(message, args));
    }

    public void info(Element element, String message, Object... args) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, String.format(message, args), element);
    }

    public String getBinaryName(TypeElement element) {
        return processingEnv.getElementUtils().getBinaryName(element).toString();
    }

    public Map<String, Set<String>> getExtensions() {
        return extensions;
    }

    public Map<String, Set<String>> getOldExtensions() {
        return oldExtensions;
    }

    public ExtensionStorage getStorage() {
        return storage;
    }

    private void processExtensionElement(Element element) {
        // verifique se @Extension é colocado na classe e não no método ou construtor
        if (!(element instanceof TypeElement)) {
            error(element, "Coloque anotações apenas em classes (sem métodos, sem campos)");
            return;
        }

        // verifique se a classe estende / implementa um ponto de extensão
        if (!ignoreExtensionPoint && !isExtension(element.asType())) {
            error(element, "%s não é uma extensão (não implementa ExtensionPoint)", element);
            return;
        }

        TypeElement extensionElement = (TypeElement) element;
        List<TypeElement> extensionPointElements = findExtensionPoints(extensionElement);
        if (extensionPointElements.isEmpty()) {
            error(element, "Nenhum ponto de extensão encontrado para extensão %s", extensionElement);
            return;
        }

        String extension = getBinaryName(extensionElement);
        for (TypeElement extensionPointElement : extensionPointElements) {
            String extensionPoint = getBinaryName(extensionPointElement);
            Set<String> extensionPoints = extensions.computeIfAbsent(extensionPoint, k -> new TreeSet<>());
            extensionPoints.add(extension);
        }
    }

    @SuppressWarnings("all")
    private List<TypeElement> findExtensionPoints(TypeElement extensionElement) {
        List<TypeElement> extensionPointElements = new ArrayList<>();

        // usar pontos de extensão, que foram explicitamente definidos na anotação de extensão
        AnnotationValue annotatedExtensionPoints = ClassUtils.getAnnotationValue(extensionElement, Extension.class, "points");
        List<? extends AnnotationValue> extensionPointClasses = (annotatedExtensionPoints != null) ?
                (List<? extends AnnotationValue>) annotatedExtensionPoints.getValue() :
                null;
        if (extensionPointClasses != null && !extensionPointClasses.isEmpty()) {
            for (AnnotationValue extensionPointClass : extensionPointClasses) {
                String extensionPointClassName = extensionPointClass.getValue().toString();
                TypeElement extensionPointElement = processingEnv.getElementUtils().getTypeElement(extensionPointClassName);
                extensionPointElements.add(extensionPointElement);
            }
        }
        // detectar pontos de extensão automaticamente, se eles não estiverem configurados explicitamente (comportamento padrão)
        else {
            // pesquisa em interfaces
            List<? extends TypeMirror> interfaces = extensionElement.getInterfaces();
            for (TypeMirror item : interfaces) {
                boolean isExtensionPoint = processingEnv.getTypeUtils().isSubtype(item, getExtensionPointType());
                if (isExtensionPoint) {
                    extensionPointElements.add(getElement(item));
                }
            }

            // pesquisa na superclasse
            TypeMirror superclass = extensionElement.getSuperclass();
            if (superclass.getKind() != TypeKind.NONE) {
                boolean isExtensionPoint = processingEnv.getTypeUtils().isSubtype(superclass, getExtensionPointType());
                if (isExtensionPoint) {
                    extensionPointElements.add(getElement(superclass));
                }
            }

            // pegue a primeira interface
            if (extensionPointElements.isEmpty() && ignoreExtensionPoint) {
                if (interfaces.isEmpty()) {
                    error(extensionElement, "Não é possível usar %s como ponto de extensão com o argumento do compilador %s (não implementa nenhuma interface)",
                            extensionElement, IGNORE_EXTENSION_POINT);
                } else if (interfaces.size() == 1) {
                    extensionPointElements.add(getElement(interfaces.get(0)));
                } else {
                    error(extensionElement, "Não é possível usar %s como ponto de extensão com argumento do compilador %s (implementa várias interfaces)",
                            extensionElement, IGNORE_EXTENSION_POINT);
                }
            }
        }

        return extensionPointElements;
    }

    private boolean isExtension(TypeMirror typeMirror) {
        return processingEnv.getTypeUtils().isAssignable(typeMirror, getExtensionPointType());
    }

    private TypeMirror getExtensionPointType() {
        return processingEnv.getElementUtils().getTypeElement(ExtensionPoint.class.getName()).asType();
    }

    @SuppressWarnings("unchecked")
    private void initStorage() {
        // pesquisa em opções de processamento
        String storageClassName = processingEnv.getOptions().get(STORAGE_CLASS_NAME);
        if (storageClassName == null) {
            // pesquisa nas propriedades do sistema
            storageClassName = System.getProperty(STORAGE_CLASS_NAME);
        }

        if (storageClassName != null) {
            // use reflexão para criar a instância de armazenamento
            try {
                Class<?> storageClass = getClass().getClassLoader().loadClass(storageClassName);
                Constructor<?> constructor = storageClass.getConstructor(ExtensionAnnotationProcessor.class);
                storage = (ExtensionStorage) constructor.newInstance(this);
            } catch (Exception e) {
                error(e.getMessage());
            }
        }

        if (storage == null) {
            // armazenamento padrão
            storage = new LegacyExtensionStorage(this);
        }
    }

    private void initIgnoreExtensionPoint() {
        // pesquisar nas opções de processamento e propriedades do sistema
        ignoreExtensionPoint = getProcessingEnvironment().getOptions().containsKey(IGNORE_EXTENSION_POINT) ||
                System.getProperty(IGNORE_EXTENSION_POINT) != null;
    }

    private TypeElement getElement(TypeMirror typeMirror) {
        return (TypeElement) ((DeclaredType) typeMirror).asElement();
    }

}
