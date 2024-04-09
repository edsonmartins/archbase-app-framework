package br.com.archbase.shared.kernel.bean.metadata;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Um MethodDescriptor descreve um método particular que um Java Bean
 * Suporta acesso externo de outros componentes.
 */
@SuppressWarnings("java:S2583")
public class MethodDescriptor extends FeatureDescriptor {

    private Reference<?> methodRef;

    private String[] paramNames;

    private List<WeakReference<?>> params;

    private ParameterDescriptor[] parameterDescriptors;

    /**
     * Constrói um <code> MethodDescriptor </code> a partir de um <code> Método </code>.
     * <p>
     * método @param
     * As informações do método de baixo nível.
     */
    public MethodDescriptor(Method method) {
        this(method, null);
    }

    /**
     * Constrói um <code> MethodDescriptor </code> a partir de um <code> Método </code>
     * fornecendo informações descritivas para cada
     * dos parâmetros do método.
     * <p>
     * método @param
     * As informações do método de baixo nível.
     *
     * @param parameterDescriptors Informações descritivas para cada um dos
     *                             parâmetros do método.
     */
    public MethodDescriptor(Method method, ParameterDescriptor[] parameterDescriptors) {
        setName(method.getName());
        setMethod(method);
        this.parameterDescriptors = parameterDescriptors;
    }

    /**
     * Construtor de pacote privado
     * Mesclar dois descritores de método. Onde eles entrarem em conflito, dê o
     * prioridade do segundo argumento (y) sobre o primeiro argumento (x).
     *
     * @param x O primeiro MethodDescriptor (prioridade mais baixa)
     * @param y O segundo MethodDescriptor (de maior prioridade)
     */
    MethodDescriptor(MethodDescriptor x, MethodDescriptor y) {
        super(x, y);

        methodRef = x.methodRef;
        if (y.methodRef != null) {
            methodRef = y.methodRef;
        }
        params = x.params;
        if (y.params != null) {
            params = y.params;
        }
        paramNames = x.paramNames;
        if (y.paramNames != null) {
            paramNames = y.paramNames;
        }

        parameterDescriptors = x.parameterDescriptors;
        if (y.parameterDescriptors != null) {
            parameterDescriptors = y.parameterDescriptors;
        }
    }

    /**
     * Construtor dup privado de pacote
     * Isso deve isolar o novo objeto de quaisquer alterações no objeto antigo.
     */
    MethodDescriptor(MethodDescriptor old) {
        super(old);

        methodRef = old.methodRef;
        params = old.params;
        paramNames = old.paramNames;

        if (old.parameterDescriptors != null) {
            int len = old.parameterDescriptors.length;
            parameterDescriptors = new ParameterDescriptor[len];
            for (int i = 0; i < len; i++) {
                parameterDescriptors[i] = new ParameterDescriptor(old.parameterDescriptors[i]);
            }
        }
    }

    /**
     * Obtém o método que este encapsualtes MethodDescriptor.
     *
     * @return A descrição de baixo nível do método
     */
    @SuppressWarnings("java:S3776")
    public synchronized Method getMethod() {
        Method method = getMethod0();
        if (method == null) {
            Class<?> cls = getClass0();
            if (cls != null) {
                Class<?>[] parameters = getParams();
                if (parameters != null) {
                    method = BeanUtils.findAccessibleMethodIncludeInterfaces(cls, getName(), parameters.length, parameters);
                } else {
                    for (int i = 0; i < 3; i++) {
                        // Encontre métodos para até 2 parâmetros. Estamos supondo
                        // aqui.
                        // Este bloco nunca deve ser executado a menos que o
                        // classloader
                        // que carregou as classes de argumento desaparece.
                        method = BeanUtils.findAccessibleMethodIncludeInterfaces(cls, getName(), i, null);
                        if (method != null) {
                            break;
                        }
                    }
                }
                setMethod(method);
            }
        }
        return method;
    }

    private synchronized void setMethod(Method method) {
        if (method == null) {
            return;
        }
        if (getClass0() == null) {
            setClass0(method.getDeclaringClass());
        }
        setParams(method.getParameterTypes());
        methodRef = createReference(method, true);
    }

    private Method getMethod0() {
        return (Method) getObject(methodRef);
    }

    // pp getParamNames usado como uma otimização para evitar
    // method.getParameterTypes.
    String[] getParamNames() {
        return paramNames;
    }

    private synchronized Class<?>[] getParams() {
        Class<?>[] clss = new Class[params.size()];

        for (int i = 0; i < params.size(); i++) {
            Reference<?> ref = params.get(i);
            Class<?> cls = (Class<?>) ref.get();
            if (cls == null) {
                return new Class<?>[]{};
            } else {
                clss[i] = cls;
            }
        }
        return clss;
    }

    private synchronized void setParams(Class<?>[] param) {
        if (param == null) {
            return;
        }
        paramNames = new String[param.length];
        params = new ArrayList<>(param.length);
        for (int i = 0; i < param.length; i++) {
            paramNames[i] = param[i].getName();
            params.add(new WeakReference<>(param[i]));
        }
    }

    /**
     * Obtém o ParameterDescriptor para cada um deste MethodDescriptor
     * parâmetros do método.
     *
     * @return Os nomes independentes do local dos parâmetros. Pode voltar
     * uma matriz nula se os nomes dos parâmetros não forem conhecidos.
     */
    public ParameterDescriptor[] getParameterDescriptors() {
        return parameterDescriptors;
    }

}
