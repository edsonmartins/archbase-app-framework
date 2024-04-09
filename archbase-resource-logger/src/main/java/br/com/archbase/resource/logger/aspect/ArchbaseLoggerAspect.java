package br.com.archbase.resource.logger.aspect;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Classe base para qualquer implementação de aspecto do registrador.
 * Ele contém configurações e ajustes globais a serem usados por todos os registradores.
 *
 * @author edsonmartins
 */
public abstract class ArchbaseLoggerAspect {


    protected Set<String> paramBlacklist = new HashSet<>(Arrays.asList(
            "password",
            "passwd",
            "senha",
            "token",
            "pwd",
            "secret",
            "authorization",
            "api_key",
            "apikey",
            "access_token",
            "accesstoken"
    ));

    @Nonnull
    protected String scrubbedValue = "xxxxx";

    protected boolean enableDataScrubbing = true;

    @Nullable
    protected Pattern paramBlacklistRegex;

    public void setDefaultScrubbedValue(@Nonnull String defaultScrubbedValue) {
        scrubbedValue = defaultScrubbedValue;
    }

    public void setEnableDataScrubbing(boolean enableDataScrubbing) {
        this.enableDataScrubbing = enableDataScrubbing;
    }

    public void setParamBlacklistRegex(@Nonnull String paramBlacklistRegex) {
        this.paramBlacklistRegex = Pattern.compile(paramBlacklistRegex);
    }

    public void setCustomParamBlacklist(@Nonnull Set<String> customParamBlacklist) {
        customParamBlacklist.forEach(i -> paramBlacklist.add(i.toLowerCase()));
    }

}
