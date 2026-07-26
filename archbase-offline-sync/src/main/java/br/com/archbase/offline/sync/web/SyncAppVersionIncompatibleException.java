package br.com.archbase.offline.sync.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** App em versão incompatível: força atualização (HTTP 426 Upgrade Required). */
@ResponseStatus(HttpStatus.UPGRADE_REQUIRED)
public class SyncAppVersionIncompatibleException extends RuntimeException {
    public SyncAppVersionIncompatibleException(String message) {
        super(message);
    }
}
