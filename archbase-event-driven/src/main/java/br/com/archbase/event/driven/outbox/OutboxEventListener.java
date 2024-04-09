package br.com.archbase.event.driven.outbox;

import br.com.archbase.event.driven.spec.outbox.contracts.OutboxListener;
import br.com.archbase.event.driven.spec.outbox.contracts.Outboxable;

public class OutboxEventListener implements OutboxListener {

    @Override
    public void onExportedEvent(Outboxable event) {
        //
    }
}
