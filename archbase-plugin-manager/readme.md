# archbase-plugin-manager

Um plugin é uma maneira de terceiros estenderem a funcionalidade de um aplicativo. Um plug-in implementa pontos de extensão declarados pelo aplicativo ou outros plug-ins. Além disso, um plugin pode definir pontos de extensão.

Você pode marcar qualquer interface ou classe abstrata como um ponto de extensão (com interface de marcador `ExtensionPoint`) e especificar que uma classe é uma extensão com anotação `@Extension`.

#### Componentes

-   **ArchbasePlugin** é a classe base para todos os tipos de plug-ins. Cada plugin é carregado em um carregador de classes separado para evitar conflitos.
-   **O ArchbasePluginManager** é usado para todos os aspectos do gerenciamento de plug-ins (carregar, iniciar, parar). Você pode usar uma implementação embutida `DefaultArchbasePluginManager`ou pode implementar um gerenciador de plug-ins customizado começando por `AbstractArchbasePluginManager`(implementar apenas métodos de fábrica).
-   **ArchbasePluginLoader** carrega todas as informações (classes) necessárias para um plugin.
-   **ExtensionPoint** é um ponto no aplicativo onde o código personalizado pode ser chamado. É um marcador de interface java.  
    Qualquer interface java ou classe abstrata pode ser marcada como um ponto de extensão (implementa `ExtensionPoint`interface).
-   **Extension** é uma implementação de um ponto de extensão. É uma anotação java em uma classe.


É muito simples adicionar o framework em seu aplicativo:

```java
public static void main(String[] args) {
    ...

    ArchbasePluginManager pluginManager = new DefaultArchbasePluginManager();
    pluginManager.loadPlugins();
    pluginManager.startPlugins();

    ...
}
```


No código acima, criei um **DefaultArchbasePluginManager** (é a implementação padrão da interface **ArchbasePluginManager** ) que carrega e inicia todos os plug-ins ativos (resolvidos).

Cada plugin disponível é carregado usando um carregador de classe java diferente, **PluginClassLoader** .

O **PluginClassLoader** contém apenas classes encontradas em **PluginClasspath** ( _classes_ padrão e pastas _lib_ ) de plug-ins e classes de tempo de execução e bibliotecas dos plug-ins necessários / dependentes. Este carregador de classes é um _Último ClassLoader Pai_ - ele carrega as classes dos jars do plug-in antes de delegar ao carregador de classes pai.

Os plug-ins são armazenados em uma pasta. Você pode especificar a pasta de plug-ins no construtor de DefaultArchbasePluginManager. Se a pasta de plug-ins não for especificada, o local será retornado por `System.getProperty("archbase.pluginsDir", "plugins")`.

A estrutura da pasta de plug-ins é:

-   plugin1.zip (ou pasta plugin1)
-   plugin2.zip (ou pasta plugin2)

Na pasta de plug-ins você pode colocar um plug-in como pasta ou arquivo (zip). Uma pasta de plug-in tem esta estrutura por padrão:

-   `classes` pasta
-   `lib` pasta (opcional - se o plug-in usou bibliotecas de terceiros)

O gerenciador de plug-ins pesquisa metadados de plug-ins usando um **PluginDescriptorFinder** .

**DefaultArchbasePluginDescriptorFinder** é um “link” para **ManifestPluginDescriptorFinder** que pesquisa descritores de plug-ins no arquivo MANIFEST.MF.

Neste caso, o `classes/META-INF/MANIFEST.MF`arquivo se parece com:

```properties
Manifest-Version: 1.0
Archiver-Version: Plexus Archiver
Created-By: Apache Maven
Built-By: Archbase
Build-Jdk: 1.6.0_17
Plugin-Class: br.com.archbase.plugin.demo.BemVindoPlugin
Plugin-Dependencies: x, y, z
Plugin-Id: bemvindo-plugin
Plugin-Provider: Archbase
Plugin-Version: 1.0.0

```

No manifesto acima, descrevemos um plugin com id `bemvindo-plugin`, com classe `br.com.archbase.plugin.demo.BemVindoPlugin`, com versão `1.0.0`e com dependências para plugins `x, y, z`.

**NOTA:** A versão do plugin deve ser compatível com o Controle de [Versão Semântico](https://semver.org/) 

Você pode definir um ponto de extensão em seu aplicativo usando o marcador de interface **ExtensionPoint** .

```java
public interface Saudacao extends ExtensionPoint {

    String getSaudacao();

}

```

Outro componente interno importante é **ExtensionFinder,** que descreve como o gerenciador de plug-ins descobre extensões para os pontos de extensão.

**DefaultExtensionFinder** procura extensões usando a anotação de **extensão** .

DefaultExtensionFinder procura extensões em todos os arquivos de índice de extensões `META-INF/extensions.idx`. O framerwork usa Java Annotation Processing para processar em tempo de compilação todas as classes anotadas com @Extension e para produzir o arquivo de índice de extensões.

```java
public class BemVindoPlugin extends Plugin {

    public BemVindoPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Extension
    public static class Saudacao implements Greeting {

        public String getSaudacao() {
            return "Bem vindo";
        }

    }

}

```

No código acima, fornecemos uma extensão para o ponto de extensão `Saudacao`.

Você pode recuperar todas as extensões de um ponto de extensão com:

```java
List<Saudacao> saudacoes = pluginManager.getExtensions(Saudacao.class);
for (Saudacao saudacao : saudacoes) {
    System.out.println(">>> " + saudacao.getSaudacao());
}

```

O resultado é:

```bash
>>> Bem vindo
>>> Olá

```

Você pode injetar seu componente personalizado (por exemplo, PluginDescriptorFinder, ExtensionFinder, PluginClasspath,…) em DefaultArchbasePluginManager apenas substituir `create...`métodos (padrão de método de fábrica).

Exemplo:

```
protected PluginDescriptorFinder createPluginDescriptorFinder() {
    return new PropertiesPluginDescriptorFinder();
}

```

e no repositório do plug-in, você deve ter um arquivo plugin.properties com o conteúdo abaixo:

```properties
plugin.class=br.com.archbase.plugin.demo.WelcomePlugin
plugin.dependencies=x, y, z
plugin.id=welcome-plugin
plugin.provider=Archbase
plugin.version=1.0.0

```

Você pode controlar o método `createExtensionFactory` de substituição de criação de instância de extensão em DefaultArchbaseExtensionFinder. Além disso, você pode controlar o `createPluginFactory`método de substituição de criação de instância do plug-in em DefaultArchbaseExtensionFinder.


**NOTA:** Se seu aplicativo não encontrou extensões, certifique-se de que você tenha um arquivo com o nome `extensions.idx` gerado pelo framework no jar do plugin. É mais provável que sejam alguns problemas com o mecanismo de processamento de anotações do Java. Uma solução possível para resolver seu problema é adicionar uma configuração à sua compilação de maven. O `maven-compiler-plugin`pode ser configurado para fazer isso da seguinte maneira:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>2.5.1</version>
    <configuration>
        <annotationProcessors>
            <annotationProcessor>br.com.archbase.plugin.manager.processor.ExtensionAnnotationProcessor</annotationProcessor>
        </annotationProcessors>
    </configuration>
</plugin>
```


## Carregando classes

Os carregadores de classes são responsáveis ​​por carregar classes Java durante o tempo de execução de forma dinâmica para a JVM (Java Virtual Machine). Os carregadores de classes fazem parte do Java Runtime Environment. Quando a JVM solicita uma classe, o carregador de classes tenta localizar a classe e carregar a definição da classe no tempo de execução usando o nome de classe totalmente qualificado. O `java.lang.ClassLoader.loadClass()`método é responsável por carregar a definição da classe no tempo de execução. Ele tenta carregar a classe com base em um nome totalmente qualificado.

Se a classe ainda não estiver carregada, ele delega a solicitação ao carregador de classe pai. Esse processo acontece recursivamente.

O framework usa PluginClassLoader para carregar classes de plugins.  
Portanto, **cada plugin disponível é carregado usando um diferente`PluginClassLoader`** .  
Uma instância de `PluginClassLoader`deve ser criada pelo gerenciador de plug-ins para cada plug-in disponível.  
Por padrão, este carregador de classes é um Último ClassLoader Pai - ele carrega as classes dos jars do plug-in antes de delegar ao carregador de classes pai.

Por padrão (pai por último), `PluginClassLoader`usa a estratégia abaixo quando uma solicitação de classe de carga é recebida por meio do `loadClass(String className)`método:

-   se a classe for uma classe do sistema ( `className`começa com `java.`), delegue ao carregador do sistema;
-   se a classe fizer parte do mecanismo de plug-in ( `className`começa com `br.com.archbase.plugin`), use o carregador de classe pai ( `ApplicationClassLoader`em geral);
-   tente carregar usando a instância atual do PluginClassLoader;
-   se o PluginClassLoader atual não pode carregar a classe, tente delegar para `PluginClassLoader`s das dependências do plugin
-   delegar carga de classe ao carregador de classe pai

Use o `parentFirst`parâmetro de `PluginClassLoader`para alterar a estratégia de carregamento.  
Por exemplo, se eu quiser usar uma estratégia Parent First em meu aplicativo, tudo que preciso para conseguir isso é:

```java
new DefaultArchbasePluginManager() {
    
    @Override
    protected PluginClassLoader createPluginClassLoader(Path pluginPath, PluginDescriptor pluginDescriptor) {
        return new PluginClassLoader(pluginManager, pluginDescriptor, getClass().getClassLoader(), true);
    }

};

```

Se você quiser saber qual plugin carregou uma classe específica, você pode usar:

```
pluginManager.whichPlugin(MinhaClasse.class);

```

O framework usa por padrão um carregador de classes separado para cada plugin, mas isso não significa que você não pode usar o mesmo carregador de classes (provavelmente o carregador de classes do aplicativo) para todos os plug-ins. Se seu aplicativo requer este caso de uso, o que você deve fazer é retornar o mesmo carregador de classes de `ArchbasePluginLoader.loadPlugin`:

```java
public interface ArchbasePluginLoader {

    boolean isApplicable(Path pluginPath);

    ClassLoader loadPlugin(Path pluginPath, PluginDescriptor pluginDescriptor);

}

```

Se você usar, `DefaultArchbasePluginManager`pode escolher substituir `DefaultArchbasePluginManager.createPluginLoader`e / ou `DefaultArchbasePluginManager.createClassLoader`.



## Empacotamento

Depois de desenvolver e testar seu plug-in, você deve empacotar e lançar.  
Atualmente, o framework suporta dois tipos de pacotes integrados

-   fat/shade/one-jar file (`.jar`)
-   zip file com  `lib`  e  `classes`  directories (.zip)

Para instalar um plugin em seu aplicativo, você precisa adicioná-lo ao diretório de`plugins` (pluginsRoot). Seu conteúdo do diretório `plugins` pode ser semelhante a:

```
$ tree plugins
plugins
├── disabled.txt
├── enabled.txt
├── demo-plugin1-2.4.0.zip
└── demo-plugin2-2.4.0.zip

```

ou

```
$ tree plugins
plugins
├── disabled.txt
├── enabled.txt
├── demo-plugin1-2.4.0.jar
└── demo-plugin2-2.4.0.jar

```

se você usar `.jar`formato de pacote de plug-in.

Se quiser, você pode misturar vários formatos de embalagem. Por exemplo, por padrão, você pode misturar `.jar`plug- `.zip`ins com plug-ins:

```bash
$ tree plugins
plugins
├── disabled.txt
├── enabled.txt
├── demo-plugin1-2.4.0.jar
└── demo-plugin2-2.4.0.zip

```

Recomendamos que você use `.jar`porque é o mais simples e é um formato padrão em Java.

Todos os plug-ins são carregados por `ArchbasePluginManager`do diretório `plugins`.  
Você pode especificar outro local usando a `archbase.pluginsDir` do sistema ( `-Darchbase.pluginsDir=plugins`) ou programaticamente ao criar `DefaultArchbasePluginManager`.


## Plugins

### Sobre plugins

Um plugin agrupa classes e bibliotecas Java (arquivos JAR), que podem ser carregados / descarregados pelo framework no tempo de execução do aplicativo.

Caso você não precise carregar / descarregar certas partes do código Java em seu aplicativo em tempo de execução, não é estritamente necessário usar plug-ins. Você também pode fazer uso apenas de ***extensões*** e colocar as classes compiladas no classpath do aplicativo (as chamadas ***extensões do sistema*** ).

### Como os plug-ins são definidos

Cada plugin deve fornecer uma classe, que é derivada da classe `br.com.archbase.plugin.manager.ArchbasePlugin`:

```java
import br.com.archbase.plugin.manager.ArchbasePlugin;
import br.com.archbase.plugin.manager.PluginWrapper;

public class MeuPlugin extends ArchbasePlugin {

    public MeuPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        // Este método é chamado pelo aplicativo quando o plugin é iniciado.
    }

    @Override
    public void stop() {
        // Este método é chamado pelo aplicativo quando o plugin é interrompido.
    }

    @Override
    public void delete() {
        // Este método é chamado pelo aplicativo quando o plugin é excluído.
    }

}
```

### Como os metadados do plugin são definidos

Para tornar o plugin carregável pelo framework, você também deve fornecer alguns metadados.

-   O nome de classe totalmente qualificado da classe de plug-in (derivado de `br.com.archbase.plugin.manager.ArchbasePlugin`) **(opcional)** .
-   O identificador exclusivo do plugin.
-   A versão do plugin de acordo com a [Especificação de Controle de Versão Semântica](https://semver.org/) .
-   A versão necessária do aplicativo de acordo com a [Especificação de Controle de Versão Semântica](https://semver.org/) **(opcional)** .
-   Dependências com outros plug-ins **(opcional)** .
-   Uma descrição do plugin **(opcional)** .
-   O nome do provedor / autor do plugin **(opcional)** .
-   A licença do plugin **(opcional)** .

Um nome de classe de plugin é opcional. Você pode criar uma classe de plug-in apenas se quiser ser notificado quando seu plug-in for `started`, `stopped`ou `deleted`.

Existem várias maneiras de fornecer metadados para um plugin.

#### Fornece metadados de plug-in por meio de MANIFEST.MF

Adicione o seguinte conteúdo ao `META-INF/MANIFEST.MF`arquivo do plugin:


```properties
Plugin-Class: br.com.archbase.plugin.demo.BemVindoPlugin
Plugin-Id: bemvindo-plugin
Plugin-Version: 1.0.0
Plugin-Requires: 1.0.0
Plugin-Dependencies: x, y, z
Plugin-Description: Meu plugin exemplo
Plugin-Provider: Archbase
Plugin-License: Apache License 2.0
```

Caso esteja usando o Maven, você pode definir esses valores em seu `pom.xml`via `maven-jar-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <configuration>
        <archive>
            <manifest>
                <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
            </manifest>
            <manifestEntries>
                <Plugin-Class>br.com.archbase.plugin.demo.BemVindoPlugin</Plugin-Class>
                <Plugin-Id>bemvindo-plugin</Plugin-Id>
                <Plugin-Version>1.0.0</Plugin-Version>
                <Plugin-Requires>1.0.0</Plugin-Requires>
                <Plugin-Dependencies>x, y, z</Plugin-Dependencies>
                <Plugin-Description>Meu plugin exemplo</Plugin-Description>
                <Plugin-Provider>Archbase</Plugin-Provider>
                <Plugin-License>Apache License 2.0</Plugin-License>
            </manifestEntries>
        </archive>
    </configuration>
</plugin>
```
Ou via `maven-assembly-plugin`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <configuration>
        <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
        <finalName>${project.artifactId}-${project.version}-plugin</finalName>
        <appendAssemblyId>false</appendAssemblyId>
        <attach>false</attach>
        <archive>
            <manifest>
                <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
            </manifest>
            <manifestEntries>
                <Plugin-Class>br.com.archbase.plugin.demo.BemVindoPlugin</Plugin-Class>
                <Plugin-Id> bemvindo-plugin</Plugin-Id>
                <Plugin-Version>1.0.0</Plugin-Version>
                <Plugin-Requires>1.0.0</Plugin-Requires>
                <Plugin-Dependencies>x, y, z</Plugin-Dependencies>
                <Plugin-Description>Meu plugin exemplo</Plugin-Description>
                <Plugin-Provider>Archbase</Plugin-Provider>
                <Plugin-License>Apache License 2.0</Plugin-License>
            </manifestEntries>
        </archive>
    </configuration>
    <executions>
        <execution>
            <id>make-assembly</id>
            <phase>package</phase>
            <goals>
                <goal>single</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### Fornece metadados do plugin por meio do arquivo de propriedades

Crie um arquivo chamado `plugin.properties`na raiz da pasta do seu plugin (ou arquivo ZIP):

```properties
plugin.class=br.com.archbase.plugin.demo.BemVindoPlugin
plugin.id=bemvindo-plugin
plugin.version=1.0.0
plugin.requires=1.0.0
plugin.dependencies=x, y, z
plugin.description=Meu plugin exemplo
plugin.provider=Archbase
plugin.license=Apache License 2.0
```

### Notas sobre dependências de plugins

Os plug-ins podem ter dependências uns dos outros. Essas dependências são especificadas nos metadados do plug-in conforme descrito acima. Para fazer referência a um determinado plugin como uma dependência, você precisa fornecer seu id de plugin especificado.

-   Se o **pluginA** depende de outro **pluginB,** você pode definir nos metadados do **plugin A** :
    
    ```
    Plugin-Dependencies: pluginB
    
    ```
    
-   Se o **pluginA** depender de outro **pluginB** na versão 1.0.0, você pode definir nos metadados do **pluginA** :
    
    ```
    Plugin-Dependencies: pluginB@1.0
    
    ```
    
-   Se o **pluginA** depende de outro **pluginB a** partir da versão 1.0.0, você pode definir nos metadados do **pluginA** :
    
    ```
    Plugin-Dependencies: pluginB@>=1.0.0
    
    ```
    
-   Se o **pluginA** depende de outro **plugin B a** partir da versão 1.0.0 até 2.0.0 (excluindo), você pode definir nos metadados do **plugin A** :
    
    ```
    Plugin-Dependencies: pluginB@>=1.0.0 & <2.0.0
    
    ```
    
-   Se o **pluginA** depende de outro **pluginB a** partir da versão 1.0.0 até 2.0.0 (incluindo), você pode definir nos metadados do **pluginA** :
    
    ```
    Plugin-Dependencies: pluginB@>=1.0.0 & <=2.0.0
    
    ```
    
-   Você também pode definir várias dependências de plug-in com o mesmo padrão, separadas por uma vírgula:
    
    ```
    Plugin-Dependencies: pluginB@>=1.0.0 & <=2.0.0, pluginC@>=0.0.1 & <=0.1.0
    
    ```
    

Esses tipos de dependências são consideradas **necessárias** . O gerenciador de plug-ins apenas disponibilizará um plug-in em tempo de execução, se todas as suas dependências forem cumpridas.

#### Dependências opcionais do plugin

Alternativamente, você também pode definir dependências **opcionais** entre os plug-ins, adicionando um ponto de interrogação atrás do id do plug-in - por exemplo:

```
Plugin-Dependencies: pluginB?

```

ou

```
Plugin-Dependencies: pluginB?@1.0

```

Nesse caso, o **pluginA** ainda está sendo carregado, mesmo que a dependência não seja realizada em tempo de execução. 


## Ciclo de vida do plugin

Cada plugin passa por um conjunto predefinido de estados. *PluginState* define todos os estados possíveis.  
Os principais estados do plugin são:

-   `CREATED`
-   `DISABLED`
-   `RESOLVED`
-   `STARTED`
-   `STOPPED`

O `DefaultArchbasePluginManager`contém a seguinte lógica:

-   todos os plug-ins são resolvidos e carregados
-   `DISABLED`plugins **NÃO** são automaticamente `STARTED` em `startPlugins()`  **MAS** você pode iniciar manualmente (e portanto habilitar) um plugin _DISABLED_ chamando ao `startPlugin(pluginId)`invés de `enablePlugin(pluginId)`+`startPlugin(pluginId)`
-   apenas `STARTED`plug-ins podem contribuir com extensões. Qualquer outro estado não deve ser considerado pronto para contribuir com uma extensão para o sistema em execução.

As diferenças entre um `DISABLED`plugin e um `STARTED`plugin são:

-   um `STARTED`plugin foi executado `ArchbasePlugin.start()`, um `DISABLED`plugin não
-   um `STARTED`plugin pode contribuir com instâncias de extensão, um `DISABLED`plugin não pode

`DISABLED`plug-ins ainda têm carregadores de classe válidos e suas classes podem ser carregadas e exploradas manualmente, mas o carregamento de recursos - que é importante para inspeção - foi prejudicado pela verificação `DISABLED`.

À medida que os integradores do framework desenvolvem suas APIs de extensão, será necessário especificar uma versão mínima do sistema para carregar plug-ins. Carregar e iniciar um plugin mais novo em um sistema mais antigo pode resultar em falhas de tempo de execução devido a alterações de assinatura de método ou outras diferenças de classe.

Por este motivo foi adicionado um atributo de manifesto (in `PluginDescriptor`) para especificar uma versão 'necessária' que é uma versão mínima do sistema no formato xyz, ou uma ***Expressão SemVer***. `DefaultArchbasePluginManager` contém também um método para especificar a versão do sistema do gerenciador de plug-ins e a lógica para desabilitar plug-ins no carregamento se a versão do sistema for muito antiga (se você quiser controle total, substitua `isPluginValid()`). Isso funciona para ambos `loadPlugins()`e `loadPlugin()`.

**PluginStateListener** define a interface para um objeto que escuta as mudanças de estado do plugin. Você pode usar `addPluginStateListener()`e `removePluginStateListener()`do PluginManager se quiser adicionar ou remover um ouvinte de estado do plug-in.

Seu aplicativo, como consumidor do framework, tem controle total sobre cada plugin (estado). Assim, você pode carregar, descarregar, habilitar, desabilitar, iniciar, parar e deletar um determinado plugin usando `ArchbasePluginManager`(programaticamente).


## Montagem de plug-in

Depois de desenvolver um plug-in, a próxima etapa é implementá-lo em seu aplicativo. Para esta tarefa, uma opção é criar um arquivo zip com a estrutura descrita na seção ***Como usar*** desde o início do documento.


## Plugin Manager personalizado

Para criar um gerenciador de plugins personalizado, você deve escolher uma das opções abaixo:

-   implementar interface `PluginManager` (crie um gerenciador de plugins do zero);
-   modificar alguns aspectos / comportamentos de implementações integradas ( `DefaultArchbasePluginManager`);
-   estender a classe `AbstractPluginManager`.

No caso mais comum, um plugin é um fat jar, um jar que contém classes de todas as bibliotecas das quais depende o seu projeto e, claro, as classes do projeto atual.

`AbstractArchbasePluginManager`adiciona um pouco de cola que o ajuda a criar rapidamente um gerenciador de plugins. Tudo que você precisa fazer é implementar alguns métodos de fábrica. PF4J usa em muitos lugares o padrão de método de fábrica para implementar o conceito de injeção de dependência (DI) em um modo manual. Veja abaixo os métodos abstratos para `AbstractArchbasePluginManager`:

```java
public abstract class AbstractArchbasePluginManager implements ArchbasePluginManager {

    protected abstract PluginRepository createPluginRepository();
    protected abstract ArchbasePluginFactory createPluginFactory();
    protected abstract ExtensionFactory createExtensionFactory();
    protected abstract PluginDescriptorFinder createPluginDescriptorFinder();
    protected abstract ExtensionFinder createExtensionFinder();
    protected abstract PluginStatusProvider createPluginStatusProvider();
    protected abstract ArchbasePluginLoader createPluginLoader();

    // outros métodos não abstratos

}
```

`DefaultArchbasePluginManager`contribui com componentes “default” ( `DefaultArchbaseExtensionFactory`, `DefaultArchbasePluginFactory`, `DefaultArchbasePluginLoader`, ...) para `AbstractArchbasePluginManager`.  
Na maioria das vezes basta estender `DefaultArchbasePluginManager`e fornecer seus componentes personalizados.

É possível coexistir vários tipos de plug-ins (jar, zip, diretório) no mesmo `ArchbasePluginManager`. Por exemplo, `DefaultArchbasePluginManager`funciona imediatamente com plug-ins jar, zip e diretório. A ideia é que `DefaultArchbasePluginManager`use uma versão composta para:

-   `PluginDescriptorFinder`( `CompoundPluginDescriptorFinder`)
-   `ArchbasePluginLoader`( `CompoundPluginLoader`)
-   `PluginRepository`( `CompoundPluginRepository`)

```java
public class DefaultPluginManager extends AbstractArchbasePluginManager {
   
    // outros métodos
    
    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        return new CompoundPluginDescriptorFinder()
            .add(new PropertiesPluginDescriptorFinder())
            .add(new ManifestPluginDescriptorFinder());
    }
    
    @Override
    protected PluginRepository createPluginRepository() {
        return new CompoundPluginRepository()
            .add(new DefaultArchbasePluginRepository(getPluginsRoot(), isDevelopment()))
            .add(new JarPluginRepository(getPluginsRoot()));
    }
    
    @Override
    protected PluginLoader createPluginLoader() {
        return new CompoundPluginLoader()
            .add(new DefaultArchbasePluginLoader(this, pluginClasspath))
            .add(new JarPluginLoader(this));
    }

}
```

Se você usar apenas jars como plug-ins (sem arquivos zip, sem diretórios), e os metadados do plug-in estiverem disponíveis no `MANIFEST.MF`arquivo, você deve usar um gerenciador de plug-ins personalizado, algo como:

```java
PluginManager pluginManager = new DefaultArchbasePluginManager() {

    @Override
    protected PluginLoader createPluginLoader() {
        // carrega apenas plug-ins jar
        return new JarPluginLoader(this);
    }

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        // lê o descritor do plugin do manifesto contido no jar
        return new ManifestPluginDescriptorFinder();
    }

}; 
```

Portanto, é muito fácil adicionar novas estratégias para localizador de descritor de plugin, carregador de plugin e repositório de plugin.

## Modo de desenvolvimento

O framework pode ser executado em dois modos: **DESENVOLVIMENTO** e **IMPLEMENTAÇÃO** .

O modo DEPLOYMENT (padrão) é o fluxo de trabalho padrão para a criação de plug-ins: crie um novo módulo Maven para cada plug-in, codificando o plug-in (declara novos pontos de extensão e / ou adiciona novas extensões), empacote o plug-in em um arquivo zip, implante o zip arquivo para a pasta de plug-ins. Essas operações são demoradas e, por isso, introduzimos o modo de tempo de execução DEVELOPMENT.

A principal vantagem do modo de tempo de execução DEVELOPMENT para um desenvolvedor de plug-ins é que ele / ela não é obrigado a empacotar e implementar os plug-ins. No modo DEVELOPMENT você pode desenvolver plugins de forma simples e rápida.

Vamos descrever como o modo runtime DEVELOPMENT funciona.

Primeiro, você pode alterar o modo de tempo de execução usando a propriedade de sistema “archbase.mode” ou substituindo `DefaultArchbasePluginManager.getRuntimeMode()`.

Por exemplo, eu executo o demo  no eclipse no modo DEVELOPMENT adicionando apenas `"-Darchbase.mode=development"`ao iniciador demo.

Você pode recuperar o modo de tempo de execução atual usando `ArchbasePluginManager.getRuntimeMode()`ou em sua implementação de plug-in com `getWrapper().getRuntimeMode()`.

O DefaultArchbasePluginManager determina automaticamente o modo de tempo de execução correto e para o modo DEVELOPMENT substitui alguns componentes (pluginsDirectory é **”../plugins”** , **PropertiesPluginDescriptorFinder** como PluginDescriptorFinder, **DevelopmentPluginClasspath** como PluginClassPath).

Outra vantagem do modo de tempo de execução DEVELOPMENT é que você pode executar algumas linhas de código apenas neste modo (por exemplo, mais mensagens de depuração).

**NOTA:** Se você usar o Eclipse, certifique-se de que o processamento de anotações esteja ativado pelo menos para todos os projetos que registram objetos usando anotações. Nas propriedades do seu novo projeto, vá para **Compilador Java> Processamento de anotação** Marque **“Ativar configurações específicas do projeto”** e certifique-se de que **“Ativar processamento de anotação”** esteja marcado.

Se você usar o Maven como gerenciador de compilação, após cada modificação de dependência em seu plug-in (módulo Maven), você deve executar **Maven> Atualizar projeto ...**

## Desativar plugins

Em teoria, é uma relação **1: N** entre um ponto de extensão e as extensões para este ponto de extensão.

Isso funciona bem, exceto quando você desenvolve vários plug-ins para este ponto de extensão como opções diferentes para seus clientes decidirem qual deles usar.

Nesta situação, você deseja a possibilidade de desativar todas as extensões, exceto uma.

Por exemplo, eu tenho um ponto de extensão para envio de email (interface EmailSender) com duas extensões: uma baseada em Sendgrid e outra baseada em Amazon Simple Email Service.

A primeira extensão está localizada no Plugin1 e a segunda extensão está localizada no Plugin2.

Desejo ir apenas com uma extensão (relação **1: 1** entre ponto de extensão e extensões) e para isso tenho duas opções:

1) desinstale o Plugin1 ou Plugin2 (remova a pasta pluginX.zip e o pluginX da pasta de plug-ins)  
2) desative o Plugin1 ou Plugin2

Para a opção dois, você deve criar um arquivo simples **enabled.txt** ou **disabled.txt** na pasta de plug-ins.

O conteúdo de **enabled.txt** é semelhante a:

```properties
##############################################
# - carregue apenas estes plugins
# - adicione um id de plugin em cada linha
# - coloque este arquivo na pasta de plug-ins
##############################################
bemvindo-plugin
```
O conteúdo de **disabled.txt** é semelhante a:

```properties
#############################################
# - carregue todos os plug-ins, exceto estes
# - adicione um id de plugin em cada linha
# - coloque este arquivo na pasta de plug-ins
##############################################
bemvindo-plugin
```
Todas as linhas de comentário (linha que começam com # caractere) são ignoradas.

Se um arquivo com enabled.txt existir, disabled.txt será ignorado. Consulte enabled.txt e disabled.txt da pasta demo.

## Extensões

### Sobre pontos de extensão

Para estender a funcionalidade de um aplicativo, ele deve definir um chamado ponto de extensão. Esta é uma interface ou classe abstrata, que define um comportamento específico para uma extensão.

O exemplo a seguir define um ponto de extensão para estender um `javax.swing.JMenuBar`com entradas de menu adicionais:

```java
import javax.swing.JMenuBar;
import br.com.archbase.plugin.manager.ExtensionPoint;

interface MainMenuExtensionPoint extends ExtensionPoint {

    void buildMenuBar(JMenuBar menuBar);

}

```

### Sobre extensões

Uma extensão é uma implementação concreta de um ponto de extensão.

O exemplo a seguir adiciona um menu com o título “Olá Mundo” à barra de menus implementando a `MainMenuExtensionPoint`interface:

```java
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import br.com.archbase.plugin.manager.Extension;

@Extension
public class MyMainMenuExtension implements MainMenuExtensionPoint {

    public void buildMenuBar(JMenuBar menuBar) {
        JMenu exampleMenu = new JMenu("Examplo");
        exampleMenu.add(new JMenuItem("Olá Mundo"));
        menuBar.add(exampleMenu);
    }

}
```

Uma extensão pode ser carregada do classpath do aplicativo (chamadas ***extensões do sistema*** ) ou pode ser fornecida por um plugin.

Por favor, observe a `@Extension`anotação. Esta anotação marca a classe como uma extensão carregável para o framework. Todas as classes marcadas com a anotação `@Extension` são publicadas automaticamente em tempo de compilação no arquivo JAR criado - no `META-INF/extensions.idx`arquivo ou como serviço na `META-INF/services`pasta. Usando a `@Extension`anotação, você não precisa criar esses arquivos manualmente!

### Como as extensões são carregadas

De acordo com o exemplo acima, o aplicativo pode construir a barra de menus assim:

```java
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import br.com.archbase.plugin.manager.DefaultArchbasePluginManager;
import br.com.archbase.plugin.manager.ArchbasePluginManager;

public static void main(String[] args) {
    // Inicie o ambiente do plugin.
    // Isso deve ser feito uma vez durante o processo de inicialização do aplicativo.
    final ArchbasePluginManager pluginManager = new DefaultArchbasePluginManager();
    pluginManager.loadPlugins();
    pluginManager.startPlugins();

    // Abra o aplicativo Swing.
    java.awt.EventQueue.invokeLater(new Runnable() {

        public void run() {
            // Construir a barra de menu usando as extensões disponíveis.
            JMenuBar mainMenu = new JMenuBar();
            for (MainMenuExtensionPoint extension : pluginManager.getExtensions(MainMenuExtensionPoint.class)) {
                extension.buildMenuBar(mainMenu);
            }

            // Cria e mostra um diálogo com a barra de menu.
            JDialog dialog = new JDialog();
            dialog.setTitle("Exemplo de dialog");
            dialog.setSize(450,300);
            dialog.setJMenuBar(mainMenu);
            dialog.setVisible(true);
        }

    });

}
```

### Parâmetros de extensão adicionais

A anotação `@Extension` pode fornecer algumas opções adicionais, que podem ser úteis em certas situações.

#### Extensões de pedido

Vamos supor que temos várias extensões para a barra de menu e gostamos de ter controle, em que ordem as entradas do menu aparecem na barra de menu. Nesse caso, podemos usar o `ordinal`na anotação `@Extension`:

```java
@Extension(ordinal = 1)
public class FirstMainMenuExtension implements MainMenuExtensionPoint {

    public void buildMenuBar(JMenuBar menuBar) {
        JMenu menu = new JMenu("Primeiro");
        menu.add(new JMenuItem("Olá Mundo!"));
        menuBar.add(menu);
    }

}
```

Ao definir `@Extension(ordinal = 1)`o gerenciador de plug-ins, sempre carregará essa extensão primeiro. Portanto, a primeira entrada da barra de menu é sempre chamada de “Primeira”.

Ao definir `@Extension(ordinal = 2)`o gerenciador de plugins sempre carregará esta extensão após a primeira.

```java
@Extension(ordinal = 2)
public class SecondMainMenuExtension implements MainMenuExtensionPoint {

    public void buildMenuBar(JMenuBar menuBar) {
        JMenu menu = new JMenu("Segundo");
        menu.add(new JMenuItem("Olá Mundo"));
        menuBar.add(menu);
    }

}
```

#### Configure explicitamente um ponto de extensão

Em aplicativos do mundo real, é bastante comum criar classes abstratas para interfaces. Vamos supor a seguinte hierarquia de classes:

![enter image description here](https://imgur.com/download/5agQqNd/Diagrama+1)


Nesse caso, a classe de extensão ( `Plugin1MainMenuExtension`) **não** é **derivada diretamente** da interface `br.com.archbase.plugin.manager.ExtensionPoint`. Em vez disso, o aplicativo estende essa interface com sua própria interface`BaseExtensionPoint` para adicionar alguns métodos adicionais. Além disso, o aplicativo fornece uma classe abstrata `MainMenuAdapter` , que é finalmente estendida pelo `Plugin1MainMenuExtension`.

Você pode encontrar uma abordagem semelhante, por exemplo, na [`java.awt.event.WindowListener`](https://docs.oracle.com/javase/7/docs/api/java/awt/event/WindowListener.html)interface e na [`java.awt.event.WindowAdapter`](https://docs.oracle.com/javase/7/docs/api/java/awt/event/WindowAdapter.html)classe abstrata .

Neste cenário, é necessário registrar explicitamente o ponto de extensão na anotação `@Extension`:

```java
@Extension(points = {MainMenuExtensionPoint.class})
public class Plugin1MainMenuExtension extends MainMenuAdapter {

    public void buildMenuBar(JMenuBar menuBar) {
        // alguma implementação ...
    }

}
```

Caso contrário, o framework pode não ser capaz de detectar automaticamente os pontos de extensão corretos para a extensão em tempo de compilação .

#### Configure explicitamente as dependências do plugin

Os plug-ins podem ter uma dependência **opcional** uns dos outros . Isso pode levar a uma situação em que uma determinada extensão depende de um plugin, que não está disponível em tempo de execução do aplicativo. Vamos supor a seguinte hierarquia de classes:

![enter image description here](https://imgur.com/download/8d799cm/Diagrama2)

Este cenário descreve um aplicativo que fornece um plug-in para gerenciamento de contatos ( `ContactosPlugin`) e outro plug-in para gerenciamento de calendário ( `CalendarPlugin`). Ambos os plug-ins fornecem um formulário que permite ao usuário editar contatos / entradas de calendário. Esses formulários podem ser estendidos com pontos de extensão:

-   O formulário de contato mostra um painel com entradas de calendário atribuídas (via `CalendarContatosFormExtension`), caso o plugin de calendário esteja disponível em tempo de execução.
-   O formulário do calendário mostra um painel com entradas de contato atribuídas (via `ContatosCalendarFormExtension`), caso o plugin de contatos esteja disponível em tempo de execução.

Para fazer essas dependências circulares funcionarem, as extensões no `br.com.archbase.demo.plugins.contatos.addons`pacote são fornecidas como um arquivo JAR separado, que é empacotado na `lib`pasta do plug-in de contatos.

Ambos os plug-ins também precisam funcionar de forma independente. Por exemplo, o usuário pode não precisar de gerenciamento de calendário em seu aplicativo. Nesse caso, ele pode desativar / remover o plug-in de calendário totalmente e o plug-in de contatos ainda deve funcionar. Neste cenário específico, ambos os plug-ins devem ter uma *dependência* **opcional** um do outro. O gerenciador de plug-ins ainda precisa carregar o plug-in de contatos, mesmo se o plug-in de calendário não estiver habilitado / disponível em tempo de execução.

Mas essas dependências opcionais podem levar à situação de que uma certa extensão depende de um plugin não existente. Para evitar erros de carregamento de classe neste caso particular, você pode definir os plug-ins, que são necessários para carregar uma determinada extensão através da anotação `@Extension`:

```java
@Extension(plugins = {ContatosPlugin.ID, CalendarioPlugin.ID})
public class CalendarContatosFormExtension implements ContatosFormExtension {

    public JPanel getPanel() {
        // alguma implementação ...
    }
    public void load(ContatosEntry entry) {
        // alguma implementação ...
    }
    public void load(ContatosEntry save) {
        // alguma implementação ...
    }

}
```

```java
@Extension(plugins = {ContatosPlugin.ID, CalendarioPlugin.ID})
public class ContatosCalendarioFormExtension implements CalendarioFormExtension {

    public JPanel getPanel() {
        // alguma implementação ...
    }

    public void load(CalendarioEntry entry) {
        // alguma implementação ...
    }

    public void load(CalendarioEntry save) {
        // alguma implementação ...
    }

}
```

Nesse caso, o gerenciador de plug-ins só carregará essas extensões se todos os plug-ins necessários estiverem disponíveis / habilitados no tempo de execução.

**Observe:** Este recurso só é necessário se você usar plug-ins com *dependência* **opcional** uns dos outros. Nesse caso, você deve adicionar a [`asm`biblioteca](https://asm.ow2.io/) ao classpath de seu aplicativo.


## Instanciação de extensão

Para criar instâncias de extensões, o framework usa uma ***ExtensionFactory***. Por padrão, usamos `DefaultArchbaseExtensionFactory`como implementação de `ExtensionFactory`.

Você pode alterar a implementação padrão com:

```java
new DefaultArchbasePluginManager() {
    
    @Override
    protected ExtensionFactory createExtensionFactory() {
        return MinhaExtensionFactory();
    }

};
```

`DefaultArchbaseExtensionFactory`usa o [método Class # newInstance ()](https://docs.oracle.com/javase/7/docs/api/java/lang/Class.html#newInstance()) para criar a instância de extensão.

Uma instância de extensão é criada sob demanda, quando `plugin.getExtensions(MinhaExtensionPoint.class)`é chamada. Por padrão, se você ligar `plugin.getExtensions(MinhaExtensionPoint.class)`duas vezes:

```java
plugin.getExtensions(MinhaExtensionPoint.class);
plugin.getExtensions(MinhaExtensionPoint.class);

```

então, para cada chamada, uma nova instância da extensão é criada.

Se você deseja retornar a mesma instância de extensão (singleton), você precisa usar ***SingletonExtensionFactory***

```java
new DefaultArchbasePluginManager() {
    
    @Override
    protected ExtensionFactory createExtensionFactory() {
        return SingletonExtensionFactory();
    }

};
```

## Extensão do sistema

Uma extensão também pode ser definida diretamente no jar do aplicativo (ou seja, você não é obrigado a colocar a extensão em um plug-in - você pode ver essa extensão como padrão ou _extensão do sistema_ ). 

Isso é ótimo para iniciar a fase de aplicação. Neste cenário, você tem uma estrutura de plug-in minimalista com um carregador de classes (o carregador de classes do aplicativo), semelhante ao Java [ServiceLoader,](https://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html) mas com os seguintes benefícios:

-   não há necessidade de escrever arquivos de configuração do provedor no diretório de recursos `META-INF/services`, você está usando a elegante anotação `@Extension` do framework;
-   a qualquer momento, você pode alternar para o mecanismo de carregador de classes múltiplas sem alterações de código em seu aplicativo.

O código presente na `Boot`classe do aplicativo demo é funcional, mas você pode usar um código mais minimalista, ignorando `pluginManager.loadPlugins()`e `pluginManager.startPlugins()`.

```java
public static void main(String[] args) {
    ArchbasePluginManager pluginManager = new DefaultArchbasePluginManager();
    pluginManager.loadPlugins();
    pluginManager.startPlugins();
    List<Saudacao> saudacoes = pluginManager.getExtensions(Saudacao.class);
    for (Saudacao saudacao : saudacoes) {
        System.out.println(">>> " + saudacao.getSaudacao());
    }
}
```
O código acima pode ser escrito:
```java
public static void main(String[] args) {
    ArchbasePluginManager pluginManager = new DefaultArchbasePluginManager();
    List<Saudacao> saudacoes = pluginManager.getExtensions(Saudacao.class);
    for (Saudacao saudacao : saudacoes) {
        System.out.println(">>> " + saudacao.getSaudacao());
    }
}
```


## ServiceLoader

O framework pode ler `META-INF/services`(mecanismo do provedor de serviços Java) como extensões, portanto, se você tiver um aplicativo modular baseado em `java.util.ServiceLoader`classe, pode substituir totalmente as `ServiceLoader.load()` chamadas de seu aplicativo `ArchbasePluginManager.getExtensions()`e migrar sem problemas de ServiceLoader para o framework.

Além disso, você tem a possibilidade de alterar o `ExtensionStorage`usado em `ExtensionAnnotationProcessor`. Por padrão, usamos o formato com `META-INF/extensions.idx`:

```java
br.com.archbase.plugin.demo.OlaSaudacao;
br.com.archbase.plugin.demo.SaudacaoWazzup;
```

mas você pode usar um local e formato mais padrão,, `META-INF/services/<extension-point>`usado pelo Java Service Provider (consulte `java.util.ServiceLoader`Recursos) por meio da `ServiceProviderExtensionStorage`implementação. Neste caso, o formato de `META-INF/services/br.com.archbase.plugin.demo.Saudacao`é:

```
# Generated by archbase
br.com.archbase.plugin.demo.OlaSaudacao
br.com.archbase.plugin.demo.SaudacaoWazzup # archbase extension

```

onde a entrada`br.com.archbase.plugin.demo.OlaSaudacao` é legada (não é gerada pelo framework), mas é vista como uma extensão de `Saudacao` (em tempo de execução).

Você pode conectar sua implementação `ExtensionStorage` personalizada `ExtensionAnnotationProcessor`em dois modos possíveis:

-   defina a opção do processador de anotações com a tecla `archbase.storageClassName`
-   defina a propriedade do sistema com a chave `archbase.storageClassName`

Por exemplo, se eu quiser usar `ServiceProviderExtensionStorage`, o valor da chave `archbase.storageClassName` deve ser `br.com.archbase.plugin.manager.processor.ServiceProviderExtensionStorage`

**NOTA:**  `ServiceLoaderExtensionFinder` a classe que pesquisa extensões armazenadas na `META-INF/services`pasta não é adicionada / habilitada por padrão. Para fazer isso, substitua `createExtensionFinder`de `DefaultPluginManager`:

```java
final ArchbasePluginManager pluginManager = new DefaultArchbasePluginManager() {

    protected ExtensionFinder createExtensionFinder() {
        DefaultExtensionFinder extensionFinder = (DefaultExtensionFinder) super.createExtensionFinder();
        extensionFinder.addServiceProviderExtensionFinder();

        return extensionFinder;
    }

};
```

## Assíncrono

### Carregar e iniciar a sincronização de plug-ins

```java
// crie o gerenciador de plugins
final ArchbasePluginManager pluginManager = new DefaultArchbasePluginManager();

// carregue os plugins
pluginManager.loadPlugins();

// iniciar (ativo / resolvido) os plug-ins
pluginManager.startPlugins();

// recupera as extensões para o ponto de extensão de saudação
List<Saudacao> saudacoes = pluginManager.getExtensions(Saudacao.class);
```

### Carregar e iniciar plugins assíncronos

```java
// cria o gerenciador de plugins
final AsyncArchbasePluginManager pluginManager = new DefaultAsyncArchbasePluginManager();

// carregue os plugins
CompletionStage<Void> stage = pluginManager.loadPluginsAsync();
stage.thenRun(() -> System.out.println("Plugins carregados")); // optional

// iniciar (ativo / resolvido) os plug-ins
stage.thenCompose(v -> pluginManager.startPluginsAsync());
stage.thenRun(() -> System.out.println("Plugins iniciados")); // optional

// bloquear e esperar que o futuro seja concluído (não é a melhor abordagem em aplicativos reais)
stage.toCompletableFuture().get();

// recupera as extensões para o ponto de extensão de saudação
List<Saudacao> saudacoes = pluginManager.getExtensions(Saudacao.class);
```

## Solução de problemas

Abaixo estão listados alguns problemas que podem ocorrer ao tentar usar o framework e sugestões para resolvê-los.

-   **Nenhuma extensão encontrada**

Veja se você tem um arquivo `extensions.idx`em cada plugin.  
Se o arquivo `extensions.idx`não existir, provavelmente há algo errado com a etapa de processamento da anotação (habilite o processamento da anotação em seu IDE ou em seu script Maven).  
Se o arquivo `extensions.idx`existir e não estiver vazio, certifique-se de ter um problema com o carregador de classes (você tem o mesmo ponto de extensão em dois carregadores de classes diferentes), nesta situação, você deve remover algumas bibliotecas (provavelmente o jar da API) do plug-in.

Se o problema persistir ou você quiser encontrar mais informações relacionadas ao processo de descoberta de extensões (por exemplo, quais interfaces / classes são carregadas por cada plugin, quais classes não são reconhecidas como extensões para um ponto de extensão), então você deve colocar `TRACE`o logger para `PluginClassLoader`e `AbstractExtensionFinder`.