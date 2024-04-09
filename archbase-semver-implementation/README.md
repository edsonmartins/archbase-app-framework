Archbase SemVer 
===============

Archbase SemVer é uma implementação Java da Especificação de Controle de Versão Semântica
(http://semver.org/).


### Criando versões ###
A classe principal da biblioteca é `Version`, que implementa o
Padrão de design de facade. Por design, a classe `Version` se torna imutável 
tornando seus construtores package-private, de modo que não possa ser subclassificado ou
instanciado diretamente. Em vez de construtores públicos, a classe `Version`
fornece alguns _métodos de fábrica estáticos_.

Um dos métodos é o `Version.valueOf`.

~~~ java
import br.com.archbase.semver.implementation.Version;

Version v = Version.valueOf("1.0.0-rc.1+build.1");

int major = v.getMajorVersion(); // 1
int minor = v.getMinorVersion(); // 0
int patch = v.getPatchVersion(); // 0

String normal     = v.getNormalVersion();     // "1.0.0"
String preRelease = v.getPreReleaseVersion(); // "rc.1"
String build      = v.getBuildMetadata();     // "build.1"

String str = v.toString(); // "1.0.0-rc.1+build.1"
~~~

O outro método de fábrica estático é `Version.forIntegers`, que também é
sobrecarregado para permitir menos argumentos.

~~~ java
import br.com.archbase.semver.implementation.Version;

Version v1 = Version.forIntegers(1);
Version v2 = Version.forIntegers(1, 2);
Version v3 = Version.forIntegers(1, 2, 3);
~~~

Outra forma de criar uma `Versão` é usar uma classe _builder_` Version.Builder`.
~~~ java
import br.com.archbase.semver.implementation.Version;

Version.Builder builder = new Version.Builder("1.0.0");
builder.setPreReleaseVersion("rc.1");
builder.setBuildMetadata("build.1");

Version v = builder.build();

int major = v.getMajorVersion(); // 1
int minor = v.getMinorVersion(); // 0
int patch = v.getPatchVersion(); // 0

String normal     = v.getNormalVersion();     // "1.0.0"
String preRelease = v.getPreReleaseVersion(); // "rc.1"
String build      = v.getBuildMetadata();     // "build.1"

String str = v.toString(); // "1.0.0-rc.1+build.1"
~~~

### Versões incrementais ###
Porque a classe `Version` é imutável, os _incrementors_ retornam uma nova
instância de `Version` em vez de modificar o dado. Cada um dos normais
incrementadores de versão tem um método sobrecarregado que leva uma versão de pré-lançamento
como um argumento.

~~~ java
import br.com.archbase.semver.implementation.Version;

Version v1 = Version.valueOf("1.2.3");

// Incrementando a versão principal
Version v2 = v1.incrementMajorVersion();        // "2.0.0"
Version v2 = v1.incrementMajorVersion("alpha"); // "2.0.0-alpha"

// Incrementando a versão secundária
Version v3 = v1.incrementMinorVersion();        // "1.3.0"
Version v3 = v1.incrementMinorVersion("alpha"); // "1.3.0-alpha"

// Incrementando a versão do patch
Version v4 = v1.incrementPatchVersion();        // "1.2.4"
Version v4 = v1.incrementPatchVersion("alpha"); // "1.2.4-alpha"

// A versão original ainda é a mesma
String str = v1.toString(); // "1.2.3"
~~~

Também existem métodos incrementadores para a versão de pré-lançamento e o build
metadados.

~~~ java
import br.com.archbase.semver.implementation.Version;

// Incrementando a versão de pré-lançamento
Version v1 = Version.valueOf("1.2.3-rc");        // considered as "rc.0"
Version v2 = v1.incrementPreReleaseVersion();    // "1.2.3-rc.1"
Version v3 = v2.incrementPreReleaseVersion();    // "1.2.3-rc.2"

// Incrementando os metadados de construção
Version v1 = Version.valueOf("1.2.3-rc+build");  // considered as "build.0"
Version v2 = v1.incrementBuildMetadata();        // "1.2.3-rc+build.1"
Version v3 = v2.incrementBuildMetadata();        // "1.2.3-rc+build.2"
~~~

Ao incrementar as versões normais ou de pré-lançamento, os metadados de construção são
sempre caiu.

~~~ java
import br.com.archbase.semver.implementation.Version;

Version v1 = Version.valueOf("1.2.3-beta+build");

// Incrementando a versão normal
Version v2 = v1.incrementMajorVersion();        // "2.0.0"
Version v2 = v1.incrementMajorVersion("alpha"); // "2.0.0-alpha"

Version v3 = v1.incrementMinorVersion();        // "1.3.0"
Version v3 = v1.incrementMinorVersion("alpha"); // "1.3.0-alpha"

Version v4 = v1.incrementPatchVersion();        // "1.2.4"
Version v4 = v1.incrementPatchVersion("alpha"); // "1.2.4-alpha"

// Incrementando a versão de pré-lançamento
Version v2 = v1.incrementPreReleaseVersion();   // "1.2.3-beta.1"
~~~


### Comparando versões ###
Comparar versões com o framework é fácil. A classe `Version` implementa o
Interface `Comparable`, ele também substitui o método` Object.equals` e fornece
mais alguns métodos para comparação conveniente.

~~~ java
import br.com.archbase.semver.implementation.Version;

Version v1 = Version.valueOf("1.0.0-rc.1+build.1");
Version v2 = Version.valueOf("1.3.7+build.2.b8f12d7");

int result = v1.compareTo(v2);  // < 0
boolean result = v1.equals(v2); // false

boolean result = v1.greaterThan(v2);           // false
boolean result = v1.greaterThanOrEqualTo(v2);  // false
boolean result = v1.lessThan(v2);              // true
boolean result = v1.lessThanOrEqualTo(v2);     // true
~~~

Ao determinar a precedência da versão, os metadados de construção são ignorados (SemVer p.10).
~~~ java
import br.com.archbase.semver.implementation.Version;

Version v1 = Version.valueOf("1.0.0+build.1");
Version v2 = Version.valueOf("1.0.0+build.2");

int result = v1.compareTo(v2);  // = 0
boolean result = v1.equals(v2); // true
~~~

Às vezes, no entanto, você pode querer comparar as versões com os metadados de construção
em mente. Para tais casos, o framework fornece um _comparador_ `Versão.BUILD_AWARE_ORDER`
e um método de conveniência `Version.compareWithBuildsTo`.

~~~ java
import br.com.archbase.semver.implementation.Version;

Version v1 = Version.valueOf("1.0.0+build.1");
Version v2 = Version.valueOf("1.0.0+build.2");

int result = Version.BUILD_AWARE_ORDER.compare(v1, v2);  // < 0

int result     = v1.compareTo(v2);            // = 0
boolean result = v1.equals(v2);               // true
int result     = v1.compareWithBuildsTo(v2);  // < 0
~~~


API Expressions (intervalos)
----------------------------
O framework suporta a API SemVer Expressions, que é implementada como
DSL interno e DSL externo. O ponto de entrada para a API é
os métodos `Version.satisfies`.

### DSL interno ###
O DSL interno é implementado pela classe `CompositeExpression` usando fluent
interface. Por conveniência, ele também fornece a classe `Helper` com estática
métodos auxiliares.

~~~ java
import br.com.archbase.semver.implementation.Version;
import static br.com.archbase.semver.implementation.expr.CompositeExpression.Helper.*;

Version v = Version.valueOf("1.0.0-beta");
boolean result = v.satisfies(gte("1.0.0").and(lt("2.0.0")));  // false
~~~

### DSL externo ###
A gramática BNF para o DSL externo pode ser encontrada no correspondente

~~~ java
import br.com.archbase.semver.implementation.Version;

Version v = Version.valueOf("1.0.0-beta");
boolean result = v.satisfies(">=1.0.0 & <2.0.0");  // false
~~~

Abaixo estão exemplos de alguns casos de uso comuns, bem como açúcar sintático e alguns
outras capacidades interessantes do DSL externo SemVer Expressions.
* Intervalos de curinga (`*`|`X`|`x`) - `1.*` que é equivalente a `>=1.0.0 & <2.0.0`
* Intervalos de Til (`~`) - `~1.5` que é equivalente a `>=1.5.0 & <1.6.0`
* Intervalos de hifen (`-`) - `1.0-2.0` que é equivalente a `>=1.0.0 & <=2.0.0`
* Faixas circulares (`^`) - `^0.2.3` que é equivalente a `>=0.2.3 & <0.3.0`
* Intervalos de versão parcial - `1` que é equivalente a `1.X` or `>=1.0.0 & <2.0.0`
* Operador de negação - `!(1.x)` que é equivalente a `<1.0.0 & >=2.0.0`
* Expressões entre parênteses - `~1.3 | (1.4.* & !=1.4.5) | ~2`


Manipulação de exceção
------------------
Existem dois tipos de erros que podem ocorrer durante o uso do Archbase SemVer
* `IllegalArgumentException` é lançada quando o valor passado é` NULL` ou vazio
  se um método aceita o argumento `string` ou um inteiro negativo se um método aceita
  argumentos `int`.
* `ParseException` é lançada por métodos que realizam a análise da versão SemVer
  strings ou expressões SemVer. Existem alguns subtipos de `ParseException`
  erro
  - `UnexpectedCharacterException` é lançada quando uma string de versão SemVer contém
    um personagem inesperado ou ilegal
  - `LexerException` é lançada quando uma expressão SemVer contém um caractere ilegal
  - `UnexpectedTokenException` é lançada quando um token inesperado é encontrado
    durante a análise da expressão SemVer
