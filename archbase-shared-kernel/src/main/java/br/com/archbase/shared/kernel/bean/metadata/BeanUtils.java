package br.com.archbase.shared.kernel.bean.metadata;

import lombok.SneakyThrows;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class BeanUtils {

    private static Map<Class<?>, SoftReference<?>> declaredMethodCache = Collections.synchronizedMap(new WeakHashMap<>());

    private BeanUtils() {
    }

    /**
     * Encontre métodos acessíveis e inclua todas as interfaces herdadas também.
     * <p>
     * É um pouco lento quando comparado com
     * {@link MethodUtils#findAccessibleMethod(Class, String, Class...)}
     * </p>
     *
     * @param start
     * @param methodName
     * @param argCount      número de argumentos
     * @param argumentTypes lista de tipos de argumento. Se nulo, o método é determinado com base
     *                      em <code>argCount</code>
     * @return
     */
    @SuppressWarnings("all")
    public static Method findAccessibleMethodIncludeInterfaces(Class<?> start, String methodName,
                                                               int argCount, Class<?>[] argumentTypes) {

        if (methodName == null) {
            return null;
        }
        // Para métodos sobrescritos, precisamos encontrar a versão mais derivada.
        // Então, começamos com a classe fornecida e subimos na cadeia da superclasse.

        Method method = null;

        for (Class<?> cl = start; cl != null; cl = cl.getSuperclass()) {
            Method[] methods = getPublicDeclaredMethods(cl);
            for (int i = 0; i < methods.length; i++) {
                method = methods[i];
                if (method == null) {
                    continue;
                }

                // certifique-se de que a assinatura do método corresponda.
                Class[] params = method.getParameterTypes();
                if (method.getName().equals(methodName) && params.length == argCount) {
                    if (argumentTypes != null) {
                        boolean different = false;
                        if (argCount > 0) {
                            for (int j = 0; j < argCount; j++) {
                                if (params[j] != argumentTypes[j]) {
                                    different = true;
                                    continue;
                                }
                            }
                            if (different) {
                                continue;
                            }
                        }
                    }
                    return method;
                }
            }
        }
        method = null;

        // Agora verifique todas as interfaces herdadas. Isso é necessário tanto quando
        // a classe do argumento é ela própria uma interface, e quando o argumento
        // classe é uma classe abstrata.
        Class[] ifcs = start.getInterfaces();
        for (int i = 0; i < ifcs.length; i++) {
            // Observação: a implementação original tinha ambos os métodos chamando
            // o método de 3 arg. Isso é preservado, mas talvez devesse
            // passa a matriz args em vez de nula.
            method = findAccessibleMethodIncludeInterfaces(ifcs[i], methodName, argCount, null);
            if (method != null) {
                break;
            }
        }
        return method;
    }


    /**
     * Obtenha os métodos declarados como públicos dentro da classe
     *
     * @param clz classe para encontrar métodos públicos
     * @return
     */
    @SuppressWarnings("all")
    public static synchronized Method[] getPublicDeclaredMethods(Class<?> clz) {
        // Procurar Class.getDeclaredMethods é relativamente caro,
        // então armazenamos os resultados em cache.
        Method[] result = null;
        final Class<?> fclz = clz;
        Reference<Method[]> ref = (Reference<Method[]>) declaredMethodCache.get(fclz);
        if (ref != null) {
            result = ref.get();
            if (result != null) {
                return result;
            }
        }

        // Temos que aumentar o privilégio para getDeclaredMethods
        result = AccessController.doPrivileged((PrivilegedAction<Method[]>) fclz::getDeclaredMethods);

        // Anula quaisquer métodos não públicos.
        for (int i = 0; i < result.length; i++) {
            Method method = result[i];
            int mods = method.getModifiers();
            if (!Modifier.isPublic(mods)) {
                result[i] = null;
            }
        }
        // Adicione ao cache.
        declaredMethodCache.put(fclz, new SoftReference<>(result));
        return result;
    }

    /**
     * Método utilitário para pegar uma string e convertê-la em uma variável Java normal
     * capitalização do nome. Isso normalmente significa converter o primeiro caractere
     * de maiúsculas para minúsculas, mas no caso especial (incomum) quando
     * há mais de um caractere e o primeiro e o segundo caracteres
     * são maiúsculas, vamos deixá-lo sozinho.
     * <p>
     * Assim, "FooBah" torna-se "fooBah" e "X" torna-se "x", mas "URL" permanece como
     * "URL".
     *
     * @param name A string a ser descapitalizado.
     * @return A versão decapitalizada da string.
     */
    public static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))
                && Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    /**
     * Retorna verdadeiro se o método dado lançar a exceção dada
     *
     * @param method    O método que lança a exceção
     * @param exception
     * @return
     */
    public static boolean throwsException(Method method, Class<?> exception) {
        Class<?>[] exs = method.getExceptionTypes();
        for (int i = 0; i < exs.length; i++) {
            if (exs[i] == exception) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retorne verdadeiro se a classe a for equivalente à classe b, ou
     * se a classe a for uma subclasse da classe b, ou seja, se um "estende"
     * ou "implementa" b.
     * Observe que um ou ambos os objetos de "Classe" podem representar interfaces.
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean isSubclass(Class<?> a, Class<?> b) {
        // Contamos com o fato de que, para qualquer classe java ou
        // tipo primitivo existe um objeto Class exclusivo, então
        // podemos usar a equivalência de objetos nas comparações.
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        for (Class<?> x = a; x != null; x = x.getSuperclass()) {
            if (x == b) {
                return true;
            }
            if (b.isInterface()) {
                Class<?>[] interfaces = x.getInterfaces();
                for (int i = 0; i < interfaces.length; i++) {
                    if (isSubclass(interfaces[i], b)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Tente criar uma instância de uma classe nomeada.
     * Primeiro tente o carregador de classe de "irmão" e, em seguida, tente o sistema
     * classloader e então o carregador de classes do Thread atual.
     *
     * @param sibling
     * @param className
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    @SneakyThrows
    public static Object instantiate(Class<?> sibling, String className) throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        // Primeiro verifique com o carregador de classe do irmão (se houver).
        ClassLoader cl = sibling.getClassLoader();
        if (cl != null) {
            try {
                Class<?> cls = cl.loadClass(className);
                return cls.getConstructor().newInstance();
            } catch (Exception ex) {
                // Apenas pule e experimente o carregador de classe do sistema.
            }
        }

        // Agora tente o carregador de classe do sistema.
        try {
            cl = ClassLoader.getSystemClassLoader();
            if (cl != null) {
                Class<?> cls = cl.loadClass(className);
                return cls.getConstructor().newInstance();
            }
        } catch (Exception ex) {
            // Não temos permissão para acessar o carregador de classes do sistema ou
            // a criação da classe falhou.
            // Passe direto.
        }

        // Use o classloader do Thread atual.
        cl = Thread.currentThread().getContextClassLoader();
        Class<?> cls = cl.loadClass(className);
        return cls.getConstructor().newInstance();
    }


}
