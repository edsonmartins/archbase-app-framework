package br.com.archbase.event.driven.bus.event;

import br.com.archbase.event.driven.spec.event.contracts.EventBus;
import br.com.archbase.event.driven.spec.event.contracts.EventSubscriber;
import br.com.archbase.event.driven.spec.event.contracts.Subscriber;

public class SimpleEventBusSubscriber implements EventSubscriber {

    private final EventBus mEventBus;

    public SimpleEventBusSubscriber(EventBus eventBus) {
        mEventBus = eventBus;
    }

    @Override
    public <R> R subscribe(Subscriber subscriber) {
        return mEventBus.subscribe(subscriber);
    }

    @Override
    public <R> R unsubscribe(Subscriber subscriber) {
        return mEventBus.unsubscribe(subscriber);
    }
}


