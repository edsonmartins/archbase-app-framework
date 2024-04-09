package br.com.archbase.shared.kernel.bean.metadata;

import java.lang.ref.Reference;
import java.lang.reflect.Method;

/**
 * Um IndexedPropertyDescriptor descreve uma propriedade que atua como uma matriz e tem uma leitura indexada e / ou indexada
 * método de gravação para acessar elementos específicos do array.
 * <p>
 * Uma propriedade indexada também pode fornecer métodos simples de leitura e gravação não indexados. Se estiverem presentes, eles lêem e
 * escrever matrizes do tipo retornado pelo método de leitura indexado.
 */

public class IndexedPropertyDescriptor extends PropertyDescriptor {


    private Reference<Object> indexedPropertyTypeRef;
    private Reference<Object> indexedReadMethodRef;
    private Reference<Object> indexedWriteMethodRef;

    private String indexedReadMethodName;
    private String indexedWriteMethodName;

    /**
     * Este construtor constrói um IndexedPropertyDescriptor para uma propriedade que segue o padrão Java
     * convenções por ter os métodos de acesso getFoo e setFoo, para acesso indexado e acesso à matriz.
     * <p>
     * Assim, se o nome do argumento for "fred", ele assumirá que existe um método de leitor indexado "getFred", um
     * método de leitor não indexado (array), também chamado de "getFred", um método de gravação indexado "setFred" e, finalmente, um
     * método de gravação não indexado "setFred".
     *
     * @param propertyName O nome programático da propriedade.
     * @param beanClass    O objeto Class para o bean de destino.
     * @throws IntrospectionException se ocorrer uma exceção durante a introspecção.
     */
    public IndexedPropertyDescriptor(String propertyName, Class<?> beanClass) throws IntrospectionException {
        this(propertyName, beanClass, "get" + capitalize(propertyName), "set" + capitalize(propertyName), "get" + capitalize(propertyName), "set"
                + capitalize(propertyName));
    }

    /**
     * Este construtor recebe o nome de uma propriedade simples e nomes de métodos para ler e escrever a propriedade, ambos
     * indexado e não indexado.
     *
     * @param propertyName           O nome programático da propriedade.
     * @param beanClass              O objeto Class para o bean de destino.
     * @param readMethodName         O nome do método usado para ler os valores da propriedade como uma matriz. Pode ser nulo se a propriedade
     *                               é somente gravação ou deve ser indexado.
     * @param writeMethodName        O nome do método usado para escrever os valores da propriedade como uma matriz. Pode ser nulo se a propriedade
     *                               é somente leitura ou deve ser indexado.
     * @param indexedReadMethodName  O nome do método usado para ler um valor de propriedade indexado. Pode ser nulo se a propriedade for
     *                               somente gravação.
     * @param indexedWriteMethodName O nome do método usado para escrever um valor de propriedade indexado. Pode ser nulo se a propriedade for
     *                               somente leitura.
     * @throws IntrospectionException se ocorrer uma exceção durante a introspecção.
     */
    public IndexedPropertyDescriptor(String propertyName, Class<?> beanClass, String readMethodName, String writeMethodName, String indexedReadMethodName,
                                     String indexedWriteMethodName) throws IntrospectionException {
        super(propertyName, beanClass, readMethodName, writeMethodName);

        this.indexedReadMethodName = indexedReadMethodName;
        if (indexedReadMethodName != null && getIndexedReadMethod() == null) {
            throw new IntrospectionException("IntrospectionException " + indexedReadMethodName);
        }

        this.indexedWriteMethodName = indexedWriteMethodName;
        if (indexedWriteMethodName != null && getIndexedWriteMethod() == null) {
            throw new IntrospectionException("IntrospectionException " + indexedReadMethodName);
        }
        // Implemented only for type checking.
        findIndexedPropertyType(getIndexedReadMethod(), getIndexedWriteMethod());
    }

    /**
     * Este construtor recebe o nome de uma propriedade simples e objetos Method para ler e escrever a propriedade.
     *
     * @param propertyName       O nome programático da propriedade.
     * @param readMethod         O método usado para ler os valores da propriedade como uma matriz. Pode ser nulo se a propriedade for somente gravação
     *                           ou deve ser indexado.
     * @param writeMethod        O método usado para escrever os valores da propriedade como um array. Pode ser nulo se a propriedade for somente leitura
     *                           ou deve ser indexado.
     * @param indexedReadMethod  O método usado para ler um valor de propriedade indexado. Pode ser nulo se a propriedade for somente gravação.
     * @param indexedWriteMethod O método usado para escrever um valor de propriedade indexado. Pode ser nulo se a propriedade for somente leitura.
     * @throws IntrospectionException se ocorrer uma exceção durante a introspecção.
     */
    public IndexedPropertyDescriptor(String propertyName, Method readMethod, Method writeMethod, Method indexedReadMethod, Method indexedWriteMethod)
            throws IntrospectionException {
        super(propertyName, readMethod, writeMethod);

        setIndexedReadMethod0(indexedReadMethod);
        setIndexedWriteMethod0(indexedWriteMethod);

        // Verificação de tipo
        setIndexedPropertyType(findIndexedPropertyType(indexedReadMethod, indexedWriteMethod));
    }

    /**
     * Construtor de pacote privado. Mescle dois descritores de propriedade. Onde houver conflito, dê o segundo argumento (y)
     * prioridade sobre o primeiro argumento (x).
     *
     * @param x O primeiro (prioridade mais baixa) PropertyDescriptor
     * @param y O segundo (prioridade mais alta) PropertyDescriptor
     */

    @SuppressWarnings("all")
    IndexedPropertyDescriptor(PropertyDescriptor x, PropertyDescriptor y) {
        super(x, y);
        if (x instanceof IndexedPropertyDescriptor) {
            IndexedPropertyDescriptor ix = (IndexedPropertyDescriptor) x;
            try {
                Method xr = ix.getIndexedReadMethod();
                if (xr != null) {
                    setIndexedReadMethod(xr);
                }

                Method xw = ix.getIndexedWriteMethod();
                if (xw != null) {
                    setIndexedWriteMethod(xw);
                }
            } catch (IntrospectionException ex) {
                // Não deveria acontecer
                throw new AssertionError(ex);
            }
        }
        if (y instanceof IndexedPropertyDescriptor) {
            IndexedPropertyDescriptor iy = (IndexedPropertyDescriptor) y;
            try {
                Method yr = iy.getIndexedReadMethod();
                if (yr != null && yr.getDeclaringClass() == getClass0()) {
                    setIndexedReadMethod(yr);
                }

                Method yw = iy.getIndexedWriteMethod();
                if (yw != null && yw.getDeclaringClass() == getClass0()) {
                    setIndexedWriteMethod(yw);
                }
            } catch (IntrospectionException ex) {
                // Não deveria acontecer
                throw new AssertionError(ex);
            }
        }
    }

    /**
     * Package-private dup constructor This must isolate the new object from any changes to the old object.
     */
    IndexedPropertyDescriptor(IndexedPropertyDescriptor old) {
        super(old);
        indexedReadMethodRef = old.indexedReadMethodRef;
        indexedWriteMethodRef = old.indexedWriteMethodRef;
        indexedPropertyTypeRef = old.indexedPropertyTypeRef;
        indexedWriteMethodName = old.indexedWriteMethodName;
        indexedReadMethodName = old.indexedReadMethodName;
    }

    /**
     * Obtém o método que deve ser usado para ler um valor de propriedade indexado.
     *
     * @return O método que deve ser usado para ler um valor de propriedade indexado. Pode retornar nulo se a propriedade não for
     * indexado ou é somente gravação.
     */
    public synchronized Method getIndexedReadMethod() {
        Method indexedReadMethod = getIndexedReadMethod0();
        if (indexedReadMethod == null) {
            Class<?> cls = getClass0();
            if (cls == null || (indexedReadMethodName == null && indexedReadMethodRef == null)) {
                // o readMethod indexado foi explicitamente definido como nulo.
                return null;
            }
            if (indexedReadMethodName == null) {
                Class<?> type = getIndexedPropertyType0();
                if (type == boolean.class || type == null) {
                    indexedReadMethodName = "is" + getBaseName();
                } else {
                    indexedReadMethodName = "get" + getBaseName();
                }
            }

            Class<?>[] args = {int.class};

            indexedReadMethod = BeanUtils.findAccessibleMethodIncludeInterfaces(cls, indexedReadMethodName, 1, args);
            if (indexedReadMethod == null) {
                // nenhum método "is", então procure um método "get".
                indexedReadMethodName = "get" + getBaseName();
                indexedReadMethod = BeanUtils.findAccessibleMethodIncludeInterfaces(cls, indexedReadMethodName, 1, args);
            }
            setIndexedReadMethod0(indexedReadMethod);
        }
        return indexedReadMethod;
    }

    /**
     * Define o método que deve ser usado para ler um valor de propriedade indexado.
     *
     * @param readMethod O novo método de leitura indexado.
     */
    public synchronized void setIndexedReadMethod(Method readMethod) throws IntrospectionException {

        // o tipo de propriedade indexada é definido pelo leitor.
        setIndexedPropertyType(findIndexedPropertyType(readMethod, getIndexedWriteMethod0()));
        setIndexedReadMethod0(readMethod);
    }

    /**
     * Obtém o método que deve ser usado para escrever um valor de propriedade indexado.
     *
     * @return O método que deve ser usado para escrever um valor de propriedade indexado. Pode retornar nulo se a propriedade não for
     * indexado ou é somente leitura.
     */
    @SuppressWarnings("all")
    public synchronized Method getIndexedWriteMethod() {
        Method indexedWriteMethod = getIndexedWriteMethod0();
        if (indexedWriteMethod == null) {
            Class<?> cls = getClass0();
            if (cls == null || (indexedWriteMethodName == null && indexedWriteMethodRef == null)) {
                // o writeMethod Indexado foi explicitamente definido como nulo.
                return null;
            }

            // Precisamos do tipo indexado para garantir que obtemos o correto
            // método.
            // Não é possível usar o método getIndexedPropertyType, pois isso poderia
            // resulta em um loop infinito.
            Class<?> type = getIndexedPropertyType0();
            if (type == null) {
                try {
                    type = findIndexedPropertyType(getIndexedReadMethod(), null);
                    setIndexedPropertyType(type);
                } catch (IntrospectionException ex) {
                    // Set iprop type to be the classic type
                    Class<?> propType = getPropertyType();
                    if (propType.isArray()) {
                        type = propType.getComponentType();
                    }
                }
            }

            if (indexedWriteMethodName == null) {
                indexedWriteMethodName = "set" + getBaseName();
            }
            indexedWriteMethod = BeanUtils.findAccessibleMethodIncludeInterfaces(cls, indexedWriteMethodName, 2, (type == null) ? null : new Class[]{
                    int.class, type});
            setIndexedWriteMethod0(indexedWriteMethod);
        }
        return indexedWriteMethod;
    }

    /**
     * Define o método que deve ser usado para escrever um valor de propriedade indexado.
     *
     * @param writeMethod O novo método de gravação indexado.
     */
    public synchronized void setIndexedWriteMethod(Method writeMethod) throws IntrospectionException {

        // Se o tipo de propriedade indexada não foi definido, defina-o.
        Class<?> type = findIndexedPropertyType(getIndexedReadMethod(), writeMethod);
        setIndexedPropertyType(type);
        setIndexedWriteMethod0(writeMethod);
    }

    /**
     * Obtém o objeto <code> Class </code> do tipo das propriedades indexadas. A <code> Classe </code> retornada pode descrever
     * um tipo primitivo como <code> int </code>.
     *
     * @return A <code> Classe </code> para o tipo de propriedades indexadas; pode retornar <code> null </code> se o tipo não puder
     * seja determinado.
     */
    public synchronized Class<?> getIndexedPropertyType() {
        Class<?> type = getIndexedPropertyType0();
        if (type == null) {
            try {
                type = findIndexedPropertyType(getIndexedReadMethod(), getIndexedWriteMethod());
                setIndexedPropertyType(type);
            } catch (IntrospectionException ex) {
                //
            }
        }
        return type;
    }

    // Métodos privados que definem obter / definir os objetos de referência

    private synchronized void setIndexedPropertyType(Class<?> type) {
        indexedPropertyTypeRef = createReference(type);
    }

    private Class<?> getIndexedPropertyType0() {
        return (Class) getObject(indexedPropertyTypeRef);
    }

    private Method getIndexedReadMethod0() {
        return (Method) getObject(indexedReadMethodRef);
    }

    private void setIndexedReadMethod0(Method readMethod) {
        if (readMethod == null) {
            indexedReadMethodName = null;
            indexedReadMethodRef = null;
            return;
        }
        setClass0(readMethod.getDeclaringClass());

        indexedReadMethodName = readMethod.getName();
        indexedReadMethodRef = createReference(readMethod);
    }

    private Method getIndexedWriteMethod0() {
        return (Method) getObject(indexedWriteMethodRef);
    }

    private void setIndexedWriteMethod0(Method writeMethod) {
        if (writeMethod == null) {
            indexedWriteMethodName = null;
            indexedWriteMethodRef = null;
            return;
        }
        setClass0(writeMethod.getDeclaringClass());

        indexedWriteMethodName = writeMethod.getName();
        indexedWriteMethodRef = createReference(writeMethod);
    }

    @SuppressWarnings("all")
    private Class<?> findIndexedPropertyType(Method indexedReadMethod, Method indexedWriteMethod) throws IntrospectionException {
        Class<?> indexedPropertyType = null;

        if (indexedReadMethod != null) {
            Class<?>[] params = indexedReadMethod.getParameterTypes();
            if (params.length != 1) {
                throw new IntrospectionException("Contagem de argumentos de método de leitura indexado incorreto.");
            }
            if (params[0] != Integer.TYPE) {
                throw new IntrospectionException("Índice não interno para método de leitura indexado.");
            }
            indexedPropertyType = indexedReadMethod.getReturnType();
            if (indexedPropertyType == Void.TYPE) {
                throw new IntrospectionException("Método de leitura indexado retorna void.");
            }
        }
        if (indexedWriteMethod != null) {
            Class<?>[] params = indexedWriteMethod.getParameterTypes();
            if (params.length != 2) {
                throw new IntrospectionException("Contagem de argumentos de método de gravação indexado incorreto.");
            }
            if (params[0] != Integer.TYPE) {
                throw new IntrospectionException("Índice não interno para método de gravação indexado");
            }
            if (indexedPropertyType != null && indexedPropertyType != params[1]) {
                throw new IntrospectionException("Incompatibilidade de tipo entre os métodos de leitura e gravação indexados: " + getName());
            }
            indexedPropertyType = params[1];
        }
        Class<?> propertyType = getPropertyType();
        if (propertyType != null && (!propertyType.isArray() || propertyType.getComponentType() != indexedPropertyType)) {
            throw new IntrospectionException("Incompatibilidade de tipo entre métodos indexados e não indexados: " + getName());
        }
        return indexedPropertyType;
    }

    /**
     * Compara este <code> PropertyDescriptor </code> com o objeto especificado. Retorna verdadeiro se os objetos são
     * mesmo. Dois <code> PropertyDescriptor </code> s são os mesmos se os tipos de leitura, gravação, propriedade, editor de propriedade e
     * sinalizadores são equivalentes.
     */
    @Override
    public boolean equals(Object obj) {
        // Observação: isso seria idêntico a PropertyDescriptor, mas não
        // compartilhe os mesmos campos.
        if (this == obj) {
            return true;
        }

        if (obj instanceof IndexedPropertyDescriptor) {
            IndexedPropertyDescriptor other = (IndexedPropertyDescriptor) obj;
            Method otherIndexedReadMethod = other.getIndexedReadMethod();
            Method otherIndexedWriteMethod = other.getIndexedWriteMethod();

            if (!compareMethods(getIndexedReadMethod(), otherIndexedReadMethod)) {
                return false;
            }

            if (!compareMethods(getIndexedWriteMethod(), otherIndexedWriteMethod)) {
                return false;
            }

            if (getIndexedPropertyType() != other.getIndexedPropertyType()) {
                return false;
            }
            return super.equals(obj);
        }
        return false;
    }

    /**
     * Retorna um valor de código hash para o objeto. Consulte {@link Object # hashCode} para uma descrição completa.
     *
     * @return um valor de código hash para este objeto.
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();

        result = 37 * result + ((indexedWriteMethodName == null) ? 0 : indexedWriteMethodName.hashCode());
        result = 37 * result + ((indexedReadMethodName == null) ? 0 : indexedReadMethodName.hashCode());
        result = 37 * result + ((getIndexedPropertyType() == null) ? 0 : getIndexedPropertyType().hashCode());

        return result;
    }

}
