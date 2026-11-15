package br.com.archbase.architecture.rules.fixtures.layered.domain;

import br.com.archbase.architecture.rules.fixtures.layered.infra.InfraClock;

/** Domain -> Infrastructure: válido (infraestrutura é a camada de baixo). */
public class DomainModel {
    InfraClock clock;
}
