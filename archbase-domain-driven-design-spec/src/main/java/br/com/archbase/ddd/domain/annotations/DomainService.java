package br.com.archbase.ddd.domain.annotations;

import java.lang.annotation.*;

/**
 * Identifica um serviço {@link DomainService} de domínio. Um serviço é um processo ou transformação significativa no domínio que não é um
 * responsabilidade natural de uma entidade ou objeto de valor, adicionar uma operação ao modelo como uma interface autônoma declarada
 * como serviço. Defina um contrato de serviço, um conjunto de afirmações sobre as interações com o serviço. (Veja as afirmações.)
 * Enuncie essas afirmações na linguagem ubiqua de um contexto limitado específico. Dê um nome ao serviço, que também
 * torna-se parte da linguagem ubiqua.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface DomainService {

}
