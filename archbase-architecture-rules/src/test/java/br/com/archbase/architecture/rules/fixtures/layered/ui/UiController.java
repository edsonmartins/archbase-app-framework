package br.com.archbase.architecture.rules.fixtures.layered.ui;

import br.com.archbase.architecture.rules.fixtures.layered.app.AppService;
import br.com.archbase.architecture.rules.fixtures.layered.domain.DomainModel;

/** Interface -> Application (válido) e Interface -> Domain (válido só no modo relaxado). */
public class UiController {
    AppService service;
    DomainModel model;
}
