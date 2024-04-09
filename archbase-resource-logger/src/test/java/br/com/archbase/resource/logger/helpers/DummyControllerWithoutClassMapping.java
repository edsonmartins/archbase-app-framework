package br.com.archbase.resource.logger.helpers;

import jakarta.annotation.Nonnull;

public class DummyControllerWithoutClassMapping {

    public String nonRestApiMethodWithArgs(@Nonnull String arg) {
        return arg;
    }

}
