package br.com.archbase.event.driven.spec.outbox.contracts;

public interface OutboxListener {

    public void onExportedEvent(Outboxable event);

}
