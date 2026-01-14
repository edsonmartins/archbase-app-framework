Archabase é um framework para aplicações java e foi desenvolvido com a intenção de facilitar o 

desenvolvimento de projetos. Os módulos desenvolvidos para ajudar no trabalho de 

desenvolvimento permitem tanto o uso para aplicações simples com modelos anêmicos, CRUD's 

e assim como aplicações mais complexas usando os conceitos de DDD (Domain Driven Design) de 

forma ágil e bem estruturada. Módulos do framework Conceituação 

Antes de falarmos de cada módulo desenvolvido e sua reponsabilidade dentro do contexto do 

framework vamos conceituar alguns pontos importantes que fazem parte da vida dos 

desenvolvedores e quem rendem muitas dúvidas no dia a dia e os quais procuramos desenvolver 

soluções para ajudar a resolver. 

# DDD 

Domain-driven Design ( DDD ) ou desenvolvimento dirigido ao domínio é um conjunto de padrões 

e princípios usados para ajudar no desenvolvimento de aplicações que vão influenciar na melhor 

compreensão e uso do projeto. É visto como uma nova maneira de pensar sobre a metodologia 

de desenvolvimento. 

O desenvolvimento de um software é feito com base nos objetivos do cliente. Porém, na hora de 

escrever o código e durante todo o projeto, existe o risco de não atingir a expectativa. Diversos 

fatores podem ocasionar esse problema, entre eles as falhas de comunicação, a pressa em 

entregar o projeto, entre outros. 

Nenhum projeto deve ser iniciado até que todas as necessidades do domínio estejam definidas, 

alinhadas e com suas devidas soluções apresentadas. O Domain-driven Design ( DDD ) é uma 

metodologia que visa garantir que as aplicações serão construídas para atender as necessidades 

do domínio. 

A ideia básica do DDD está centrada no conhecimento do problema para o qual o software foi 

proposto. Na prática, trata-se de uma coleção de padrões e princípios de design que buscam 

auxiliar o desenvolvedor na tarefa de construir aplicações que reflitam um entendimento do 

negócio. É a construção do software a partir da modelagem do domínio real como classes de 

domínio que, por sua vez, possuirão relacionamentos entre elas. A fundação de DDD está no conceito de linguagem onipresente (ubiquitous language , no 

original). Não está em determinar o que é uma entidade, objeto de valor, agregado, serviço ou 

repositório. 

# Principais conceitos do DDD 

Ubiquitous Language : É uma linguagem que deve ser comum entre o "Cliente" e o 

"Desenvolvedor". Seu código deve expressar as terminologias do do cliente, para que os 

desenvolvedores e o cliente falem a mesma lingua. 

Bounded Context : Criar delimitações específicas entre cada parte do sistema. Uma classe 

Produto não tem o mesmo sentido num contexto de "Suporte" do que num contexto de 

"Venda". Essa "Separação" de sentido entre contextos DEVE SER CLARA! 

Core Domain : Foca no principal problema à ser resolvido. Detalhes menores, podem ser 

delegados ou integrados com soluções de terceiros que já estão prontas. O importante é 

FOCAR SEMPRE NO NEGÓCIO. 

# DDD Is Not Only About Writing Code 

Os desenvolvedores devem estar em constante contato com o negócio e evoluírem 

constatamente no entendimento sobre o domínio. Todos os desenvolvedores devem estar 

cientes do ponto de vista do Domain Expert e do que estão cumprindo. Arquitetura da Aplicação 

Arquitetura DDD pode ser visualizada como uma Cebola (veja Onion Architecture), é uma 

arquitetura composta por Camadas. 

A regra é que cada "Bloco" pode ter conhecimento dos outros blocos da mesma camada, e das 

camadas inferiores. Mas nunca das SUPERIORES. 

Obs: O banco de dados estará sempre na camada Repository , se você vai utilizar uma ORM ou 

efetuar as query diretamente, não importa! Mas as interações com o banco ocorrerão por lá. 

E porque essa separação é importante? Por causa da separação de preocupações. 

As regras de negócios estarão (se não toda, a maioria esmagadora) nos objetos centrais, em 

especial, as Entity e Value Object . Esses objetos não devem ter CONHECIMENTO NENHUM, 

de como vão ser persistidos, construídos ou mapeados no banco de dados. Tudo que eles devem 

saber é o Domínio que representam. 

Lembre: Quanto mais limpo e claro você manter seu modelo, mais fácil será entendê-lo mais 

tarde. 

A falta de separação de responsabilidades, é a principal razão que bases de códigos se tornam 

gigantes e confusas. 

# Boas práticas de modelagem Como melhores práticas, tente sempre começar o código pelo Core Domain e testes´ unitários. 

Deixe a camada de apresentação e de banco de dados por último .

Lembre: DDD == Foco no domínio .

# Seja pragmático! 

Ter um código com 100% Coverage em testes é algo lindo, mas custoso e muitas vezes não 

necessário. Isso varia de projeto para projeto, mas na maioria das "Enterprise-level Application", o 

valor que o teste agrega ao projeto cresce junto com o esforço para criá-lo e mantê-lo, dessa 

forma, quando o valor agregado não justifica mais o esforço necessário, é a hora de parar de criar 

o Coverage. 

Na prática, queremos ter nossa camada de negócios (primeira camada na arquitetura acima) com 

100% Code Coverage. Quanto as camadas superiores, ao invés de focar nos testes unitário, o 

ideal é aplicarmos testes de Integração, para garantir que as peças estão se "encaixando". Por que utilizar DDD? Vale a pena usar DDD? 

Claro que a resposta é: depende! 

DDD não é uma bala de prata. Para um aplicativo CRUD simples ou com pouca lógica de 

negócios, pode ser um exagero. Assim que seu aplicativo se tornar grande, vale a pena considerar 

o DDD . Apontando mais uma vez para os principais benefícios que você pode obter usando DDD :

melhor expressão da lógica de negócios em objetos de domínio por meio de métodos 

significativos. Os 

objetos de domínio delimitam os limites da transação manipulando apenas seus internos, o que 

simplifica a implementação da lógica de negócios e não aumenta seu gráfico de objetos de 

domínio conectados 

estrutura de pacotes muito simples, 

melhor separação entre domínio e mecanismo de persistência 

# Quando não utilizar DDD: Como implementar um modelo efetivo de Domain-

# driven design? 

Um modelo efetivo de Domain driven design deve ter riqueza de conhecimento, um código 

expressivo, com regras de negócio e processos bem definidos. Além disto, expressar seu 

conhecimentos e resolver os problemas do domínio. Para que isso seja possível, é necessário 

seguir algumas etapas: 

# Vincular o modelo com a implementação 

Essa ação deve ser realizada ainda no início do modelo e ser mantido até o final. O objetivo é que 

a implementação seja reflexo do modelo. 

# Cultivar a linguagem baseada no modelo 

Os desenvolvedores e os especialistas em domínio devem entender sobre os termos uns dos 

outros. O objetivo é organizar a comunicação de forma estruturada, consistente e alinhada com o 

modelo. Evite ambiguidades. 

# Desenvolver um modelo rico em conhecimento 

O comportamento e os dados dos objetos são associados, mas o modelo não pode ser formado 

por uma estrutura de dados apenas. O modelo deve capturar o conhecimento do domínio para 

resolver os problemas que foram encontrados no caminho. 

# Destilar o modelo 

Conceitos importantes devem ser adicionados ao modelo. Da mesma forma, aqueles que não são 

relevantes devem ser removidos. Com isso, sempre que houver uma interação o modelo 

agregará mais valor. 

# Fazer brainstorming e experimentação 

São as conversas, reuniões, trocas de informação e interação que vão enriquecer o modelo. 

Brainstorming e diagramas ajudam no desenvolvimento das soluções e identificação das 

melhores caminhos a serem aplicados. 

Em DDD a modelagem e a implementação do código andam juntas, diferentemente de outro 

projetos, onde essas etapas são feitas de forma separada e quase não há comunicação entre 

seus envolvidos. Com isso, o DDD apresenta algumas vantagens. Veja quais são! 

# As vantagens do design controlado por domínio Torna a comunicação entre as equipes mais fácil 

Em ambientes de desenvolvimento é natural que termos mais técnicos sejam usados para 

discutir os projetos. Na modelagem DDD, a participação de profissionais de outras áreas é mais 

frequente, por isso a comunicação entre esses profissionais deve ser simples. 

# Aumenta a flexibilidade dos projetos 

O modelo DDD é baseado no conceito orientado por análises e design, por isso praticamente 

tudo dentro do domínio é baseado em um objeto. Isso faz com que o projeto seja mais modular, 

permitindo a alteração do sistema e a inclusão de novos componentes, de forma regular e 

contínua. 

# Enfatiza a Domain Over Interface 

A prática de construir em torno de conceitos e do que é mais aconselhável dentro de um projeto, 

contribui para que aplicativos mais adequados ao projeto em si. Isso quer dizer que o projeto 

pode ser mais adequado aos público do domínio. 

A implementação de um modelo efetivo de Domain Driven Design pode ser realizada por 

profissionais de dentro da própria empresa, desde que sejam especialistas ou já tenham 

realizado esse tipo de implementação em outros projetos. Modelos anêmicos 

# Modelo orientado a domínio vs. modelo anêmico. Como 

# eles se diferem? 

Comparação de um aplicativo em camadas comum e um aplicativo com arquitetura DDD. 

# Modelo anêmico e serviços volumosos 

O modelo de domínio anêmico nada mais é do que entidades representadas por classes 

contendo apenas dados e conexões com outras entidades. Essas classes carecem da lógica de 

negócios, que geralmente é colocada em serviços, utilitários, auxiliares etc. Esse design (três 

camadas mostradas no lado esquerdo da imagem) é a maneira natural com que modelamos as 

classes responsáveis por casos de negócios. 

Quando implementamos uma funcionalidade específica, primeiro falamos sobre as classes que 

serão persistidas. Nós os chamamos de entidades. Eles são representados como a letra E no 

gráfico da imagem acima. Essas entidades são, na verdade, uma representação orientada a 

objetos para as tabelas do banco de dados. Como resultado, não implementamos nenhuma 

lógica de negócios dentro deles. Sua única função é ser mapeada por algum ORM para seus 

equivalentes de banco de dados. Quando nossas entidades mapeadas estiverem prontas, a próxima etapa é criar classes para lê-

las e gravá-las. Acabamos com a camada DAO (Data Access Objects). Normalmente, cada uma de 

nossas entidades representa um caso de negócios separado, portanto, o número de classes DAO 

corresponde ao número de entidades. Eles não contêm nenhuma lógica de negócios. As classes 

DAO nada mais são do que ferramentas para recuperar e persistir entidades. Podemos usar uma 

estrutura existente para criá-los, ou seja. spring-data com hibernate embaixo. 

A última camada, acima do DAO, é a quintessência de nossa implementação - Serviços. Os 

serviços fazem uso do DAO para implementar a lógica de negócios para funcionalidades 

específicas. Independentemente do número de funcionalidades, um serviço típico sempre 

executa as seguintes operações: carrega entidades usando DAO, modifica seu estado de acordo 

com os requisitos e os persiste. Essa arquitetura foi descrita por Martin Fowler como uma série 

de Scripts de Transação. Quanto mais complicada a funcionalidade, maior o número de 

operações entre o carregamento e a persistência. Muitas vezes alguns serviços passam a fazer 

uso dos outros serviços, resultando em crescimento em toneladas de código, mas apenas nos 

serviços. Entidades ou DAO permanecem os mesmos, a menos que forneçamos novos campos 

para persistência. 

O DDD vem com uma abordagem totalmente diferente em termos de colocar o código em 

camadas. 

# Modelo rico e serviços limitados 

A arquitetura comum com o modelo Domain Driven Design é apresentada no lado direito da 

imagem. 

Observe uma camada de serviços que é muito mais fina do que seu equivalente em um modelo 

anêmico. O motivo é que a maior parte da lógica de negócios está incluída em agregados, 

entidades e objetos de valor. Nos serviços colocamos apenas operações trabalhando em muitas 

áreas do domínio e logicamente não se encaixando em nenhuma delas.Nos serviços colocamos 

apenas operações trabalhando em muitas áreas do domínio e logicamente não se encaixando em 

nenhuma delas. 

A camada de domínio possui mais tipos de objetos. Existem os Objetos de Valor, as Entidades e 

os objetos que os montam - os Agregados. Os agregados podem ser conectados apenas por 

identificadores. Eles não podem compartilhar quaisquer outros dados entre si. 

A última camada também é mais fina do que no modelo anterior. O número de repositórios 

corresponde ao número de agregados em DDD, portanto, o aplicativo com o modelo anêmico 

tem mais repositórios do que aquele com o modelo rico. 

# Mudanças mais fáceis e menos bugs? 

A aplicação da arquitetura Domain Driven Design oferece aos desenvolvedores alguns benefícios 

importantes. 

Graças à divisão de objetos nas Entidades e nos Objetos de Valor, podemos gerenciar com mais 

precisão os objetos recém-criados em nossa aplicação. Os agregados nos permitem encapsular 

partes de nosso domínio para que nossa API se torne mais simples e as alterações dentro dos 

agregados sejam mais fáceis de implementar (não precisamos analisar o impacto de nossas alterações em algumas outras partes do domínio porque ele não existe). 

Menos número de conexões entre os elementos do domínio reduz o risco de criar um bug 

durante o desenvolvimento e modificação do código. A representação de domínio estendida 

torna o código melhor refletindo como os especialistas em domínio descrevem as regras de 

negócios. Como resultado, muito mais fácil é concluir sobre a correção da solução e, se 

necessário, alterá-la e desenvolvê-la. 

# Contextos limitados 

“Um Contexto é o cenário em que uma palavra ou afirmação aparece e isso determina o seu 

significado”. 

Um contexto limitado é um limite explícito dentro do qual existe um modelo de domínio. O 

modelo de domínio expressa uma linguagem ubíqua como um modelo de software. 

Ao começar com a modelagem de software, os contextos limitados são conceituais e fazem parte 

do espaço do problema . Nesta fase, a tarefa é tentar encontrar limites reais de contextos 

específicos e, em seguida, visualizar quais são as relações entre esses contextos. 

Conforme o modelo começa a adquirir um significado e clareza mais profundos, os contextos 

limitados farão a transição para o espaço de solução , com o modelo de software sendo 

refletido no código-fonte do projeto. 

De uma perspectiva de tempo de execução, os contextos limitados representam limites lógicos, 

definidos por contratos dentro de artefatos de software onde o modelo é implementado. 

# Estrutura organizacional 

A inversão da Lei de Conway permite que a estrutura organizacional se alinhe com os contextos 

limitados. 

“Qualquer organização que projeta um sistema produzirá um projeto cuja estrutura é uma cópia da 

estrutura de comunicação da organização.” 

Portanto, há uma série de regras que devem ser seguidas: 

Definir limites explicitamente em termos de organização da equipe. 

Mantenha o modelo estritamente consistente dentro desses limites e não se distraia ou se 

confunda com questões externas. 

Idealmente, mantenha um modelo de subdomínio por contexto limitado. 

Deve haver uma única equipe designada para trabalhar em um contexto limitado. Também deve 

haver um repositório de código-fonte separado para cada Contexto limitado. É possível que uma 

equipe trabalhe em vários contextos limitados, mas várias equipes não devem trabalhar em um 

único contexto limitado. 

# Mapeamento de Contexto Um contexto limitado nunca vive inteiramente por conta própria. Informações de diferentes 

contextos serão eventualmente sincronizadas. É útil modelar essa interação explicitamente. O 

Domain-Driven Design nomeia algumas relações entre contextos, que orientam a maneira como 

eles interagem: 

parceria (dois contextos / equipes combinam esforços para construir interação) 

cliente-fornecedor (dois contextos / equipes no relacionamento upstream / downstream - o 

upstream pode ter sucesso independentemente dos contextos downstream) 

conformista (dois contextos / equipes no relacionamento upstream / downstream - o 

upstream não tem motivação para fornecer ao downstream, e o contexto downstream não 

se esforça na tradução) 

kernel compartilhado (explicitamente, compartilhando uma parte do modelo) 

caminhos separados (corte-os) 

camada anticorrupção (o contexto / equipe downstream constrói uma camada para evitar 

que o design upstream "vaze" para seus próprios modelos, transformando as interações) Design orientado a domínio tático 

O foco desta conceituação é o uso do framework e vamos nos concentrar mais na parte tática do 

DDD. 

DDD tático é um conjunto de padrões de design e blocos de construção que você pode usar para 

projetar sistemas orientados por domínio. Mesmo para projetos que não são orientados por 

domínio, você pode se beneficiar do uso de alguns dos padrões DDD táticos. 

Blocos de construção DDD em java 

Quando se trata de implementar blocos de construção de DDD, os desenvolvedores 

geralmente lutam para encontrar um bom equilíbrio entre pureza conceitual e pragmatismo 

técnico. Archbase ajuda a expressar expressar alguns dos conceitos de design tático de 

DDD no código Java e derivar os metadados para implementar persistência sem poluir o 

modelo de domínio com anotações por um lado e uma camada de mapeamento adicional 

no de outros. Em aplicativos Java, os blocos de construção do Domain-Driven Design podem ser 

implementados de várias maneiras. Essas maneiras geralmente fazem diferentes 

compensações ao separar o modelo de domínio real dos aspectos específicos da tecnologia. 

Muitos projetos Java erram por ainda anotar suas classes de modelo com, por exemplo, 

anotações JPA para facilitar a persistência, de forma que eles não tenham que manter um 

modelo de persistência separado. Archbase tem como foco principal tornar o modelo mais 

focado no DDD, apesar de permitir o uso de forma diferente. 

Outro aspecto é como tornar os blocos de construção DDD visíveis dentro do código. 

Freqüentemente, muitos deles podem ser identificados indiretamente, por exemplo, analisando 

se o tipo de domínio gerenciado por um repositório Spring Data deve ser um agregado por 

definição. No entanto, nesse caso específico, estamos contando com uma tecnologia de 

persistência específica para ser usada para derivar exatamente essa informação. Além disso, seria 

bom se pudéssemos raciocinar sobre a função de um tipo olhando para ele sem qualquer outro 

contexto. 

Comparado ao design orientado por domínio estratégico, o design tático é muito mais prático e 

mais próximo do código real. O design estratégico lida com todos abstratos, enquanto o design 

tático lida com classes e módulos. O objetivo do design tático é refinar o modelo de domínio até 

um estágio em que possa ser convertido em código funcional. 

O design é um processo iterativo e, portanto, faz sentido combinar o design estratégico e o tático. 

Você começa com o design estratégico, seguido pelo design tático. As maiores revelações e 

avanços de design de modelo de domínio provavelmente acontecerão durante o design tático e 

isso, por sua vez, pode afetar o design estratégico e, portanto, você repete o processo. 

Objetos de negócio ou de domínio, em DDD, não constituem camadas; ao contrário, eles residem 

todos na mesma camada, a camada de negócios (naturalmente), geralmente chamada de 

domain .

As camadas em DDD são: 

Interfaces: é a interface do sistema com o mundo exterior. Pode ser por exemplo uma 

interface gráfica com o usuário ou uma fachada de serviços. 

Application: contém a mecânica do aplicativo, direciona aos objetos de negócio as 

interações do usuário ou de outros sistemas. 

Domain: camada onde residem os objetos de negócio (Entities, Value Objects, Aggregations, 

Services, Factories, Repositories). 

Infrastructure: oferece suporte às demais camadas, oferecendo por exemplo mapeamento 

entre objetos de negócio e banco de dados e serviços de acesso a estes bancos de dados. 

# Como organizar a estrutura de pacotes da nossa aplicação 

No livro DDD canônico, Eric Evans menciona 4 camadas em aplicativos: Interface do usuário, 

aplicativo, domínio e infraestrutura. A infraestrutura tem uma função transversal de servir às 

outras três partes do sistema. Para fazer isso, unificamos conceitualmente a infraestrutura em 

uma unidade multifuncional que dá suporte a todas as outras camadas.      

> src /main /java
> !"" UserInterface

# Shared Kernel 

Se duas equipes desejam colaborar estreitamente e ter muitos cruzamentos em termos de lógica 

e conceitos de domínio, a sobrecarga de traduzir de um contexto limitado para outro pode ser 

muito grande. Portanto, eles podem decidir compartilhar essa parte do modelo, que é conhecida 

como kernel compartilhado. 

Kernel compartilhado apresenta risco em termos de uma equipe pode quebrar o código da outra 

equipe. Isso também requer que as duas equipes alinhem suas línguas ubíquas. É muito 

importante ter regras estritas de coordenação em torno do kernel compartilhado. Quaisquer 

mudanças a serem feitas no kernel compartilhado devem ser consentidas com as equipes de 

compartilhamento. Se a coordenação parece cara e pode não ser possível no futuro, pode ser 

melhor não colocar nada no kernel compartilhado. 

Vejamos agora os principais conceitos dentro de DDD tático: 

# Objetos de valor 

Um dos conceitos mais importantes no DDD tático é o objeto de valor . Um objeto de valor é um 

objeto cujo valor é importante. Isso significa que dois objetos de valor com exatamente o mesmo 

valor podem ser considerados o mesmo objeto de valor e, portanto, são intercambiáveis. Por esse 

motivo, os objetos de valor sempre devem ser imutáveis . Em vez de alterar o estado do objeto de 

valor, você o substitui por uma nova instância. Para objetos de valor complexos, considere usar o 

padrão construtor ou essência .

# $"" ... (arquivos java) 

!"" Aplicativo 

# !"" OneUseCase.java 

!"" # AnotherUseCase.java 

# $"" YetAnotherUseCase.java 

!"" Domínio 

# !"" SubDomain1 

# # $"" ... (arquivos java) 

# !"" SubDomain2 

# # $"" ... (arquivos java) 

# !"" SubDomain3 

# # $"" ... (arquivos java) 

# $"" SubDomain3 

# $"" ... (arquivos java) 

$"" Infraestrutura 

!"" banco de dados 

# $"" ... (arquivos java) 

!"" registro 

# $"" ... (arquivos java) 

$"" httpclient 

$"" ... (arquivos java) Objetos de valor não são apenas contêineres de dados - eles também podem conter lógica de 

negócios. O fato de que os objetos de valor também são imutáveis torna as operações de 

negócios thread-safe e livres de efeitos colaterais. Este é um dos motivos pelos quais gosto tanto 

de objetos de valor e por que você deve tentar modelar o máximo possível de seus conceitos de 

domínio como objetos de valor. Além disso, tente fazer com que os objetos de valor sejam os 

mais pequenos e coerentes possíveis - isso os torna mais fáceis de manter e reutilizar. 

Um bom ponto de partida para criar objetos de valor é pegar todas as propriedades de valor 

único que tenham um significado comercial e agrupá-las como objetos de valor. Por exemplo: 

Em vez de usar um BigDecimal para valores monetários, use um Money objeto de valor que 

envolve a BigDecimal . Se você está lidando com mais de uma moeda, você pode querer 

criar um Currency objeto de valor bem e fazer o seu Money objeto embrulhar um  

> BigDecimal

- Currency par. 

Em vez de usar strings para números de telefone e endereços de e-mail, use objetos  

> PhoneNumber

e EmailAddress valor que envolvam strings. 

Usar objetos de valor como esse tem várias vantagens. Em primeiro lugar, eles contextualizam o 

valor. Você não precisa saber se uma string específica contém um número de telefone, um 

endereço de e-mail, um nome ou um código postal, nem precisa saber se a BigDecimal é um 

valor monetário, uma porcentagem ou algo completamente diferente. O próprio tipo dirá 

imediatamente com o que você está lidando. 

Em segundo lugar, você pode adicionar todas as operações de negócios que podem ser 

realizadas em valores de um tipo específico ao próprio objeto de valor. Por exemplo, um  

> Money

objeto pode conter operações para adicionar e subtrair somas de dinheiro ou calcular 

percentagens, ao mesmo tempo que garante que a precisão do subjacente BigDecimal está 

sempre correta e que todos os Money objetos envolvidos na operação têm a mesma moeda. 

Em terceiro lugar, você pode ter certeza de que o objeto de valor sempre contém um valor válido. 

Por exemplo, você pode validar a string de entrada do endereço de e-mail no construtor do seu  

> EmailAddress

objeto de valor. 

# Exemplos de código 

Um Money objeto de valor em Java pode ser parecido com isto:                              

> @DomainValueObject
> public class Money implements Serializable ,Comparable <Money >{
> private final BigDecimal amount ;
> private final Currency currency ;// Moeda éum enum ou outro objeto de
> valor
> public Money (BigDecimal amount ,Currency currency ){
> this .currency =Objects .requireNonNull (currency );
> this .amount =
> Objects .requireNonNull (amount ). setScale (currency .getScale (),
> currency .getRoundingMode ());
> }

Um StreetAddress objeto de valor e o construtor correspondente em Java podem ser parecidos 

com isto (o código não foi testado e algumas implementações de método foram omitidas para 

maior clareza): 

public Money add (Money other ) {

assertSameCurrency (other ); 

return new Money (amount .add (other .amount ), currency ); 

}

public Money subtract (Money other ) {

assertSameCurrency (other ); 

return new Money (amount .subtract (other .amount ), currency ); 

}

private void assertSameCurrency (Money other ) {

if (!other .currency .equals (this .currency )) {

throw new IllegalArgumentException ("Objetos de dinheiro devem ter a

mesma moeda" ); 

}

}

public boolean equals (Object o) {

// Verifique se a moeda e o valor são iguais 

}

public int hashCode () {

// Calcule o código hash com base na moeda e no valor 

}

public int compareTo (Money other ) {

// Compare com base na moeda e valor 

}

}

@DomainValueObject 

public class StreetAddress implements Serializable , Comparable <StreetAddress > {

private final String streetAddress ;

private final PostalCode postalCode ; // PostalCode é outro objeto de valor 

private final String city ;

private final Country country ; // Country é um enum 

public StreetAddress (String streetAddress , PostalCode postalCode , String 

city , Country country ) {

// Verifique se os parâmetros necessários não são nulos 

// Atribua os valores dos parâmetros aos seus campos correspondentes 

}// Getters e possíveis métodos de lógica de negócios omitidos 

public boolean equals (Object o) {

// Verifique se os campos são iguais 

}

public int hashCode () {

// Calcule o código hash com base em todos os campos 

}

public int compareTo (StreetAddress other ) {

// Compare como quiser 

}

public static class Builder {

private String streetAddress ;

private PostalCode postalCode ;

private String city ;

private Country country ;

public Builder () { //Para criar novos StreetAddresses 

}

public Builder (StreetAddress original ) { // Para "modificar" 

StreetAddresses existentes 

streetAddress = original .streetAddress ;

postalCode = original .postalCode ;

city = original .city ;

country = original .country ;

}

public Builder withStreetAddress (String streetAddress ) {

this .streetAddress = streetAddress ;

return this ;

}

// O resto dos métodos 'com ...' omitidos 

public StreetAddress build () {

return new StreetAddress (streetAddress , postalCode , city , country ); 

}

}

}Entidades de domínio 

As entidades representam objetos de domínio e são definidas principalmente pela identidade, a 

continuidade e a persistência ao longo do tempo e não apenas pelos atributos que as compõem. 

Como Eric Evans diz, "um objeto definido principalmente por sua identidade é chamado de 

entidade". As entidades são muito importantes no modelo de domínio, pois elas são a base para 

um modelo. Portanto, você deve identificá-las e criá-las com cuidado. 

A mesma identidade (ou seja, o mesmo valor Id , embora talvez não a mesma entidade de 

domínio) pode ser modelada em vários Contextos Limitados ou microsserviços. No entanto, isso 

não significa que a mesma entidade, com os mesmos atributos e a mesma lógica, possa ser 

implementada em vários Contextos Limitados. Em vez disso, as entidades em cada contexto 

limitado limitam seus atributos e comportamentos aos necessários no domínio do contexto 

limitado. 

As entidades de domínio precisam implementar o comportamento, além de implementar os atributos 

de dados. 

Uma entidade de domínio no DDD precisa implementar a lógica do domínio ou o comportamento 

relacionado aos dados da entidade (o objeto acessado na memória). Por exemplo, como parte de 

uma classe de entidade de pedido, você precisa ter a lógica de negócios e as operações 

implementadas como métodos para tarefas como adicionar um item de pedido, validação de 

dados e cálculo de total. Os métodos da entidade cuidam das invariáveis e das regras da 

entidade, em vez de fazer com que essas regras se espalhem pela camada do aplicativo. Uma entidade de modelo de domínio implementa comportamentos por meio de métodos, ou 

seja, não é um modelo "anêmico".Obviamente, também é possível haver entidades que não 

implementam nenhuma lógica como parte da classe da entidade. Isso poderá ocorrer em 

entidades filhas dentro de uma agregação se a entidade filha não tiver nenhuma lógica especial 

porque a maioria da lógica está definida na raiz da agregação. 

# Exemplo de código 

@DomainEntity 

@DomainAggregateRoot 

public class Order implements AggregateRoot <Order , Order .OrderId > {

private OrderId id ;

private MemberAssociation member ;

private List <OrderItem > orderItems = new ArrayList <> (); 

private DeliveryAssociation delivery ;

private OrderStatus status ;

private LocalDateTime orderDate ;

public void addMember (Member member ) {

this .member = MemberAssociation .of (member .getId ()); 

}

public void addOrderItem (OrderItem orderItem ) {

orderItems .add (orderItem ); 

orderItem .setOrder (this ); 

}

public void addDelivery (Delivery delivery ) {

this .delivery = DeliveryAssociation .of (delivery .getId ()); 

delivery .setOrder (OrderAssociation .of (this .getId ())); 

}

public List <OrderItem > getOrderItems () {

return Collections .unmodifiableList (orderItems ); 

}

public static Order createOrder (Member member , Delivery delivery ,

OrderItem ... orderItems ) {

Order order = new Order (); 

order .addMember (member ); 

order .addDelivery (delivery ); 

Arrays .stream (orderItems ). forEach (oi -> order .addOrderItem (oi )); 

order .setStatus (OrderStatus .ORDER ); 

order .setOrderDate (LocalDateTime .now ()); 

return order ;

}

public void cancel () {

Delivery delivery = this .delivery .load (); if (!delivery .isOrderCancelable ()) {

throw new IllegalStateException ("Não é possível cancelar a ordem em : "

+ delivery .getStatus (). name ()); 

}

setStatus (OrderStatus .CANCEL ); 

getOrderItems (). forEach (oi -> oi .cancel ()); 

}

public int getTotalOrderPrice () {

return getOrderItems (). stream (). mapToInt (oi -> oi .getTotalPrice ()). sum (); 

}

@Override 

public Order getAggregateRoot () {

return this ;

}

@Override 

public OrderId getId () {

return id ;

}

public void setId (OrderId id ) {

this .id = id ;

}

public MemberAssociation getMember () {

return member ;

}

private void setMember (Member member ) {

this .member = MemberAssociation .of (member .getId ()); 

}

private void setOrderItems (List <OrderItem > orderItems ) {

this .orderItems = orderItems ;

}

public DeliveryAssociation getDelivery () {

return delivery ;

}

private void setDelivery (DeliveryAssociation delivery ) {

this .delivery = delivery ;

}

public OrderStatus getStatus () {

return status ;Agregações 

Um modelo de domínio contém diferentes clusters de entidades de dados e processos que 

podem controlar uma área significativa de funcionalidades, como inventário ou execução de 

pedidos. Uma unidade de DDD mais refinada é a agregação, que descreve um cluster ou o grupo 

de entidades e os comportamentos que podem ser tratados como uma unidade coesa. 

Normalmente, a agregação é definida com base nas transações que são necessárias. Um 

exemplo clássico é um pedido que também contém uma lista de itens do pedido. Geralmente, 

um item do pedido será uma entidade. Mas ela será uma entidade filha dentro da agregação de 

pedido, que também conterá a entidade de pedido como entidade raiz, geralmente chamada de 

raiz de agregação. 

Pode ser difícil identificar agregações. Uma agregação é um grupo de objetos que precisam ser 

consistentes em conjunto, mas não basta apenas selecionar um grupo de objetos e rotulá-lo 

como uma agregação. Você precisa começar com um conceito de domínio e pensar sobre as 

entidades que são usadas nas transações mais comuns relacionadas a esse conceito. As 

entidades que precisam ser consistentes entre as transações são as que formam uma agregação. 

Pensar sobre as operações de transação provavelmente é a melhor maneira de identificar as 

agregações.                                    

> }
> private void setStatus (OrderStatus status ){
> this .status =status ;
> }
> public LocalDateTime getOrderDate () {
> return orderDate ;
> }
> private void setOrderDate (LocalDateTime orderDate ){
> this .orderDate =orderDate ;
> }
> @DomainIdentifier
> public class OrderId <Long >implements Identifier {
> private Long id ;
> public OrderId () {
> }
> public Long getId () {
> return id ;
> }
> public void setId (Long id ){
> this .id =id ;
> }
> }
> }

# Raiz de agregação ou de entidade raiz 

Uma agregação é composta por pelo menos uma entidade: a raiz de agregação, também 

chamada de entidade raiz ou entidade principal. Além disso, ela pode ter várias entidades filhas e 

objetos de valor, com todas as entidades e os objetos trabalhando juntos para implementar as 

transações e os comportamentos necessários. 

A finalidade de uma raiz de agregação é garantir a consistência da agregação. Ela deve ser o único 

ponto de entrada para as atualizações da agregação por meio de métodos ou operações na 

classe raiz de agregação. Você deve fazer alterações nas entidades na agregação apenas por 

meio da raiz de agregação. É o guardião de consistência da agregação, considerando todas as 

invariáveis e regras de consistência que você pode precisar obedecer em sua agregação. Se você 

alterar uma entidade filha ou um objeto de valor de forma independente, a raiz de agregação não 

poderá garantir que a agregação esteja em um estado válido. Isso seria semelhante a uma tabela 

com um segmento flexível. Manter a consistência é o principal objetivo da raiz de agregação. 

# Repositórios 

Repositórios são classes ou componentes que encapsulam a lógica necessária para acessar 

fontes de dados. Eles centralizam a funcionalidade comum de acesso a dados, melhorando a 

sustentabilidade e desacoplando a infraestrutura ou a tecnologia usada para acessar os bancos 

de dados da camada do modelo de domínio. 

O padrão de repositório é uma maneira bem documentada de trabalhar com uma fonte de 

dados. No livro Padrões de Arquitetura de Aplicações Corporativas , Martin Fowler descreve um 

repositório da seguinte maneira: 

Um repositório executa as tarefas de um intermediário entre as camadas de modelo de 

domínio e o mapeamento de dados, funcionando de maneira semelhante a um conjunto de 

objetos de domínio na memória. Os objetos de clientes criam consultas de forma 

declarativa e enviam-nas para os repositórios buscando respostas. Conceitualmente, um 

repositório encapsula um conjunto de objetos armazenados no banco de dados e as 

operações que podem ser executadas neles, fornecendo uma maneira que é mais próxima da camada de persistência. Os repositórios também oferecem a capacidade de separação, 

de forma clara e em uma única direção, a dependência entre o domínio de trabalho e a 

alocação de dados ou o mapeamento. 

# Definir um repositório por agregação 

Para cada agregação ou raiz de agregação, você deve criar uma classe de repositório. Em um 

microsserviço baseado nos padrões de DDD (Design Orientado por Domínio), o único canal que 

você deve usar para atualizar o banco de dados são os repositórios. Isso ocorre porque eles têm 

uma relação um-para-um com a raiz agregada, que controla as invariáveis da agregação e a 

consistência transacional. É possível consultar o banco de dados por outros canais (como ao 

seguir uma abordagem de CQRS), porque as consultas não alteram o estado do banco de dados. 

No entanto, a área transacional (ou seja, as atualizações) sempre precisa ser controlada pelos 

repositórios e pelas raízes de agregação. 

Basicamente, um repositório permite popular na memória dados que são provenientes do banco 

de dados, em forma de entidades de domínio. Depois que as entidades estão na memória, elas 

podem ser alteradas e persistidas novamente no banco de dados por meio de transações. 

# Serviços de domínio 

Os serviços de domínio implementam a lógica de negócios a partir da definição de um expert de 

domínio. Trabalham com diversos fluxos de diversas entidades e agregações, utilizam os 

repositórios como interface de acesso aos dados e consomem recursos da camada de 

infraestrutura, como: enviar email, disparar eventos, entre outros. 

# Eventos de domínio 

Até agora, vimos apenas as "coisas" no modelo de domínio. No entanto, elas só podem ser 

usadas para descrever o estado estático em que o modelo está em um determinado momento. 

Em muitos modelos de negócios, você também precisa ser capaz de descrever coisas que 

acontecem e alterar o estado do modelo. Para isso, você pode usar eventos de domínio. 

Os eventos de domínio não foram incluídos no livro de Evans sobre design orientado a domínio. 

Eles foram adicionados à caixa de ferramentas posteriormente e estão incluídos no livro de 

Vernon. 

Um evento de domínio é tudo o que acontece no modelo de domínio que pode ser de interesse 

para outras partes do sistema. Os eventos de domínio podem ser de baixa granularidade (por 

exemplo, uma raiz de agregação específica é criada ou um processo é iniciado) ou de baixa 

granulação (por exemplo, um determinado atributo de uma determinada raiz agregada é 

alterado). 

Os eventos de domínio geralmente têm as seguintes características: 

Eles são imutáveis (afinal, você não pode mudar o passado). 

Eles têm um carimbo de data / hora quando o evento em questão ocorreu. 

Eles podem ter um ID único que ajuda a distinguir um evento de outro, dependendo do tipo 

de evento e de como os eventos são distribuídos. 

Eles são publicados por raízes agregadas ou serviços de domínio (mais sobre isso mais tarde). 

Depois que um evento de domínio é publicado, ele pode ser recebido por um ou mais ouvintes de 

eventos de domínio que, por sua vez, podem acionar processamento adicional e novos eventos 

de domínio, etc. O editor não está ciente do que acontece com o evento, nem deve ser capaz de 

afetar o editor (em outras palavras, publicar eventos de domínio deve ser livre de efeitos 

colaterais do ponto de vista do editor). Por isso, é recomendado que ouvintes de evento de 

domínio não sejam executados dentro da mesma transação que publicou o evento. 

Do ponto de vista do design, a maior vantagem dos eventos de domínio é que eles tornam o 

sistema extensível. Você pode adicionar quantos ouvintes de eventos de domínio precisar para 

acionar uma nova lógica de negócios sem ter que alterar o código existente. Isso naturalmente 

pressupõe o correto eventos são publicados em primeiro lugar. Alguns eventos você pode saber 

antecipadamente, mas outros se revelarão mais adiante. Você pode, é claro, tentar adivinhar 

quais tipos de eventos serão necessários e adicioná-los ao seu modelo, mas também corre o risco 

de entupir o sistema com eventos de domínio que não são usados em lugar nenhum. Uma 

abordagem melhor é tornar o mais fácil possível a publicação de eventos de domínio e adicionar 

os eventos ausentes quando perceber que precisa deles. 

Uma nota sobre Event Source 

Event Source é um padrão de design em que o estado de um sistema é persistido como um 

log de eventos ordenados. Cada um altera o estado do sistema e o estado atual pode ser 

calculado a qualquer momento reproduzindo o log de eventos do início ao fim. O padrão é 

especialmente útil em aplicativos como controles financeiros ou registros médicos em que o 

histórico é tão importante (ou até mais importante) que o estado atual. 

# Eventos de domínio versus eventos de integração 

Semanticamente, os eventos de integração e de domínio são a mesma coisa: notificações sobre 

algo que acabou de ocorrer. No entanto, a implementação deles deve ser diferente. Os eventos 

de domínio são apenas mensagens enviadas por push para um dispatcher de evento de domínio, 

que pode ser implementado como um mediador na memória, com base em um contêiner de IoC ou qualquer outro método. 

Por outro lado, a finalidade dos eventos de integração é a propagação de transações e 

atualizações confirmadas para outros subsistemas, independentemente de serem outros 

microsserviços, contextos delimitados ou, até mesmo, aplicativos externos. Assim, eles deverão 

ocorrer somente se a entidade for persistida com êxito, caso contrário, será como se toda a 

operação nunca tivesse acontecido. 

Conforme o que foi mencionado antes, os eventos de integração devem ser baseados em 

comunicação assíncrona entre vários microsserviços (outros contextos delimitados) ou mesmo 

aplicativos/sistemas externos. 

Assim, a interface do barramento de eventos precisa de alguma infraestrutura que permita a 

comunicação entre processos e distribuída entre serviços potencialmente remotos. Ela pode ser 

baseada em um barramento de serviço comercial, em filas, em um banco de dados 

compartilhado usado como uma caixa de correio ou em qualquer outro sistema de mensagens 

distribuídas e, idealmente, baseado em push. 

# Anti-corruption layer: 

É uma camada que fornece comunicação entre os sistemas, "traduzindo" as entradas e saídas. 

Pode ser uma forma eficaz de conviver com projetos legados - com isso, o domínio principal não 

ficará poluído com dívidas técnicas associadas ao projeto legado. 

# Validação em DDD 

A validação é um assunto amplo porque prevalece em todas as áreas de um aplicativo. A 

validação é difícil de implementar na prática porque deve ser implementada em todas as áreas de 

um aplicativo, normalmente empregando métodos diferentes para cada área. Em um sentido 

geral, a validação é um mecanismo para garantir que as operações resultem em estados válidos. 

A ambigüidade nessa declaração não deve ser negligenciada porque ela ilustra várias 

características importantes de validação. Uma característica é o contexto - o contexto sob o qual a 

validação é chamada. 

# Sempre Válido 

No design orientado por domínio, existem duas escolas de pensamento sobre validação que 

giram em torno da noção da entidade sempre válida . Jeffrey Palermo propõe que a entidade 

sempre válida é uma falácia. Ele sugere que a lógica de validação deve ser desacoplada da 

entidade, o que adiaria a determinação das regras de validação a serem invocadas até o tempo 

de execução. A outra escola de pensamento, apoiada por Greg Young e outros, afirma que as 

entidades devem ser sempre válidas. 

Depois de tudo conceituado vamos ver cada módulo do framework e qual sua responsabilidade 

dentro do contexto. 

# CQRS O CQRS é um padrão de arquitetura que separa os modelos para ler e gravar dados. O termo 

relacionado CQS (Separação de Comando-Consulta) foi originalmente definido por Bertrand 

Meyer em seu livro Object Oriented Software Construction (Construção de software orientada a 

objeto). A idéia básica é que você pode dividir as operações de um sistema em duas categorias 

bem separadas: 

Consultas. Essas retornam um resultado, não alteram o estado do sistema e são livres de 

efeitos colaterais. 

comandos. Esses alteram o estado de um sistema. 

CQS é um conceito simples: trata-se de métodos dentro do mesmo objeto sendo consultas ou 

comandos. Cada método retorna um estado ou muda um estado, mas não ambos. Até mesmo 

um único objeto de padrão de repositório pode estar em conformidade com o CQS. O CQS pode 

ser considerado um princípio fundamental para o CQRS. 

O CQRS (Segregação de Responsabilidade de Consulta e Comando) foi introduzido por Greg 

Young e altamente promovido por Udi Dahan e outros. Ele se baseia no princípio do CQS, 

embora seja mais detalhado. Ele pode ser considerado um padrão com base em comandos e 

eventos, além de ser opcional em mensagens assíncronas. Em muitos casos, o CQRS está 

relacionado a cenários mais avançados, como ter um banco de dados físico para leituras 

(consultas) diferente do banco de dados para gravações (atualizações), mas isso não impede que 

seja usada uma única instância de um banco de dados. Além disso, um sistema CQRS mais 

evoluído pode implementar ES (fonte de eventos) em seu banco de dados de atualizações. 

Assim, você deve apenas armazenar eventos no modelo de domínio, em vez de armazenar os 

dados do estado atual. Práticas recomendadas de CQRS / ES 

Melhores práticas, orientação e antipadrões a serem evitados ao construir um aplicativo seguindo 

os princípios CQRS / ES. 

# Orientação 

Defina um esquema para comandos e eventos 

Os serviços compartilham esquema e contrato, não classe. 

Não compartilhe tipos (ou seja, classes compiladas em assemblies) como uma forma de expressar 

contratos em um sistema de mensagens. 

Defina e compartilhe um esquema e um contrato. Publique-o para que os consumidores 

construam sua própria representação interna de seus dados. 

# Exemplos 

Esquema JSON 

Buffers de protocolo 

# Objetos de valor em eventos de domínio 

Use tipos simples em eventos de domínio (strings, números, listas). 

Não use objetos de valor em eventos, pois os eventos são imutáveis, enquanto a definição de 

objetos de valor pode mudar com o tempo. 

Os objetos de valor são imutáveis, mas sua definição (classe ou esquema) pode ser alterada a 

qualquer momento pelos desenvolvedores. Fazer isso interromperá inadvertidamente qualquer 

evento existente, a menos que uma estratégia de atualização explícita seja implementada. O uso 

de tipos simples em eventos de domínio alivia isso um pouco porque os desenvolvedores devem 

entender que alterar um evento afetará os eventos armazenados existentes. 

Use eventos de integração entre serviços, não eventos de domínio 

Não exponha eventos de domínio fora do serviço ou do contexto limitado que os cria. Use 

eventos de integração. 

Um evento de integração externa é um evento público produzido por um contexto limitado que 

pode ser interessante para outros domínios, aplicativos ou serviços de terceiros. O objetivo dos 

eventos de integração é propagar explicitamente transações confirmadas e atualizações para 

subsistemas adicionais. Você pode converter e publicar um evento de domínio em serviços 

externos, como um evento de integração, após ter sido confirmado. 

O uso de eventos de integração torna mais fácil alterar eventos de domínio interno, enquanto 

permite que eventos de integração externos forneçam uma API estável aos consumidores. 

Também permite que um serviço exponha apenas um subconjunto de seus eventos de domínio 

como eventos de integração. Os eventos de integração podem ser enriquecidos com dados 

adicionais, como a partir de uma projeção de modelo de leitura, que é relevante para usos 

externos. 

# Módulos archbase Módulos archbase 

# archbase-domain-driven-design-spec 

Este módulo é o coração do framework pois fornece todos os contratos, anotações e 

especificações necessárias serem seguidas para a implementação de um projeto java seguinte os 

conceitos de DDD. 

Vejamos os principais contratos :

# Objetos de valor 

Interface para marcação de quais objetos são ValueObject. A principal caracteristica 

desses objetos é serem imutáveis. Devem ser usados em entidades para enriquecer o domínio. 

# Identificadores 

Identifier é apenas uma interface de marcação para equipar os tipos de identificadores. Isso 

encoraja tipos dedicados a descrever identificadores. A intenção principal disso é evitar que cada 

entidade seja identificada por um tipo comum (como Long ou UUID ). Embora possa parecer uma 

boa ideia do ponto de vista da persistência, é fácil misturar um identificador de uma Entidade 

com o identificador de outra. Os tipos de identificadores explícitos evitam esse problema. 

# Identificáveis 

Identifiable é uma interface de marcação para informar que determinados tipos como por 

exemplo Entidades são identificáveis. Com isso garantem que associações sejam feitas 

apenas com objetos identificáveis. 

# Entidades 

Identifica um o objeto como sendo uma Entidade que possui uma identidade, passando por 

um ciclo de vida, embora seus atributos possam mudar. Os meios de identificação podem 

vir de fora ou podem ser arbitrários identificador criado por e para o sistema, mas deve 

corresponder às distinções de identidade no modelo. O modelo deve definir o que significa 

ser a mesma coisa. 

public interface ValueObject extends Serializable 

public interface Identifier {

public interface Identifiable <ID >

public interface Entity <T extends AggregateRoot <T, ?> , ID > extends 

Identifiable <ID > {

public T getAggregateRoot (); 

}Ela também está vinculado a um AggregateRoot. Isso pode parecer contra-intuitivo à 

primeira vista, mas permite verificar que uma Entity não é acidentalmente referido a partir 

de um agregado diferente. 

Usando essas interfaces, foi criado o módulo archbase-validation-ddd-model para fazer a 

análise do código estático para verificar a estrutura do nosso modelo. 

# Agregadores 

Identifica uma raiz agregada, ou seja, a entidade raiz de um agregado. Um agregado forma 

um cluster de regras consistentes geralmente formado em torno de um conjunto de 

entidades, definindo invariantes com base nas propriedades do agregado que devem ser 

encontrado antes e depois das operações dele. 

Os agregados geralmente se referem a outros agregados por seu identificador. Referências 

a agregados internos devem ser evitadas e, pelo menos, não consideradas fortemente 

consistentes (ou seja, uma referência retido pode ter desaparecido ou se tornar inválido a 

qualquer momento). Eles também atuam como escopo de consistência, ou seja, as 

mudanças em um único agregado devem ser fortemente consistentes, enquanto as 

mudanças em vários outros devem ser apenas consistência eventual. 

# Associação 

É basicamente uma indireção em direção a um identificador de agregado relacionado que 

serve puramente à expressividade dentro do modelo. 

# Repositórios                                                          

> public interface AggregateRoot <Textends AggregateRoot <T,ID >,ID extends
> Identifier >extends Entity <T,ID >{
> public interface Association <Textends AggregateRoot <T,ID >,ID extends
> Identifier >extends Identifiable <ID >{
> public Tload ();
> }
> public interface Repository <T,ID ,Nextends Number &Comparable <N>> extends
> QuerydslPredicateExecutor <T>,PagingAndSortingRepository <T,ID >,
> RevisionRepository <T,ID ,N>{
> /**
> *Recupera todas as entidades *@return Lista de entidades
> */
> List <T>findAll ();
> /**
> *Recupera todas as entidades com ordenação. *@param sort ordenação
> *@return Lista de entidades

*/ 

List <T> findAll (Sort sort ); 

/** 

* Recupera todas as entidades de estão na lista de ID's * passadas como 

parâmetro. * @param ids Lista de ID's 

* @return Lista de entidades 

*/ 

List <T> findAllById (Iterable <ID > ids ); 

/** 

* Salva uma lista de entidades * @param entities Entidades para salvar 

* @param <S> Tipo de entidade 

* @return Lista de entidades salvas. 

*/ 

<S extends T> List <S> saveAll (Iterable <S> entities ); 

/** 

* Libera todas as alterações pendentes no banco de dados. 

*/ 

void flush (); 

/** 

* Salva uma entidade e elimina as alterações instantaneamente. * * @param 

entity 

* @return a entidade salva 

*/ 

<S extends T> S saveAndFlush (S entity ); 

/** 

* Exclui as entidades fornecidas em um lote, o que significa que criará um 

único comando. * * @param entities 

*/ 

void deleteInBatch (Iterable <T> entities ); 

/** 

* Exclui todas as entidades em uma chamada em lote. 

*/ 

void deleteAllInBatch (); 

/** 

* Retorna uma referência à entidade com o identificador fornecido. * * @param 

id não deve ser {@literal null}. 

* @return uma referência à entidade com o identificador fornecido. 

* */ 

T getOne (ID id ); 

/** 

* Salva uma lista de entidades * @param iterable Lista de entidades * @return Lista de entidades salvas 

*/ 

List <T> save (T... iterable ); 

/** 

* Recupera todas as entidades que atendam o predicado. * @param predicate 

Predicado. 

* @return Lista de entidades 

*/ @Override 

List <T> findAll (Predicate predicate ); 

/** 

* Recupera todas as entidades que atendam o predicado e retorna * de forma 

ordenada. * @param predicate Predicado 

* @param sort Ordenação 

* @return Lista de entidades ordenada. 

*/ @Override 

List <T> findAll (Predicate predicate , Sort sort ); 

/** 

* Recupera todas as entidades que atendam o predicado e retorna * de forma 

ordenada. * @param predicate 

* @param orderSpecifiers 

* @return Lista de entidades ordenada. 

*/ @Override 

List <T> findAll (Predicate predicate , OrderSpecifier <?> ... orderSpecifiers ); 

/** 

* Recupera todas as entidades de forma ordenada. * @param orderSpecifiers 

Ordenação 

* @return Lista de entidades ordenadas 

*/ @Override 

List <T> findAll (OrderSpecifier <?> ... orderSpecifiers ); 

/** 

* Recupera todas as entidades que correspondem a uma especificação. * @param 

archbaseSpecification 

* @return lista de entidades 

*/ 

List <T> matching (ArchbaseSpecification <T> archbaseSpecification ); 

/** 

* Conte quantas entidades correspondem a uma especificação. * @param 

archbaseSpecification 

* @return quantidade de entidades 

*/ 

Long howMany (ArchbaseSpecification <T> archbaseSpecification ); 

/** Repositórios são classes ou componentes que encapsulam a lógica necessária para acessar 

fontes de dados. Eles centralizam a funcionalidade comum de acesso a dados, melhorando 

a sustentabilidade e desacoplando a infraestrutura ou a tecnologia usada para acessar os 

bancos de dados da camada do modelo de domínio. 

Um repositório executa as tarefas de um intermediário entre as camadas de modelo de 

domínio e o mapeamento de dados, funcionando de maneira semelhante a um conjunto de 

objetos de domínio na memória. Os objetos de clientes criam consultas de forma 

declarativa e enviam-nas para os repositórios buscando respostas. 

Conceitualmente, um repositório encapsula um conjunto de objetos armazenados no banco 

de dados e as operações que podem ser executadas neles, fornecendo uma maneira que é 

mais próxima da camada de persistência. 

Os repositórios também oferecem a capacidade de separação, de forma clara e em uma 

única direção, a dependência entre o domínio de trabalho e a alocação de dados ou o 

mapeamento. 

# Serviços 

Os serviços de domínio implementam a lógica de negócios a partir da definição de um 

expert de domínio. Trabalham com diversos fluxos de diversas entidades e agregações, 

utilizam os repositórios como interface de acesso aos dados e consomem recursos da 

camada de infraestrutura, como: enviar email, disparar eventos, entre outros. 

Os serviços se subdividem em dois contratos quando aplicados junto como o padrão CQRS, 

ficando desta forma: 

CommandService                                                     

> *Determine se alguma de nossas entidades corresponde auma especificação. *
> @param archbaseSpecification
> *@return true se alguma atende
> */
> Boolean containsAny (ArchbaseSpecification <T>archbaseSpecification );
> /**
> *Recupera todos os objetos que atendam ao filtro *@param Filter Filtro
> *@param pageable Configuração de página
> *@return Página
> */
> Page <T>findAll (String Filter ,Pageable pageable );
> }
> public interface Service <T,ID ,Nextends Number &Comparable <N>> {
> public Repository <T,ID ,N>getRepository ();
> }

QueryService 

# Fábricas 

# Anotações 

Anotações criadas para auxiliar na demarcação do contexto e identificação das classes dentro do 

conceito de DDD, isto serve tanto para o framework para transformação, geração e validação de 

código como para ser utilizando para criação de uma documentação mais rica. 

DomainAggregateRoot : Identifica uma raiz agregada, ou seja, a entidade raiz de um 

agregado. 

DomainAssociation : Identifica uma associação entre duas entidades dentro do modelo. 

DomainBoundedContext : Identifica um contexto limitado. Uma descrição de um limite 

(normalmente um subsistema ou o trabalho de uma equipe específica) dentro do qual um 

determinado modelo é definido e aplicável. Um contexto limitado tem um estilo 

arquitetônico e contém lógica de domínio e lógica técnica. 

DomainEntity : Identifica uma Entidade de domínio que pode ser também um Agregador. 

DomainFactory : Identifica uma Fábrica. As fábricas encapsulam a responsabilidade de criar 

objetos complexos em geral e Agregados em particular. Os objetos retornados pelos 

métodos de fábrica têm a garantia de estar em estado válido. 

DomainIdentifier : Identifica um classe com papel de Identificador de entidade. 

Identificadores são parte das Entidades de dominio. Para que elas possam ser identificáveis 

precisam estar marcadas com a interface Identifier . Usamos esta anotação para 

documentações e geração de código também. 

DomainModule : Identifica um módulo DDD. Pode ser usado para criar documentação. 

DomainRepository :Identifica um Repositório de domínio. Repositórios simulam uma 

coleção de agregados para os quais as instâncias agregadas podem ser adicionados, 

atualizadas e removidos. 

DomainService : Identifica um serviço de domínio. 

DomainTransient : Marca um campo de Entidade de dominio como transient(temporário). 

DomainValueObject Identifica um objeto de valor. 

Contextualizando os desafios:                   

> public interface CommandService <T,ID ,Nextends Number &Comparable <N>> extends
> Service <T,ID ,N>{
> }
> public interface QueryService <T,ID ,Nextends Number &Comparable <N>> extends
> Service <T,ID ,N>{
> }

Comecemos com um exemplo rápido que nos permite destacar os desafios. Observe que o 

modelo não é a única maneira de projetá-lo. Estamos apenas descrevendo o que poderia ser o 

resultado de um projeto em um determinado contexto. É sobre como um agregado, entidade ou 

objeto de valor pode ser representado em código e o efeito de uma maneira particular de fazer 

isso. Modelamos Customer s que consistem em Address es, Order s que consistem em  

> LineItem

s que, por sua vez, apontam para Product s e apontam para o Customer que fez o 

pedido. Ambos Customer e Order são agregados conceitualmente. 

> «AggregateRoot»
> Order
> id : OrderId
> …
> «Entity»
> LineItem
> amount : MonetaryAmount
> …
> «AggregateRoot»
> Product
> id : ProductId
> …
> «AggregateRoot»
> Customer
> id : CustomerId
> …
> «ValueObject»
> Address
> zipCode : ZipCode
> …

1

*

1 1

Um exemplo de modelo de domínio 

Vamos começar com o Order relacionamento de Customer agregado. Uma representação muito 

ingênua no código que usa anotações JPA diretamente provavelmente seria algo assim: 

Embora isso constitua um código funcional, grande parte da semântica do modelo permanece 

implícita. No JPA, o conceito de granulação mais grosseira é uma entidade. Não sabe sobre 

agregados. Ele também usará automaticamente o carregamento antecipado para 

relacionamentos com um. Para uma relação de agregação cruzada, não é isso que queremos.                 

> @Entity
> class Order {
> @EmbeddedId OrderId id ;
> @ManyToOne Customer customer ;
> @OneToMany List <LineItem >items ;
> }
> @Entity
> class LineItem {
> @ManyToOne Product product .
> }
> @Entity
> class Customer {
> @EmbeddedId CustomerId id ;
> }

Uma reação focada na tecnologia seria mudar para o carregamento lento. No entanto, isso cria 

novos problemas, estamos começando a cavar uma toca de coelho e, na verdade, mudamos da 

modelagem de domínio para a tecnologia de modelagem, algo que queríamos evitar em primeiro 

lugar. Também podemos querer recorrer apenas a identificadores de mapa em vez de tipos de 

agregação, por exemplo, substituindo Customer por CustomerId in Order . Embora isso resolva 

o problema de cascata, agora é ainda menos claro que essa propriedade estabelece efetivamente 

uma relação de agregação cruzada. 

Para os LineItem referenciados, um mapeamento padrão adequado seria mais rápido (em vez 

de preguiçoso) e cascateamento de todas as operações, pois o agregado geralmente governa o 

ciclo de vida de seus internos. 

Para melhorar a situação descrita acima, poderíamos começar introduzindo tipos que nos 

permitem atribuir funções explicitamente para modelar artefatos e restringir a composição deles 

usando genéricos. Vejamos como a idéia a qual archbase implementa:  

> Identifier

é apenas uma interface de marcador para equipar os tipos de identificadores. Isso 

encoraja tipos dedicados a descrever identificadores. A intenção principal disso é evitar que cada 

entidade seja identificada por um tipo comum (como Long ou UUID ). Embora possa parecer uma 

boa ideia do ponto de vista da persistência, é fácil misturar um Customer identificador de com  

> Order

um de. Os tipos de identificadores explícitos evitam esse problema. 

Um DDD Entity é um conceito identificável, o que significa que ele precisa expor seu 

identificador. Ele também está vinculado a um AggregateRoot . Isso pode parecer contra-

intuitivo à primeira vista, mas permite verificar que Entity tal como LineItem não é 

acidentalmente referido a partir de um agregado diferente. Usando essas interfaces, podemos 

configurar ferramentas de análise de código estático para verificar a estrutura do nosso modelo.  

> Association

é basicamente uma indireção em direção a um identificador de agregado 

relacionado que serve puramente à expressividade dentro do modelo. 

Blocos de construção explícitos em nosso exemplo:                                    

> interface Identifier {}
> interface Identifiable <ID extends Identifier >{
> ID getId ();
> }
> interface Entity <Textends AggregateRoot <T,?> ,ID extends Identifier >
> extends Identifiable <ID >{}
> interface AggregateRoot <Textends AggregateRoot <T,ID >,ID extends Identifier >
> extends Entity <T,ID >{}
> interface Association <Textends AggregateRoot <T,ID >,ID extends Identifier >
> extends Identifiable <ID >{}

Com isso, podemos extrair muitas informações adicionais observando apenas os tipos e os 

campos: 

O tipo de identificador para Order é OrderId , algo que poderíamos ter derivado antes, mas 

envolvendo apenas a interpretação de anotação JPA. 

LineItem é uma entidade pertencente ao Order agregado. 

A customer propriedade indica claramente que representa uma associação com o 

Customer agregado. 

# archbase-domain-driven-design 

# archbase-shared-kernel 

# archbase-transformation 

Plugin para aplicar as anotações de persistência do metamodel nas entidades e agregados. 

Isso foi uma técnica usada para não poluir as classes do dominio com anotações de persistência e 

eliminar o acoplamento. Para trocar a forma de persistência basta apenas criar um novo 

metamodel para o formato desejado e escrever um novo plugin. Desta formas as entidades do 

domínio podem ser transformadas para persistir o formato que desejarmos. 

# archbase-annotation-processor 

# archbase-cqrs 

# archbase-query 

Os repositórios possuem um alto grau de flexibilidade para a criação de filtros usando 

predicados, consultas nativas, quero by Example, specification, etc. Porém isso faz parte de uma 

série de recursos de uso interno da api para manipulação das entidades dentro dos serviços. Isso 

não é possível de ser utilizado pelos consumidores da api, no caso desenvolvimento do frontend, 

integradores, etc. 

class OrderId implements Identifier {} 

class Order implements AggregateRoot <Order , OrderId > {

OrderId id ;

CustomerAssociation customer ;

List <LineItem > items ;

}

class LineItem implements Entity <Order , LineItemId > { … }

class CustomerAssociation implements Association <Customer , CustomerId > { … }

class Customer implements AggregateRoot <Customer , CustomerId > { … }Com isso somos obrigados a criar filtros específicos para atender as demandas dos consumidores 

da api. Isso gera um número de atividades que poderíamos evitar. Em alguns casos realmente é 

preciso criar novos métodos para atender alguns consumidores, mas quando por exemplo os 

consumidores da api somos nós mesmos criando nossos produtos poderíamos criar formas de 

dar mais flexibilidade aos desenvolvedores do frontend ou os que iram necessitar acessar os 

serviços. 

Pensando desta forma precisamos estabelecer uma forma segura que os consumidores possam 

fazer chamadas ao serviços usando para isto uma forma realizar o filtro dos dados. 

# RSQL 

RSQL é uma linguagem de consulta para filtragem parametrizada de entradas em APIs RESTful. 

Este padrão foi criado baseado no FIQL (Feed Item Query Language). RSQL fornece uma sintaxe 

amigável e de fácil entendimento para a criação de filtros. 

Gramática e semântica 

A seguinte especificação de gramática foi escrita em notação EBNF ( ISO 14977 ). 

A expressão RSQL é composta por uma ou mais comparações, relacionadas entre si com 

operadores lógicos: 

Logical AND : ; or and 

Logical OR : , or or 

Por padrão, o operador AND tem precedência (ou seja, é avaliado antes de qualquer operador 

OR). No entanto, uma expressão entre parênteses pode ser usada para alterar a precedência, 

produzindo o que quer que a expressão contida produza. 

A comparação é composta por um seletor, um operador e um argumento. 

O seletor identifica um campo (ou atributo, elemento, ...) da representação do recurso pelo qual 

filtrar. Pode ser qualquer string Unicode não vazia que não contenha caracteres reservados (veja 

abaixo) ou um espaço em branco. A sintaxe específica do seletor não é imposta por este 

analisador. 

Operadores de comparação: 

Equal to : ==                                  

> input =or, EOF;
> or =and, {"," ,and };
> and =constraint, {";" ,constraint };
> constraint =(group |comparison );
> group ="(", or, ")";
> comparison =selector, comparison-op, arguments;
> selector =unreserved-str;

Not equal to : != 

Less than : =lt= or <

Less than or equal to : =le= or ⇐

Greater than operator : =gt= or >

Greater than or equal to : =ge= or >= 

In : =in= 

Not in : =out= 

O argumento pode ser um único valor ou vários valores entre parênteses separados por 

vírgula. O valor que não contém nenhum caractere reservado ou um espaço em branco 

pode ficar sem aspas; outros argumentos devem ser colocados entre aspas simples ou 

duplas. 

Se precisar usar aspas simples e duplas dentro de um argumento entre aspas, você deve escapar 

uma delas usando \ (barra invertida). Se você quiser usar \ literalmente, dobre como \\ . A 

barra invertida tem um significado especial apenas dentro de um argumento entre aspas, e não 

em um argumento sem aspas. 

Como adicionar operadores personalizados 

Caso haja a necessidade de criarmos mais operadores podemos fazer isto da seguinte forma: 

# Integração JPA e Querydsl 

Para que o filtro seja usado pelo consumidor com base na sintaxe RSQL o que é bem simples é 

necessário que possamos fazer a tradução do mesmo para os formatos usados nos repositórios 

como: JPA Specification e QueryDsl Predicate. 

Para isso foram criadas duas classes para dar suporte a essa tradução:                                                                                                          

> arguments =("(", value, {"," ,value }, ")" )|value;
> value =unreserved-str |double-quoted |single-quoted;
> unreserved-str =unreserved, {unreserved }
> single-quoted ="'", {(escaped |all-chars -("'" |"\" ))}, "'";
> double-quoted ='"', {(escaped |all-chars -('"' |"\" ))}, '"';
> reserved ='"' |"'" |"(" |")" |";" |"," |"="|"!" |"~" |"<" |
> ">";
> unreserved =all-chars -reserved -"";
> escaped ="\", all-chars;
> all-chars =?all unicode characters ?;
> Set <ComparisonOperator >operators =RSQLOperators .defaultOperators ();
> operators .add (new ComparisonOperator ("=all=" ,true ));
> Node rootNode =new RSQLParser (operators ). parse ("genres=all=('thriller','sci-
> fi')" ); Classe Descrição
> br.com.archbase.query.rsql.jpa. ArchbaseRSQLJPASupport Converte filtro no formato RSQL para JPA Specification.
> br.com.archbase.query.rsql.querydsl. ArchbaseRSQLQueryDslSupport Converte filtro no formato RSQL para QueryDsl Predicate.

# Sintaxe RSQL para uso com JPA e Querydsl: 

filtro = "id =bt =(2,4)";// id> =2 && id< =4 //between 

filtro = "id =nb =(2,4)";// id<2 || id>4 //not between 

filtro = "empresa.nome =like =em"; //like %em% 

filtro = "empresa.nome =ilike =EM"; //ignore case like %EM% 

filtro = "empresa.nome =icase =EM"; //ignore case equal EM 

filtro = "empresa.nome =notlike =em"; //not like %em% 

filtro = "empresa.nome =inotlike =EM"; //ignore case not like %EM% 

filtro = "empresa.nome =ke =e*m"; //like %e*m% 

filtro = "empresa.nome =ik =E*M"; //ignore case like %E*M% 

filtro = "empresa.nome =nk =e*m"; //not like %e*m% 

filtro = "empresa.nome =ni =E*M"; //ignore case not like %E*M% 

filtro = "empresa.nome =ic =E^^M"; //ignore case equal E^^M 

filtro = "empresa.nome == demo"; //equal 

filtro = "empresa.nome == 'demo'"; //equal 

filtro = "empresa.nome == ''"; //equal to empty string 

filtro = "empresa.nome == dem*"; //like dem% 

filtro = "empresa.nome == *emo"; //like %emo 

filtro = "empresa.nome == *em*"; //like %em% 

filtro = "empresa.nome == ^EM"; //ignore case equal EM 

filtro = "empresa.nome == ^*EM*"; //ignore case like %EM% 

filtro = "empresa.nome == '^*EM*'"; //ignore case like %EM% 

filtro = "empresa.nome! =demo"; //not equal 

filtro = "empresa.nome =in =(*)"; //equal to *

filtro = "empresa.nome =in =(^)"; //equal to ^

filtro = "empresa.nome =in =(demo,real)"; //in 

filtro = "empresa.nome =out =(demo,real)"; //not in 

filtro = "empresa.id =gt =100"; //greater than 

filtro = "empresa.id =lt =100"; //less than 

filtro = "empresa.id =ge =100"; //greater than or equal 

filtro = "empresa.id =le =100"; //less than or equal 

filtro = "empresa.id>100"; //greater than 

filtro = "empresa.id<100"; //less than 

filtro = "empresa.id> =100"; //greater than or equal 

filtro = "empresa.id< =100"; //less than or equal 

filtro = "empresa.nome =isnull =''"; //is null 

filtro = "empresa.nome =null =''"; //is null 

filtro = "empresa.nome =na =''"; //is null 

filtro = "empresa.nome =nn =''"; //is not null 

filtro = "empresa.nome =notnull =''"; //is not null 

filtro = "empresa.nome =isnotnull =''"; //is not null 

filtro = "empresa.nome == 'demo';empresa.id>100"; //and 

filtro = "empresa.nome == 'demo' and empresa.id>100"; //and Usando filtro RSQL com JPA Specification: 

# Sintaxe ordenação: 

# Ordenação com JPA Specification: 

filtro = "empresa.nome == 'demo',empresa.id>100"; //or 

filtro = "empresa.nome == 'demo' or empresa.id>100"; //or 

Pageable pageable = PageRequest .of (0, 5); //page 1 and page size is 5

repository .findAll (RSQLSupport .toSpecification (filter )); 

repository .findAll (RSQLSupport .toSpecification (filter ), pageable ); 

repository .findAll (RSQLSupport .toSpecification (filter , true )); // select 

distinct 

repository .findAll (RSQLSupport .toSpecification (filter , true ), pageable ); 

// use static import 

import static br .com .archbase .query .rsql .common .RSQLSupport .*;

repository .findAll (toSpecification (filter )); 

repository .findAll (toSpecification (filter ), pageable ); 

repository .findAll (toSpecification (filter , true )); // select distinct 

repository .findAll (toSpecification (filter , true ), pageable ); 

// property path remap 

filter = "empNome=='demo';empId>100" ; // "empresa.nome=='demo';empresa.id>100" 

Map <String , String > propertyPathMapper = new HashMap <> (); 

propertyPathMapper .put ("empId" , "empresa.id" ); 

propertyPathMapper .put ("empNome" , "empresa.nome" ); 

repository .findAll (toSpecification (filter , propertyPathMapper )); 

repository .findAll (toSpecification (filter , propertyPathMapper ), pageable ); 

sort = "id,asc" ; // order by id asc 

sort = "id,asc;company.id,desc" ; // order by id asc, empresa.id desc Filtrando e ordenando com JPA Specification: 

# Querydsl predicado (expressão booleano) 

repository .findAll (RSQLSupport .toSort ("id,asc;company.id,desc" )); 

// sort com mapeamento de campos 

Map <String , String > propertyMapping = new HashMap <> (); 

propertyMapping .put ("userID" , "id" ); 

propertyMapping .put ("empresaID" , "empresa.id" ); 

repository .findAll (RSQLSupport .toSort ("userID,asc;empresaID,desc" ,

propertyMapping )); 

Specification <?> specification =

RSQLSupport .toSpecification ("empresa.nome==demo" )

.and (RSQLSupport .toSort ("empresa.nome,asc,user.id,desc" )); 

repository .findAll (specification ); 

Pageable pageable = PageRequest .of (0, 5); //page 1 and page size is 5

repository .findAll (RSQLSupport .toPredicate (filter , QUser .user )); 

repository .findAll (RSQLSupport .toPredicate (filter , QUser .user ), pageable ); 

// use static import 

import static br .com .archbase .query .rsql .common .RSQLSupport .*;

repository .findAll (toPredicate (filter , QUser .user )); 

repository .findAll (toPredicate (filter , QUser .user ), pageable ); 

// property path remap 

filter = "empNome=='demo';empId>100" ; // "empresa.nome=='demo';empresa.id>100" 

- protegendo nosso modelo de dominio 

Map <String , String > propertyPathMapper = new HashMap <> (); 

propertyPathMapper .put ("empId" , "empresa.id" ); 

propertyPathMapper .put ("empNome" , "empresa.code" ); 

repository .findAll (toPredicate (filter , QUser .user , propertyPathMapper )); 

repository .findAll (toPredicate (filter , QUser .user , propertyPathMapper ), 

pageable ); Conversor de valor personalizado 

# Operador personalizado e predicado 

# archbase-validation 

Validar dados é uma tarefa comum que ocorre em qualquer aplicativo, especialmente na camada 

de lógica de negócios. Quanto a alguns cenários bastante complexos, muitas vezes as mesmas 

validações ou validações semelhantes estão espalhadas em todos os lugares, portanto, é difícil 

reutilizar o código e quebrar a regra DRY .

SimpleDateFormat sdf = new SimpleDateFormat ("dd-MM-yyyy hh:mm:ss" ); 

RSQLJPASupport .addConverter (Date .class , s -> {

try {

return sdf .parse (s); 

} catch (ParseException e) {

return null ;

}

}); 

String rsql = "criarData=diaDaSemana='2'" ;

RSQLCustomPredicate <Long > customPredicate = new RSQLCustomPredicate <> (new 

ComparisonOperator ("=diaDaSemana=" ), Long .class , input -> {

Expression <Long > function =

input .getCriteriaBuilder (). function ("DAY_OF_WEEK" , Long .class ,

input .getPath ()); 

return input .getCriteriaBuilder (). lessThan (function , (Long )

input .getArguments (). get (0)); 

}); 

List <User > users = userRepository .findAll (toSpecification (rsql ,

Arrays .asList (customPredicate ))); 

String rsql = "data=emTorno='Mai'" ;

RSQLCustomPredicate <String > customPredicate = new RSQLCustomPredicate <> (new 

ComparisonOperator ("=emTorno=" ), String .class , input -> {

if ("Mai" .equals (input .getArguments (). get (0))) {

return input .getPath (). in (Arrays .asList ("Abril" , "Mai" , "Junho" )); 

}

return input .getCriteriaBuilder (). equal (input .getPath (), (String )

input .getArguments (). get (0)); 

}); 

List <User > users = userRepository .findAll (toSpecification (rsql ,

Arrays .asList (customPredicate ))); Para evitar duplicação e fazer validações o mais fácil possível, foi criado no archbase um módulo 

para fazer validações de uma forma fluente interface fluent e JSR 303 - especificação de bean 

validation , e aqui escolhemos o Hibernate Validator, que provavelmente é o mais conhecido 

como a implementação deste JSR. 

# Criar um modelo de domínio 

Crie um modelo de domínio ou você pode chamá-lo de entidade para ser validado 

posteriormente. Por exemplo, uma instância de carro é criada conforme abaixo. 

# Aplicando restrições 

Primeiro, pegue uma instância Car e, em seguida, use FluentValidator.checkAll() para obter 

uma instância stateful de FluentValidator. Em segundo lugar, vincule cada placa, fabricante e 

número de assentos a alguns validadores implementados de maneira personalizada, a validação 

deve ser aplicada aos campos de uma instância de carro em uma ordem específica em que são 

adicionados pelo on() método de chamada . Em terceiro lugar, a execução de uma operação 

intermediária como on() não performa de fato até que doValidate() seja chamada. Por fim, 

produza um simples Result contendo mensagens de erro das operações acima chamando  

> result(toSimple())

.

Você pode achar a interface fluente e as operações de estilo funcional muito semelhantes à API 

de fluxo que o JDK8 fornece. 

Vamos dar uma olhada em um dos validadores customizados - CarSeatCountValidator.                         

> public class Car {
> private String manufacturer;
> private String licensePlate;
> private int seatCount;
> // getter and setter...
> }
> Car car =getCar ();
> Result ret =FluentValidator .checkAll ()
> .on (car .getLicensePlate (), new CarLicensePlateValidator ())
> .on (car .getManufacturer (), new CarManufacturerValidator ())
> .on (car .getSeatCount (), new CarSeatCountValidator ())
> .doValidate (). result (toSimple ());
> System .out .println (ret );

Para realizar a validação da restrição, temos a classe de extensão da classe ValidatorHandler e

a interface Validator de implementação , de modo que CarSeatCountValidator seja capaz de 

fazer a validação de um valor int primitivo . 

Se a contagem de assentos for inferior a dois, ele retornará falso e uma mensagem de erro será 

inserida no contexto. Se a estratégia de falha rápida estiver ativada, a saída do resultado será: 

Se a contagem de assentos for validada com sucesso, ele retornará verdadeiro. Para que o 

processo passe para o próximo validador. Se nenhum dos campos violar qualquer restrição, a 

saída do resultado seria: 

# Validação básica passo a passo 

O Fluent Valiator é inspirado na Fluent Interface, que definiu um DSL interno na linguagem Java 

para os programadores usarem. Uma interface fluente implica que seu objetivo principal é tornar 

mais fácil FALAR e COMPREENDER. E é isso que o FluentValiator se dedica a fazer, para fornecer 

um código mais legível para você. 

Obtenha uma instância FluentValidator 

O FluentValiator é o principal ponto de entrada para realizar a validação. A primeira etapa para 

validar uma instância de entidade é obter uma instância FluentValidator. A única maneira é usar o  

> FluentValidator.checkAll()

método estático :                                   

> public class CarSeatCountValidator extends ValidatorHandler <Integer >implements
> Validator <Integer >{
> @Override
> public boolean validate (ValidatorContext context ,Integer t){
> if (t<2){
> context .addErrorMsg (String .format ("A contagem de assentos não é
> válida, valor inválido=%s" ,t));
> return false ;
> }
> return true ;
> }
> }
> Result{isSuccess=false, errors=[Seat count is not valid, invalid value=99]}
> Result{isSuccess=true, errors=null}
> FluentValidator .checkAll ();

Observe que o FluentValidator não é seguro para thread e com monitoração de estado. 

Posteriormente, aprenderemos como usar os diferentes métodos da classe FluentValidator. 

Criar validador personalizado 

Crie um validador implementando a Validator interface.  

> accept()

método é onde você pode determinar se deve realizar a validação no destino, 

portanto, se false for retornado, o método de validação não será chamado.  

> validate()

método é onde permanece o trabalho de validação principal. Retornando verdadeiro 

para que FluentValidator passe para o próximo validador se houver algum restante. Retornar 

false provavelmente irá parar o processo de validação apenas se a estratégia de falha rápida 

estiver habilitada. Você pode aprender mais sobre failover rápido e failover no próximo episódio.  

> onException()

fornece o poder de fazer alguns trabalhos de retorno de chamada sempre que 

uma exceção é lançada no método accept() ou validate(). 

Observe que se você não quiser implementar todos os métodos para o seu validador, você pode 

ter um validador implementado de forma personalizada estendendo-se ValidatorHandler como 

abaixo: 

Quando há erros, há duas maneiras de inserir as informações do erro no contexto e não se 

esqueça de retornar false no passado. 

Maneira simples de lidar com mensagens de erro:                                              

> public interface Validator <T>{
> boolean accept (ValidatorContext context ,Tt);
> boolean validate (ValidatorContext context ,Tt);
> void onException (Exception e,ValidatorContext context ,Tt);
> }
> public class CarSeatCountValidator extends ValidatorHandler <Integer >implements
> Validator <Integer >{
> @Override
> public boolean validate (ValidatorContext context ,Integer t){
> if (t<2){
> context .addErrorMsg (String .format ("Algo está errado com acontagem
> do assento do carro %s!" ,t));
> return false ;
> }
> return true ;
> }
> }

A maneira mais recomendada de colocar as informações de erro no contexto seria: 

Validar em campos ou instâncias 

As operações de validação são divididas em operações intermediárias e operação de terminal e são 

combinadas para formar algo como um estilo de interface fluente ou pipelines. 

As operações intermediárias são sempre preguiçosas , executando uma operação intermediária, 

como on() ou onEach() não realiza nenhuma validação até que a operação de terminal   

> doValidate ()

seja chamada. 

A operação do terminal, como doValidate() ou toResult() pode fazer validação real ou 

produzir um resultado. Após a operação do terminal ser realizada, o trabalho de validação é 

considerado concluído. 

O FluentValidator usa on() ou onEach() método para validar entidades inteiras ou apenas 

algumas propriedades da entidade. Você pode adicionar tantos destinos e seus validadores 

especificados quanto possível. 

O seguinte mostra a validação de algumas propriedades da instância Car. 

O seguinte mostra a validação na entidade Car. 

Ao aplicar restrições em um argumento de tipo Iterable, o FluentValidator validará cada elemento. 

O seguinte mostra a validação em uma coleção de entidade Car, cada um dos elementos será 

validado.                           

> context.addErrorMsg("Algo está errado com acontagem do assento do carro!");
> return false;
> context.addError(ValidationError.create("Algo está errado com acontagem do
> assento do
> carro!").setErrorCode(100).setField("seatCount").setInvalidValue(t));
> return false;
> FluentValidator .checkAll ()
> .on (car .getLicensePlate (), new CarLicensePlateValidator ())
> .on (car .getManufacturer (), new CarManufacturerValidator ())
> .on (car .getSeatCount (), new CarSeatCountValidator ());
> FluentValidator .checkAll ()
> .on (car ,new CarValidator ());

O seguinte mostra a validação em uma matriz de entidade Car. 

Falha rápido ou falha 

Use o failFast() método para evitar que os seguintes validadores sejam validados se algum 

validador falhar e retornar falso no doValidate() método. 

Use o failOver() método para ignorar as falhas para que todos os validadores funcionem em 

ordem. 

Em que condição fazer a validação 

Use o when() método em uma expressão regular especificada para determinar se a validação 

deve ser feita no destino ou não. Observe que o escopo de trabalho é aplicado apenas ao 

Validator anterior ou ao ValidatorChain adicionado. 

Executar validação 

Uma vez que o doValidate() método é chamado, significa realizar a validação de todas as 

restrições das entidades ou campos fornecidos. É aqui que todos os validadores são realmente 

executados. Na verdade, você também pode fazer algum trabalho de retorno de chamada. O 

procedimento será apresentado na seção Recursos avançados.               

> FluentValidator .checkAll ()
> .onEach (Lists .newArrayList (new Car (), new Car ()), new
> CarValidator ());
> FluentValidator .checkAll ()
> .onEach (new Car []{}, new CarValidator ());
> FluentValidator .checkAll (). failFast ()
> .on (car .getManufacturer (), new CarManufacturerValidator ())
> FluentValidator .checkAll (). failOver ()
> .on (car .getManufacturer (), new CarManufacturerValidator ())
> FluentValidator .checkAll (). failOver ()
> .on (car .getManufacturer (), new CarManufacturerValidator ()). when (a
> == b)

Obter resultado 

Em quase todos os casos, as operações de terminal como result() são ansiosas, porque 

precisamos saber o que acontece após todas as operações sequenciais. 

Se receber mensagens de erro simples pode se adequar à sua situação, você pode simplesmente 

extrair o resultado como abaixo. 

Existem alguns métodos úteis que você pode usar no resultado da validação. 

isSuccess() , getErrorMsgs() , getErrorNumber() .

O seguinte mostra a obtenção de um resultado mais complexo que não apenas contém 

mensagens de erro, mas também permite que você conheça o campo, o código de erro e o valor 

inválido se você os adicionou ao contexto. 

Por exemplo, a saída ComplexResult seria: 

toSimple() e toComplex() são métodos estáticos em 

com.baidu.unbiz.fluentvalidator.ResultCollectors .

Result ret = FluentValidator .checkAll () 

.on (car .getManufacturer (), new 

CarManufacturerValidator ()). when (true )

.doValidate (); 

Result ret = FluentValidator .checkAll () 

.on (car .getLicensePlate (), new CarLicensePlateValidator ()) 

.on (car .getManufacturer (), new CarManufacturerValidator ()) 

.on (car .getSeatCount (), new CarSeatCountValidator ()). failFast () 

.doValidate (). result (toSimple ()); 

ComplexResult ret = FluentValidator .checkAll (). failOver () 

.on (company , new CompanyCustomValidator ()) 

.doValidate (). result (toComplex ()); 

Result{isSuccess=false, errors=[ValidationError{errorCode=101, 

errorMsg='{departmentList} pode não ser nulo', field='departmentList', 

invalidValue=null}, ValidationError{errorCode=99, errorMsg='ID da empresa não é

válido, invalid value=-1', field='id', invalidValue=8}], timeElapsed(ms)=164} Recursos avançados 

ValidatorChain 

Além disso Validator , tem suporte para a aplicação de várias restrições da mesma instância ou 

valor. Você pode agrupar todos os validadores em um ValidatorChain . Isso é muito útil quando 

se trata de reutilizar alguns dos validadores básicos e combiná-los para construir uma cadeia. 

Especialmente se você estiver usando o framework Spring , achará muito fácil manter a cadeia 

dentro do contêiner. 

Validação baseada em anotação 

As restrições podem ser expressas anotando um campo de uma classe @FluentValidate que 

recebe várias classes de implementação de Validator interface como valor. O seguinte mostra 

um exemplo de configuração de nível de campo: 

Ao usar restrições de nível de campo, deve haver métodos getter para cada um dos campos 

anotados. 

A seguir, você pode usar o método configure(new SimpleRegistry()) que permitirá configurar 

onde procurar os validadores anotados para a instância do FluentValidator. Por padrão,  

> SimpleRegistry

está bem configurado, o que significa que não há necessidade de configurá-lo 

com antecedência.                          

> ValidatorChain chain =new ValidatorChain ();
> List <Validator >validators =new ArrayList <Validator >();
> validators .add (new CarValidator ());
> chain .setValidators (validators );
> Result ret =FluentValidator .checkAll (). on (car ,
> chain ). doValidate (). result (toSimple ());
> public class Car {
> @FluentValidate({CarManufacturerValidator.class})
> private String manufacturer;
> @FluentValidate({CarLicensePlateValidator.class})
> private String licensePlate;
> @FluentValidate({CarSeatCountValidator.class})
> private int seatCount;
> // getter and setter ...
> }

Observe que você pode usar onEach() para validar por meio de uma matriz ou coleção. 

Validação de grupos 

Os grupos permitem que você restrinja o conjunto de restrições aplicadas durante a validação. 

Por exemplo, a classe Add está nos grupos de @FluentValidate .

Então, ao aplicar restrições como abaixo, apenas o campo do fabricante é validado e os outros 

dois campos são ignorados. 

Ao aplicar restrições como antes, sem parâmetros no checkAll() método, todas as restrições 

são aplicadas na classe Car. 

Validação em cascata 

FluentValidator não só permite que você valide instâncias de classe única, mas também completa 

validação em cascata (gráficos de objetos). Para isso, basta anotar um campo ou propriedade que 

represente uma referência a outro objeto @FluentValid conforme demonstrado a seguir.                          

> Car car =getCar ();
> Result ret =FluentValidator .checkAll (). configure (new SimpleRegistry ())
> .on (car )
> .doValidate ()
> .result (toSimple ());
> public class Car {
> @FluentValidate (value ={CarManufacturerValidator .class }, groups =
> {Add .class })
> private String manufacturer ;
> @FluentValidate ({ CarLicensePlateValidator .class })
> private String licensePlate ;
> @FluentValidate ({ CarSeatCountValidator .class })
> private int seatCount ;
> }
> Result ret =FluentValidator .checkAll (new Class <?> [] {Add .class })
> .on (car )
> .doValidate ()
> .result (toSimple ());

O objeto List referenciado também será validado, pois o campo carList é anotado com  

> @FluentValid

. Observe que a validação em cascata também funciona para campos com tipos de 

coleção. Isso significa que cada elemento contido pode ser validado. Além disso, @FluentValid e 

> @FluentValidate

podem funcionar bem juntos. 

A validação em cascata é recursiva, ou seja, se uma referência marcada para validação em cascata 

aponta para um objeto que possui propriedades anotadas com @FluentValid , essas referências 

também serão acompanhadas pelo mecanismo de validação. 

Observe que o mecanismo de validação atualmente NÃO garantirá que nenhum loop infinito 

ocorra durante a validação em cascata, por exemplo, se dois objetos contiverem referências um 

ao outro. 

putAttribute2Context 

Use o putAttribute2Context() método, permite que você injete algumas das propriedades no 

validador ou cadeia de validadores do chamador - onde as validações são realizadas. 

Por exemplo, você pode colocar ignoreManufacturer como true no contexto e obter o valor 

invocando context.getAttribute(key, class type) em qualquer validador.                                    

> public class Garage {
> @FluentValidate ({ CarNotExceedLimitValidator .class })
> @FluentValid
> private List <Car >carList ;
> }
> FluentValidator .checkAll ()
> .putAttribute2Context ("ignoreManufacturer" ,true )
> .on (car .getManufacturer (), new CarManufacturerValidator ())
> .doValidate (). result (toSimple ());
> public class CarManufacturerValidator extends ValidatorHandler <String >
> implements Validator <String >{
> private ManufacturerService manufacturerService =new
> ManufacturerServiceImpl ();
> @Override
> public boolean validate (ValidatorContext context ,String t){
> Boolean ignoreManufacturer =context .getAttribute ("ignoreManufacturer" ,
> Boolean .class );
> if (ignoreManufacturer != null && ignoreManufacturer ){
> return true ;
> }
> // ...
> }

putClosure2Context 

putClosure2Context() Método de uso , oferece a funcionalidade de fechamento. Em algumas 

situações, quando o chamador deseja obter uma instância ou valor em que a invocação é 

delegada ao validador para fazer uma chamada real posteriormente, o que pode ser um trabalho 

demorado e complexo, e você não quer perder tempo ou qualquer código lógico para defini-lo 

novamente do chamador, é o melhor lugar para usar putClosure2Context (). 

Abaixo está um exemplo de reutilização de allManufacturers que é definido invocando o 

closure.executeAndGetResult() método dentro do validador, observe que 

manufacturerService.getAllManufacturers() pode executar uma chamada rpc. E o chamador 

pode obter o resultado simplesmente invocando o closure.getResult() método. 

}

Car car = getValidCar (); 

Closure <List <String >> closure = new ClosureHandler <List <String >> () {

private List <String > allManufacturers ;

@Override 

public List <String > getResult () {

return allManufacturers ;

}

@Override 

public void doExecute (Object ... input ) {

allManufacturers = manufacturerService .getAllManufacturers (); 

}

}; 

ValidatorChain chain = new ValidatorChain (); 

List <Validator > validators = new ArrayList <Validator >(); 

validators .add (new CarValidator ()); 

chain .setValidators (validators ); 

Result ret = FluentValidator .checkAll () 

.putClosure2Context ("manufacturerClosure" , closure )

.on (car , chain )

.doValidate (). result (toSimple ()); 

System .out .println (closure .getResult ()); ValidateCallback 

Até agora, ignoramos o argumento opcional ValidateCallback que o doValidate() método 

usa, mas é hora de dar uma olhada mais de perto. Um retorno de chamada pode ser colocado no 

doValidate() método como abaixo: 

onSuccess() método é chamado quando tudo vai bem. 

onFail() é chamado quando ocorrem falhas. 

onUncaughtException() método é chamado quando há uma exceção não detectada lançada 

durante o processo de validação. 

public class CarValidator extends ValidatorHandler <Car > implements 

Validator <Car > {

@Override 

public boolean validate (ValidatorContext context , Car car ) {

Closure <List <String >> closure =

context .getClosure ("manufacturerClosure" ); 

List <String > manufacturers = closure .executeAndGetResult (); 

if (!manufacturers .contains (car .getManufacturer ())) {

context .addErrorMsg (String .format (CarError .MANUFACTURER_ERROR .msg (), 

car .getManufacturer ())); 

return false ;

}

return FluentValidator .checkAll () 

.on (car .getLicensePlate (), new CarLicensePlateValidator ()) 

.on (car .getSeatCount (), new CarSeatCountValidator ()) 

.doValidate (). result (toSimple ()). isSuccess (); 

}

}

Result ret = FluentValidator .checkAll (). failOver () 

.on (car .getLicensePlate (), new CarLicensePlateValidator ()) 

.on (car .getManufacturer (), new CarManufacturerValidator ()) 

.on (car .getSeatCount (), new CarSeatCountValidator ()) 

.doValidate (new DefaulValidateCallback () {

@Override 

public void onFail (ValidatorElementList chained , List <String >

errorMsgs ) {

throw new CustomException ("ERRO AQUI" ); 

}

}). result (toSimple ()); Se você não deseja implementar todos os métodos da interface, pode simplesmente usar  

> DefaulValidateCallback

como o exemplo acima e implementar métodos seletivamente. 

RuntimeValidateException 

Por último, mas não menos importante, se houver alguma exceção que não seja tratada, uma 

RuntimeValidateException será lançada contendo a exceção de causa raiz do  

> doValidate()

método. 

Se houver alguma exceção que re-lançar de onException() ou  

> onUncaughtException()

método, uma RuntimeValidateException envolvendo a nova causa será 

lançada. 

Você pode tentar pegar ou manipular com o recurso Spring AOP por conta própria. 

# JSR 303 - Suporte para bean validation 

JSR 303 - Bean Validation define um modelo de metadados e API para validação de entidades e o 

Hibernate Validator é a implementação mais conhecida. 

Se você está se perguntando o que é a especificação JSR 303 - Bean Validation, passe algum 

tempo aqui antes de prosseguir. E obtenha um histórico de conhecimento do que é o Validador 

Hibernate através deste link .

Validar usando o Hibernate Validator 

Já que usar restrições baseadas em anotação pode ser uma maneira fácil de fazer a validação em 

uma instância. O Fluent Validator definitivamente alavancará o recurso útil fornecido pelo 

Hibernate Validator. 

Abaixo está um exemplo de aplicação de restrições baseadas em anotações na instância Car. As 

anotações @NotEmpty, @Pattern, @NotNull, @Size, @Length e @Valid são usadas para declarar 

as restrições. Para obter mais informações, consulte a documentação oficial do Hibernate 

Validator .                  

> public interface ValidateCallback {
> void onSuccess (ValidatorElementList validatorElementList );
> void onFail (ValidatorElementList validatorElementList ,List <String >
> errorMsgs );
> void onUncaughtException (Validator validator ,Exception e,Object target )
> throws Exception ;
> }
> public class Company {
> @NotEmpty

Para realizar a validação, pode-se usar FluentValidator sem nenhum problema. Basta usar o 

HibernateSupportedValidator como um dos validadores que deseja aplicar no alvo. 

Observe que HibernateSupportedValidator deve primeiro ter 

javax.validation.Validator definido em sua propriedade, caso contrário, 

HibernateSupportedValidator não funcionará normalmente. O seguinte mostra como obter a 

versão implementada do Hibernate javax.validation.Validator e configurá-la no 

HibernateSupportedValidator. Se você usa a estrutura Spring, certamente existem algumas 

maneiras de injetar javax.validation.Validator, e o procedimento é omitido aqui. 

@Pattern (regexp = "[0-9a-zA-Z\4e00-\u9fa5]+" )

private String name ;

@NotNull (message = "O establishTime não deve ser nulo" )

private Date establishTime ;

@NotNull 

@Size (min = 0, max = 10 )

@Valid 

private List <Department > departmentList ;

// getter and setter... 

}

public class Department {

@NotNull 

private Integer id ;

@Length (max = 30 )

private String name ;

// getter and setter... 

}

Result ret = FluentValidator.checkAll() 

.on(company, new HibernateSupportedValidator<Company> 

().setHibernateValidator(validator)) 

.on(company, new CompanyCustomValidator()) 

.doValidate().result(toSimple()); 

System.out.println(ret); Por exemplo, quando o nome da empresa é inválido, o resultado seria: 

Além disso, o HibernateSupportedValidator funciona bem com outros validadores 

personalizados, você pode adicionar validadores por meio de quantos on() desejar, conforme 

abaixo: 

Usar grupo e sequência de grupo 

No caso de alguém querer fazer uma validação baseada em anotação usando restrições de 

agrupamento , FluentValidator também é capaz, o checkAll() método leva um grupo de 

argumentos var-arg. 

No exemplo acima da classe Company, uma vez que nenhum grupo é especificado para qualquer 

anotação, o grupo padrão javax.validation.groups.Default é assumido. 

Se uma propriedade ceo for adicionada à classe Company e especificando o grupo como 

AddCompany.class, que você pode definir como uma interface: 

Ao usar FluentValidator.checkAll() , o ceo não será validado. Somente quando 

AddCompany.class atua como um membro do argumento var-arg que  

> FluentValidator.checkAll()

aceita, @Length funcionará, mas as outras restrições padrão não 

funcionarão. 

Abaixo está um exemplo se for necessário apenas validar a propriedade ceo.                                   

> Locale .setDefault (Locale .ENGLISH ); // especificando linguagem
> ValidatorFactory factory =Validation .buildDefaultValidatorFactory ();
> javax .validation .Validator validator =factory .getValidator ();
> Result{isSuccess=false, errors=[{name} must match "[0-9a-zA-Z\4e00-\u9fa5]+"]}
> Result ret =FluentValidator.checkAll()
> .on(company, new HibernateSupportedValidator<Company>
> ().setHibernateValidator(validator))
> .on(company, new CompanyCustomValidator())
> .doValidate().result(toSimple());
> @Length (message ="CEO da empresa não éválido" ,min =10 ,groups =
> {AddCompany .class })
> private String ceo ;

Abaixo está outro exemplo se for necessário validar a propriedade ceo e outras propriedades 

baseadas em anotação padrão. Por padrão, as restrições são avaliadas em nenhuma ordem 

específica, independentemente dos grupos aos quais pertencem. 

Se você deseja especificar a ordem de validação, você só precisa definir uma interface e anotar  

> @GroupSequence

como a seguir. Portanto, as restrições serão aplicadas em AddCompany.class 

primeiro e em outras propriedades a seguir. Observe que se pelo menos uma restrição falhar em 

um grupo sequenciado, nenhuma das restrições dos grupos a seguir na sequência será validada. 

Contraints 

O Hibernate vem com uma série de validadores prontos. Mas desenvolvemos mais alguns pra 

facilitar o trabalho na API. São eles:                                                             

> Result ret =FluentValidator .checkAll (AddCompany .class )
> .on (company ,new HibernateSupportedValidator <Company >
> (). setHibernateValidator (validator ))
> .on (company ,new CompanyCustomValidator ())
> .doValidate (). result (toSimple ());
> Result ret =FluentValidator .checkAll (Default .class ,AddCompany .class )
> .on (company ,new HibernateSupportedValidator <Company >
> (). setHibernateValidator (validator ))
> .on (company ,new CompanyCustomValidator ())
> .doValidate (). result (toSimple ());
> @GroupSequence ({ AddCompany .class ,Default .class })
> public interface GroupingCheck {
> }After FromDatetimeBeforeOrSameAsToDatetime NotInstanceOf
> Alpha GreaterOrEqualsThan NotNullIfAnotherFieldHasValue
> Alphanumeric GreaterThan NullIfAnotherFieldHasValue
> AlphanumericSpace IE Numeric
> AlphaSpace InstanceOf OneOfChars
> AsciiPrintable IPv4 OneOfDoubles
> Before IPv6 OneOfIntegers
> CNPJ IsDate OneOfLongs
> CompositionType JsAssert OneOfStrings
> ConstraintComposition JsAsserts Parseable
> CPF Length ParseableType
> CreditCardNumber LessOrEqualsThan Password
> Domain LessThan Range
> EAN LowerCase Required
> CurrencyFieldValidation LuhnCheck SafeHtml
> PercentFieldValidation Mod10Check StartsWith
> Email Mod11Check TituloEleitoral
> EndsWith ModCheck UpperCase
> EqualsFields NIT URL
> FieldMatch NotBlank UUID
> FromDateBeforeOrSameAsToDate NotEmpty

Por padrão as mensagens padrões de Bean Validation não retorna o nome do campo. Para 

resolver este problema 

criamos uma anotação @Label para que possamos adicionar um label para os campos. 

Foi criado uma classe para tratar estas mensagens e acrescentar o label: 

br.com.archbase.validation.message.ArchbaseMessageInterpolator 

E acrescentado nas configurações da API para usá-la: 

@Configuration 

public class BeanValidateConfiguration {

@Bean (name = "validator" )

public Validator getCurrentValidate () {

ValidatorFactory validatorFactory =

Validation .byProvider (HibernateValidator .class ). configure (). messageInterpolator 

(

new ArchbaseMessageInterpolator () 

). buildValidatorFactory (); 

return validatorFactory .usingContext () 

.messageInterpolator (new ArchbaseMessageInterpolator ()) 

.getValidator (); 

}archbase-error-handling 

# Tratamento de erros 

Um dos pontos principais no desenvolvimento de uma aplicação é como iremos reportar os erros 

para o usuário e até mesmo para o desenvolvedor. Muitas vezes o desenvolvedor está em área 

diferente, usando tecnologia diferente do desenvolvimento da API e para ajudar ainda não tem 

acesso acesso aos fontes. Como ele pode interpretar um ou procurar informações sobre o erro se 

isso não for tratado adequadamente e de uma forma fácil de entendimento? Muito difícil esta 

situação mas vemos isso com frequência gerando uma dependência muito grande entre as 

equipes na resolução dos problemas. Em muitas empresas acabam criando uma equipe de 

suporte que passam a fazer o papel de intermediador dos problemas. 

Visando tudo isso que foi dito foi desenvolvido novas funcionalidades para tratamento de erros 

na API usando os próprios ganchos deixados pelo Spring para esta customização. Com isso 

criamos uma forma de padronização dos erros e dos códigos do erros. 

# Requisição com erros de validação no body 

Suponhamos que haja um erro de validação para um corpo de uma requisição semelhante a este: 

Ao enviar um corpo JSON como este: 

Este é o padrão que o Spring Boot retorna: 

}

public static class PessoaRequestBody {

@Size (min = 10 )

private String nome ;

@NotBlank 

private String filmeFavorito ;

// getters and setters 

}

{

"nome" : "" ,

"filmeFavorito" : null 

}

{

"timestamp" : "2020-10-04T09:03:16" ,

"status" : 400 ,

"error" : "Bad Request" ,"errors" : [

{

"codes" : [

"Size.PessoaRequestBody.nome" ,

"Size.nome" ,

"Size.java.lang.String" ,

"Size" 

], 

"arguments" : [

{

"codes" : [

"pessoaRequestBody.nome" ,

"nome" 

], 

"arguments" : null ,

"defaultMessage" : "nome" ,

"code" : "nome" 

}, 

2147483647 ,

10 

], 

"defaultMessage" : "size must be between 10 and 2147483647" ,

"objectName" : "pessoaRequestBody" ,

"field" : "nome" ,

"rejectedValue" : "" ,

"bindingFailure" : false ,

"code" : "Size" 

}, 

{

"codes" : [

"NotBlank.pessoaRequestBody.filmeFavorito" ,

"NotBlank.filmeFavorito" ,

"NotBlank.java.lang.String" ,

"NotBlank" 

], 

"arguments" : [

{

"codes" : [

"pessoaRequestBody.filmeFavorito" ,

"filmeFavorito" 

], 

"arguments" : null ,

"defaultMessage" : "filmeFavorito" ,

"code" : "filmeFavorito" 

}

], 

"defaultMessage" : "must not be blank" ,

"objectName" : "pessoaRequestBody" ,

"field" : "filmeFavorito" ,Esta seria a forma que imaginamos ser melhor e que foi implementada: 

# Substituindo o código de erro para uma anotação de validação 

É possível substituir os códigos padrões que são usados para cada erro de validação ocorrido nos 

campos. 

Fazendo da seguinte forma: 

Os erros devem começar com archbase.error.handling.codes e sufixo com o nome da 

anotação de validação usada ( @Size neste exemplo). 

"rejectedValue" : null ,

"bindingFailure" : false ,

"code" : "NotBlank" 

}

], 

"message" : "Validation failed for object='pessoaRequestBody'. Error count: 

2" ,

"path" : "/api/exception/invalidbody" 

}

{

"code" : "VALIDATION_FAILED" ,

"message" : "Validation failed for object='pessoaRequestBody'. Error count: 

2" ,

"fieldErrors" : [

{

"code" : "INVALID_SIZE" ,

"property" : "nome" ,

"message" : "size must be between 10 and 2147483647" ,

"rejectedValue" : "" 

}, 

{

"code" : "REQUIRED_NOT_BLANK" ,

"property" : "filmeFavorito" ,

"message" : "must not be blank" ,

"rejectedValue" : null 

}

]

}

archbase.error.handling.codes.Size = SIZE_REQUIREMENT_NOT_MET 

{

"code" : "VALIDATION_FAILED" ,

"message" : "Validation failed for object='pessoaRequestBody'. Error count: 

2" ,Substituindo o código de erro para um campo específico 

É possível configurar um código de erro específico que só será usado para uma combinação de 

um campo com uma anotação de validação. 

Suponha que você adicione um regex para validar as regras de senha: 

Por padrão, esse erro estaria na resposta: 

"fieldErrors" : [

{

"code" : "SIZE_REQUIREMENT_NOT_MET" ,

"property" : "nome" ,

"message" : "size must be between 10 and 2147483647" ,

"rejectedValue" : "" 

}, 

{

"code" : "REQUIRED_NOT_BLANK" ,

"property" : "filmeFavorito" ,

"message" : "must not be blank" ,

"rejectedValue" : null 

}

]

}

public class CriarUsuarioRequestBody {

@Pattern (".*{8}" )

private String senha ;

// getters and setters 

}

{

"code" : "VALIDATION_FAILED" ,

"message" : "Validation failed for object='criarUsuarioRequestBody'. Error 

count: 1" ,

"fieldErrors" : [

{

"code" : "REGEX_PATTERN_VALIDATION_FAILED" ,

"property" : "senha" ,

"message" : "must match \".*{8}\"" ,

"rejectedValue" : "" 

}

]

}Se usarmos archbase.error.handling.codes.Pattern para a substituição, todas as 

@Pattern anotações em todo o aplicativo usarão um código diferente. Se quisermos substituir 

isso apenas para os campos nomeados senha , podemos usar: 

Isto resulta em: 

# Substituindo a mensagem de erro de validação por anotação 

É possível substituir as mensagens padrão que são usadas para cada erro de campo. 

Você precisa usar o seguinte: 

Usamos o erro começando com archbase.error.handling.messages e sufixo com o nome da 

anotação de validação usada ( @NotBlank neste exemplo). 

archbase.error.handling.codes.senha.Pattern =

PASSWORD_COMPLEXITY_REQUIREMENTS_NOT_MET 

{

"code" : "VALIDATION_FAILED" ,

"message" : "Validation failed for object='criarUsuarioRequestBody'. Error 

count: 1" ,

"fieldErrors" : [

{

"code" : "PASSWORD_COMPLEXITY_REQUIREMENTS_NOT_MET" ,

"property" : "senha" ,

"message" : "must match \".*{8}\"" ,

"rejectedValue" : "" 

}

]

}

archbase.error.handling.messages.NotBlank = O campo não deve ficar em branco. 

{

"code" : "VALIDATION_FAILED" ,

"message" : "Validation failed for object='criarUsuarioRequestBody'. Error 

count: 1" ,

"fieldErrors" : [

{

"code" : "REQUIRED_NOT_BLANK" ,

"property" : "nome" ,

"message" : "O campo não deve ficar em branco" ,//(1) 

"rejectedValue" : "" 

}

]

}Substituindo a mensagem de erro para um campo específico 

É possível configurar uma mensagem de erro específica que só será utilizada para uma 

combinação de um campo com uma anotação de validação. 

Suponha que você adicione um regex para validar as regras de senha: 

Por padrão, esse erro estará na resposta: 

Usamos o erro começando com archbase.error.handling.messages.Pattern para a 

substituição, todas as @Pattern anotações em todo o aplicativo usarão uma mensagem 

diferente. Se quisermos substituir isso apenas para os campos nomeados senha , podemos usar: 

Isto resulta em: 

public class CriarUsuarioRequestBody {

@Pattern (".*{8}" )

private String senha ;

// getters and setters 

}

{

"code" : "VALIDATION_FAILED" ,

"message" : "Validation failed for object='criarUsuarioRequestBody'. Error 

count: 1" ,

"fieldErrors" : [

{

"code" : "REGEX_PATTERN_VALIDATION_FAILED" ,

"property" : "senha" ,

"message" : "must match \".*{8}\"" ,

"rejectedValue" : "" 

}

]

}

archbase.error.handling.messages.senha.Pattern = As regras de complexidade de 

senha não foram atendidas. A senha deve ter no mínimo 8 caracteres. Erros globais 

Se houvesse erros globais ao lado dos erros relacionados ao campo, eles apareceriam na 

propriedade globalErrors :

O code e message usado é baseado na anotação que foi usada para validação: 

{

"code" : "VALIDATION_FAILED" ,

"message" : "Validation failed for object='criarUsuarioRequestBody'. Error 

count: 1" ,

"fieldErrors" : [

{

"code" : "REGEX_PATTERN_VALIDATION_FAILED" ,

"property" : "senha" ,

"message" : "As regras de complexidade de senha não foram atendidas. A

senha deve ter no mínimo 8 caracteres." ,

"rejectedValue" : "" 

}

]

}

{

"code" : "VALIDATION_FAILED" ,

"message" : "Validation failed for object='pessoaRequestBody'. Error count: 

2" ,

"globalErrors" : [

{

"code" : "ValidCliente" ,

"message" : "Cliente inválido" 

}, 

{

"code" : "ValidCliente" ,

"message" : "UserAlreadyExists" 

}

]

}

@Target (ElementType .TYPE )

@Retention (RetentionPolicy .RUNTIME )

@Constraint (validatedBy = ClienteValidator .class )

public @interface ValidCliente {

String message () default "cliente inválido" ;

Class <?> [] groups () default {}; 

Class <? extends Payload >[] payload () default {}; 

}Bem como o modelo que é usado no próprio validador: 

# Substituindo códigos em erros de campo e erros globais 

Usando a propriedade archbase.error.handling.codes em application.properties , os 

códigos usados podem ser substituídos. Suponha que você tenha este: 

Então, a resposta resultante para o exemplo de erros de campo será: 

E para o exemplo de erros globais: 

public class ClienteValidator implements ConstraintValidator <ValidCliente ,

CreateClienteFormData > {

@Override 

public boolean isValid (CreateClienteFormData formData ,

ConstraintValidatorContext context ) {

if (...) {

context .buildConstraintViolationWithTemplate ("UserAlreadyExists" ). addConstraint 

Violation (); 

}

}

}

archbase.error.handling.codes.NotBlank =NOT_BLANK 

archbase.error.handling.codes.Size =BAD_SIZE 

archbase.error.handling.codes.ValidCustomer =INVALID_CLIENTE 

{

"code" : "VALIDATION_FAILED", 

"message" : "Validation failed for object ='exemploRequestBody'. Error count :

2", 

"fieldErrors" : [

{

"code" : "BAD_SIZE", 

"property" : "name", 

"message" : "size must be between 10 and 2147483647", 

"rejectedValue" : "" 

}, 

{

"code" : "NOT_BLANK", 

"property" : "filmeFavorito", 

"message" : "must not be blank", 

"rejectedValue" : null 

}

]

}Se você quiser alterar a mensagem para os erros globais, o mecanismo padrão do Spring 

continua funcionando. 

Portanto, use {} para indicar que o Spring deve pesquisar o arquivo messages.properties :

Agora adicione a tradução ao messages.properties :

Isto resulta em: 

{

"code" : "VALIDATION_FAILED", 

"message" : "Validation failed for object ='pessaRequestBody'. Error count : 2", 

"globalErrors" : [

{

"code" : "INVALID_CLIENTE", 

"message" : "Cliente inválido" 

}, 

{

"code" : "INVALID_CLIENTE", 

"message" : "UserAlreadyExists" 

}

]

}

context .buildConstraintViolationWithTemplate ("

{UserAlreadyExists}" ). addConstraintViolation (); 

UserAlreadyExists =The user already exists 

{

"code" : "VALIDATION_FAILED" ,

"message" : "Validation failed for object='exemploRequestBody'. Error count: 

2" ,

"globalErrors" : [

{

"code" : "INVALID_CLIENTE" ,

"message" : "Cliente inválido" 

}, 

{

"code" : "INVALID_CLIENTE" ,

"message" : "The user already exists" 

}

]

}Tipo de exceções de conversão 

As exceções de conversão de tipo como MethodArgumentTypeMismatchException e

TypeMismatchException terão algumas informações extras sobre a classe que era esperada e o 

valor que foi rejeitado: 

# Exceções de bloqueio otimistas 

Quando um org.springframework.orm.ObjectOptimisticLockingFailureException é

lançada, a resposta resultante será algo como: 

# Exceções Spring Security 

Se Spring Security estiver no caminho de classes fornecido para aplicar o tratamentos de erros, 

essas exceções serão tratadas. Eles terão apenas um code e um message .

Por exemplo: 

{

"code" : "ARGUMENT_TYPE_MISMATCH" ,

"message" : "Failed to convert value of type 'java.lang.String' to required 

type 'com.example.user.UserId'; nested exception is 

org.springframework.core.convert.ConversionFailedException: Failed to convert 

from type [java.lang.String] to type 

[@org.springframework.web.bind.annotation.PathVariable com.example.user.UserId] 

for value 'fake_UUID'; nested exception is java.lang.IllegalArgumentException: 

Invalid UUID string: fake_UUID" ,

"expectedType" : "br.com.exemplo.usuario.UsuarioId" ,

"property" : "usuarioId" ,

"rejectedValue" : "fake_UUID" 

}

{

"code" : "OPTIMISTIC_LOCKING_ERROR" ,

"message" : "Object of class [br.com.exemplo.usuario.Usuario] with identifier 

[87518c6b-1ba7-4757-a5d9-46e84c539f43]: optimistic locking failed" ,

"identifier" : "87518c6b-1ba7-4757-a5d9-46e84c539f43" ,

"persistentClassName" : "br.com.exemplo.usuario.Usuario" 

}

{

"code" : "ACCESS_DENIED" ,

"message" : "Access is denied" 

}Exceções personalizadas 

Se você definir uma exceção personalizada e lançá-la de um método no @RestController , o 

Spring Boot irá transformá-la em uma exceção 500 INTERNAL SERVER ERROR por padrão. O 

status da resposta é facilmente alterado usando @ResponseStatus :

Esta é a resposta padrão do Spring Boot para isso: 

Da forma proposta seria assim: 

Agora podemos melhorar ainda mais a resposta de duas maneiras: 

Defina o código a ser usado em vez do nome totalmente qualificado da classe Exception; 

Adicione campos adicionais para enriquecer a resposta do erro. 

# Definir o código de erro 

Para substituir o código de erro, alteramos a classe de exceção para: 

@ResponseStatus (HttpStatus .NOT_FOUND )

public class UsuarioNaoEncontradoException extends RuntimeException {

public UsuarioNaoEncontradoException (UserId userId ) {

super (String .format ("Could not find user with id %s" , userId )); 

}

}

{

"timestamp" : "2020-07-02T06:06:41.400+0000" ,

"status" : 404 ,

"error" : "Not Found" ,

"message" : "Could not find user with id UserId{id=b8285c14-06bd-41db-a4df-

724d0d1e590b}" ,

"path" : "/api/exception/test" 

}

{

"code" : "br.com.exemplo.usuario.UsuarioNaoEncontradoException" ,

"message" : "Could not find user with id UserId{id=a6cd68f2-b305-4b2d-8442-

ee1696e6eb8f}" 

}A resposta seria: 

# Definindo o código de erro por meio de propriedades 

Também é possível definir o código de erro via application.properties .

Suponha que algum método lance uma com.amazonaws.AmazonClientException . Não podemos 

fazer anotações na classe @ResponseErrorCode , pois é um código de terceiros. 

Para definir um código de erro, adicione o seguinte ao seu application.properties :

A resposta seria: 

# Adicionando propriedades extras na resposta 

Para adicionar propriedades extras na resposta de erro, você pode anotar campos e / ou 

métodos em suas classes de exceção com @ResponseErrorProperty . Por exemplo: 

@ResponseStatus (HttpStatus .NOT_FOUND )

@ResponseErrorCode ("USER_NOT_FOUND" ) // (1) 

public class UsuarioNaoEncontradoException extends RuntimeException {

public UsuarioNaoEncontradoException (UserId userId ) {

super (String .format ("Could not find user with id %s" , userId )); 

}

}

{

"code" : "USER_NOT_FOUND" ,

"message" : "Could not find user with id UserId{id=8c7fb13c-0924-47d4-821a-

36f73558c898}" 

}

archbase.error.handling.codes.com.amazonaws.AmazonClientException =CLOUD_PROVIDE 

R_ERROR 

{

"code" : "CLOUD_PROVIDER_ERROR" ,

"message" : "Some exception message from Amazon here" 

}

@ResponseStatus (HttpStatus .NOT_FOUND )

@ResponseErrorCode ("USER_NOT_FOUND" )

public class UsuarioNaoEncontradoException extends RuntimeException {

private final UserId userId ;

public UsuarioNaoEncontradoException (UserId userId ) {A resposta seria: 

O @ResponseErrorProperty pode ser usado em um método ou em um campo. 

# Substituindo o nome da propriedade 

Também é possível substituir o nome da propriedade que será usado na resposta usando o 

argumento value da anotação. 

A resposta seria: 

super (String .format ("Could not find user with id %s" , userId )); 

this .userId = userId ;

}

@ResponseErrorProperty // (1) 

public String getUserId () {

return userId .asString (); 

}

}

{

"code" : "USER_NOT_FOUND" ,

"message" : "Could not find user with id UserId{id=8c7fb13c-0924-47d4-821a-

36f73558c898}" ,

"userId" : "8c7fb13c-0924-47d4-821a-36f73558c898" 

}

@ResponseStatus (HttpStatus .NOT_FOUND )

@ResponseErrorCode ("USER_NOT_FOUND" )

public class UsuarioNaoEncontradoException extends RuntimeException {

... 

@ResponseErrorProperty ("id" )

public String getUserId () {

return userId .asString (); 

}

}

{

"code" : "USER_NOT_FOUND" ,

"message" : "Could not find user with id UserId{id=8c7fb13c-0924-47d4-821a-

36f73558c898}" ,

"id" : "8c7fb13c-0924-47d4-821a-36f73558c898" 

}Propriedade Descrição Padrão           

> archbase.error.handling.enabled Permite habilitar ou desabilitar o tratamento de erros true
> archbase.error.handling.exception-
> logging
> Permite definir como a exceção deve ser registrada. Um dos
> seguintes: NO_LOGGING ,MESSAGE_ONLY ,WITH_STACKTRACE MESSAGE_ONLY
> archbase.error.handling.codes Permite definir o código que deve ser usado para o nome
> totalmente qualificado de uma Exception

# Propriedades 

# archbase-plugin-manager 

Um plugin é uma maneira de terceiros estenderem a funcionalidade de um aplicativo. Um plug-in 

implementa pontos de extensão declarados pelo aplicativo ou outros plug-ins. Além disso, um 

plugin pode definir pontos de extensão. 

Você pode marcar qualquer interface ou classe abstrata como um ponto de extensão (com 

interface de marcador ExtensionPoint ) e especificar que uma classe é uma extensão com 

anotação @Extension .

# Componentes 

ArchbasePlugin é a classe base para todos os tipos de plug-ins. Cada plugin é carregado em 

um carregador de classes separado para evitar conflitos. 

O ArchbasePluginManager é usado para todos os aspectos do gerenciamento de plug-ins 

(carregar, iniciar, parar). Você pode usar uma implementação embutida 

DefaultArchbasePluginManager ou pode implementar um gerenciador de plug-ins 

customizado começando por AbstractArchbasePluginManager (implementar apenas 

métodos de fábrica). 

ArchbasePluginLoader carrega todas as informações (classes) necessárias para um plugin. 

ExtensionPoint é um ponto no aplicativo onde o código personalizado pode ser chamado. É 

um marcador de interface java. 

Qualquer interface java ou classe abstrata pode ser marcada como um ponto de extensão 

(implementa ExtensionPoint interface). 

Extension é uma implementação de um ponto de extensão. É uma anotação java em uma 

classe. 

É muito simples adicionar o framework em seu aplicativo: 

public static void main (String [] args ) {

... 

ArchbasePluginManager pluginManager = new DefaultArchbasePluginManager (); 

pluginManager .loadPlugins (); 

pluginManager .startPlugins (); 

... 

}No código acima, criei um DefaultArchbasePluginManager (é a implementação padrão da 

interface ArchbasePluginManager ) que carrega e inicia todos os plug-ins ativos (resolvidos). 

Cada plugin disponível é carregado usando um carregador de classe java diferente, 

PluginClassLoader .

O PluginClassLoader contém apenas classes encontradas em PluginClasspath ( classes padrão e 

pastas lib ) de plug-ins e classes de tempo de execução e bibliotecas dos plug-ins necessários / 

dependentes. Este carregador de classes é um Último ClassLoader Pai - ele carrega as classes dos 

jars do plug-in antes de delegar ao carregador de classes pai. 

Os plug-ins são armazenados em uma pasta. Você pode especificar a pasta de plug-ins no 

construtor de DefaultArchbasePluginManager. Se a pasta de plug-ins não for especificada, o local 

será retornado por System.getProperty("archbase.pluginsDir", "plugins") .

A estrutura da pasta de plug-ins é: 

plugin1.zip (ou pasta plugin1) 

plugin2.zip (ou pasta plugin2) 

Na pasta de plug-ins você pode colocar um plug-in como pasta ou arquivo (zip). Uma pasta de 

plug-in tem esta estrutura por padrão:  

> classes

pasta  

> lib

pasta (opcional - se o plug-in usou bibliotecas de terceiros) 

O gerenciador de plug-ins pesquisa metadados de plug-ins usando um PluginDescriptorFinder .

DefaultArchbasePluginDescriptorFinder é um “link” para ManifestPluginDescriptorFinder 

que pesquisa descritores de plug-ins no arquivo MANIFEST.MF. 

Neste caso, o classes/META-INF/MANIFEST.MF arquivo se parece com: 

No manifesto acima, descrevemos um plugin com id bemvindo-plugin , com classe  

> br.com.archbase.plugin.demo.BemVindoPlugin

, com versão 1.0.0 e com dependências para 

plugins x, y, z .

NOTA: A versão do plugin deve ser compatível com o Controle de Versão Semântico               

> Manifest-Version :1.0
> Archiver-Version :Plexus Archiver
> Created-By :Apache Maven
> Built-By :Archbase
> Build-Jdk :1.6.0_17
> Plugin-Class :br.com.archbase.plugin.demo.BemVindoPlugin
> Plugin-Dependencies :x, y, z
> Plugin-Id :bemvindo-plugin
> Plugin-Provider :Archbase
> Plugin-Version :1.0.0

Você pode definir um ponto de extensão em seu aplicativo usando o marcador de interface 

ExtensionPoint .

Outro componente interno importante é ExtensionFinder, que descreve como o gerenciador de 

plug-ins descobre extensões para os pontos de extensão. 

DefaultExtensionFinder procura extensões usando a anotação de extensão .

DefaultExtensionFinder procura extensões em todos os arquivos de índice de extensões META- 

> INF/extensions.idx

. O framerwork usa Java Annotation Processing para processar em tempo 

de compilação todas as classes anotadas com @Extension e para produzir o arquivo de índice de 

extensões. 

No código acima, fornecemos uma extensão para o ponto de extensão Saudacao .

Você pode recuperar todas as extensões de um ponto de extensão com:                                     

> public interface Saudacao extends ExtensionPoint {
> String getSaudacao ();
> }
> public class BemVindoPlugin extends Plugin {
> public BemVindoPlugin (PluginWrapper wrapper ){
> super (wrapper );
> }
> @Extension
> public static class Saudacao implements Greeting {
> public String getSaudacao () {
> return "Bem vindo" ;
> }
> }
> }
> List <Saudacao >saudacoes =pluginManager .getExtensions (Saudacao .class );
> for (Saudacao saudacao :saudacoes ){
> System .out .println (">>> "+saudacao .getSaudacao ());
> }

O resultado é: 

Você pode injetar seu componente personalizado (por exemplo, PluginDescriptorFinder, 

ExtensionFinder, PluginClasspath,…) em DefaultArchbasePluginManager apenas substituir  

> create...

métodos (padrão de método de fábrica). 

Exemplo: 

e no repositório do plug-in, você deve ter um arquivo plugin.properties com o conteúdo abaixo: 

Você pode controlar o método createExtensionFactory de substituição de criação de instância 

de extensão em DefaultArchbaseExtensionFinder. Além disso, você pode controlar o  

> createPluginFactory

método de substituição de criação de instância do plug-in em 

DefaultArchbaseExtensionFinder. 

NOTA: Se seu aplicativo não encontrou extensões, certifique-se de que você tenha um arquivo 

com o nome extensions.idx gerado pelo framework no jar do plugin. É mais provável que 

sejam alguns problemas com o mecanismo de processamento de anotações do Java. Uma 

solução possível para resolver seu problema é adicionar uma configuração à sua compilação de 

maven. O maven-compiler-plugin pode ser configurado para fazer isso da seguinte maneira:           

> >>> Bem vindo
> >>> Olá
> protected PluginDescriptorFinder createPluginDescriptorFinder() {
> return new PropertiesPluginDescriptorFinder();
> }
> plugin.class =br.com.archbase.plugin.demo.WelcomePlugin
> plugin.dependencies =x, y, z
> plugin.id =welcome-plugin
> plugin.provider =Archbase
> plugin.version =1.0.0

# Carregando classes 

Os carregadores de classes são responsáveis por carregar classes Java durante o tempo de 

execução de forma dinâmica para a JVM (Java Virtual Machine). Os carregadores de classes fazem 

parte do Java Runtime Environment. Quando a JVM solicita uma classe, o carregador de classes 

tenta localizar a classe e carregar a definição da classe no tempo de execução usando o nome de 

classe totalmente qualificado. O java.lang.ClassLoader.loadClass() método é responsável 

por carregar a definição da classe no tempo de execução. Ele tenta carregar a classe com base em 

um nome totalmente qualificado. 

Se a classe ainda não estiver carregada, ele delega a solicitação ao carregador de classe pai. Esse 

processo acontece recursivamente. 

O framework usa PluginClassLoader para carregar classes de plugins. 

Portanto, cada plugin disponível é carregado usando um diferente PluginClassLoader .

Uma instância de PluginClassLoader deve ser criada pelo gerenciador de plug-ins para cada 

plug-in disponível. 

Por padrão, este carregador de classes é um Último ClassLoader Pai - ele carrega as classes dos 

jars do plug-in antes de delegar ao carregador de classes pai. 

Por padrão (pai por último), PluginClassLoader usa a estratégia abaixo quando uma solicitação 

de classe de carga é recebida por meio do loadClass(String className) método: 

se a classe for uma classe do sistema ( className começa com java. ), delegue ao 

carregador do sistema; 

se a classe fizer parte do mecanismo de plug-in ( className começa com  

> br.com.archbase.plugin

), use o carregador de classe pai ( ApplicationClassLoader em 

geral); 

tente carregar usando a instância atual do PluginClassLoader; 

se o PluginClassLoader atual não pode carregar a classe, tente delegar para  

> PluginClassLoader

s das dependências do plugin 

delegar carga de classe ao carregador de classe pai 

> <plugin >
> <groupId >org.apache.maven.plugins </ groupId >
> <artifactId >maven-compiler-plugin </ artifactId >
> <version >2.5.1 </ version >
> <configuration >
> <annotationProcessors >
> <annotationProcessor >br.com.archbase.plugin.manager.processor.ExtensionAnnotat
> ionProcessor </ annotationProcessor >
> </ annotationProcessors >
> </ configuration >
> </ plugin >

Use o parentFirst parâmetro de PluginClassLoader para alterar a estratégia de 

carregamento. 

Por exemplo, se eu quiser usar uma estratégia Parent First em meu aplicativo, tudo que preciso 

para conseguir isso é: 

Se você quiser saber qual plugin carregou uma classe específica, você pode usar: 

O framework usa por padrão um carregador de classes separado para cada plugin, mas isso não 

significa que você não pode usar o mesmo carregador de classes (provavelmente o carregador de 

classes do aplicativo) para todos os plug-ins. Se seu aplicativo requer este caso de uso, o que você 

deve fazer é retornar o mesmo carregador de classes de ArchbasePluginLoader.loadPlugin :

Se você usar, DefaultArchbasePluginManager pode escolher substituir  

> DefaultArchbasePluginManager.createPluginLoader

e / ou  

> DefaultArchbasePluginManager.createClassLoader

.                    

> new DefaultArchbasePluginManager () {
> @Override
> protected PluginClassLoader createPluginClassLoader (Path pluginPath ,
> PluginDescriptor pluginDescriptor ){
> return new PluginClassLoader (pluginManager ,pluginDescriptor ,
> getClass (). getClassLoader (), true );
> }
> };
> pluginManager.whichPlugin(MinhaClasse.class);
> public interface ArchbasePluginLoader {
> boolean isApplicable (Path pluginPath );
> ClassLoader loadPlugin (Path pluginPath ,PluginDescriptor pluginDescriptor );
> }

# Empacotamento 

Depois de desenvolver e testar seu plug-in, você deve empacotar e lançar. 

Atualmente, o framework suporta dois tipos de pacotes integrados 

fat/shade/one-jar file ( .jar )

zip file com lib e classes directories (.zip) 

Para instalar um plugin em seu aplicativo, você precisa adicioná-lo ao diretório de plugins 

(pluginsRoot). Seu conteúdo do diretório plugins pode ser semelhante a: 

ou 

se você usar .jar formato de pacote de plug-in. 

Se quiser, você pode misturar vários formatos de embalagem. Por exemplo, por padrão, você 

pode misturar .jar plug- .zip ins com plug-ins: 

Recomendamos que você use .jar porque é o mais simples e é um formato padrão em Java. 

Todos os plug-ins são carregados por ArchbasePluginManager do diretório plugins .

Você pode especificar outro local usando a archbase.pluginsDir do sistema ( - 

> Darchbase.pluginsDir=plugins

) ou programaticamente ao criar  

> DefaultArchbasePluginManager

.                  

> $tree plugins
> plugins
> !"" disabled.txt
> !"" enabled.txt
> !"" demo-plugin1-2.4.0.zip
> $"" demo-plugin2-2.4.0.zip
> $tree plugins
> plugins
> !"" disabled.txt
> !"" enabled.txt
> !"" demo-plugin1-2.4.0.jar
> $"" demo-plugin2-2.4.0.jar
> $tree plugins
> plugins
> !"" disabled.txt
> !"" enabled.txt
> !"" demo-plugin1-2.4.0.jar
> $"" demo-plugin2-2.4.0.zip

# Plugins 

Sobre plugins 

Um plugin agrupa classes e bibliotecas Java (arquivos JAR), que podem ser carregados / 

descarregados pelo framework no tempo de execução do aplicativo. 

Caso você não precise carregar / descarregar certas partes do código Java em seu aplicativo em 

tempo de execução, não é estritamente necessário usar plug-ins. Você também pode fazer uso 

apenas de extensões e colocar as classes compiladas no classpath do aplicativo (as chamadas 

extensões do sistema ). 

Como os plug-ins são definidos 

Cada plugin deve fornecer uma classe, que é derivada da classe 

br.com.archbase.plugin.manager.ArchbasePlugin :

import br .com .archbase .plugin .manager .ArchbasePlugin ;

import br .com .archbase .plugin .manager .PluginWrapper ;

public class MeuPlugin extends ArchbasePlugin {

public MeuPlugin (PluginWrapper wrapper ) {

super (wrapper ); 

}

@Override 

public void start () {

// Este método é chamado pelo aplicativo quando o plugin é iniciado. 

}

@Override 

public void stop () {

// Este método é chamado pelo aplicativo quando o plugin é

interrompido. 

}

@Override 

public void delete () {

// Este método é chamado pelo aplicativo quando o plugin é excluído. 

}

}Como os metadados do plugin são definidos 

Para tornar o plugin carregável pelo framework, você também deve fornecer alguns metadados. 

O nome de classe totalmente qualificado da classe de plug-in (derivado de 

br.com.archbase.plugin.manager.ArchbasePlugin ) (opcional) .

O identificador exclusivo do plugin. 

A versão do plugin de acordo com a Especificação de Controle de Versão Semântica .

A versão necessária do aplicativo de acordo com a Especificação de Controle de Versão 

Semântica (opcional) .

Dependências com outros plug-ins (opcional) .

Uma descrição do plugin (opcional) .

O nome do provedor / autor do plugin (opcional) .

A licença do plugin (opcional) .

Um nome de classe de plugin é opcional. Você pode criar uma classe de plug-in apenas se quiser 

ser notificado quando seu plug-in for started , stopped ou deleted .

Existem várias maneiras de fornecer metadados para um plugin. 

Fornece metadados de plug-in por meio de MANIFEST.MF 

Adicione o seguinte conteúdo ao META-INF/MANIFEST.MF arquivo do plugin: 

Caso esteja usando o Maven, você pode definir esses valores em seu pom.xml via maven-jar-

plugin :

Plugin-Class : br.com.archbase.plugin.demo.BemVindoPlugin 

Plugin-Id : bemvindo-plugin 

Plugin-Version : 1.0.0 

Plugin-Requires : 1.0.0 

Plugin-Dependencies : x, y, z

Plugin-Description : Meu plugin exemplo 

Plugin-Provider : Archbase 

Plugin-License : Apache License 2.0 

<plugin >

<groupId >org.apache.maven.plugins </ groupId >

<artifactId >maven-jar-plugin </ artifactId >

<configuration >

<archive >

<manifest >

<addDefaultImplementationEntries >true </ addDefaultImplementationEntries >

<addDefaultSpecificationEntries >true </ addDefaultSpecificationEntries >

</ manifest >

<manifestEntries >Ou via maven-assembly-plugin :

<Plugin-

Class >br.com.archbase.plugin.demo.BemVindoPlugin </ Plugin-Class >

<Plugin-Id >bemvindo-plugin </ Plugin-Id >

<Plugin-Version >1.0.0 </ Plugin-Version >

<Plugin-Requires >1.0.0 </ Plugin-Requires >

<Plugin-Dependencies >x, y, z</ Plugin-Dependencies >

<Plugin-Description >Meu plugin exemplo </ Plugin-Description >

<Plugin-Provider >Archbase </ Plugin-Provider >

<Plugin-License >Apache License 2.0 </ Plugin-License >

</ manifestEntries >

</ archive >

</ configuration >

</ plugin >

<plugin >

<groupId >org.apache.maven.plugins </ groupId >

<artifactId >maven-assembly-plugin </ artifactId >

<configuration >

<descriptorRefs >

<descriptorRef >jar-with-dependencies </ descriptorRef >

</ descriptorRefs >

<finalName >${project.artifactId}-${project.version}-plugin </ finalName >

<appendAssemblyId >false </ appendAssemblyId >

<attach >false </ attach >

<archive >

<manifest >

<addDefaultImplementationEntries >true </ addDefaultImplementationEntries >

<addDefaultSpecificationEntries >true </ addDefaultSpecificationEntries >

</ manifest >

<manifestEntries >

<Plugin-

Class >br.com.archbase.plugin.demo.BemVindoPlugin </ Plugin-Class >

<Plugin-Id > bemvindo-plugin </ Plugin-Id >

<Plugin-Version >1.0.0 </ Plugin-Version >

<Plugin-Requires >1.0.0 </ Plugin-Requires >

<Plugin-Dependencies >x, y, z</ Plugin-Dependencies >

<Plugin-Description >Meu plugin exemplo </ Plugin-Description >

<Plugin-Provider >Archbase </ Plugin-Provider >

<Plugin-License >Apache License 2.0 </ Plugin-License >

</ manifestEntries >

</ archive >

</ configuration >

<executions >

<execution >Fornece metadados do plugin por meio do arquivo de propriedades 

Crie um arquivo chamado plugin.properties na raiz da pasta do seu plugin (ou arquivo ZIP): 

# Notas sobre dependências de plugins 

Os plug-ins podem ter dependências uns dos outros. Essas dependências são especificadas nos 

metadados do plug-in conforme descrito acima. Para fazer referência a um determinado plugin 

como uma dependência, você precisa fornecer seu id de plugin especificado. 

Se o pluginA depende de outro pluginB, você pode definir nos metadados do plugin A :

Se o pluginA depender de outro pluginB na versão 1.0.0, você pode definir nos metadados 

do pluginA :

Se o pluginA depende de outro pluginB a partir da versão 1.0.0, você pode definir nos 

metadados do pluginA :

Se o pluginA depende de outro plugin B a partir da versão 1.0.0 até 2.0.0 (excluindo), você 

pode definir nos metadados do plugin A :

<id >make-assembly </ id >

<phase >package </ phase >

<goals >

<goal >single </ goal >

</ goals >

</ execution >

</ executions >

</ plugin >

plugin.class =br.com.archbase.plugin.demo.BemVindoPlugin 

plugin.id =bemvindo-plugin 

plugin.version =1.0.0 

plugin.requires =1.0.0 

plugin.dependencies =x, y, z

plugin.description =Meu plugin exemplo 

plugin.provider =Archbase 

plugin.license =Apache License 2.0 

Plugin-Dependencies: pluginB 

Plugin-Dependencies: pluginB@1.0 

Plugin-Dependencies: pluginB@>=1.0.0 Se o pluginA depende de outro pluginB a partir da versão 1.0.0 até 2.0.0 (incluindo), você 

pode definir nos metadados do pluginA :

Você também pode definir várias dependências de plug-in com o mesmo padrão, separadas 

por uma vírgula: 

Esses tipos de dependências são consideradas necessárias . O gerenciador de plug-ins apenas 

disponibilizará um plug-in em tempo de execução, se todas as suas dependências forem 

cumpridas. 

# Dependências opcionais do plugin 

Alternativamente, você também pode definir dependências opcionais entre os plug-ins, 

adicionando um ponto de interrogação atrás do id do plug-in - por exemplo: 

ou 

Nesse caso, o pluginA ainda está sendo carregado, mesmo que a dependência não seja realizada 

em tempo de execução. 

# Ciclo de vida do plugin 

Cada plugin passa por um conjunto predefinido de estados. PluginState define todos os estados 

possíveis. 

Os principais estados do plugin são:               

> CREATED
> DISABLED
> RESOLVED
> STARTED
> STOPPED
> Plugin-Dependencies: pluginB@>=1.0.0 &<2.0.0
> Plugin-Dependencies: pluginB@>=1.0.0 &<=2.0.0
> Plugin-Dependencies: pluginB@>=1.0.0 &<=2.0.0, pluginC@>=0.0.1 &<=0.1.0
> Plugin-Dependencies: pluginB?
> Plugin-Dependencies: pluginB?@1.0

O DefaultArchbasePluginManager contém a seguinte lógica: 

todos os plug-ins são resolvidos e carregados  

> DISABLED

plugins NÃO são automaticamente STARTED em startPlugins() MAS você 

pode iniciar manualmente (e portanto habilitar) um plugin DISABLED chamando ao  

> startPlugin(pluginId)

invés de enablePlugin(pluginId) + startPlugin(pluginId) 

apenas STARTED plug-ins podem contribuir com extensões. Qualquer outro estado não deve 

ser considerado pronto para contribuir com uma extensão para o sistema em execução. 

As diferenças entre um DISABLED plugin e um STARTED plugin são: 

um STARTED plugin foi executado ArchbasePlugin.start() , um DISABLED plugin não 

um STARTED plugin pode contribuir com instâncias de extensão, um DISABLED plugin não 

pode  

> DISABLED

plug-ins ainda têm carregadores de classe válidos e suas classes podem ser carregadas 

e exploradas manualmente, mas o carregamento de recursos - que é importante para inspeção -

foi prejudicado pela verificação DISABLED .

À medida que os integradores do framework desenvolvem suas APIs de extensão, será necessário 

especificar uma versão mínima do sistema para carregar plug-ins. Carregar e iniciar um plugin 

mais novo em um sistema mais antigo pode resultar em falhas de tempo de execução devido a 

alterações de assinatura de método ou outras diferenças de classe. 

Por este motivo foi adicionado um atributo de manifesto (in PluginDescriptor ) para especificar 

uma versão 'necessária' que é uma versão mínima do sistema no formato xyz, ou uma Expressão 

SemVer . DefaultArchbasePluginManager contém também um método para especificar a versão 

do sistema do gerenciador de plug-ins e a lógica para desabilitar plug-ins no carregamento se a 

versão do sistema for muito antiga (se você quiser controle total, substitua isPluginValid() ). 

Isso funciona para ambos loadPlugins() e loadPlugin() .

PluginStateListener define a interface para um objeto que escuta as mudanças de estado do 

plugin. Você pode usar addPluginStateListener() e removePluginStateListener() do 

PluginManager se quiser adicionar ou remover um ouvinte de estado do plug-in. 

Seu aplicativo, como consumidor do framework, tem controle total sobre cada plugin (estado). 

Assim, você pode carregar, descarregar, habilitar, desabilitar, iniciar, parar e deletar um 

determinado plugin usando ArchbasePluginManager (programaticamente). 

# Montagem de plug-in 

Depois de desenvolver um plug-in, a próxima etapa é implementá-lo em seu aplicativo. Para esta 

tarefa, uma opção é criar um arquivo zip com a estrutura descrita na seção Como usar desde o 

início do documento. Plugin Manager personalizado 

Para criar um gerenciador de plugins personalizado, você deve escolher uma das opções abaixo: 

implementar interface PluginManager (crie um gerenciador de plugins do zero); 

modificar alguns aspectos / comportamentos de implementações integradas ( 

DefaultArchbasePluginManager ); 

estender a classe AbstractPluginManager .

No caso mais comum, um plugin é um fat jar, um jar que contém classes de todas as bibliotecas 

das quais depende o seu projeto e, claro, as classes do projeto atual. 

AbstractArchbasePluginManager adiciona um pouco de cola que o ajuda a criar rapidamente 

um gerenciador de plugins. Tudo que você precisa fazer é implementar alguns métodos de 

fábrica. PF4J usa em muitos lugares o padrão de método de fábrica para implementar o conceito 

de injeção de dependência (DI) em um modo manual. Veja abaixo os métodos abstratos para 

AbstractArchbasePluginManager :

DefaultArchbasePluginManager contribui com componentes “default” ( 

DefaultArchbaseExtensionFactory , DefaultArchbasePluginFactory ,

DefaultArchbasePluginLoader , ...) para AbstractArchbasePluginManager .

Na maioria das vezes basta estender DefaultArchbasePluginManager e fornecer seus 

componentes personalizados. 

É possível coexistir vários tipos de plug-ins (jar, zip, diretório) no mesmo 

ArchbasePluginManager . Por exemplo, DefaultArchbasePluginManager funciona 

imediatamente com plug-ins jar, zip e diretório. A ideia é que 

DefaultArchbasePluginManager use uma versão composta para: 

PluginDescriptorFinder ( CompoundPluginDescriptorFinder )

ArchbasePluginLoader ( CompoundPluginLoader )

PluginRepository ( CompoundPluginRepository )

public abstract class AbstractArchbasePluginManager implements 

ArchbasePluginManager {

protected abstract PluginRepository createPluginRepository (); 

protected abstract ArchbasePluginFactory createPluginFactory (); 

protected abstract ExtensionFactory createExtensionFactory (); 

protected abstract PluginDescriptorFinder createPluginDescriptorFinder (); 

protected abstract ExtensionFinder createExtensionFinder (); 

protected abstract PluginStatusProvider createPluginStatusProvider (); 

protected abstract ArchbasePluginLoader createPluginLoader (); 

// outros métodos não abstratos 

}

public class DefaultPluginManager extends AbstractArchbasePluginManager {Se você usar apenas jars como plug-ins (sem arquivos zip, sem diretórios), e os metadados do 

plug-in estiverem disponíveis no MANIFEST.MF arquivo, você deve usar um gerenciador de plug-

ins personalizado, algo como: 

// outros métodos 

@Override 

protected PluginDescriptorFinder createPluginDescriptorFinder () {

return new CompoundPluginDescriptorFinder () 

.add (new PropertiesPluginDescriptorFinder ()) 

.add (new ManifestPluginDescriptorFinder ()); 

}

@Override 

protected PluginRepository createPluginRepository () {

return new CompoundPluginRepository () 

.add (new DefaultArchbasePluginRepository (getPluginsRoot (), 

isDevelopment ())) 

.add (new JarPluginRepository (getPluginsRoot ())); 

}

@Override 

protected PluginLoader createPluginLoader () {

return new CompoundPluginLoader () 

.add (new DefaultArchbasePluginLoader (this , pluginClasspath )) 

.add (new JarPluginLoader (this )); 

}

}

PluginManager pluginManager = new DefaultArchbasePluginManager () {

@Override 

protected PluginLoader createPluginLoader () {

// carrega apenas plug-ins jar 

return new JarPluginLoader (this ); 

}

@Override 

protected PluginDescriptorFinder createPluginDescriptorFinder () {

// lê o descritor do plugin do manifesto contido no jar 

return new ManifestPluginDescriptorFinder (); 

}

}; Portanto, é muito fácil adicionar novas estratégias para localizador de descritor de plugin, 

carregador de plugin e repositório de plugin. 

# Modo de desenvolvimento 

O framework pode ser executado em dois modos: DESENVOLVIMENTO e IMPLEMENTAÇÃO .

O modo DEPLOYMENT (padrão) é o fluxo de trabalho padrão para a criação de plug-ins: crie um 

novo módulo Maven para cada plug-in, codificando o plug-in (declara novos pontos de extensão e 

/ ou adiciona novas extensões), empacote o plug-in em um arquivo zip, implante o zip arquivo 

para a pasta de plug-ins. Essas operações são demoradas e, por isso, introduzimos o modo de 

tempo de execução DEVELOPMENT. 

A principal vantagem do modo de tempo de execução DEVELOPMENT para um desenvolvedor de 

plug-ins é que ele / ela não é obrigado a empacotar e implementar os plug-ins. No modo 

DEVELOPMENT você pode desenvolver plugins de forma simples e rápida. 

Vamos descrever como o modo runtime DEVELOPMENT funciona. 

Primeiro, você pode alterar o modo de tempo de execução usando a propriedade de sistema 

“archbase.mode” ou substituindo DefaultArchbasePluginManager.getRuntimeMode() .

Por exemplo, eu executo o demo no eclipse no modo DEVELOPMENT adicionando apenas "- 

> Darchbase.mode=development"

ao iniciador demo. 

Você pode recuperar o modo de tempo de execução atual usando  

> ArchbasePluginManager.getRuntimeMode()

ou em sua implementação de plug-in com  

> getWrapper().getRuntimeMode()

.

O DefaultArchbasePluginManager determina automaticamente o modo de tempo de execução 

correto e para o modo DEVELOPMENT substitui alguns componentes (pluginsDirectory é 

”../plugins” , PropertiesPluginDescriptorFinder como PluginDescriptorFinder, 

DevelopmentPluginClasspath como PluginClassPath). 

Outra vantagem do modo de tempo de execução DEVELOPMENT é que você pode executar 

algumas linhas de código apenas neste modo (por exemplo, mais mensagens de depuração). 

NOTA: Se você usar o Eclipse, certifique-se de que o processamento de anotações esteja ativado 

pelo menos para todos os projetos que registram objetos usando anotações. Nas propriedades 

do seu novo projeto, vá para Compilador Java> Processamento de anotação Marque “Ativar 

configurações específicas do projeto” e certifique-se de que “Ativar processamento de 

anotação” esteja marcado. 

Se você usar o Maven como gerenciador de compilação, após cada modificação de dependência 

em seu plug-in (módulo Maven), você deve executar Maven> Atualizar projeto ... 

# Desativar plugins 

Em teoria, é uma relação 1: N entre um ponto de extensão e as extensões para este ponto de 

extensão. 

Isso funciona bem, exceto quando você desenvolve vários plug-ins para este ponto de extensão 

como opções diferentes para seus clientes decidirem qual deles usar. Nesta situação, você deseja a possibilidade de desativar todas as extensões, exceto uma. 

Por exemplo, eu tenho um ponto de extensão para envio de email (interface EmailSender) com 

duas extensões: uma baseada em Sendgrid e outra baseada em Amazon Simple Email Service. 

A primeira extensão está localizada no Plugin1 e a segunda extensão está localizada no Plugin2. 

Desejo ir apenas com uma extensão (relação 1: 1 entre ponto de extensão e extensões) e para 

isso tenho duas opções: 

1) desinstale o Plugin1 ou Plugin2 (remova a pasta pluginX.zip e o pluginX da pasta de plug-ins) 

2) desative o Plugin1 ou Plugin2 

Para a opção dois, você deve criar um arquivo simples enabled.txt ou disabled.txt na pasta de 

plug-ins. 

O conteúdo de enabled.txt é semelhante a: 

O conteúdo de disabled.txt é semelhante a: 

Todas as linhas de comentário (linha que começam com # caractere) são ignoradas. 

Se um arquivo com enabled.txt existir, disabled.txt será ignorado. Consulte enabled.txt e 

disabled.txt da pasta demo. 

# Extensões 

Sobre pontos de extensão 

Para estender a funcionalidade de um aplicativo, ele deve definir um chamado ponto de 

extensão. Esta é uma interface ou classe abstrata, que define um comportamento específico para 

uma extensão. 

O exemplo a seguir define um ponto de extensão para estender um javax.swing.JMenuBar com 

entradas de menu adicionais:                                               

> ##############################################
> #-carregue apenas estes plugins
> #-adicione um id de plugin em cada linha
> #-coloque este arquivo na pasta de plug-ins
> ##############################################
> bemvindo-plugin
> #############################################
> #-carregue todos os plug-ins, exceto estes
> #-adicione um id de plugin em cada linha
> #-coloque este arquivo na pasta de plug-ins
> ##############################################
> bemvindo-plugin

Sobre extensões 

Uma extensão é uma implementação concreta de um ponto de extensão. 

O exemplo a seguir adiciona um menu com o título “Olá Mundo” à barra de menus 

implementando a MainMenuExtensionPoint interface: 

Uma extensão pode ser carregada do classpath do aplicativo (chamadas extensões do sistema )

ou pode ser fornecida por um plugin. 

Por favor, observe a @Extension anotação. Esta anotação marca a classe como uma extensão 

carregável para o framework. Todas as classes marcadas com a anotação @Extension são 

publicadas automaticamente em tempo de compilação no arquivo JAR criado - no META- 

> INF/extensions.idx

arquivo ou como serviço na META-INF/services pasta. Usando a  

> @Extension

anotação, você não precisa criar esses arquivos manualmente! 

Como as extensões são carregadas 

De acordo com o exemplo acima, o aplicativo pode construir a barra de menus assim:                               

> import javax .swing .JMenuBar ;
> import br .com .archbase .plugin .manager .ExtensionPoint ;
> interface MainMenuExtensionPoint extends ExtensionPoint {
> void buildMenuBar (JMenuBar menuBar );
> }
> import javax .swing .JMenu ;
> import javax .swing .JMenuBar ;
> import javax .swing .JMenuItem ;
> import br .com .archbase .plugin .manager .Extension ;
> @Extension
> public class MyMainMenuExtension implements MainMenuExtensionPoint {
> public void buildMenuBar (JMenuBar menuBar ){
> JMenu exampleMenu =new JMenu ("Examplo" );
> exampleMenu .add (new JMenuItem ("Olá Mundo" ));
> menuBar .add (exampleMenu );
> }
> }
> import javax .swing .JDialog ;
> import javax .swing .JMenu ;
> import javax .swing .JMenuBar ;

# Parâmetros de extensão adicionais 

A anotação @Extension pode fornecer algumas opções adicionais, que podem ser úteis em 

certas situações. 

Extensões de pedido 

Vamos supor que temos várias extensões para a barra de menu e gostamos de ter controle, em 

que ordem as entradas do menu aparecem na barra de menu. Nesse caso, podemos usar o 

ordinal na anotação @Extension :

import br .com .archbase .plugin .manager .DefaultArchbasePluginManager ;

import br .com .archbase .plugin .manager .ArchbasePluginManager ;

public static void main (String [] args ) {

// Inicie o ambiente do plugin. 

// Isso deve ser feito uma vez durante o processo de inicialização do 

aplicativo. 

final ArchbasePluginManager pluginManager = new 

DefaultArchbasePluginManager (); 

pluginManager .loadPlugins (); 

pluginManager .startPlugins (); 

// Abra o aplicativo Swing. 

java .awt .EventQueue .invokeLater (new Runnable () {

public void run () {

// Construir a barra de menu usando as extensões disponíveis. 

JMenuBar mainMenu = new JMenuBar (); 

for (MainMenuExtensionPoint extension :

pluginManager .getExtensions (MainMenuExtensionPoint .class )) {

extension .buildMenuBar (mainMenu ); 

}

// Cria e mostra um diálogo com a barra de menu. 

JDialog dialog = new JDialog (); 

dialog .setTitle ("Exemplo de dialog" ); 

dialog .setSize (450 ,300 ); 

dialog .setJMenuBar (mainMenu ); 

dialog .setVisible (true ); 

}

}); 

}Ao definir @Extension(ordinal = 1) o gerenciador de plug-ins, sempre carregará essa extensão 

primeiro. Portanto, a primeira entrada da barra de menu é sempre chamada de “Primeira”. 

Ao definir @Extension(ordinal = 2) o gerenciador de plugins sempre carregará esta extensão 

após a primeira. 

Configure explicitamente um ponto de extensão 

Em aplicativos do mundo real, é bastante comum criar classes abstratas para interfaces. Vamos 

supor a seguinte hierarquia de classes: 

@Extension (ordinal = 1)

public class FirstMainMenuExtension implements MainMenuExtensionPoint {

public void buildMenuBar (JMenuBar menuBar ) {

JMenu menu = new JMenu ("Primeiro" ); 

menu .add (new JMenuItem ("Olá Mundo!" )); 

menuBar .add (menu ); 

}

}

@Extension (ordinal = 2)

public class SecondMainMenuExtension implements MainMenuExtensionPoint {

public void buildMenuBar (JMenuBar menuBar ) {

JMenu menu = new JMenu ("Segundo" ); 

menu .add (new JMenuItem ("Olá Mundo" )); 

menuBar .add (menu ); 

}

}Nesse caso, a classe de extensão ( Plugin1MainMenuExtension ) não é derivada diretamente 

da interface br.com.archbase.plugin.manager.ExtensionPoint . Em vez disso, o aplicativo 

estende essa interface com sua própria interface BaseExtensionPoint para adicionar alguns 

métodos adicionais. Além disso, o aplicativo fornece uma classe abstrata MainMenuAdapter , que 

é finalmente estendida pelo Plugin1MainMenuExtension .

Você pode encontrar uma abordagem semelhante, por exemplo, na  

> java.awt.event.WindowListener

interface e na java.awt.event.WindowAdapter classe 

abstrata . 

Neste cenário, é necessário registrar explicitamente o ponto de extensão na anotação  

> @Extension

:Caso contrário, o framework pode não ser capaz de detectar automaticamente os pontos de 

extensão corretos para a extensão em tempo de compilação . 

# Configure explicitamente as dependências do plugin 

Os plug-ins podem ter uma dependência opcional uns dos outros . Isso pode levar a uma 

situação em que uma determinada extensão depende de um plugin, que não está disponível em 

tempo de execução do aplicativo. Vamos supor a seguinte hierarquia de classes: 

Este cenário descreve um aplicativo que fornece um plug-in para gerenciamento de contatos (  

> ContactosPlugin

) e outro plug-in para gerenciamento de calendário ( CalendarPlugin ). Ambos 

os plug-ins fornecem um formulário que permite ao usuário editar contatos / entradas de 

calendário. Esses formulários podem ser estendidos com pontos de extensão: 

O formulário de contato mostra um painel com entradas de calendário atribuídas (via  

> CalendarContatosFormExtension

), caso o plugin de calendário esteja disponível em tempo 

de execução. 

O formulário do calendário mostra um painel com entradas de contato atribuídas (via  

> ContatosCalendarFormExtension

), caso o plugin de contatos esteja disponível em tempo 

de execução. 

Para fazer essas dependências circulares funcionarem, as extensões no  

> br.com.archbase.demo.plugins.contatos.addons

pacote são fornecidas como um arquivo JAR 

separado, que é empacotado na lib pasta do plug-in de contatos.               

> @Extension (points ={MainMenuExtensionPoint .class })
> public class Plugin1MainMenuExtension extends MainMenuAdapter {
> public void buildMenuBar (JMenuBar menuBar ){
> // alguma implementação ...
> }
> }

Ambos os plug-ins também precisam funcionar de forma independente. Por exemplo, o usuário 

pode não precisar de gerenciamento de calendário em seu aplicativo. Nesse caso, ele pode 

desativar / remover o plug-in de calendário totalmente e o plug-in de contatos ainda deve 

funcionar. Neste cenário específico, ambos os plug-ins devem ter uma dependência opcional um 

do outro. O gerenciador de plug-ins ainda precisa carregar o plug-in de contatos, mesmo se o 

plug-in de calendário não estiver habilitado / disponível em tempo de execução. 

Mas essas dependências opcionais podem levar à situação de que uma certa extensão depende 

de um plugin não existente. Para evitar erros de carregamento de classe neste caso particular, 

você pode definir os plug-ins, que são necessários para carregar uma determinada extensão 

através da anotação @Extension :

Nesse caso, o gerenciador de plug-ins só carregará essas extensões se todos os plug-ins 

necessários estiverem disponíveis / habilitados no tempo de execução.                                                        

> @Extension (plugins ={ContatosPlugin .ID ,CalendarioPlugin .ID })
> public class CalendarContatosFormExtension implements ContatosFormExtension {
> public JPanel getPanel () {
> // alguma implementação ...
> }
> public void load (ContatosEntry entry ){
> // alguma implementação ...
> }
> public void load (ContatosEntry save ){
> // alguma implementação ...
> }
> }
> @Extension (plugins ={ContatosPlugin .ID ,CalendarioPlugin .ID })
> public class ContatosCalendarioFormExtension implements CalendarioFormExtension
> {
> public JPanel getPanel () {
> // alguma implementação ...
> }
> public void load (CalendarioEntry entry ){
> // alguma implementação ...
> }
> public void load (CalendarioEntry save ){
> // alguma implementação ...
> }
> }

Observe: Este recurso só é necessário se você usar plug-ins com dependência opcional uns dos 

outros. Nesse caso, você deve adicionar a asm biblioteca ao classpath de seu aplicativo. 

# Instanciação de extensão 

Para criar instâncias de extensões, o framework usa uma ExtensionFactory . Por padrão, usamos 

DefaultArchbaseExtensionFactory como implementação de ExtensionFactory .

Você pode alterar a implementação padrão com: 

DefaultArchbaseExtensionFactory usa o método Class # newInstance () para criar a instância 

de extensão. 

Uma instância de extensão é criada sob demanda, quando 

plugin.getExtensions(MinhaExtensionPoint.class) é chamada. Por padrão, se você ligar 

plugin.getExtensions(MinhaExtensionPoint.class) duas vezes: 

então, para cada chamada, uma nova instância da extensão é criada. 

Se você deseja retornar a mesma instância de extensão (singleton), você precisa usar 

SingletonExtensionFactory 

new DefaultArchbasePluginManager () {

@Override 

protected ExtensionFactory createExtensionFactory () {

return MinhaExtensionFactory (); 

}

}; 

plugin .getExtensions (MinhaExtensionPoint .class ); 

plugin .getExtensions (MinhaExtensionPoint .class ); 

new DefaultArchbasePluginManager () {

@Override 

protected ExtensionFactory createExtensionFactory () {

return SingletonExtensionFactory (); 

}

}; Extensão do sistema 

Uma extensão também pode ser definida diretamente no jar do aplicativo (ou seja, você não é 

obrigado a colocar a extensão em um plug-in - você pode ver essa extensão como padrão ou 

extensão do sistema ). 

Isso é ótimo para iniciar a fase de aplicação. Neste cenário, você tem uma estrutura de plug-in 

minimalista com um carregador de classes (o carregador de classes do aplicativo), semelhante ao 

Java ServiceLoader, mas com os seguintes benefícios: 

não há necessidade de escrever arquivos de configuração do provedor no diretório de 

recursos META-INF/services , você está usando a elegante anotação @Extension do 

framework; 

a qualquer momento, você pode alternar para o mecanismo de carregador de classes 

múltiplas sem alterações de código em seu aplicativo. 

O código presente na Boot classe do aplicativo demo é funcional, mas você pode usar um código 

mais minimalista, ignorando pluginManager.loadPlugins() e 

> pluginManager.startPlugins()

.

O código acima pode ser escrito: 

# ServiceLoader 

O framework pode ler META-INF/services (mecanismo do provedor de serviços Java) como 

extensões, portanto, se você tiver um aplicativo modular baseado em  

> java.util.ServiceLoader

classe, pode substituir totalmente as ServiceLoader.load() 

chamadas de seu aplicativo ArchbasePluginManager.getExtensions() e migrar sem problemas 

de ServiceLoader para o framework.                                         

> public static void main (String [] args ){
> ArchbasePluginManager pluginManager =new DefaultArchbasePluginManager ();
> pluginManager .loadPlugins ();
> pluginManager .startPlugins ();
> List <Saudacao >saudacoes =pluginManager .getExtensions (Saudacao .class );
> for (Saudacao saudacao :saudacoes ){
> System .out .println (">>> "+saudacao .getSaudacao ());
> }
> }
> public static void main (String [] args ){
> ArchbasePluginManager pluginManager =new DefaultArchbasePluginManager ();
> List <Saudacao >saudacoes =pluginManager .getExtensions (Saudacao .class );
> for (Saudacao saudacao :saudacoes ){
> System .out .println (">>> "+saudacao .getSaudacao ());
> }
> }

Além disso, você tem a possibilidade de alterar o ExtensionStorage usado em 

ExtensionAnnotationProcessor . Por padrão, usamos o formato com META-

INF/extensions.idx :

mas você pode usar um local e formato mais padrão,, META-INF/services/<extension-

point> usado pelo Java Service Provider (consulte java.util.ServiceLoader Recursos) por meio 

da ServiceProviderExtensionStorage implementação. Neste caso, o formato de META-

INF/services/br.com.archbase.plugin.demo.Saudacao é: 

onde a entrada br.com.archbase.plugin.demo.OlaSaudacao é legada (não é gerada pelo 

framework), mas é vista como uma extensão de Saudacao (em tempo de execução). 

Você pode conectar sua implementação ExtensionStorage personalizada 

ExtensionAnnotationProcessor em dois modos possíveis: 

defina a opção do processador de anotações com a tecla archbase.storageClassName 

defina a propriedade do sistema com a chave archbase.storageClassName 

Por exemplo, se eu quiser usar ServiceProviderExtensionStorage , o valor da chave 

archbase.storageClassName deve ser 

br.com.archbase.plugin.manager.processor.ServiceProviderExtensionStorage 

NOTA: ServiceLoaderExtensionFinder a classe que pesquisa extensões armazenadas na 

META-INF/services pasta não é adicionada / habilitada por padrão. Para fazer isso, substitua 

createExtensionFinder de DefaultPluginManager :

br .com .archbase .plugin .demo .OlaSaudacao ;

br .com .archbase .plugin .demo .SaudacaoWazzup ;

# Generated by archbase 

br.com.archbase.plugin.demo.OlaSaudacao 

br.com.archbase.plugin.demo.SaudacaoWazzup # archbase extension 

final ArchbasePluginManager pluginManager = new DefaultArchbasePluginManager () 

{

protected ExtensionFinder createExtensionFinder () {

DefaultExtensionFinder extensionFinder = (DefaultExtensionFinder )

super .createExtensionFinder (); 

extensionFinder .addServiceProviderExtensionFinder (); 

return extensionFinder ;

}

}; Assíncrono 

Carregar e iniciar a sincronização de plug-ins 

Carregar e iniciar plugins assíncronos 

# Solução de problemas 

Abaixo estão listados alguns problemas que podem ocorrer ao tentar usar o framework e 

sugestões para resolvê-los. 

Nenhuma extensão encontrada 

Veja se você tem um arquivo extensions.idx em cada plugin. 

Se o arquivo extensions.idx não existir, provavelmente há algo errado com a etapa de 

processamento da anotação (habilite o processamento da anotação em seu IDE ou em seu script 

Maven). 

Se o arquivo extensions.idx existir e não estiver vazio, certifique-se de ter um problema com o 

// crie o gerenciador de plugins 

final ArchbasePluginManager pluginManager = new DefaultArchbasePluginManager (); 

// carregue os plugins 

pluginManager .loadPlugins (); 

// iniciar (ativo / resolvido) os plug-ins 

pluginManager .startPlugins (); 

// recupera as extensões para o ponto de extensão de saudação 

List <Saudacao > saudacoes = pluginManager .getExtensions (Saudacao .class ); 

// cria o gerenciador de plugins 

final AsyncArchbasePluginManager pluginManager = new 

DefaultAsyncArchbasePluginManager (); 

// carregue os plugins 

CompletionStage <Void > stage = pluginManager .loadPluginsAsync (); 

stage .thenRun (() -> System .out .println ("Plugins carregados" )); // optional 

// iniciar (ativo / resolvido) os plug-ins 

stage .thenCompose (v -> pluginManager .startPluginsAsync ()); 

stage .thenRun (() -> System .out .println ("Plugins iniciados" )); // optional 

// bloquear e esperar que o futuro seja concluído (não é a melhor abordagem em 

aplicativos reais) 

stage .toCompletableFuture (). get (); 

// recupera as extensões para o ponto de extensão de saudação 

List <Saudacao > saudacoes = pluginManager .getExtensions (Saudacao .class ); carregador de classes (você tem o mesmo ponto de extensão em dois carregadores de classes 

diferentes), nesta situação, você deve remover algumas bibliotecas (provavelmente o jar da API) 

do plug-in. 

Se o problema persistir ou você quiser encontrar mais informações relacionadas ao processo de 

descoberta de extensões (por exemplo, quais interfaces / classes são carregadas por cada plugin, 

quais classes não são reconhecidas como extensões para um ponto de extensão), então você 

deve colocar TRACE o logger para PluginClassLoader e AbstractExtensionFinder .

# archbase-design-patterns 

É uma estrutura e catálogo de padrões de design Java aperfeiçoados e com componentes. Um 

padrão componentizado é, em essência, uma variação independente do contexto, reutilizável e 

com segurança de tipo do padrão original que cobre pelo menos tantos casos de uso quanto o 

padrão original e que não exige que os desenvolvedores reimplementem o mesmo código clichê 

em cada contexto diferente. Os padrões de design são reutilizáveis em termos de design, os 

padrões componentizados são reutilizáveis em termos de design e código. 

# Várias vantagens de contar com uma biblioteca de Padrões de Projeto 

# componentizados: 

Os desenvolvedores são aliviados do fardo de ter que reimplementar, portanto, duplicar o 

mesmo código repetidamente com apenas diferenças específicas do contexto, por exemplo, 

no Padrão Observer, a implementação do Assunto requer manipulação e iteração de uma 

coleção de instâncias do Observer. 

Como os Design Patterns são especificados por meio de descrições, diagramas UML e 

códigos de amostra não reutilizáveis; na maioria das vezes, as implementações dos 

desenvolvedores finais são inexatas, ineficientes, defeituosas ou dificilmente reutilizáveis. 

Os padrões de projeto podem ser combinados naturalmente para resolver problemas de 

projeto específicos; confiar em uma Biblioteca de Padrões impulsionará a criação de tais 

combinações ou até mesmo fornecerá combinações naturais prontas para uso, por exemplo, 

Comandos Compostos. 

# Padrões de Design 

Padrão de método de fábrica GoF 

Defina uma interface para criar um objeto, mas deixe as subclasses decidirem qual classe 

instanciar. O Factory Method permite que uma classe adie a instanciação para as subclasses. Padrão de Método de Fábrica Componentizado 

A versão do Factory Method Pattern fornece apenas uma interface de base do supertipo. A 

interface de fábrica de base comum define um método comum create(). Os possíveis parâmetros 

exigidos por subtipos para criar as instâncias específicas podem ser adicionados usando Java 

Beans Setters . Fornecer parâmetros necessários para o código de criação por meio de 

configuradores Java Beans em vez de construtores torna as instâncias IFactoryMethod 

reutilizáveis. 

Padrão de observador GoF 

Defina uma dependência um-para-muitos entre os objetos para que, quando um objeto mudar 

de estado, todos os seus dependentes sejam notificados e atualizados automaticamente. Padrão Observador Componentizado 

A versão componentizada de Observer Pattern oferece as seguintes vantagens: 

Oferece os modelos Push e Pull, mas acho que o Push é melhor pelos seguintes motivos. No 

modelo Push, os observadores se inscrevem em um único tipo de evento, portanto, exigem 

um único tipo de dados de evento (se houver). O modelo Push é mais geral do que o modelo 

Pull e desacopla completamente os observadores do assunto. O modelo Push garante que 

os Observadores recebam exatamente as informações de que precisam, esta solução 

oferece as seguintes vantagens: 

Os observadores não estão acoplados a uma interface externa, por exemplo, de 

consulta de estado. Este acoplamento adicional existe no modelo Pull mostrado no 

exemplo GoF onde os Observadores precisam saber onde procurar quando um evento 

ocorre, por exemplo, o Assunto. 

Os observadores não precisam trabalhar muito para descobrir quais informações foram 

alteradas. 

O estado autoconsistente é garantido o tempo todo. Uma vez que os Observadores 

recebem um instantâneo do estado exato no momento da notificação, não há risco de 

que os Observadores tenham uma visão errada do estado. 

Fornece uma solução de segurança de tipo: Assunto e Observador usam genericidade para 

identificar que tipo de dados está associado a um evento, por exemplo, subtipo IEventData. 

A segurança de tipo é imposta porque as instâncias de Assunto somente assinarão / 

anexarão Observadores que estejam em conformidade com o tipo de dados de evento 

correto, enquanto, por exemplo, a implementação de Observer do Sun JDK depende de um 

parâmetro de tipo inseguro. 

Fornece proteção contra Observadores transgressivos: A implementação do assunto protege 

a si mesma e a seus clientes de Observadores defeituosos que lançam exceções não 

verificadas. Os observadores que durante o tratamento do update(...) método lançam uma 

exceção não verificada são automaticamente desanexados. Não fazer isso colocaria as 

instâncias do Assunto em risco de comportamento inesperado vindo de Observadores 

defeituosos. 

Padrão de Cadeia de Responsabilidade GoF 

Evite acoplar o remetente de uma solicitação a seu receptor, dando a mais de um objeto a chance 

de lidar com a solicitação. Encadeie os objetos de recebimento e passe a solicitação ao longo da 

cadeia até que um objeto o trate. Padrão de Cadeia de Responsabilidade Componentizada 

A versão componentizada de Chain of Responsibility Pattern oferece as seguintes vantagens: 

Parametriza a interface Handler com parâmetro de solicitação genérico, tornando o tipo de 

padrão seguro e flexível para definições de solicitação específicas de contexto definidas pelo 

usuário. 

A separação clara de interesses, visto que a lógica de tratamento é separada da lógica de tomada 

de decisão, ou seja, handle() versus start(), consulte o artigo sobre armadilhas e melhorias do 

padrão da Cadeia de Responsabilidade 

Tomada de decisão flexível com o padrão de estratégia, por exemplo, decisão de encaminhar 

solicitações para instâncias sucessoras do manipulador. IHandleras instâncias podem ser 

configuradas com um IChainStrategy que parametriza o comportamento de continuação em 

cadeia. Foram fornecidas duas implementações concretas AllHandleStrategy e 

OnlyOneHandleStrategy onde a última é o caso de uso coberto na implementação original do GoF 

e na Estratégia padrão. 

Padrão de Comando GoF 

Encapsule uma solicitação como um objeto, permitindo, assim, parametrizar clientes com 

diferentes solicitações, solicitações de fila ou log e suporte a operações que podem ser desfeitas. Padrão de Comando Componentizado 

A versão componentizada de Comando oferece as seguintes vantagens: 

Parametriza o Invoker, Command e Receiver com parâmetros genéricos e tipos de retorno. Esta 

parametrização permite passar argumentos definidos pelo usuário para o Invoker e, portanto, 

para o Comando e Receptor; também suporta a associação opcional de um possível valor de 

Resultado fora da execução do Receptor. 

Padrão de visitante GoF 

Representam uma operação a ser executada nos elementos de uma estrutura de objeto. Visitante 

permite definir uma nova operação sem alterar as classes dos elementos nos quais opera. Padrão de Visitante Componentizado 

A versão componentizada do padrão de visitante difere da versão original do GoF no sentido de 

que: 

Adicionar novas operações de visita ainda é fácil, mas adicionar novos Elementos Concretos 

também é fácil porque não requer a adição de uma nova operação abstrata no tipo IVisitor para 

cada novo tipo de Elemento Concreto. O AbstractVisitor .visit( ) método ou sua variação estática 

reusableVisit manipula todos os subtipos de Elemento por meio de despacho duplo. 

Fornece implementação não intrusiva que não impõe nenhum requisito na hierarquia de 

Elementos, ou seja, a implementação de GoF requer que a hierarquia de Elemento forneça um 

accept(...) método repetitivo e sujeito a erros . 

AbstractVisitor Implementação pronta para uso livre de contexto que fornece o mecanismo de 

despacho duplo implementado em cima dos Delegados do frameworkk. Adicionar um novo 

Visitante concreto é tão simples quanto estender AbstractVisitor (ou de outra forma reutilizar a 

reusableVisit implementação estática ) e implementar os visitXXX(...) métodos relevantes para 

cada subtipo de Elemento de interesse. Os nomes dos métodos de visita dos Visitantes Concretos 

podem ser escolhidos livremente, embora uma boa convenção seja o uso, visitXXX(...) mas não é 

obrigatório. Padrão Adaptador GoF 

Converta a interface de uma classe em outra interface que os clientes esperam. O adaptador 

permite que as classes trabalhem juntas de outra forma, devido a interfaces incompatíveis. 

Padrão de Adaptador Componentizado 

A versão componentizada do Adapter Pattern oferece as seguintes vantagens: 

Adaptação automática : a implementação do Adaptador minimiza ou até mesmo reduz a 

zero a quantidade de trabalho necessária para adaptar uma determinada implementação do 

Adaptee a uma interface de destino. Estes são os cenários possíveis ao adaptar um Adaptee 

arbitrário a uma interface de destino: 

A interface de destino é um subconjunto exato da implementação do Adaptador com 

combinação perfeita de nomes de métodos e assinaturas, então a adaptação é feita 

automaticamente, ou seja, apenas usando Adapter; 

A interface de destino é um subconjunto do Adaptee com pequenas diferenças no 

nome ou tipo do método (estático ou instância), então a adaptação é feita 

automaticamente, ou seja, apenas usando Adapter; 

A implementação do Adaptador requer conversão de parâmetro para corresponder à 

interface de destino, então você precisa estender Adaptere fornecer implementação 

apenas para os métodos que requerem a conversão; 

A implementação do Adaptee requer um comportamento diferente ou métodos 

adicionais para corresponder à interface de destino, então você precisa estender 

Adaptere fornecer implementação apenas para os métodos que requerem um 

comportamento diferente ou implementar os métodos extras, consulte o exemplo 

abaixo. 

Estratégia de adaptação plugável : a implementação do Adaptador apresenta estratégias 

configuráveis para adaptar as interfaces Target às implementações Adaptee. A 

implementação atual oferece duas estratégias concretas: 

ExactMatchAdaptingStrategy : Valida e resolve métodos Target procurando correspondências exatas em nomes e assinaturas de métodos Adapter e Adaptee. Esta 

é a estratégia padrão. 

NameMatchAdaptingStrategy : Usa o mapeamento definido pelo usuário de nomes de 

métodos de Adaptee para nomes de métodos de interface de destino. Os nomes de 

métodos não especificados serão padronizados para a implementação 

ExactMatchAdaptingStrategy. 

A implementação do Adaptador oferece uma boa troca entre flexibilidade e forte verificação do 

compilador. A reutilização e flexibilidade avançadas aumentam fortemente a produtividade, 

reduzindo o tempo de desenvolvimento ao custo de mudar para o Runtime alguns dos problemas 

que, de outra forma, seriam detectados pelo compilador. O compromisso é porque o Adaptador 

fará o melhor para combinar a implementação do Adaptee com a interface de destino, mas, por 

exemplo, mudanças nas assinaturas do método do Adaptee podem levar a violações da pré-

condição do Runtime durante a construção do Adaptador. 

Tecnicamente falando, a implementação do Adaptador é segura para o tipo, você nunca obterá 

um erro de tempo de execução devido a uma operação de incompatibilidade de tipo de dados ao 

usar API do framework, ou seja, você nunca obterá um ClassCastException . Se você fizer isso, 

será um bug e precisará ser corrigido. Observe que, embora o possível erro tenha sido alterado 

de Compiler para Runtime, tecnicamente falando isso não é inseguro. O que acontece é que a 

implementação do framework detectará a situação errônea no nível da estrutura e a traduzirá em 

uma violação de pré-condição no nível da API usando, por exemplo, IllegalArgumentException . As 

violações da pré-condição devem ser descobertas no momento do teste, tendo uma cobertura de 

teste adequada instalada. 

Padrão Composto GoF 

Componha objetos em estruturas de árvore para representar hierarquias parte-todo. O Composite 

permite que os clientes tratem objetos individuais e composições de objetos uniformemente. Padrão Composto Componentizado 

O framework oferece uma versão totalmente componentizada do padrão Composite em Java. Os 

usuários precisam apenas fornecer a interface do componente e usar diretamente a 

implementação do Composite com segurança de tipo . Veja o exemplo abaixo. 

Se a interface do componente de contexto específico contém funções, ou seja, métodos que 

devem retornar um valor, a Composite implementação padrão retorna nulo. Os usuários são 

responsáveis por criar uma subclasse Composite e definir como agregar os vários valores 

retornados de um método específico. Em versões futuras, o mecanismo de agregação será 

aprimorado. 

A Composite implementação se estende ArrayList para que exponha todos os List recursos 

para criar as composições. ArrayList a implementação foi preferida a outras para maximizar a 

velocidade das iterações. 

Padrão de Decorador GoF 

Anexe responsabilidades adicionais a um objeto dinamicamente. Os decoradores fornecem uma 

alternativa flexível à subclasse para estender a funcionalidade. Padrão Decorador Componentizado 

O framework oferece uma versão com componentes do padrão Decorator. Para implementar 

decoradores e aproveitar as vantagens da implementação, os usuários devem estender 

AbstractDecorator 

Implementar decoradores usando AbstractDecorator oferece as seguintes vantagens: 

A identidade do Decorador, é automaticamente tratada para corresponder à do 

Componente, portanto, para o mundo externo, o Decorador ainda é o Componente, 

assumindo que equals define a identidade 

Decoração automática : decoração de interfaces com grande número de recursos em um 

piscar de olhos. Esses métodos de componente não definidos pelo decorador serão 

automaticamente encaminhados para o componente para que você não precise fazer isso. 

Os usuários são obrigados apenas a fornecer implementação para esses métodos extras ou 

métodos decorados minimamente. 

Capacidade de manutenção : a interface do componente pode mudar, mas os decoradores 

serão minimamente ou não serão afetados. Na implementação manual tradicional, uma vez 

que a interface do Component mudou, todo o Decorator também teve que ser alterado, o 

que representa um alto custo na manutenção. Padrão de Proxy GoF 

Anexe responsabilidades adicionais a um objeto dinamicamente. Proxys fornece uma alternativa 

flexível para subclasses para estender a funcionalidade._ 

# Padrão de Proxy com componentes* 

O framework oferece uma versão com componentes do padrão Proxy. Para implementar proxies 

e aproveitar as vantagens da implementação, os usuários devem estender AbstractProxy e, 

opcionalmente, substituir o método invokeUnderlying . O invokeUnderlying irá interceptar 

todas as chamadas de método na instância real de Subject. Veja o exemplo abaixo ou verifique a  

> SynchronizedProxy.java

implementação que protegerá qualquer tipo de Assunto de condições 

de execução. Esta implementação seria substituir a necessidade de, por exemplo  

> Collections.synchronizedCollection(...)

, Collections.synchronizedSet(...) , 

> Collections.synchronizedList(...)

, etc. Veja exemplo, sob proxies de execução usando de o 

framework oferece as seguintes vantagens: TestAsynchronousSubject.java 

> AbstractProxy

A identidade do proxy, é tratada automaticamente para corresponder à do Sujeito, 

portanto, para o mundo externo, o Proxy ainda é o Sujeito assumindo que equals define a 

identidade 

Proxy automático : interfaces de proxy com grande número de recursos em um piscar de 

olhos. Esses métodos de Assunto não definidos pelo Proxy serão automaticamente 

encaminhados para o Assunto para que você não precise fazer isso. Os usuários são 

obrigados apenas a fornecer implementação mínima para esses métodos proxy. 

Manutenibilidade : a interface do Assunto pode mudar, mas os Proxies serão minimamente 

ou não serão afetados. Na implementação do manual tradicional, uma vez que a interface do 

Assunto mudou, todos os proxies também tiveram que ser alterados, o que representa um alto custo na manutenção. 

# Delegados 

Permite que vários objetos implementem métodos com nome ou tipo diferente (instância ou 

estático), mas assinaturas compatíveis sejam usadas de forma intercambiável. 

Implementação 

Delegados é conceitualmente semelhante à noção de ponteiros de função. Java não fornece 

delegados nativamente, portanto, a necessidade dessa implementação, consulte o artigo Um 

programador Java examina os delegados C # . Veja também a crítica da Sun aos Delegados .

A introdução da implementação de Delegates no framework é mais um meio de componentizar 

alguns dos Design Patterns em vez de oferecer Delegates como bloco de construção de design 

final para aplicativos finais. O recurso Delegates está, no entanto, incluído na API pública para 

casos em que seria necessário, por exemplo, abstrai efetivamente de Java Reflection de baixo 

nível. 

Notas sobre a implementação de Delegados: 

Um ponteiro de função é representado como uma interface Java que expõe um único 

método 

Não há uma maneira fortemente tipada de identificar um método Java por meio do 

Reflection até agora. Ao construir o delegado, o método de destino é referido por seu  

> String

nome. A tentativa de construir um delegado sobre um método que não existe mais 

ou sua assinatura não corresponde, resultará em uma exceção de tempo de execução 

predefinida. 

Uma vez que o Delegado foi construído com sucesso, as regras de covariância do tipo de 

retorno do método de interface se aplicam. 

Tecnicamente falando, a implementação de Delegates do framework é segura para o tipo, você 

nunca obterá um erro de Runtime devido a uma operação de incompatibilidade de tipo de dados 

ao usar a API, ou seja, você nunca obterá uma ClassCastException . Se você fizer isso, é um bug e 

precisará ser corrigido. Observe que, embora o possível erro tenha sido alterado de Compiler 

para Runtime, tecnicamente falando isso não é inseguro. O que acontece é que a implementação 

detectará a situação errônea no nível da estrutura e a traduzirá em uma violação de pré-condição 

no nível da API usando, por exemplo, IllegalArgumentException . As violações da pré-condição 

devem ser descobertas no momento do teste, tendo uma cobertura de teste adequada instalada. 

# archbase-workflow-process 

Trata-se de um framework de fluxo de trabalho para Java. Ele fornece APIs e blocos de construção 

simples para facilitar a criação e a execução de fluxos de trabalho combináveis. Uma unidade de trabalho no framework é representada pela interface Work . Um fluxo de 

trabalho é representado pela interface WorkFlow . O framework fornece 4 implementações da 

interface WorkFlow :

Esses são os únicos fluxos básicos que você precisa saber para começar a criar fluxos de trabalho 

com o framework. Você não precisa aprender uma notação ou conceitos complexos, apenas 

algumas APIs naturais que são fáceis de pensar. 

# Definindo uma unidade de trabalho 

Uma unidade de trabalho no framework é representada pela interface Work :

As implementações desta interface devem: 

capturar qualquer exceção marcada ou não verificada e retornar  

> WorkStatus#FAILED

no WorkReport 

certificar-se de que o trabalho seja concluído em um período de tempo finito 

Um nome de trabalho deve ser exclusivo em uma definição de fluxo de trabalho. Cada trabalho 

deve retornar um WorkReport no final da execução. Este relatório pode servir como condição 

para o próximo trabalho no fluxo de trabalho por meio de WorkReportPredicate .      

> public interface Work {
> String getName ();
> WorkReport execute (WorkContext workContext );
> }

# Definindo um fluxo de trabalho 

Um fluxo de trabalho no Framework é representado pela interface WorkFlow :

Um fluxo de trabalho também é um trabalho. Isso é o que torna os fluxos de trabalho 

combináveis. O framework vem com 4 implementações da interface WorkFlow :

Fluxo condicional; 

Fluxo sequencial; 

Fluxo repetido; 

Fluxo paralelo. 

Fluxo condicional 

Um fluxo condicional é definido por 4 artefatos: 

A unidade de trabalho a ser executada primeiro; 

A WorkReportPredicate para a lógica condicional; 

A unidade de trabalho a ser executada se o predicado for satisfeito; 

A unidade de trabalho a ser executada se o predicado não for satisfeito (opcional). 

Para criar um ConditionalFlow , você pode usar ConditionalFlow.Builder :

Fluxo sequencial 

A SequentialFlow , como o próprio nome indica, executa um conjunto de unidades de trabalho 

em sequência. Se uma unidade de trabalho falhar, as próximas unidades de trabalho no pipeline 

serão ignoradas. Para criar um SequentialFlow , você pode usar SequentialFlow.Builder :

public interface WorkFlow extends Work {

}

ConditionalFlow conditionalFlow = ConditionalFlow .Builder .aNewConditionalFlow () 

.named ("meu fluxo condicional" )

.execute (work1 )

.when (WorkReportPredicate .COMPLETED )

.then (work2 )

.otherwise (work3 )

.build (); 

SequentialFlow sequentialFlow = SequentialFlow .Builder .aNewSequentialFlow () 

.named ("executar 'work1', 'work2' e 'work3' em sequência" )

.execute (work1 )

.then (work2 )

.then (work3 )

.build (); Fluxo paralelo 

Um fluxo paralelo executa um conjunto de unidades de trabalho em paralelo. O status de uma 

execução de fluxo paralelo é definido como: 

WorkStatus#COMPLETED : Se todas as unidades de trabalho foram concluídas com sucesso; 

WorkStatus#FAILED : Se uma das unidades de trabalho falhou. 

Para criar um ParallelFlow , você pode usar ParallelFlow.Builder :

Obs: É responsabilidade do chamador gerenciar o ciclo de vida do serviço do executor. 

Fluxo repetido 

A RepeatFlow executa um determinado trabalho em loop até que uma condição se torne 

true ou por um número fixo de vezes. A condição é expressa usando um 

WorkReportPredicate . Para criar um RepeatFlow , você pode usar RepeatFlow.Builder :

Esses são os fluxos básicos que você precisa saber para começar a criar fluxos de trabalho com o 

framework. 

# Criação de fluxos personalizados 

Você pode criar seus próprios fluxos implementando a interface WorkFlow . O 

WorkFlowEngine funciona com interfaces, portanto, sua implementação deve ser interoperável 

com fluxos integrados sem qualquer problema. 

ExecutorService executorService = .. 

ParallelFlow parallelFlow = ParallelFlow .Builder .aNewParallelFlow () 

.named ("executar 'work1', 'work2' e 'work3' em paralelo" )

.execute (work1 , work2 , work3 )

.with (executorService )

.build (); 

executorService .shutdown (); 

RepeatFlow repeatFlow = RepeatFlow .Builder .aNewRepeatFlow () 

.named ("executar work 3 vezes" )

.repeat (work )

.times (3)

.build (); 

// ou 

RepeatFlow repeatFlow = RepeatFlow .Builder .aNewRepeatFlow () 

.named ("executar work indefinidamente!" )

.repeat (work )

.until (WorkReportPredicate .ALWAYS_TRUE )

.build (); Exemplo 

Este é um tutorial simples sobre as principais APIs do framework. Primeiro, vamos escrever um 

trabalho: 

Esta unidade de trabalho imprime uma determinada mensagem na saída padrão. Agora, vamos 

supor que queremos criar o seguinte fluxo de trabalho: 

1. imprima "Olá" três vezes 

2. em seguida, imprima "olá" e "mundo" em paralelo 

3. então, se "olá" e "mundo" forem impressos com sucesso no console, imprima "ok", caso 

contrário imprima "nok" 

Este fluxo de trabalho pode ser ilustrado da seguinte forma:  

> flow1

é um RepeatFlow dos work1 quais está imprimindo "Vasco" três vezes  

> flow2

é um ParallelFlow de work2 e work3 que imprimem respectivamente "olá" e "                        

> class ImprimirMensagemWork implements Work {
> private String mensagem ;
> public ImprimirMensagemWork (String mensagem ){
> this .mensagem =mensagem ;
> }
> public String getName () {
> return "imprimir mensagem" ;
> }
> public WorkReport execute (WorkContext workContext ){
> System .out .println (mensagem );
> return new DefaultWorkReport (WorkStatus .COMPLETED ,workContext );
> }
> }

mundo" em paralelo 

flow3 é um ConditionalFlow . Ele primeiro executa flow2 (um fluxo de trabalho também é 

um trabalho), então se flow2 for concluído, ele executa work4 (imprimir "ok"), caso 

contrário, executa work5 (imprimir "não") 

flow4 é um SequentialFlow . Em flow1 seguida, ele executa flow3 em sequência. 

Com o framework, este fluxo de trabalho pode ser implementado com o seguinte trecho de 

código: 

# archbase-logger 

A falta de informação sobre quem está acessando, quando acessou e de onde acessou nossa API 

pode ser frustrante para um monitoramento e para a resolução de problemas. 

Com isso a proposta é uma API de logs que gere de forma automática através de AOP todo o 

controle de log dos nossos Recursos. Os itens registrados no log incluem todos os parâmetros, 

valor retornado e alguns dados de contexto, como URL de solicitação da web e nome de usuário 

do usuário. 

ImprimirMensagemWork work1 = new ImprimirMensagemWork ("Vasco" ); 

ImprimirMensagemWork work2 = new ImprimirMensagemWork ("Olá" ); 

ImprimirMensagemWork work3 = new ImprimirMensagemWork ("Mundo" ); 

ImprimirMensagemWork work4 = new ImprimirMensagemWork ("ok" ); 

ImprimirMensagemWork work5 = new ImprimirMensagemWork ("não" ); 

ExecutorService executorService = Executors .newFixedThreadPool (2); 

WorkFlow workflow = aNewSequentialFlow () // flow 4

.execute (aNewRepeatFlow () // flow 1

.named ("imprimindo Vasco 3 vezes" )

.repeat (work1 )

.times (3)

.build ()) 

.then (aNewConditionalFlow () // flow 3

.execute (aNewParallelFlow () // flow 2

.named ("imprimindo 'Olá' e 'Mundo' em paralelo" )

.execute (work2 , work3 )

.with (executorService )

.build ()) 

.when (WorkReportPredicate .COMPLETED )

.then (work4 )

.otherwise (work5 )

.build ()) 

.build (); 

WorkFlowEngine workFlowEngine = aNewWorkFlowEngine (). build (); 

WorkContext workContext = new WorkContext (); 

WorkReport workReport = workFlowEngine .run (workflow , workContext ); 

executorService .shutdown (); Funcionalidades do controle de log: 

1. Registra automaticamente todas as APIs, incluindo entrada e saída. 

2. Registra automaticamente os erros que ocorrem na API. 

3. Nenhum efeito colateral na implementação real da API devido à lógica AOP. 

4. Vincula-se automaticamente a novas APIs graças à AOP weaving. 

5. Remove informações confidenciais em registros para manter a segurança e privacidade. 

6. Exibe o tamanho do arquivo se uma das entradas ou saídas da API for qualquer objeto de 

arquivo. 

7. Funciona com testes de integração. 

8. Detecta objetos simulados na entrada e saída e os exibe de acordo, como pode acontecer 

durante o teste de integração. 

9. O comportamento de registro é facilmente personalizável. 

Habilitando o @Logging nas classes controladoras(recursos): 

# Limpeza de dados 

Esta biblioteca permite ocultar o registro de informações confidenciais. No momento, isso 

funciona apenas com argumentos de método, mas o suporte para campos arbitrários em objetos 

está a caminho. 

A depuração de dados é habilitada por padrão e é recomendado mantê-la dessa forma. 

Um parâmetro de método é limpo se seu nome cair dentro dos seguintes critérios: 

É um dos seguintes valores (não diferencia maiúsculas de minúsculas): 

password 

passwd 

secret 

authorization 

api_key 

apikey 

access_token 

accesstoken 

senha 

Está contido na lista negra personalizada fornecida a setCustomParamBlacklist() 

Corresponde ao regex personalizado fornecido para setParamBlacklistRegex() 

O valor de qualquer parâmetro que corresponda aos critérios mencionados acima é apagado e 

substituído por "xxxxx". O valor limpo também pode ser personalizado, passando o valor 

desejado para o setDefaultScrubbedValue() método.    

> @RestController
> @Logging
> public class PessoaResource {
> ...
> }

Um exemplo completo com todas as opções de personalização usadas: 

# Customização 

O registro é controlado por duas anotações @Logging e @NoLogging . Os dois podem ser usados 

juntos para obter um controle refinado sobre quais métodos são registrados e quais não são. 

As anotações @Logging e @NoLogging podem ser usadas na classe e também nos métodos. A 

anotação em nível de método tem prioridade sobre a anotação em nível de classe. Isso pode ser 

usado para habilitar o registro para todos os métodos do controlador e excluir alguns, ou vice-

versa. 

Outras personalizações podem ser feitas estendendo a GenericControllerAspect classe ou 

criar seu outro aspecto implementando a ControllerAspect interface. 

Exemplo de log gerado: 

2018-02-26 16:52:35.419 INFO : getUser() called via url [ http://localhost:8080/get-random-

user ], username [username] 

2018-02-26 16:52:35.624 INFO : getUser() took [1.8 ms] to complete                   

> @Bean
> public GenericControllerAspect genericControllerAspect () {
> GenericControllerAspect aspect =new GenericControllerAspect ();
> aspect .setEnableDataScrubbing (true );
> aspect .setDefaultScrubbedValue ("*******" );
> aspect .setParamBlacklistRegex ("account.*" );
> aspect .setCustomParamBlacklist (new HashSet <>
> (Arrays .asList ("securityProtocol" )));
> return aspect ;
> }
> @RestController
> @Logging
> public class PessoaResource {
> @RequestMapping ("/olaMundo" )
> public String olaMundo () {
> ...
> }
> @RequestMapping ("/bye" )
> @NoLogging
> public String bye () {
> ...
> }
> }

2018-02-26 16:52:35.624 INFO : getUser() returned: [User(id=SP-937-215, email= jeanlucpic 

ard@starfleet.com , password=xxxxx, firstName=Jean-Luc, lastName=Picard)] 

2018-02-26 16:52:35.419 INFO : getMemo() called via url [ http://localhost:8080/get-memo ], 

username [username] 

2018-02-26 16:52:35.624 INFO : getMemo() took [0.2 ms] to complete 

2018-02-26 16:52:35.624 INFO : getMemo() returned: [Memo(id=m_xyz_123, text=Hello, 

World! From a Memo!)] 

2018-02-26 16:52:35.419 INFO : getUser() called via url [ http://localhost:8080/get-random-

user ], username [username] 

2018-02-26 16:52:35.624 INFO : getUser() took [1.8 ms] to complete 

2018-02-26 16:52:35.624 INFO : getUser() returned: [User(id=SP-937-215, email= ragnar@d 

b1.com.br , password=xxxxx, nome=Ragnar, apelido=Telmo)] 

2018-02-26 16:52:35.419 INFO : getMemo() called via url [ http://localhost:8080/get-memo ], 

username [username] 

# archbase-spring-boot-configuration 

# archbase-spring-boot-starter 

# archbase-validation-ddd-model 

Um dos grandes desafios no desenvolvimento de aplicações é garantir que os contratos estejam 

sendo seguidos e que o modelo está padronizado. 

# Por que testar sua arquitetura? 

A maioria dos desenvolvedores trabalhando em projetos maiores conhece a história, onde uma 

vez alguém experiente olhou para o código e desenhou alguns diagramas de arquitetura 

agradáveis, mostrando os componentes em que o sistema deveria consistir e como eles deveriam 

interagir. Mas quando o projeto ficou maior, os casos de uso mais complexos e novos 

desenvolvedores entraram e os antigos desistiram, havia mais e mais casos em que novos 

recursos eram simplesmente adicionados de qualquer maneira que se encaixasse. E de repente 

tudo dependia de tudo e toda mudança poderia ter um efeito imprevisível em qualquer outro 

componente. Claro que você pode ter um ou vários desenvolvedores experientes, tendo o papel 

do arquiteto, que olha o código uma vez por semana, identifica as violações e as corrige. 

Especialmente em um projeto ágil, onde o papel do arquiteto pode até ser distribuído, os 

desenvolvedores devem ter uma linguagem comum e compreensão dos componentes e suas 

relações. Quando o projeto evolui, os componentes sobre os quais você fala também precisam 

evoluir. Caso contrário, construções estranhas aparecerão repentinamente, tentando forçar os 

casos de uso em uma estrutura de componente que não se ajusta de forma alguma. Se você tiver 

testes de arquitetura automáticos, poderá desenvolver as regras, ver onde os componentes 

antigos precisam ser alterados e garantir que os novos componentes estejam em conformidade 

com o entendimento comum dos desenvolvedores / arquitetos. No geral, isso contribuirá para a qualidade da base de código e evitará um declínio na velocidade de desenvolvimento. Além disso, 

os novos desenvolvedores terão muito mais facilidade para se familiarizar com o código e 

acelerar seu desenvolvimento. 

Como criamos vários contratos para auxiliar na aplicação dos conceitos de DDD, fizemos também 

uma módulo para ajudar a validar este modelo. 

Foi criado conjunto de regras do ArchUnit que permite a verificação de modelos de domínio. Em 

suma, as regras aqui verificam: 

Os agregados pessoais-se apenas a entidades que são declaradas como parte dele. 

As referências a outros agregados são propriedade por meio de Associations ou referências 

de identificador.