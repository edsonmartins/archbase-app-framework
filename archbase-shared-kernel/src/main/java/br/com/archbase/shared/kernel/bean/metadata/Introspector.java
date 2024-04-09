package br.com.archbase.shared.kernel.bean.metadata;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * A classe Introspector fornece uma maneira padrão para as ferramentas aprenderem sobre o
 * propriedades, eventos e métodos suportados por um Java Bean de destino.
 * <p>
 * Para cada um desses três tipos de informação, o Introspector irá
 * analise separadamente a classe e as superclasses do bean procurando por
 * informações explícitas ou implícitas e usar essas informações para construir um BeanInfo
 * objeto que descreve de forma abrangente o bean de destino.
 * <p>
 * Para cada classe "Foo", informações explícitas podem estar disponíveis se houver um
 * classe "FooBeanInfo" correspondente que fornece um valor não nulo quando consultado
 * para informação. Primeiro, procuramos a classe BeanInfo obtendo a classe
 * nome qualificado do pacote da classe de bean de destino e anexando "BeanInfo" a
 * formar um novo nome de classe. Se isso falhar, então usamos o nome de classe final
 * componente deste nome, e procure essa classe em cada um dos pacotes
 * especificado no caminho de pesquisa do pacote BeanInfo.
 * <p>
 * Assim, para uma classe como "sun.xyz.OurButton", primeiro procuraríamos um
 * Classe BeanInfo chamada "sun.xyz.OurButtonBeanInfo" e se isso falhasse,
 * procure em cada pacote no caminho de pesquisa BeanInfo por um OurButtonBeanInfo
 * classe. Com o caminho de pesquisa padrão, isso significaria procurar
 * "sun.beans.infos.OurButtonBeanInfo".
 * <p>
 * Se uma classe fornece BeanInfo explícito sobre si mesma, então adicionamos isso ao
 * BeanInfo informações que obtivemos da análise de quaisquer classes derivadas, mas nós
 * considerar as informações explícitas como sendo definitivas para a classe atual e
 * suas classes básicas e não prossiga para cima na cadeia da superclasse.
 * <p>
 * Se não encontrarmos BeanInfo explícito em uma classe, usamos reflexão de baixo nível para
 * estudar os métodos da classe e aplicar padrões de design padrão para identificar
 * acessadores de propriedade, fontes de eventos ou métodos públicos. Em seguida, procedemos para
 * analise a superclasse da classe e adicione as informações dela (e
 * possivelmente na cadeia da superclasse).
 *
 * <p>
 * Como o Introspector armazena em cache as classes BeanInfo para melhor desempenho,
 * cuidado se você usá-lo em um aplicativo que usa vários carregadores de classes. No
 * geral, quando você destrói um <code> ClassLoader </code> que foi usado para
 * classes de introspecção, você deve usar o {@link #flushCaches
 * <code> Introspector.flushCaches </code>} ou {@link #flushFromCaches
 * método <code> Introspector.flushFromCaches </code>} para liberar todos os
 * classes introspectadas fora do cache.
 * <p>
 * Para obter mais informações sobre introspecção e padrões de design, consulte
 * a <a HREF="http://java.sun.com/products/javabeans/docs/index.html">JavaBeans
 * specification</a>.
 */

public class Introspector {

    // Sinalizadores que podem ser usados ​​para controlar getBeanInfo:
    public static final int USE_ALL_BEANINFO = 1;
    public static final int IGNORE_IMMEDIATE_BEANINFO = 2;
    public static final int IGNORE_ALL_BEANINFO = 3;
    private static final String DEFAULT_INFO_PATH = "sun.beans.infos";
    private static final EventSetDescriptor[] EMPTY_EVENTSETDESCRIPTORS = new EventSetDescriptor[0];
    private static final String ADD_PREFIX = "add";
    private static final String REMOVE_PREFIX = "remove";
    private static final String GET_PREFIX = "get";
    private static final String SET_PREFIX = "set";
    private static final String IS_PREFIX = "is";
    private static final String BEANINFO_SUFFIX = "BeanInfo";
    // Caches estáticos para acelerar a introspecção.
    private static Map<Class<?>, BeanInfo> beanInfoCache = Collections
            .synchronizedMap(new WeakHashMap<Class<?>, BeanInfo>());
    private static Class<?> eventListenerType = EventListener.class;
    private static String[] searchPath = {DEFAULT_INFO_PATH};
    private Class<?> beanClass;
    private BeanInfo explicitBeanInfo;
    private BeanInfo superBeanInfo;
    private BeanInfo[] additionalBeanInfo;
    private boolean propertyChangeSource = false;
    // Eles devem ser removidos.
    private String defaultEventName;
    private String defaultPropertyName;
    private int defaultEventIndex = -1;
    private int defaultPropertyIndex = -1;
    // Métodos mapeiam de objetos Method para MethodDescriptors
    private Map<String, Object> methods;
    // Mapas de propriedades de nomes de String para PropertyDescriptors
    private Map<String, Object> properties;
    // Mapas de eventos de nomes de String para EventSetDescriptors
    private Map<String, Object> events;
    private HashMap<String, List<PropertyDescriptor>> pdStore = new HashMap<>();

    private Introspector(Class<?> beanClass, Class<?> stopClass, int flags) throws IntrospectionException {
        this.beanClass = beanClass;

        // Verifique se stopClass é uma superClasse de startClass.
        if (stopClass != null) {
            boolean isSuper = false;
            for (Class<?> c = beanClass.getSuperclass(); c != null; c = c.getSuperclass()) {
                if (c == stopClass) {
                    isSuper = true;
                }
            }
            if (!isSuper) {
                throw new IntrospectionException(stopClass.getName() + " não é superclasse de " + beanClass.getName());
            }
        }

        if (flags == USE_ALL_BEANINFO) {
            explicitBeanInfo = findExplicitBeanInfo(beanClass);
        }

        Class<?> superClass = beanClass.getSuperclass();
        if (superClass != stopClass) {
            int newFlags = flags;
            if (newFlags == IGNORE_IMMEDIATE_BEANINFO) {
                newFlags = USE_ALL_BEANINFO;
            }
            superBeanInfo = getBeanInfo(superClass, stopClass, newFlags);
        }
        if (explicitBeanInfo != null) {
            additionalBeanInfo = explicitBeanInfo.getAdditionalBeanInfo();
        }
        if (additionalBeanInfo == null) {
            additionalBeanInfo = new BeanInfo[0];
        }
    }

    /**
     * Faça uma introspecção em um Java Bean e aprenda sobre todas as suas propriedades, expostas
     * métodos e eventos.
     * <p>
     * Se a classe BeanInfo para um Java Bean foi previamente introspectada
     * então a classe BeanInfo é recuperada do cache BeanInfo.
     *
     * @param beanClass A classe bean a ser analisada.
     * @return Um objeto BeanInfo que descreve o bean de destino.
     * @throws IntrospectionException se ocorrer uma exceção durante a introspecção.
     * @ver #flushCaches
     * @ver #flushFromCaches
     */
    public static BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
        BeanInfo bi = beanInfoCache.get(beanClass);
        if (bi == null) {
            bi = (new Introspector(beanClass, null, USE_ALL_BEANINFO)).getBeanInfo();
            beanInfoCache.put(beanClass, bi);
        }
        return bi;
    }

    /**
     * Faça uma introspecção em um bean Java e aprenda sobre todas as suas propriedades, expostas
     * métodos e eventos, sujeitos a alguns sinalizadores de controle.
     * <p>
     * Se a classe BeanInfo para um Java Bean foi previamente introspectada
     * com base nos mesmos argumentos, então a classe BeanInfo é recuperada do
     * Cache BeanInfo.
     *
     * @param beanClass A classe do bean a ser analisada.
     * @param flags     Bandeiras para controlar a introspecção. Se sinalizadores ==
     *                  USE_ALL_BEANINFO então usamos todas as classes BeanInfo que
     *                  pode descobrir. Se sinalizadores == IGNORE_IMMEDIATE_BEANINFO então nós
     *                  ignore qualquer BeanInfo associado ao beanClass especificado.
     *                  Se sinalizadores == IGNORE_ALL_BEANINFO, então ignoramos todos os BeanInfo
     *                  associado ao beanClass especificado ou qualquer uma de sua Classes pais.
     * @return Um objeto BeanInfo que descreve o bean de destino.
     * @throws IntrospectionException se ocorrer uma exceção durante a introspecção.
     */
    public static BeanInfo getBeanInfo(Class<?> beanClass, int flags) throws IntrospectionException {
        return getBeanInfo(beanClass, null, flags);
    }

    /**
     * Faça uma introspecção em um bean Java e aprenda tudo sobre suas propriedades, exposto
     * métodos, abaixo de um determinado ponto de "parada".
     * <p>
     * Se a classe BeanInfo para um Java Bean foi previamente introspectada
     * com base nos mesmos argumentos, a classe BeanInfo é recuperada de
     * o cache BeanInfo.
     *
     * @param beanClass A classe do bean a ser analisada.
     * @param stopClass A classe básica na qual interromper a análise. Qualquer
     *                  métodos / propriedades / eventos no stopClass ou em seus
     *                  classes básicas serão ignoradas na análise.
     * @throws IntrospectionException se ocorrer uma exceção durante a introspecção.
     */
    public static BeanInfo getBeanInfo(Class<?> beanClass, Class<?> stopClass) throws IntrospectionException {
        return getBeanInfo(beanClass, stopClass, USE_ALL_BEANINFO);
    }

    /**
     * Chamado apenas a partir dos métodos getBeanInfo públicos. Este método armazena em cache o
     * BeanInfo introspectado com base nos argumentos.
     */
    private static BeanInfo getBeanInfo(Class<?> beanClass, Class<?> stopClass, int flags) throws IntrospectionException {
        BeanInfo bi;
        if (stopClass == null && flags == USE_ALL_BEANINFO) {
            // Mesmos parâmetros para aproveitar as vantagens do armazenamento em cache.
            bi = getBeanInfo(beanClass);
        } else {
            bi = (new Introspector(beanClass, stopClass, flags)).getBeanInfo();
        }
        return bi;
    }

    /**
     * Obtém a lista de nomes de pacotes que serão usados ​​para encontrar BeanInfo
     * Aulas.
     *
     * @return A matriz de nomes de pacotes que serão pesquisados ​​para encontrar
     * Classes BeanInfo. O valor padrão para esta matriz é
     * dependente da implementação; por exemplo. A implementação da Sun inicialmente definido
     * para {"sun.beans.infos"}.
     */

    public static synchronized String[] getBeanInfoSearchPath() {
        return Arrays.copyOf(searchPath, searchPath.length);
    }

    /**
     * Altere a lista de nomes de pacotes que serão usados para encontrar Classes BeanInfo
     * O comportamento deste método é indefinido se o caminho do parâmetro for
     * nulo.
     *
     * <p>
     * Primeiro, se houver um gerenciador de segurança, é
     * O método <code> checkPropertiesAccess </code> é chamado. Isso pode resultar em
     * uma SecurityException.
     *
     * @param path Matriz de nomes de pacotes.
     * @throws SecurityException se existe um gerenciador de segurança e seu
     *                           O método <code> checkPropertiesAccess </code> não permite
     *                           configuração das propriedades do sistema.
     * @see SecurityManager #checkPropertiesAccess
     */

    public static synchronized void setBeanInfoSearchPath(String[] path) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPropertiesAccess();
        }
        searchPath = path;
    }

    /**
     * Libere todos os caches internos do Introspector. Este método não é
     * normalmente necessário. Normalmente, é necessário apenas para ferramentas avançadas que
     * atualizar os objetos "Class" existentes no local e precisar fazer o
     * O Introspector reanalisa objetos de classe existentes.
     */

    public static void flushCaches() {
        beanInfoCache.clear();
    }

    /**
     * Libere as informações internas do Introspector em cache para uma determinada classe.
     * Este método normalmente não é necessário. Normalmente, é necessário apenas para
     * ferramentas avançadas que atualizam objetos "Class" existentes no local e precisam
     * fazer o Introspector reanalisar um objeto de classe existente.
     * <p>
     * Observe que apenas o estado direto associado ao objeto Class de destino
     * é liberado. Não liberamos o estado de outros objetos Class com o mesmo
     * nome, nem liberamos o estado para quaisquer objetos Class relacionados (como
     * subclasses), mesmo que seu estado possa incluir informações indiretamente
     * obtido do objeto Class de destino.
     *
     * @param clz Objeto de classe a ser liberado.
     * @throws NullPointerException Se o objeto Class for nulo.
     */
    public static void flushFromCaches(Class<?> clz) {
        if (clz == null) {
            throw new NullPointerException();
        }
        beanInfoCache.remove(clz);
    }

    /**
     * Procura uma classe BeanInfo explícita que corresponda à classe. Primeiro
     * olha no pacote existente em que a classe está definida, então
     * verifica se a classe é seu próprio BeanInfo. Finalmente, o BeanInfo
     * o caminho de pesquisa é anexado à classe e pesquisado.
     *
     * @return Instância de uma classe BeanInfo explícita ou null se não for
     * encontrado.
     */
    @SuppressWarnings("java:S3776")
    private static synchronized BeanInfo findExplicitBeanInfo(Class<?> beanClass) {
        String name = beanClass.getName() + BEANINFO_SUFFIX;
        try {
            return (BeanInfo) BeanUtils.instantiate(beanClass, name);
        } catch (Exception ex) {
            //

        }
        // Agora tente verificar se o bean é seu próprio BeanInfo.
        try {
            if (BeanUtils.isSubclass(beanClass, BeanInfo.class)) {
                return (BeanInfo) beanClass.getConstructor().newInstance();
            }
        } catch (Exception ex) {
            //
        }
        // Agora tente procurar <searchPath>.fooBeanInfo
        name = name.substring(name.lastIndexOf('.') + 1);

        for (int i = 0; i < searchPath.length; i++) {
            // Esta otimização só usará o caminho de pesquisa BeanInfo se for
            // mudou
            // do original ou tentando obter o ComponentBeanInfo.
            if (!DEFAULT_INFO_PATH.equals(searchPath[i]) || DEFAULT_INFO_PATH.equals(searchPath[i])
                    && "ComponentBeanInfo".equals(name)) {
                try {
                    String fullName = searchPath[i] + "." + name;
                    BeanInfo bi = (BeanInfo) BeanUtils.instantiate(beanClass, fullName);

                    // Certifique-se de que o BeanInfo retornado corresponda à classe.
                    if (bi.getBeanDescriptor() != null) {
                        if (bi.getBeanDescriptor().getBeanClass() == beanClass) {
                            return bi;
                        }
                    } else if (bi.getPropertyDescriptors() != null) {
                        PropertyDescriptor[] pds = bi.getPropertyDescriptors();
                        for (int j = 0; j < pds.length; j++) {
                            Method method = pds[j].getReadMethod();
                            if (method == null) {
                                method = pds[j].getWriteMethod();
                            }
                            if (method != null && method.getDeclaringClass() == beanClass) {
                                return bi;
                            }
                        }
                    } else if (bi.getMethodDescriptors() != null) {
                        MethodDescriptor[] mds = bi.getMethodDescriptors();
                        for (int j = 0; j < mds.length; j++) {
                            Method method = mds[j].getMethod();
                            if (method != null && method.getDeclaringClass() == beanClass) {
                                return bi;
                            }
                        }
                    }
                } catch (Exception ex) {
                    // Ignore silenciosamente quaisquer erros.
                }
            }
        }
        return null;
    }

    /**
     * Cria uma chave para um método em um cache de método.
     */
    private static String makeQualifiedMethodName(String name, String[] params) {
        StringBuilder sb = new StringBuilder(name);
        sb.append('=');
        for (int i = 0; i < params.length; i++) {
            sb.append(':');
            sb.append(params[i]);
        }
        return sb.toString();
    }

    public static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) && Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    /**
     * Constrói uma classe GenericBeanInfo a partir do estado do Introspector
     */
    private BeanInfo getBeanInfo() throws IntrospectionException {

        // a ordem de avaliação aqui é import, conforme avaliamos o
        // conjuntos de eventos e localize PropertyChangeListeners antes de
        // procure propriedades.
        BeanDescriptor bd = getTargetBeanDescriptor();
        MethodDescriptor[] mds = getTargetMethodInfo();
        EventSetDescriptor[] esds = getTargetEventInfo();
        PropertyDescriptor[] pds = getTargetPropertyInfo();

        int defaultEvent = getTargetDefaultEventIndex();
        int defaultProperty = getTargetDefaultPropertyIndex();

        return new GenericBeanInfo(bd, esds, defaultEvent, pds, defaultProperty, mds, explicitBeanInfo);

    }

    /**
     * @return Uma matriz de PropertyDescriptors que descreve o editável
     * propriedades suportadas pelo bean de destino.
     */
    @SuppressWarnings({"java:S3776", "java:S135"})
    private PropertyDescriptor[] getTargetPropertyInfo() {

        // Verifique se o bean tem seu próprio BeanInfo que fornecerá
        // informação explícita.
        PropertyDescriptor[] explicitProperties = null;
        if (explicitBeanInfo != null) {
            explicitProperties = explicitBeanInfo.getPropertyDescriptors();
            int ix = explicitBeanInfo.getDefaultPropertyIndex();
            if (ix >= 0 && ix < explicitProperties.length) {
                defaultPropertyName = explicitProperties[ix].getName();
            }
        }

        if (explicitProperties == null && superBeanInfo != null) {
            // Não temos propriedades BeanInfo explícitas. Verifique com nosso pai.
            PropertyDescriptor[] supers = superBeanInfo.getPropertyDescriptors();
            for (int i = 0; i < supers.length; i++) {
                addPropertyDescriptor(supers[i]);
            }
            int ix = superBeanInfo.getDefaultPropertyIndex();
            if (ix >= 0 && ix < supers.length) {
                defaultPropertyName = supers[ix].getName();
            }
        }

        for (int i = 0; i < additionalBeanInfo.length; i++) {
            PropertyDescriptor[] additional = additionalBeanInfo[i].getPropertyDescriptors();
            if (additional != null) {
                for (int j = 0; j < additional.length; j++) {
                    addPropertyDescriptor(additional[j]);
                }
            }
        }

        if (explicitProperties != null) {
            // Adicione os dados BeanInfo explícitos aos nossos resultados.
            for (int i = 0; i < explicitProperties.length; i++) {
                addPropertyDescriptor(explicitProperties[i]);
            }

        } else {

            // Aplique alguma reflexão à classe atual.
            // Primeiro obtenha uma matriz de todos os métodos públicos neste nível
            Method[] methodList = BeanUtils.getPublicDeclaredMethods(beanClass);

            // Agora analise cada método.
            for (int i = 0; i < methodList.length; i++) {
                Method method = methodList[i];
                if (method == null) {
                    continue;
                }
                // skip static methods.
                int mods = method.getModifiers();
                if (Modifier.isStatic(mods)) {
                    continue;
                }
                String name = method.getName();
                Class<?>[] argTypes = method.getParameterTypes();
                Class<?> resultType = method.getReturnType();
                int argCount = argTypes.length;
                PropertyDescriptor pd = null;

                if (name.length() <= 3 && !name.startsWith(IS_PREFIX)) {
                    // Otimização. Não se preocupe com propertyNames inválidos.
                    continue;
                }

                try {

                    if (argCount == 0) {
                        if (name.startsWith(GET_PREFIX)) {
                            // Simple getter
                            pd = new PropertyDescriptor(BeanUtils.decapitalize(name.substring(3)), method, null);
                        } else if (resultType == boolean.class && name.startsWith(IS_PREFIX)) {
                            // Boolean getter
                            pd = new PropertyDescriptor(BeanUtils.decapitalize(name.substring(2)), method, null);
                        }
                    } else if (argCount == 1) {
                        if (argTypes[0] == int.class && name.startsWith(GET_PREFIX)) {
                            pd = new IndexedPropertyDescriptor(BeanUtils.decapitalize(name.substring(3)), null, null,
                                    method, null);
                        } else if (resultType == void.class && name.startsWith(SET_PREFIX)) {
                            // Simple setter
                            pd = new PropertyDescriptor(BeanUtils.decapitalize(name.substring(3)), null, method);
                            if (BeanUtils.throwsException(method, PropertyVetoException.class)) {
                                pd.setConstrained(true);
                            }
                        }
                    } else if (argCount == 2 && argTypes[0] == int.class && name.startsWith(SET_PREFIX)) {
                        pd = new IndexedPropertyDescriptor(BeanUtils.decapitalize(name.substring(3)), null, null,
                                null, method);
                        if (BeanUtils.throwsException(method, PropertyVetoException.class)) {
                            pd.setConstrained(true);
                        }
                    }
                } catch (IntrospectionException ex) {
                    // Isso acontece se um PropertyDescriptor ou
                    // IndexedPropertyDescriptor
                    // construtor descobre que o método viola os detalhes do
                    // deisgn padrão, por exemplo por ter um nome vazio ou um getter
                    // retornando vazio, ou qualquer outra coisa.
                    pd = null;
                }

                if (pd != null) {
                    // Se esta classe ou uma de suas classes base for um
                    // PropertyChange fonte, então assumimos que quaisquer propriedades que descobrirmos
                    // são ligados".
                    if (propertyChangeSource) {
                        pd.setBound(true);
                    }
                    addPropertyDescriptor(pd);
                }
            }
        }
        processPropertyDescriptors();

        // Alocar e preencher a matriz de resultado.
        PropertyDescriptor[] result = new PropertyDescriptor[properties.size()];
        result = properties.values().toArray(result);

        // Defina o índice padrão.
        if (defaultPropertyName != null) {
            for (int i = 0; i < result.length; i++) {
                if (defaultPropertyName.equals(result[i].getName())) {
                    defaultPropertyIndex = i;
                }
            }
        }

        return result;
    }

    /**
     * Adiciona o descritor de propriedade ao armazenamento de lista.
     */
    private void addPropertyDescriptor(PropertyDescriptor pd) {
        pdStore.computeIfAbsent(pd.getName(), k -> new ArrayList<>()).add(pd);
    }

    /**
     * Preenche a tabela do descritor de propriedades mesclando as listas de Propriedades
     * descritores.
     */
    @SuppressWarnings("java:S3776")
    private void processPropertyDescriptors() {
        if (properties == null) {
            properties = new TreeMap<>();
        }

        List<?> list;

        PropertyDescriptor pd;
        PropertyDescriptor gpd;
        PropertyDescriptor spd;
        IndexedPropertyDescriptor ipd;
        IndexedPropertyDescriptor igpd;
        IndexedPropertyDescriptor ispd;

        Iterator<?> it = pdStore.values().iterator();
        while (it.hasNext()) {
            gpd = null;
            spd = null;
            igpd = null;
            ispd = null;

            list = (List) it.next();

            // Primeira passagem. Encontre o método getter mais recente. Mesclar propriedades
            // dos métodos getter anteriores.
            for (int i = 0; i < list.size(); i++) {
                pd = (PropertyDescriptor) list.get(i);
                if (pd instanceof IndexedPropertyDescriptor) {
                    ipd = (IndexedPropertyDescriptor) pd;
                    if (ipd.getIndexedReadMethod() != null) {
                        if (igpd != null) {
                            igpd = new IndexedPropertyDescriptor(igpd, ipd);
                        } else {
                            igpd = ipd;
                        }
                    }
                } else {
                    if (pd.getReadMethod() != null) {
                        if (gpd != null) {
                            // Não substitua a leitura existente
                            // método se ele começar com "is"
                            Method method = gpd.getReadMethod();
                            if (!method.getName().startsWith(IS_PREFIX)) {
                                gpd = new PropertyDescriptor(gpd, pd);
                            }
                        } else {
                            gpd = pd;
                        }
                    }
                }
            }

            // Segunda passagem. Encontre o método setter mais recente que
            // tem o mesmo tipo que o método getter.
            for (int i = 0; i < list.size(); i++) {
                pd = (PropertyDescriptor) list.get(i);
                if (pd instanceof IndexedPropertyDescriptor) {
                    ipd = (IndexedPropertyDescriptor) pd;
                    if (ipd.getIndexedWriteMethod() != null) {
                        if (igpd != null) {
                            if (igpd.getIndexedPropertyType() == ipd.getIndexedPropertyType()) {
                                if (ispd != null) {
                                    ispd = new IndexedPropertyDescriptor(ispd, ipd);
                                } else {
                                    ispd = ipd;
                                }
                            }
                        } else {
                            if (ispd != null) {
                                ispd = new IndexedPropertyDescriptor(ispd, ipd);
                            } else {
                                ispd = ipd;
                            }
                        }
                    }
                } else {
                    if (pd.getWriteMethod() != null) {
                        if (gpd != null) {
                            if (gpd.getPropertyType() == pd.getPropertyType()) {
                                if (spd != null) {
                                    spd = new PropertyDescriptor(spd, pd);
                                } else {
                                    spd = pd;
                                }
                            }
                        } else {
                            if (spd != null) {
                                spd = new PropertyDescriptor(spd, pd);
                            } else {
                                spd = pd;
                            }
                        }
                    }
                }
            }

            // Nesta fase, devemos ter PDs ou IPDs para o
            // getters e setters representativos. A ordem na qual o
            // descritores de propriedade são determinados representam o
            // precedência da ordem de propriedade.
            pd = null;

            if (igpd != null && ispd != null) {
                // Conjunto completo de propriedades indexadas
                // Mesclar quaisquer descritores de propriedade clássicos
                if (gpd != null) {
                    PropertyDescriptor tpd = mergePropertyDescriptor(igpd, gpd);
                    if (tpd instanceof IndexedPropertyDescriptor) {
                        igpd = (IndexedPropertyDescriptor) tpd;
                    }
                }
                if (spd != null) {
                    PropertyDescriptor tpd = mergePropertyDescriptor(ispd, spd);
                    if (tpd instanceof IndexedPropertyDescriptor) {
                        ispd = (IndexedPropertyDescriptor) tpd;
                    }
                }
                if (igpd == ispd) {
                    pd = igpd;
                } else {
                    pd = mergePropertyDescriptor(igpd, ispd);
                }
            } else if (gpd != null && spd != null) {
                // Conjunto completo de propriedades simples
                if (gpd == spd) {
                    pd = gpd;
                } else {
                    pd = mergePropertyDescriptor(gpd, spd);
                }
            } else if (ispd != null) {
                // setter indexado
                pd = ispd;
                // Combine quaisquer descritores de propriedade clássicos
                if (spd != null) {
                    pd = mergePropertyDescriptor(ispd, spd);
                }
                if (gpd != null) {
                    pd = mergePropertyDescriptor(ispd, gpd);
                }
            } else if (igpd != null) {
                // setter indexado
                pd = igpd;
                // Combine quaisquer descritores de propriedade clássicos
                if (gpd != null) {
                    pd = mergePropertyDescriptor(igpd, gpd);
                }
                if (spd != null) {
                    pd = mergePropertyDescriptor(igpd, spd);
                }
            } else if (spd != null) {
                // getter simples
                pd = spd;
            } else if (gpd != null) {
                //getter simples
                pd = gpd;
            }

            // Caso muito especial para garantir que um IndexedPropertyDescriptor
            // não contém menos informações do que o anexo
            // PropertyDescriptor. Em caso afirmativo, recrie como um
            // PropertyDescriptor.
            if (pd instanceof IndexedPropertyDescriptor) {
                ipd = (IndexedPropertyDescriptor) pd;
                if (ipd.getIndexedReadMethod() == null && ipd.getIndexedWriteMethod() == null) {
                    pd = new PropertyDescriptor(ipd);
                }
            }

            if (pd != null) {
                properties.put(pd.getName(), pd);
            }
        }
    }

    /**
     * Adiciona o descritor de propriedade ao descritor de propriedade indexada apenas se
     * os tipos são iguais.
     * <p>
     * O descritor de propriedade mais específico terá precedência.
     */
    @SuppressWarnings("all")
    private PropertyDescriptor mergePropertyDescriptor(IndexedPropertyDescriptor ipd, PropertyDescriptor pd) {
        PropertyDescriptor result = null;

        Class<?> propType = pd.getPropertyType();
        Class<?> ipropType = ipd.getIndexedPropertyType();

        if (propType.isArray() && propType.getComponentType() == ipropType) {
            if (pd.getClass0().isAssignableFrom(ipd.getClass0())) {
                result = new IndexedPropertyDescriptor(pd, ipd);
            } else {
                result = new IndexedPropertyDescriptor(ipd, pd);
            }
        } else {
            // Não é possível mesclar o pd devido à incompatibilidade de tipo
            // Retorna o pd mais específico
            if (pd.getClass0().isAssignableFrom(ipd.getClass0())) {
                result = ipd;
            } else {
                result = pd;
                // Tente adicionar métodos que podem ter sido perdidos no tipo
                // mudança
                Method write = result.getWriteMethod();
                Method read = result.getReadMethod();

                if (read == null && write != null) {
                    read = BeanUtils.findAccessibleMethodIncludeInterfaces(result.getClass0(),
                            "get" + result.capitalize(result.getName()), 0, null);
                    if (read != null) {
                        try {
                            result.setReadMethod(read);
                        } catch (IntrospectionException ex) {
                            // sem consequências para a falha.
                        }
                    }
                }
                if (write == null && read != null) {
                    write = BeanUtils.findAccessibleMethodIncludeInterfaces(result.getClass0(),
                            "set" + result.capitalize(result.getName()), 1, new Class[]{read.getReturnType()});
                    if (write != null) {
                        try {
                            result.setWriteMethod(write);
                        } catch (IntrospectionException ex) {
                            // sem consequências para a falha.
                        }
                    }
                }
            }
        }
        return result;
    }

    // Lidar com mesclagem PDP regular
    @SuppressWarnings("java:S1144")
    private PropertyDescriptor mergePropertyDescriptor(PropertyDescriptor pd1, PropertyDescriptor pd2) {
        if (pd1.getClass0().isAssignableFrom(pd2.getClass0())) {
            return new PropertyDescriptor(pd1, pd2);
        } else {
            return new PropertyDescriptor(pd2, pd1);
        }
    }

    /**
     * @return Uma matriz de EventSetDescriptors que descreve os tipos de eventos
     * disparado pelo bean alvo.
     */
    @SuppressWarnings("all")
    private EventSetDescriptor[] getTargetEventInfo() throws IntrospectionException {
        if (events == null) {
            events = new HashMap<>();
        }

        // Verifique se o bean tem seu próprio BeanInfo que fornecerá
        // informação explícita.
        EventSetDescriptor[] explicitEvents = null;
        if (explicitBeanInfo != null) {
            explicitEvents = explicitBeanInfo.getEventSetDescriptors();
            int ix = explicitBeanInfo.getDefaultEventIndex();
            if (ix >= 0 && ix < explicitEvents.length) {
                defaultEventName = explicitEvents[ix].getName();
            }
        }

        if (explicitEvents == null && superBeanInfo != null) {
            // Não temos eventos BeanInfo explícitos. Verifique com nosso pai.
            EventSetDescriptor supers[] = superBeanInfo.getEventSetDescriptors();
            for (int i = 0; i < supers.length; i++) {
                addEvent(supers[i]);
            }
            int ix = superBeanInfo.getDefaultEventIndex();
            if (ix >= 0 && ix < supers.length) {
                defaultEventName = supers[ix].getName();
            }
        }

        for (int i = 0; i < additionalBeanInfo.length; i++) {
            EventSetDescriptor additional[] = additionalBeanInfo[i].getEventSetDescriptors();
            if (additional != null) {
                for (int j = 0; j < additional.length; j++) {
                    addEvent(additional[j]);
                }
            }
        }

        if (explicitEvents != null) {
            // Adicione os dados explicitBeanInfo explícitos aos nossos resultados.
            for (int i = 0; i < explicitEvents.length; i++) {
                addEvent(explicitEvents[i]);
            }

        } else {

            // Aplique alguma reflexão à classe atual.
            // Obtenha um array de todos os métodos de beans públicos neste nível
            Method methodList[] = BeanUtils.getPublicDeclaredMethods(beanClass);

            // Encontre todos os métodos de Listener adequados para "adicionar", "remover" e "obter"
            // O nome do tipo de ouvinte é a chave para essas tabelas de hash
            // ou seja, ActionListener
            Map<String, Object> adds = null;
            Map<String, Object> removes = null;
            Map<String, Object> gets = null;

            for (int i = 0; i < methodList.length; i++) {
                Method method = methodList[i];
                if (method == null) {
                    continue;
                }
                // pula métodos estáticos.
                int mods = method.getModifiers();
                if (Modifier.isStatic(mods)) {
                    continue;
                }
                String name = method.getName();
                // Otimização evita getParameterTypes
                if (!name.startsWith(ADD_PREFIX) && !name.startsWith(REMOVE_PREFIX) && !name.startsWith(GET_PREFIX)) {
                    continue;
                }

                Class<?>[] argTypes = method.getParameterTypes();
                Class<?> resultType = method.getReturnType();

                if (name.startsWith(ADD_PREFIX) && argTypes.length == 1 && resultType == Void.TYPE
                        && BeanUtils.isSubclass(argTypes[0], eventListenerType)) {
                    String listenerName = name.substring(3);
                    if (listenerName.length() > 0 && argTypes[0].getName().endsWith(listenerName)) {
                        if (adds == null) {
                            adds = new HashMap<>();
                        }
                        adds.put(listenerName, method);
                    }
                } else if (name.startsWith(REMOVE_PREFIX) && argTypes.length == 1 && resultType == Void.TYPE
                        && BeanUtils.isSubclass(argTypes[0], eventListenerType)) {
                    String listenerName = name.substring(6);
                    if (listenerName.length() > 0 && argTypes[0].getName().endsWith(listenerName)) {
                        if (removes == null) {
                            removes = new HashMap<>();
                        }
                        removes.put(listenerName, method);
                    }
                } else if (name.startsWith(GET_PREFIX) && argTypes.length == 0 && resultType.isArray()
                        && BeanUtils.isSubclass(resultType.getComponentType(), eventListenerType)) {
                    String listenerName = name.substring(3, name.length() - 1);
                    if (listenerName.length() > 0 && resultType.getComponentType().getName().endsWith(listenerName)) {
                        if (gets == null) {
                            gets = new HashMap<>();
                        }
                        gets.put(listenerName, method);
                    }
                }
            }

            if (adds != null && removes != null) {
                // Agora procure os pares addFooListener + removeFooListener correspondentes.
                // Bônus se houver um método getFooListeners correspondente também.
                Iterator<String> keys = adds.keySet().iterator();
                while (keys.hasNext()) {
                    String listenerName = keys.next();
                    // Pule qualquer "adicionar" que não tenha um "remover" correspondente ou
                    // um nome de ouvinte que não termina com Ouvinte
                    if (removes.get(listenerName) == null || !listenerName.endsWith("Listener")) {
                        continue;
                    }
                    String eventName = BeanUtils.decapitalize(listenerName.substring(0, listenerName.length() - 8));
                    Method addMethod = (Method) adds.get(listenerName);
                    Method removeMethod = (Method) removes.get(listenerName);
                    Method getMethod = null;
                    if (gets != null) {
                        getMethod = (Method) gets.get(listenerName);
                    }
                    Class<?> argType = addMethod.getParameterTypes()[0];


                    // gera uma lista de objetos Method para cada um dos alvos
                    // métodos:
                    Method allMethods[] = BeanUtils.getPublicDeclaredMethods(argType);
                    List<Method> validMethods = new ArrayList<>(allMethods.length);
                    for (int i = 0; i < allMethods.length; i++) {
                        if (allMethods[i] == null) {
                            continue;
                        }

                        if (isEventHandler(allMethods[i])) {
                            validMethods.add(allMethods[i]);
                        }
                    }
                    EventSetDescriptor esd = new EventSetDescriptor(eventName, argType, validMethods.toArray(new Method[validMethods.size()]), addMethod,
                            removeMethod, getMethod);

                    // Se o método adder lançar o TooManyListenersException
                    // então isto é uma fonte de evento Unicast.
                    if (BeanUtils.throwsException(addMethod, java.util.TooManyListenersException.class)) {
                        esd.setUnicast(true);
                    }
                    addEvent(esd);
                }
            }
        }
        EventSetDescriptor[] result;
        if (events.size() == 0) {
            result = EMPTY_EVENTSETDESCRIPTORS;
        } else {
            // Aloque e preencha a matriz de resultado.
            result = new EventSetDescriptor[events.size()];
            result = (EventSetDescriptor[]) events.values().toArray(result);

            // Set the default index.
            if (defaultEventName != null) {
                for (int i = 0; i < result.length; i++) {
                    if (defaultEventName.equals(result[i].getName())) {
                        defaultEventIndex = i;
                    }
                }
            }
        }
        return result;
    }

    private void addEvent(EventSetDescriptor esd) {
        String key = esd.getName();
        if (esd.getName().equals("propertyChange")) {
            propertyChangeSource = true;
        }
        EventSetDescriptor old = (EventSetDescriptor) events.get(key);
        if (old == null) {
            events.computeIfAbsent(key, v -> esd);
            return;
        }
        EventSetDescriptor composite = new EventSetDescriptor(old, esd);
        events.put(key, composite);
    }

    /**
     * @return Uma matriz de MethodDescriptors que descreve os métodos privados
     * suportado pelo bean de destino.
     */
    @SuppressWarnings("java:S3776")
    private MethodDescriptor[] getTargetMethodInfo() {
        if (methods == null) {
            methods = new HashMap<>(100);
        }

        // Verifique se o bean tem seu próprio BeanInfo que fornecerá
        // informação explícita.
        MethodDescriptor[] explicitMethods = null;
        if (explicitBeanInfo != null) {
            explicitMethods = explicitBeanInfo.getMethodDescriptors();
        }

        if (explicitMethods == null && superBeanInfo != null) {
            // Não temos métodos BeanInfo explícitos. Verifique com nosso pai.
            MethodDescriptor[] supers = superBeanInfo.getMethodDescriptors();
            for (int i = 0; i < supers.length; i++) {
                addMethod(supers[i]);
            }
        }

        for (int i = 0; i < additionalBeanInfo.length; i++) {
            MethodDescriptor[] additional = additionalBeanInfo[i].getMethodDescriptors();
            if (additional != null) {
                for (int j = 0; j < additional.length; j++) {
                    addMethod(additional[j]);
                }
            }
        }

        if (explicitMethods != null) {
            // Adicione os dados explicitBeanInfo explícitos aos nossos resultados.
            for (int i = 0; i < explicitMethods.length; i++) {
                addMethod(explicitMethods[i]);
            }

        } else {

            // Aplique alguma reflexão à classe atual.
            // Primeiro obtenha um array de todos os métodos do bean neste nível
            Method[] methodList = BeanUtils.getPublicDeclaredMethods(beanClass);

            // Now analyze each method.
            for (int i = 0; i < methodList.length; i++) {
                Method method = methodList[i];
                if (method == null) {
                    continue;
                }
                MethodDescriptor md = new MethodDescriptor(method);
                addMethod(md);
            }
        }

        // Aloque e preencha a matriz de resultado.
        MethodDescriptor[] result = new MethodDescriptor[methods.size()];
        result = methods.values().toArray(result);

        return result;
    }

    private void addMethod(MethodDescriptor md) {
        // Temos que ter cuidado aqui para distinguir o método por ambos os nomes
        // e listas de argumentos.
        // Este método é muito chamado, então tentamos ser eficientes.
        String name = md.getName();

        MethodDescriptor old = (MethodDescriptor) methods.get(name);
        if (old == null) {
            // This is the common case.
            methods.put(name, md);
            return;
        }

        // Temos uma colisão nos nomes dos métodos. Isso é raro.
        // Verifique se old e md têm o mesmo tipo.
        String[] p1 = md.getParamNames();
        String[] p2 = old.getParamNames();

        boolean match = false;
        if (p1.length == p2.length) {
            match = true;
            for (int i = 0; i < p1.length; i++) {
                if (!p1[i].equals(p2[i])) {
                    match = false;
                    break;
                }
            }
        }
        if (match) {
            MethodDescriptor composite = new MethodDescriptor(old, md);
            methods.put(name, composite);
            return;
        }

        // Temos uma colisão de nomes de métodos com diferentes assinaturas de tipo.
        // Isso é muito raro.

        String longKey = makeQualifiedMethodName(name, p1);
        old = (MethodDescriptor) methods.get(longKey);
        if (old == null) {
            methods.computeIfAbsent(longKey, v -> md);
            return;
        }


        MethodDescriptor composite = new MethodDescriptor(old, md);
        methods.put(longKey, composite);
    }

    private int getTargetDefaultEventIndex() {
        return defaultEventIndex;
    }

    private int getTargetDefaultPropertyIndex() {
        return defaultPropertyIndex;
    }

    private BeanDescriptor getTargetBeanDescriptor() {
        // Use informações explícitas, se disponíveis,
        if (explicitBeanInfo != null) {
            BeanDescriptor bd = explicitBeanInfo.getBeanDescriptor();
            if (bd != null) {
                return (bd);
            }
        }
        // OK, fabrique um BeanDescriptor padrão.
        return (new BeanDescriptor(beanClass));
    }

    private boolean isEventHandler(Method m) {
        // Assumimos que um método é um manipulador de eventos se tiver um único
        // argumento, cujo tipo é herdado de java.util.Event.
        Class<?>[] argTypes = m.getParameterTypes();
        if (argTypes.length != 1) {
            return false;
        }
        return (BeanUtils.isSubclass(argTypes[0], java.util.EventObject.class));
    }

}

/**
 * Classe de suporte de implementação privada do pacote para uso interno do Introspector.
 * <p>
 * Geralmente, isso é usado como um espaço reservado para os descritores.
 */

class GenericBeanInfo extends SimpleBeanInfo {

    private BeanDescriptor beanDescriptor;
    private EventSetDescriptor[] events;
    private int defaultEvent;
    private PropertyDescriptor[] properties;
    private int defaultProperty;
    private MethodDescriptor[] methods;
    private BeanInfo targetBeanInfo;

    public GenericBeanInfo(BeanDescriptor beanDescriptor, EventSetDescriptor[] events, int defaultEvent,
                           PropertyDescriptor[] properties, int defaultProperty, MethodDescriptor[] methods, BeanInfo targetBeanInfo) {
        this.beanDescriptor = beanDescriptor;
        this.events = events;
        this.defaultEvent = defaultEvent;
        this.properties = properties;
        this.defaultProperty = defaultProperty;
        this.methods = methods;
        this.targetBeanInfo = targetBeanInfo;
    }

    /**
     * Construtor dup privado de pacote Isso deve isolar o novo objeto de qualquer
     * muda para o objeto antigo.
     */
    GenericBeanInfo(GenericBeanInfo old) {

        beanDescriptor = new BeanDescriptor(old.beanDescriptor);
        if (old.events != null) {
            int len = old.events.length;
            events = new EventSetDescriptor[len];
            for (int i = 0; i < len; i++) {
                events[i] = new EventSetDescriptor(old.events[i]);
            }
        }
        defaultEvent = old.defaultEvent;
        if (old.properties != null) {
            int len = old.properties.length;
            properties = new PropertyDescriptor[len];
            for (int i = 0; i < len; i++) {
                PropertyDescriptor oldp = old.properties[i];
                if (oldp instanceof IndexedPropertyDescriptor) {
                    properties[i] = new IndexedPropertyDescriptor((IndexedPropertyDescriptor) oldp);
                } else {
                    properties[i] = new PropertyDescriptor(oldp);
                }
            }
        }
        defaultProperty = old.defaultProperty;
        if (old.methods != null) {
            int len = old.methods.length;
            methods = new MethodDescriptor[len];
            for (int i = 0; i < len; i++) {
                methods[i] = new MethodDescriptor(old.methods[i]);
            }
        }
        targetBeanInfo = old.targetBeanInfo;
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        return properties;
    }

    @Override
    public int getDefaultPropertyIndex() {
        return defaultProperty;
    }

    @Override
    public EventSetDescriptor[] getEventSetDescriptors() {
        return events;
    }

    @Override
    public int getDefaultEventIndex() {
        return defaultEvent;
    }

    @Override
    public MethodDescriptor[] getMethodDescriptors() {
        return methods;
    }

    @Override
    public BeanDescriptor getBeanDescriptor() {
        return beanDescriptor;
    }

}
