package br.com.archbase.event.driven.bus.event;

import br.com.archbase.event.driven.spec.event.contracts.Event;
import br.com.archbase.event.driven.spec.event.contracts.EventBus;
import br.com.archbase.event.driven.spec.event.contracts.EventPublisher;

public class SimpleEventBusPublisher implements EventPublisher {

    private final br.com.archbase.event.driven.spec.event.contracts.EventBus mEventBus;

    public SimpleEventBusPublisher(EventBus eventBus) {
        mEventBus = eventBus;
    }

    @Override
    public <R> R publish(Event<R> event) {
        mEventBus.publish(event);
        return null;
    }
}
