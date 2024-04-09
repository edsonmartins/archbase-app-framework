# archbase-workflow-process


Trata-se de um mecanismo de fluxo de trabalho para Java. Ele fornece APIs e blocos de construção simples para facilitar a criação e a execução de fluxos de trabalho combináveis.

Uma unidade de trabalho no framework é representada pela interface`Work`. Um fluxo de trabalho é representado pela interface `WorkFlow`. O framework fornece 4 implementações da interface `WorkFlow`:

![enter image description here](https://imgur.com/download/yrdzWJr/Fluxos)


Esses são os únicos fluxos básicos que você precisa saber para começar a criar fluxos de trabalho com o framework. Você não precisa aprender uma notação ou conceitos complexos, apenas algumas APIs naturais que são fáceis de pensar.


## Definindo uma unidade de trabalho

Uma unidade de trabalho no framework é representada pela interface `Work`:

```java
public interface Work {
    String getName();
    WorkReport execute(WorkContext workContext);
}
```

As implementações desta interface devem:

-   capturar qualquer exceção marcada ou não verificada e retornar `WorkStatus#FAILED`no`WorkReport`
-   certificar-se de que o trabalho seja concluído em um período de tempo finito

Um nome de trabalho deve ser exclusivo em uma definição de fluxo de trabalho. Cada trabalho deve retornar um `WorkReport`no final da execução. Este relatório pode servir como condição para o próximo trabalho no fluxo de trabalho por meio de  `WorkReportPredicate`.


## Definindo um fluxo de trabalho

Um fluxo de trabalho no Framework é representado pela interface `WorkFlow`:

```java
public interface WorkFlow extends Work {

}
```

Um fluxo de trabalho também é um trabalho. Isso é o que torna os fluxos de trabalho combináveis. O framework vem com 4 implementações da interface `WorkFlow`:

- Fluxo condicional;
- Fluxo sequencial;
- Fluxo repetido;
- Fluxo paralelo.

### Fluxo condicional

Um fluxo condicional é definido por 4 artefatos:

-   A unidade de trabalho a ser executada primeiro;
-   A `WorkReportPredicate`para a lógica condicional;
-   A unidade de trabalho a ser executada se o predicado for satisfeito;
-   A unidade de trabalho a ser executada se o predicado não for satisfeito (opcional).

Para criar um `ConditionalFlow`, você pode usar `ConditionalFlow.Builder`:

```java
ConditionalFlow conditionalFlow = ConditionalFlow.Builder.aNewConditionalFlow()
        .named("meu fluxo condicional")
        .execute(work1)
        .when(WorkReportPredicate.COMPLETED)
        .then(work2)
        .otherwise(work3)
        .build();
```
### Fluxo sequencial

A `SequentialFlow`, como o próprio nome indica, executa um conjunto de unidades de trabalho em sequência. Se uma unidade de trabalho falhar, as próximas unidades de trabalho no pipeline serão ignoradas. Para criar um `SequentialFlow` , você pode usar `SequentialFlow.Builder`:

```java
SequentialFlow sequentialFlow = SequentialFlow .Builder.aNewSequentialFlow()
        .named("executar 'work1', 'work2' e 'work3' em sequência")
        .execute(work1)
        .then(work2)
        .then(work3)
        .build();
```

### Fluxo paralelo

Um fluxo paralelo executa um conjunto de unidades de trabalho em paralelo. O status de uma execução de fluxo paralelo é definido como:

-   `WorkStatus#COMPLETED` : Se todas as unidades de trabalho foram concluídas com sucesso;
-   `WorkStatus#FAILED` : Se uma das unidades de trabalho falhou.

Para criar um `ParallelFlow`, você pode usar `ParallelFlow.Builder`:

```java
ExecutorService executorService = ..
ParallelFlow parallelFlow = ParallelFlow .Builder.aNewParallelFlow()
        .named("executar 'work1', 'work2' e 'work3' em paralelo")
        .execute(work1, work2, work3)
        .with(executorService)
        .build();
executorService.shutdown();
```
Obs: É responsabilidade do chamador gerenciar o ciclo de vida do serviço do executor.

### Fluxo repetido

A `RepeatFlow`executa um determinado trabalho em loop até que uma condição se torne `true`ou por um número fixo de vezes. A condição é expressa usando um `WorkReportPredicate`. Para criar um `RepeatFlow`, você pode usar `RepeatFlow.Builder`:

```java
RepeatFlow repeatFlow = RepeatFlow .Builder.aNewRepeatFlow()
        .named("executar work 3 vezes")
        .repeat(work)
        .times(3)
        .build();

// ou

RepeatFlow repeatFlow = RepeatFlow .Builder.aNewRepeatFlow()
        .named("executar work indefinidamente!")
        .repeat(work)
        .until(WorkReportPredicate.ALWAYS_TRUE)
        .build();
```

Esses são os fluxos básicos que você precisa saber para começar a criar fluxos de trabalho com o framework.

## Criação de fluxos personalizados

Você pode criar seus próprios fluxos implementando a interface `WorkFlow`. O `WorkFlowEngine`funciona com interfaces, portanto, sua implementação deve ser interoperável com fluxos integrados sem qualquer problema.


## Exemplo

Este é um tutorial simples sobre as principais APIs do framework. Primeiro, vamos escrever um trabalho:

```java
class ImprimirMensagemWork implements Work {

    private String mensagem;

    public ImprimirMensagemWork(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getName() {
        return "imprimir mensagem";
    }

    public WorkReport execute(WorkContext workContext) {
        System.out.println(mensagem);
        return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
    }
}
```

Esta unidade de trabalho imprime uma determinada mensagem na saída padrão. Agora, vamos supor que queremos criar o seguinte fluxo de trabalho:

1.  imprima "Olá" três vezes
2.  em seguida, imprima "olá" e "mundo" em paralelo
3.  então, se "olá" e "mundo" forem impressos com sucesso no console, imprima "ok", caso contrário imprima "nok"

Este fluxo de trabalho pode ser ilustrado da seguinte forma:

![enter image description here](https://imgur.com/download/rRxJtGK/FLUXO+EXEMPLO)

-   `flow1`é um `RepeatFlow`dos `work1`quais está imprimindo "Vasco" três vezes
-   `flow2`é um `ParallelFlow`de `work2`e `work3`que imprimem respectivamente "olá" e " mundo" em paralelo
-   `flow3`é um `ConditionalFlow`. Ele primeiro executa `flow2`(um fluxo de trabalho também é um trabalho), então se `flow2`for concluído, ele executa `work4`(imprimir "ok"), caso contrário, executa `work5` (imprimir "não")
-   `flow4`é um `SequentialFlow`. Em `flow1`seguida, ele executa `flow3`em sequência.
- 
Com o framework, este fluxo de trabalho pode ser implementado com o seguinte trecho de código:

```java
ImprimirMensagemWork work1 = new ImprimirMensagemWork("Vasco");
ImprimirMensagemWork work2 = new ImprimirMensagemWork("Olá");
ImprimirMensagemWork work3 = new ImprimirMensagemWork("Mundo");
ImprimirMensagemWork work4 = new ImprimirMensagemWork("ok");
ImprimirMensagemWork work5 = new ImprimirMensagemWork("não");

ExecutorService executorService = Executors.newFixedThreadPool(2);
WorkFlow workflow = aNewSequentialFlow() // flow 4
        .execute(aNewRepeatFlow() // flow 1
                    .named("imprimindo Vasco 3 vezes")
                    .repeat(work1)
                    .times(3)
                    .build())
        .then(aNewConditionalFlow() // flow 3
                .execute(aNewParallelFlow() // flow 2
                            .named("imprimindo 'Olá' e 'Mundo' em paralelo")
                            .execute(work2, work3)
                            .with(executorService)
                            .build())
                .when(WorkReportPredicate.COMPLETED)
                .then(work4)
                .otherwise(work5)
                .build())
        .build();

WorkFlowEngine workFlowEngine = aNewWorkFlowEngine().build();
WorkContext workContext = new WorkContext();
WorkReport workReport = workFlowEngine.run(workflow, workContext);
executorService.shutdown();
```