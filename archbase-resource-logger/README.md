# archbase-resource-logger

Aspecto que registra em log as chamadas a métodos de controller marcados com `@Logging`: argumentos, URL, usuário,
tempo de execução, retorno e exceção.

```xml
<dependency>
    <groupId>br.com.archbase</groupId>
    <artifactId>archbase-resource-logger</artifactId>
    <version>3.5.0</version>
</dependency>
```

Já vem no `archbase-starter-core`.

## O que traz

`spring-boot-starter-web`, `spring-security-core` (lê o usuário do `SecurityContextHolder`), `aspectjweaver` e
`jackson-databind`.

## Registro do aspecto

O módulo não tem auto-configuração nem propriedades. `SimpleArchbaseResourceAspect` é anotado com `@Aspect`, mas não
com `@Component`: alguém precisa declará-lo como bean.

- Com o `archbase-starter-core`, o `ArchbaseServerMvcConfiguration` declara o bean `genericControllerAspect`
  (`new SimpleArchbaseResourceAspect()`), sob `archbase.web.mvc.enabled` (padrão `true`) e na ausência de um bean
  chamado `customWebMvcConfigurer`. Ver
  [archbase-starter-core](../archbase-starter-core/README.md#auto-configuração).
- Sem o starter, declare-o:

```java
@Configuration
public class LogConfig {

    @Bean
    public SimpleArchbaseResourceAspect resourceAspect() {
        SimpleArchbaseResourceAspect aspect = new SimpleArchbaseResourceAspect(
                LoggerFactory.getLogger("api.resource"));
        aspect.setCustomParamBlacklist(Set.of("cpf"));
        return aspect;
    }
}
```

Com o starter os dois beans coexistiriam e cada chamada seria registrada duas vezes; para ajustar o aspecto do
starter, injete o bean `genericControllerAspect` e chame os setters.

## Quais métodos são registrados

O aspecto age (`@Around` e `@AfterThrowing`) quando as três condições valem:

1. a classe é anotada com `@RestController` ou `@Controller`;
2. o método tem `@Logging`, ou a classe tem `@Logging`;
3. o método **não** tem `@NoLogging`, e a classe também não (salvo se o próprio método tiver `@Logging`).

```java
@RestController
@RequestMapping("/api/v1/clientes")
@Logging
public class ClienteController {

    @GetMapping("/{id}")
    public ClienteDto buscar(@PathVariable String id) { ... }

    @NoLogging
    @PostMapping("/importar")
    public void importar(@RequestBody byte[] arquivo) { ... }
}
```

`@NoLogging` na classe silencia todos os métodos dela; um método com `@Logging` volta a ser registrado.

## O que vai para o log

Tudo em nível INFO, exceto falhas de serialização (WARN) e erro ao montar a mensagem de entrada (ERROR):

| Momento | Conteúdo |
|---|---|
| Antes | `metodo() chamado com argumentos: nome: [valor], ... chamado via url: [...], username: [...]` |
| Depois | `metodo() levou [N ms] para concluir` e `metodo() retornou: [...]` |
| Exceção | `metodo() lançou exceção: [toString da exceção]`, em INFO; a exceção segue para o chamador |

- **URL**: `request.getRequestURL()`, sem query string. **Usuário**: `Authentication.getName()` do
  `SecurityContextHolder`.
- **Argumentos**: por padrão, `toString()` do valor. `ByteArrayResource` e `MultipartFile` aparecem como tamanho em
  bytes. Argumentos `@RequestBody` são serializados em JSON quando o método tem `@RequestMapping(consumes =
  "application/json")`.
- **Retorno**: `toString()`, ou JSON quando o `@RequestMapping` do método ou da classe declara
  `produces = "application/json"`; `void` aparece como `void`.

O JSON é gerado por `JsonUtil` (Jackson 3, `tools.jackson`, com `JsonMapper` padrão, não o `ObjectMapper` da
aplicação).

### Mascaramento

Argumentos cujo **nome do parâmetro** está na lista abaixo (sem diferenciar maiúsculas) aparecem como `xxxxx`:

`password`, `passwd`, `senha`, `token`, `pwd`, `secret`, `authorization`, `api_key`, `apikey`, `access_token`,
`accesstoken`

Setters (em `ArchbaseLoggerAspect`): `setCustomParamBlacklist(Set<String>)` acrescenta nomes,
`setParamBlacklistRegex(String)` acrescenta um padrão (casamento com o nome inteiro), `setDefaultScrubbedValue(String)`
troca o `xxxxx`, `setEnableDataScrubbing(false)` desliga o mascaramento. `setLog(Logger)` troca o logger.

## Limitações

- **O mascaramento é só pelo nome do parâmetro.** Campos dentro de um DTO (`@RequestBody`), do retorno ou da
  mensagem de exceção são registrados por inteiro, sem máscara. Um `@RequestBody LoginRequest request` com senha vai
  para o log se o método for registrado e o corpo for serializado (ou se o `toString()` do DTO incluir a senha).
- Até a 3.5.0: `@GetMapping`/`@PostMapping` não contavam para `consumes`/`produces` (corpo e retorno saíam por
  `toString()`); o logger padrão tinha a categoria `java.lang.String`; `@NoLogging` na classe não tinha efeito; e
  sem `Authentication` no contexto a linha de entrada era trocada por um ERROR. O logger agora é
  `br.com.archbase.resource.logger.aspect.SimpleArchbaseResourceAspect`.
- A resposta inteira vai para o log em INFO em todo método registrado. Use `@Logging` com parcimônia em endpoints
  que devolvem listas grandes ou dados pessoais.

## Referência

- [archbase-starter-core](../archbase-starter-core/README.md)
- Módulos do framework: [CLAUDE.md](../CLAUDE.md)
