package br.com.archbase.shared.kernel.bean.metadata;

import java.lang.ref.Reference;
import java.lang.reflect.Method;


/**
 * Um EventSetDescriptor descreve um grupo de eventos que um determinado bean Java
 * incêndios.
 * <p>
 * O determinado grupo de eventos são entregues como chamadas de método em um único evento
 * interface de ouvinte, e um objeto ouvinte de evento pode ser registrado por meio de uma chamada
 * em um método de registro fornecido pela fonte do evento.
 */
public class EventSetDescriptor extends FeatureDescriptor {


    private MethodDescriptor[] listenerMethodDescriptors;
    private MethodDescriptor addMethodDescriptor;
    private MethodDescriptor removeMethodDescriptor;
    private MethodDescriptor getMethodDescriptor;

    private Reference<?> listenerMethodsRef;
    private Reference<?> listenerTypeRef;

    private boolean unicast;
    private boolean inDefaultEventSet = true;

    /**
     * Cria um <TT> EventSetDescriptor </TT> assumindo que você está seguindo
     * o padrão de design padrão mais simples em que um evento nomeado
     * & quot; fred & quot; é (1) entregue como uma chamada no método único de
     * interface FredListener, (2) tem um único argumento do tipo FredEvent, e
     * (3) onde o FredListener pode ser registrado com uma chamada em um
     * método addFredListener do componente de origem e removido com uma chamada
     * um método removeFredListener.
     *
     * @param sourceClass        A classe que está disparando o evento.
     * @param eventSetName       O nome programático do evento. Por exemplo. &quot;fred&quot;.
     *                           Observe que isso normalmente deve começar com minúsculas
     *                           personagem.
     * @param listenerType       A interface de destino para a qual os eventos serão entregues.
     * @param listenerMethodName O método que será chamado quando o evento for entregue
     *                           para sua interface de ouvinte de destino.
     * @throws IntrospectionException se ocorrer uma exceção durante a introspecção.
     */
    public EventSetDescriptor(Class<?> sourceClass, String eventSetName, Class<?> listenerType,
                              String listenerMethodName) throws IntrospectionException {
        this(sourceClass, eventSetName, listenerType, new String[]{listenerMethodName}, "add"
                + getListenerClassName(listenerType), "remove" + getListenerClassName(listenerType), "get"
                + getListenerClassName(listenerType) + "s");

        String eventName = capitalize(eventSetName) + "Event";
        Method[] listenerMethods = getListenerMethods();
        if (listenerMethods.length > 0) {
            Class[] args = listenerMethods[0].getParameterTypes();
            if (!"vetoableChange".equals(eventSetName) && !args[0].getName().endsWith(eventName)) {
                throw new IntrospectionException("IntrospectionException " + listenerMethodName + "->" + eventName);
            }
        }
    }

    /**
     * Cria um <TT> EventSetDescriptor </TT> do zero usando nomes de string.
     *
     * @param sourceClass              A classe que dispara o evento.
     * @param eventSetName             O nome programático do conjunto de eventos. Observe que isso deve
     *                                 normalmente começa com um caractere minúsculo.
     * @param listenerType             A classe da interface de destino que os eventos obterão
     *                                 entregue a.
     * @param listenerMethodNames      Os nomes dos métodos que serão chamados quando o evento
     *                                 é entregue à sua interface de ouvinte de destino.
     * @param addListenerMethodName    O nome do método na fonte do evento que pode ser usado para
     *                                 registrar um objeto ouvinte de evento.
     * @param removeListenerMethodName O nome do método na fonte do evento que pode ser usado para
     *                                 cancelar o registro de um objeto ouvinte de evento.
     * @throws IntrospectionException se ocorrer uma exceção durante a introspecção.
     */
    public EventSetDescriptor(Class<?> sourceClass, String eventSetName, Class<?> listenerType,
                              String[] listenerMethodNames, String addListenerMethodName, String removeListenerMethodName)
            throws IntrospectionException {
        this(sourceClass, eventSetName, listenerType, listenerMethodNames, addListenerMethodName,
                removeListenerMethodName, null);
    }

    /**
     * Este construtor cria um EventSetDescriptor do zero usando string
     * nomes.
     *
     * @param sourceClass              A classe que dispara o evento.
     * @param eventSetName             O nome programático do conjunto de eventos. Observe que isso deve
     *                                 normalmente começa com um caractere minúsculo.
     * @param listenerType             A classe da interface de destino que os eventos obterão
     *                                 entregue a.
     * @param listenerMethodNames      Os nomes dos métodos que serão chamados quando o evento
     *                                 é entregue à sua interface de ouvinte de destino.
     * @param addListenerMethodName    O nome do método na fonte do evento que pode ser usado para
     *                                 registrar um objeto ouvinte de evento.
     * @param removeListenerMethodName O nome do método na fonte do evento que pode ser usado para
     *                                 cancelar o registro de um objeto ouvinte de evento.
     * @param getListenerMethodName    O método na fonte do evento que pode ser usado para acessar o
     *                                 array de objetos ouvintes de eventos.
     * @throws IntrospectionException se ocorrer uma exceção durante a introspecção.
     */
    public EventSetDescriptor(Class<?> sourceClass, String eventSetName, Class<?> listenerType,
                              String[] listenerMethodNames, String addListenerMethodName, String removeListenerMethodName,
                              String getListenerMethodName) throws IntrospectionException {
        if (sourceClass == null || eventSetName == null || listenerType == null) {
            throw new NullPointerException();
        }
        setName(eventSetName);
        setClass0(sourceClass);
        setListenerType(listenerType);

        Method[] listenerMethods = new Method[listenerMethodNames.length];
        for (int i = 0; i < listenerMethodNames.length; i++) {
            // Verifique se há nomes nulos
            if (listenerMethodNames[i] == null) {
                throw new NullPointerException();
            }
            listenerMethods[i] = getMethod(listenerType, listenerMethodNames[i], 1);
        }
        setListenerMethods(listenerMethods);

        setAddListenerMethod(getMethod(sourceClass, addListenerMethodName, 1));
        setRemoveListenerMethod(getMethod(sourceClass, removeListenerMethodName, 1));

        // Seja mais indulgente por não encontrar o método getListener.
        Method method = BeanUtils.findAccessibleMethodIncludeInterfaces(sourceClass, getListenerMethodName, 0, null);
        if (method != null) {
            setGetListenerMethod(method);
        }
    }

    /**
     * Cria um <TT> EventSetDescriptor </TT> do zero usando
     * Objetos <TT> java.lang.reflect.Method </TT> e <TT> java.lang.Class </TT>.
     *
     * @param eventSetName         O nome programático do conjunto de eventos.
     * @param listenerType         A classe da interface do listener.
     * @param listenerMethods      Uma matriz de objetos Method descrevendo cada um dos eventos
     *                             métodos de manipulação no ouvinte de destino.
     * @param addListenerMethod    O método na fonte do evento que pode ser usado para registrar um
     *                             objeto ouvinte de evento.
     * @param removeListenerMethod O método na fonte do evento que pode ser usado para cancelar o registro
     *                             um objeto ouvinte de evento.
     * @throws IntrospectionException se ocorrer uma exceção durante a introspecção.
     */
    public EventSetDescriptor(String eventSetName, Class<?> listenerType, Method[] listenerMethods,
                              Method addListenerMethod, Method removeListenerMethod) throws IntrospectionException {
        this(eventSetName, listenerType, listenerMethods, addListenerMethod, removeListenerMethod, null);
    }

    /**
     * Este construtor cria um EventSetDescriptor do zero usando
     * objetos java.lang.reflect.Method e java.lang.Class.
     *
     * @param eventSetName         O nome programático do conjunto de eventos.
     * @param listenerType         A classe da interface do listener.
     * @param listenerMethods      Uma matriz de objetos Method descrevendo cada um dos eventos
     *                             métodos de manipulação no ouvinte de destino.
     * @param addListenerMethod    O método na fonte do evento que pode ser usado para registrar um
     *                             objeto ouvinte de evento.
     * @param removeListenerMethod O método na fonte do evento que pode ser usado para cancelar o registro
     *                             um objeto ouvinte de evento.
     * @param getListenerMethod    O método na fonte do evento que pode ser usado para acessar o
     *                             array de objetos ouvintes de eventos.
     * @throws IntrospectionException se ocorrer uma exceção durante a introspecção.
     */
    public EventSetDescriptor(String eventSetName, Class<?> listenerType, Method[] listenerMethods,
                              Method addListenerMethod, Method removeListenerMethod, Method getListenerMethod)
            throws IntrospectionException {
        setName(eventSetName);
        setListenerMethods(listenerMethods);
        setAddListenerMethod(addListenerMethod);
        setRemoveListenerMethod(removeListenerMethod);
        setGetListenerMethod(getListenerMethod);
        setListenerType(listenerType);
    }

    /**
     * Cria um <TT> EventSetDescriptor </TT> do zero usando
     * <TT> java.lang.reflect.MethodDescriptor </TT> e <TT> java.lang.Class </TT>
     * objetos.
     *
     * @param eventSetName              O nome programático do conjunto de eventos.
     * @param listenerType              A classe da interface do listener.
     * @param listenerMethodDescriptors Uma matriz de objetos MethodDescriptor que descreve cada um dos
     *                                  métodos de manipulação de eventos no ouvinte de destino.
     * @param addListenerMethod         O método na fonte do evento que pode ser usado para registrar um
     *                                  objeto ouvinte de evento.
     * @param removeListenerMethod      O método na fonte do evento que pode ser usado para cancelar o registro
     *                                  um objeto ouvinte de evento.
     * @throws IntrospectionException se ocorrer uma exceção durante a introspecção.
     */
    public EventSetDescriptor(String eventSetName, Class<?> listenerType,
                              MethodDescriptor[] listenerMethodDescriptors, Method addListenerMethod,
                              Method removeListenerMethod) throws IntrospectionException {
        setName(eventSetName);
        this.listenerMethodDescriptors = listenerMethodDescriptors;
        setAddListenerMethod(addListenerMethod);
        setRemoveListenerMethod(removeListenerMethod);
        setListenerType(listenerType);
    }

    /**
     * Construtor de pacote privado Mesclar dois descritores de conjunto de eventos. Onde eles
     * conflito, dê ao segundo argumento (y) prioridade sobre o primeiro argumento
     * (x).
     *
     * @param x O primeiro (prioridade inferior) EventSetDescriptor
     * @param y O segundo (prioridade mais alta) EventSetDescriptor
     */
    EventSetDescriptor(EventSetDescriptor x, EventSetDescriptor y) {
        super(x, y);
        listenerMethodDescriptors = x.listenerMethodDescriptors;
        if (y.listenerMethodDescriptors != null) {
            listenerMethodDescriptors = y.listenerMethodDescriptors;
        }

        listenerTypeRef = x.listenerTypeRef;
        if (y.listenerTypeRef != null) {
            listenerTypeRef = y.listenerTypeRef;
        }

        addMethodDescriptor = x.addMethodDescriptor;
        if (y.addMethodDescriptor != null) {
            addMethodDescriptor = y.addMethodDescriptor;
        }

        removeMethodDescriptor = x.removeMethodDescriptor;
        if (y.removeMethodDescriptor != null) {
            removeMethodDescriptor = y.removeMethodDescriptor;
        }

        getMethodDescriptor = x.getMethodDescriptor;
        if (y.getMethodDescriptor != null) {
            getMethodDescriptor = y.getMethodDescriptor;
        }

        unicast = y.unicast;
        if (!x.inDefaultEventSet || !y.inDefaultEventSet) {
            inDefaultEventSet = false;
        }
    }

    /**
     * Construtor dup privado de pacote Isso deve isolar o novo objeto de qualquer
     * muda para o objeto antigo.
     */
    EventSetDescriptor(EventSetDescriptor old) {
        super(old);
        if (old.listenerMethodDescriptors != null) {
            int len = old.listenerMethodDescriptors.length;
            listenerMethodDescriptors = new MethodDescriptor[len];
            for (int i = 0; i < len; i++) {
                listenerMethodDescriptors[i] = new MethodDescriptor(old.listenerMethodDescriptors[i]);
            }
        }
        listenerTypeRef = old.listenerTypeRef;

        addMethodDescriptor = old.addMethodDescriptor;
        removeMethodDescriptor = old.removeMethodDescriptor;
        getMethodDescriptor = old.getMethodDescriptor;

        unicast = old.unicast;
        inDefaultEventSet = old.inDefaultEventSet;
    }

    private static String getListenerClassName(Class<?> cls) {
        String className = cls.getName();
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private static Method getMethod(Class<?> cls, String name, int args) throws IntrospectionException {
        if (name == null) {
            return null;
        }
        Method method = BeanUtils.findAccessibleMethodIncludeInterfaces(cls, name, args, null);
        if (method == null) {
            throw new IntrospectionException("IntrospectionException2 " + name + " -> " + cls.getName());
        }
        return method;
    }

    /**
     * Obtém o objeto <TT> Class </TT> para a interface de destino.
     *
     * @return O objeto Class para a interface de destino que será chamada
     * quando o evento é disparado.
     */
    public Class<?> getListenerType() {
        return (Class) getObject(listenerTypeRef);
    }

    private void setListenerType(Class<?> cls) {
        listenerTypeRef = createReference(cls);
    }

    /**
     * Obtém os métodos da interface do ouvinte de destino.
     *
     * @return Uma matriz de objetos <TT> Método </TT> para os métodos de destino dentro
     * a interface do listener de destino que será chamada quando os eventos
     * são despedidos.
     */
    public synchronized Method[] getListenerMethods() {
        Method[] methods = getListenerMethods0();
        if (methods == null) {
            if (listenerMethodDescriptors != null) {
                methods = new Method[listenerMethodDescriptors.length];
                for (int i = 0; i < methods.length; i++) {
                    methods[i] = listenerMethodDescriptors[i].getMethod();
                }
            }
            setListenerMethods(methods);
        }
        return methods;
    }

    private synchronized void setListenerMethods(Method[] methods) {
        if (methods == null) {
            return;
        }
        if (listenerMethodDescriptors == null) {
            listenerMethodDescriptors = new MethodDescriptor[methods.length];
            for (int i = 0; i < methods.length; i++) {
                listenerMethodDescriptors[i] = new MethodDescriptor(methods[i]);
            }
        }
        listenerMethodsRef = createReference(methods, true);
    }

    private Method[] getListenerMethods0() {
        return (Method[]) getObject(listenerMethodsRef);
    }

    /**
     * Obtém os <code> MethodDescriptor </code> s da interface do ouvinte de destino.
     *
     * @return Uma matriz de objetos <code> MethodDescriptor </code> para o destino
     * métodos dentro da interface do listener de destino que serão chamados
     * quando os eventos são disparados.
     */
    public synchronized MethodDescriptor[] getListenerMethodDescriptors() {
        return listenerMethodDescriptors;
    }

    /**
     * Obtém o método usado para adicionar ouvintes de eventos.
     *
     * @return O método usado para registrar um ouvinte na fonte do evento.
     */
    public synchronized Method getAddListenerMethod() {
        return (addMethodDescriptor != null ? addMethodDescriptor.getMethod() : null);
    }

    private synchronized void setAddListenerMethod(Method method) {
        if (method == null) {
            return;
        }
        if (getClass0() == null) {
            setClass0(method.getDeclaringClass());
        }
        addMethodDescriptor = new MethodDescriptor(method);
    }

    /**
     * Obtém o método usado para remover ouvintes de eventos.
     *
     * @return O método usado para remover um ouvinte na fonte do evento.
     */
    public synchronized Method getRemoveListenerMethod() {
        return (removeMethodDescriptor != null ? removeMethodDescriptor.getMethod() : null);
    }

    private synchronized void setRemoveListenerMethod(Method method) {
        if (method == null) {
            return;
        }
        if (getClass0() == null) {
            setClass0(method.getDeclaringClass());
        }
        removeMethodDescriptor = new MethodDescriptor(method);
    }

    /**
     * Obtém o método usado para acessar os ouvintes de eventos registrados.
     *
     * @return O método usado para acessar a matriz de ouvintes no evento
     * source ou null se não existir.
     */
    public synchronized Method getGetListenerMethod() {
        return (getMethodDescriptor != null ? getMethodDescriptor.getMethod() : null);
    }

    private synchronized void setGetListenerMethod(Method method) {
        if (method == null) {
            return;
        }
        if (getClass0() == null) {
            setClass0(method.getDeclaringClass());
        }
        getMethodDescriptor = new MethodDescriptor(method);
    }

    /**
     * Normalmente, as fontes de eventos são multicast. No entanto, existem algumas exceções
     * que são estritamente unicast.
     *
     * @return <TT>true</TT> se o conjunto de eventos for unicast. Padrões para
     * <TT>falso</TT>.
     */
    public boolean isUnicast() {
        return unicast;
    }

    /**
     * Marque um evento definido como unicast (ou não).
     *
     * @param unicast Verdadeiro se o conjunto de eventos for unicast.
     */
    public void setUnicast(boolean unicast) {
        this.unicast = unicast;
    }

    /**
     * Informa se um evento definido está no padrão & quot; padrão & quot; conjunto.
     *
     * @return <TT>true</TT> se o conjunto de eventos estiver no & quot;padrão&quot; conjunto.
     * O padrão é <TT>true</TT>.
     */
    public boolean isInDefaultEventSet() {
        return inDefaultEventSet;
    }

    /**
     * Marca um evento definido como o & quot; padrão & quot; definido (ou não). Por
     * padrão, é <TT> verdadeiro </TT>.
     *
     * @param inDefaultEventSet <code>true</code> se o conjunto de eventos está no
     *                          &quot;default&quot;definir, <code>false</code> se não
     */
    public void setInDefaultEventSet(boolean inDefaultEventSet) {
        this.inDefaultEventSet = inDefaultEventSet;
    }
}
