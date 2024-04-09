package br.com.archbase.shared.kernel.bean.metadata;

import lombok.extern.slf4j.Slf4j;

import java.lang.ref.Reference;
import java.lang.reflect.Method;

/**
 * Um PropertyDescriptor descreve uma propriedade que um Java Bean
 * exporta por meio de um par de métodos de acesso.
 */
@Slf4j
public class PropertyDescriptor extends FeatureDescriptor {

    private Reference<?> propertyTypeRef;
    private Reference<?> readMethodRef;
    private Reference<?> writeMethodRef;
    private Reference<?> propertyEditorClassRef;

    private boolean bound;
    private boolean constrained;

    // O nome de base do nome do método que será prefixado com o
    // método de leitura e gravação. Se name == "foo" então o baseName é "Foo"
    private String baseName;

    private String writeMethodName;
    private String readMethodName;

    /**
     * Constrói um PropertyDescriptor para uma propriedade que segue
     * a convenção Java padrão com getFoo e setFoo
     * métodos de acesso. Assim, se o nome do argumento for "fred",
     * suponha que o método de gravação seja "setFred" e o método de leitura
     * é "getFred" (ou "isFred" para uma propriedade booleana). Observe que o
     * o nome da propriedade deve começar com um caractere minúsculo, que
     * ser capitalizado nos nomes dos métodos.
     *
     * @param propertyName O nome programático da propriedade.
     * @param beanClass    O objeto Class para o bean de destino. Para
     *                     exemplo sun.beans.OurButton.class.
     * @throws IntrospectionException se ocorrer uma exceção durante
     *                                introspecção.
     */
    public PropertyDescriptor(String propertyName, Class<?> beanClass) throws IntrospectionException {
        this(propertyName, beanClass, "is" + capitalize(propertyName), "set" + capitalize(propertyName));
    }

    /**
     * Este construtor recebe o nome de uma propriedade simples e método
     * nomes para leitura e escrita da propriedade.
     *
     * @param propertyName    O nome programático da propriedade.
     * @param beanClass       O objeto Class para o bean de destino. Para
     *                        exemplo sun.beans.OurButton.class.
     * @param readMethodName  O nome do método usado para ler a propriedade
     *                        valor. Pode ser nulo se a propriedade for somente gravação.
     * @param writeMethodName O nome do método usado para escrever a propriedade
     *                        valor. Pode ser nulo se a propriedade for somente leitura.
     * @throws IntrospectionException se ocorrer uma exceção durante
     *                                introspecção.
     */
    public PropertyDescriptor(String propertyName, Class<?> beanClass, String readMethodName,
                              String writeMethodName) throws IntrospectionException {
        if (beanClass == null) {
            throw new IntrospectionException("A classe Target Bean é nula");
        }
        if (propertyName == null || propertyName.length() == 0) {
            throw new IntrospectionException("Nome de propriedade ruim");
        }
        if ("".equals(readMethodName) || "".equals(writeMethodName)) {
            throw new IntrospectionException("O nome do método de leitura ou gravação não deve ser uma string vazia");
        }
        setName(propertyName);
        setClass0(beanClass);

        this.readMethodName = readMethodName;
        if (readMethodName != null && getReadMethod() == null) {
            throw new IntrospectionException("Método não encontrado: " + readMethodName);
        }
        this.writeMethodName = writeMethodName;
        if (writeMethodName != null && getWriteMethod() == null) {
            throw new IntrospectionException("Método não encontrado: " + writeMethodName);
        }

    }

    /**
     * Este construtor recebe o nome de uma propriedade simples e Método
     * objetos para ler e escrever a propriedade.
     *
     * @param propertyName O nome programático da propriedade.
     * @param readMethod   O método usado para ler o valor da propriedade.
     *                     Pode ser nulo se a propriedade for somente gravação.
     * @param writeMethod  O método usado para escrever o valor da propriedade.
     *                     Pode ser nulo se a propriedade for somente leitura.
     * @throws IntrospectionException se ocorrer uma exceção durante
     *                                introspecção.
     */
    public PropertyDescriptor(String propertyName, Method readMethod, Method writeMethod)
            throws IntrospectionException {
        if (propertyName == null || propertyName.length() == 0) {
            throw new IntrospectionException("nome de propriedade ruim");
        }
        setName(propertyName);
        setReadMethod(readMethod);
        setWriteMethod(writeMethod);
    }

    /**
     * Construtor de pacote privado.
     * Mesclar dois descritores de propriedade. Onde eles entrarem em conflito, dê o
     * prioridade do segundo argumento (y) sobre o primeiro argumento (x).
     *
     * @param x O primeiro (prioridade mais baixa) PropertyDescriptor
     * @param y O segundo (prioridade mais alta) PropertyDescriptor
     */
    @SuppressWarnings("java:S3776")
    PropertyDescriptor(PropertyDescriptor x, PropertyDescriptor y) {
        super(x, y);

        if (y.baseName != null) {
            baseName = y.baseName;
        } else {
            baseName = x.baseName;
        }

        if (y.readMethodName != null) {
            readMethodName = y.readMethodName;
        } else {
            readMethodName = x.readMethodName;
        }

        if (y.writeMethodName != null) {
            writeMethodName = y.writeMethodName;
        } else {
            writeMethodName = x.writeMethodName;
        }

        if (y.propertyTypeRef != null) {
            propertyTypeRef = y.propertyTypeRef;
        } else {
            propertyTypeRef = x.propertyTypeRef;
        }

        // Descubra o método de leitura mesclado.
        Method xr = x.getReadMethod();
        Method yr = y.getReadMethod();

        // Normalmente dá prioridade ao readMethod de y.
        try {
            if (yr != null && yr.getDeclaringClass() == getClass0()) {
                setReadMethod(yr);
            } else {
                setReadMethod(xr);
            }
        } catch (IntrospectionException ex) {
            //
        }

        // No entanto, se xey referirem métodos de leitura na mesma classe,
        // dá prioridade a um método booleano "is" sobre um método booleano "get".
        if (xr != null && yr != null && xr.getDeclaringClass() == yr.getDeclaringClass()
                && xr.getReturnType() == boolean.class && yr.getReturnType() == boolean.class
                && xr.getName().indexOf("is") == 0 && yr.getName().indexOf("get") == 0) {
            try {
                setReadMethod(xr);
            } catch (IntrospectionException ex) {
                //
            }
        }

        Method xw = x.getWriteMethod();
        Method yw = y.getWriteMethod();

        try {
            if (yw != null && yw.getDeclaringClass() == getClass0()) {
                setWriteMethod(yw);
            } else {
                setWriteMethod(xw);
            }
        } catch (IntrospectionException ex) {
            //
        }

        if (y.getPropertyEditorClass() != null) {
            setPropertyEditorClass(y.getPropertyEditorClass());
        } else {
            setPropertyEditorClass(x.getPropertyEditorClass());
        }

        bound = x.bound || y.bound;
        constrained = x.constrained || y.constrained;
    }

    /**
     * Construtor dup privado de pacote.
     * Isso deve isolar o novo objeto de quaisquer alterações no objeto antigo.
     */
    PropertyDescriptor(PropertyDescriptor old) {
        super(old);
        propertyTypeRef = old.propertyTypeRef;
        readMethodRef = old.readMethodRef;
        writeMethodRef = old.writeMethodRef;
        propertyEditorClassRef = old.propertyEditorClassRef;

        writeMethodName = old.writeMethodName;
        readMethodName = old.readMethodName;
        baseName = old.baseName;

        bound = old.bound;
        constrained = old.constrained;
    }

    /**
     * Obtém o objeto Class para a propriedade.
     *
     * @return As informações do tipo Java para a propriedade. Observe que
     * o objeto "Class" pode descrever um tipo Java integrado, como
     * "int".
     * O resultado pode ser "nulo" se esta for uma propriedade indexada que
     * não suporta acesso não indexado.
     * <p>
     * Este é o tipo que será retornado pelo ReadMethod.
     */
    public synchronized Class<?> getPropertyType() {
        Class<?> type = getPropertyType0();
        if (type == null) {
            try {
                type = findPropertyType(getReadMethod(), getWriteMethod());
                setPropertyType(type);
            } catch (IntrospectionException ex) {
                //
            }
        }
        return type;
    }

    private synchronized void setPropertyType(Class<?> type) {
        propertyTypeRef = createReference(type);
    }

    private Class<?> getPropertyType0() {
        return (Class<?>) getObject(propertyTypeRef);
    }

    /**
     * Obtém o método que deve ser usado para ler o valor da propriedade.
     *
     * @return O método que deve ser usado para ler o valor da propriedade.
     * Pode retornar nulo se a propriedade não puder ser lida.
     */
    @SuppressWarnings("java:S3776")
    public synchronized Method getReadMethod() {
        Method readMethod = getReadMethod0();
        if (readMethod == null) {
            Class<?> cls = getClass0();
            if (cls == null || (readMethodName == null && readMethodRef == null)) {
                // O método de leitura foi explicitamente definido como nulo.
                return null;
            }
            if (readMethodName == null) {
                Class<?> type = getPropertyType0();
                if (type == boolean.class || type == null) {
                    readMethodName = "is" + getBaseName();
                } else {
                    readMethodName = "get" + getBaseName();
                }
            }
            // Uma vez que pode haver vários métodos de gravação, mas apenas um getter
            // método, encontre o método getter primeiro para que você saiba qual
            // tipo de propriedade é. Para booleanos, pode haver métodos "is" e "get".
            // Se existir um método "is", este é o método oficial
            // do leitor, então procure por este primeiro.
            readMethod = BeanUtils.findAccessibleMethodIncludeInterfaces(cls, readMethodName, 0, null);
            if (readMethod == null) {
                readMethodName = "get" + getBaseName();
                readMethod = BeanUtils.findAccessibleMethodIncludeInterfaces(cls, readMethodName, 0, null);
            }
            try {
                setReadMethod(readMethod);
            } catch (IntrospectionException ex) {
                //
            }
        }
        return readMethod;
    }

    /**
     * Define o método que deve ser usado para ler o valor da propriedade.
     *
     * @param readMethod O novo método de leitura.
     */
    public synchronized void setReadMethod(Method readMethod) throws IntrospectionException {
        if (readMethod == null) {
            readMethodName = null;
            readMethodRef = null;
            return;
        }
        // O tipo de propriedade é determinado pelo método de leitura.
        setPropertyType(findPropertyType(readMethod, getWriteMethod0()));
        setClass0(readMethod.getDeclaringClass());

        readMethodName = readMethod.getName();
        readMethodRef = createReference(readMethod, true);
    }

    /**
     * Obtém o método que deve ser usado para escrever o valor da propriedade.
     *
     * @return O método que deve ser usado para escrever o valor da propriedade.
     * Pode retornar nulo se a propriedade não puder ser gravada.
     */
    @SuppressWarnings("java:S3776")
    public synchronized Method getWriteMethod() {
        Method writeMethod = getWriteMethod0();
        if (writeMethod == null) {
            Class<?> cls = getClass0();
            if (cls == null || (writeMethodName == null && writeMethodRef == null)) {
                // O método de gravação foi explicitamente definido como nulo.
                return null;
            }

            // Precisamos do tipo para buscar o método correto.
            Class<?> type = getPropertyType0();
            if (type == null) {
                try {
                    // Não é possível usar getPropertyType, pois levará a recursiva
                    // ciclo.
                    type = findPropertyType(getReadMethod(), null);
                    setPropertyType(type);
                } catch (IntrospectionException ex) {
                    // Sem o tipo de propriedade correto, não podemos garantir
                    // para encontrar o método correto.
                    return null;
                }
            }

            if (writeMethodName == null) {
                writeMethodName = "set" + getBaseName();
            }

            writeMethod = BeanUtils.findAccessibleMethodIncludeInterfaces(cls, writeMethodName, 1, (type == null) ? null
                    : new Class[]{type});
            try {
                setWriteMethod(writeMethod);
            } catch (IntrospectionException ex) {
                //
            }
        }
        return writeMethod;
    }

    /**
     * Define o método que deve ser usado para escrever o valor da propriedade.
     *
     * @param writeMethod O novo método de gravação.
     */
    public synchronized void setWriteMethod(Method writeMethod) throws IntrospectionException {
        if (writeMethod == null) {
            writeMethodName = null;
            writeMethodRef = null;
            return;
        }
        // Defina o tipo de propriedade - que valida o método
        setPropertyType(findPropertyType(getReadMethod(), writeMethod));
        setClass0(writeMethod.getDeclaringClass());

        writeMethodName = writeMethod.getName();
        writeMethodRef = createReference(writeMethod, true);

    }

    private Method getReadMethod0() {
        return (Method) getObject(readMethodRef);
    }

    private Method getWriteMethod0() {
        return (Method) getObject(writeMethodRef);
    }

    /**
     * Substituído para garantir que uma superclasse não tenha precedência
     */
    @Override
    void setClass0(Class<?> clz) {
        if (getClass0() != null && clz.isAssignableFrom(getClass0())) {
            // não substitua uma subclasse por uma superclasse
            return;
        }
        super.setClass0(clz);
    }

    /**
     * Atualizações nas propriedades "associadas" causarão um evento "PropertyChange" para
     * seja demitido quando a propriedade for alterada.
     *
     * @return True se esta for uma propriedade vinculada.
     */
    public boolean isBound() {
        return bound;
    }

    /**
     * Atualizações nas propriedades "associadas" causarão um evento "PropertyChange" para
     * seja demitido quando a propriedade for alterada.
     *
     * @param bound Verdadeiro se esta for uma propriedade vinculada.
     */
    public void setBound(boolean bound) {
        this.bound = bound;
    }

    /**
     * Tentativas de atualizações nas propriedades "restritas" causarão um
     * "VetoableChange"
     * evento a ser disparado quando a propriedade é alterada.
     *
     * @return True se esta for uma propriedade restrita.
     */
    public boolean isConstrained() {
        return constrained;
    }

    /**
     * Tentativas de atualizações nas propriedades "restritas" causarão um
     * "VetoableChange"
     * evento a ser disparado quando a propriedade é alterada.
     *
     * @param constrained Verdadeiro se esta for uma propriedade restrita.
     */
    public void setConstrained(boolean constrained) {
        this.constrained = constrained;
    }

    /**
     * Obtém qualquer classe PropertyEditor explícita que foi registrada
     * para esta propriedade.
     *
     * @return Qualquer classe PropertyEditor explícita que foi registrada
     * para esta propriedade. Normalmente, isso retornará "nulo",
     * indicando que nenhum editor especial foi registrado,
     * portanto, o PropertyEditorManager deve ser usado para localizar
     * um PropertyEditor adequado.
     */
    public Class<?> getPropertyEditorClass() {
        return (Class) getObject(propertyEditorClassRef);
    }

    /**
     * Normalmente, os PropertyEditors serão encontrados usando o PropertyEditorManager.
     * No entanto, se por algum motivo você deseja associar um determinado
     * PropertyEditor com uma determinada propriedade, então você pode fazer isso com
     * este método.
     *
     * @param propertyEditorClass A classe do PropertyEditor desejado.
     */
    public void setPropertyEditorClass(Class<?> propertyEditorClass) {
        propertyEditorClassRef = createReference(propertyEditorClass);
    }

    /**
     * Compara este <code> PropertyDescriptor </code> com o especificado
     * objeto.
     *
     * @return true se os objetos são iguais. Dois
     * <code> PropertyDescriptor </code> s
     * são os mesmos se os tipos de leitura, gravação, propriedade, editor de propriedade e
     * sinalizadores são equivalentes.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PropertyDescriptor) {
            PropertyDescriptor other = (PropertyDescriptor) obj;
            Method otherReadMethod = other.getReadMethod();
            Method otherWriteMethod = other.getWriteMethod();

            if (!compareMethods(getReadMethod(), otherReadMethod)) {
                return false;
            }

            if (!compareMethods(getWriteMethod(), otherWriteMethod)) {
                return false;
            }

            if (getPropertyType() == other.getPropertyType()
                    && getPropertyEditorClass() == other.getPropertyEditorClass()
                    && bound == other.isBound() && constrained == other.isConstrained()
                    && writeMethodName.equals(other.writeMethodName) && readMethodName.equals(other.readMethodName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Método auxiliar privado de pacote para métodos Descriptor .equals.
     *
     * @param a primeiro método para comparar
     * @param b segundo método para comparar
     * @return boolean para indicar que os métodos são equivalentes
     */
    boolean compareMethods(Method a, Method b) {
        // Nota: talvez este deva ser um método protegido em FeatureDescriptor
        if ((a == null) != (b == null)) {
            return false;
        }
        return (!((a != null && b != null) && (!a.equals(b))));
    }

    /**
     * Retorna o tipo de propriedade que corresponde ao método de leitura e gravação.
     * A precedência de tipo é fornecida ao readMethod.
     *
     * @return o tipo do descritor de propriedade ou nulo se ambos
     * os métodos de leitura e gravação são nulos.
     * @throws IntrospectionException se o método de leitura ou gravação for inválido
     */
    private Class<?> findPropertyType(Method readMethod, Method writeMethod) throws IntrospectionException {
        Class<?> propertyType = null;
        try {
            if (readMethod != null) {
                Class[] params = readMethod.getParameterTypes();
                if (params.length != 0) {
                    throw new IntrospectionException("Incorreto método de leitura. Número de argumentos: " + readMethod);
                }
                propertyType = readMethod.getReturnType();
                if (propertyType == Void.TYPE) {
                    throw new IntrospectionException("Método de leitura " + readMethod.getName()
                            + " retorna vazio");
                }
            }
            if (writeMethod != null) {
                Class<?>[] params = writeMethod.getParameterTypes();
                if (params.length != 1) {
                    throw new IntrospectionException("Incorreto método de leitura. Número de argumentos: " + writeMethod);
                }
                if (propertyType != null && propertyType != params[0]) {
                    throw new IntrospectionException("type mismatch between read and write methods");
                }
                propertyType = params[0];
            }
        } catch (IntrospectionException ex) {
            log.error(ex.getMessage());
            throw ex;
        }
        return propertyType;
    }

    /**
     * Retorna um valor de código hash para o objeto.
     * Consulte {@link Object # hashCode} para uma descrição completa.
     *
     * @return um valor de código hash para este objeto.
     */
    public int hashCode() {
        int result = 7;

        result = 37 * result + ((getPropertyType() == null) ? 0 : getPropertyType().hashCode());
        result = 37 * result + ((getReadMethod() == null) ? 0 : getReadMethod().hashCode());
        result = 37 * result + ((getWriteMethod() == null) ? 0 : getWriteMethod().hashCode());
        result = 37 * result
                + ((getPropertyEditorClass() == null) ? 0 : getPropertyEditorClass().hashCode());
        result = 37 * result + ((writeMethodName == null) ? 0 : writeMethodName.hashCode());
        result = 37 * result + ((readMethodName == null) ? 0 : readMethodName.hashCode());
        result = 37 * result + getName().hashCode();
        result = 37 * result + (bound ? 1 : 0);
        result = 37 * result + (constrained ? 1 : 0);

        return result;
    }

    String getBaseName() {
        if (baseName == null) {
            baseName = capitalize(getName());
        }
        return baseName;
    }

}
