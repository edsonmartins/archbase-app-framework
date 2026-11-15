package br.com.archbase.architecture.rules.fixtures.layered.domain;

import br.com.archbase.architecture.rules.fixtures.layered.app.AppService;

/** Domain -> Application: VIOLAÇÃO. */
public class DomainCallingUp {
    AppService service;
}
