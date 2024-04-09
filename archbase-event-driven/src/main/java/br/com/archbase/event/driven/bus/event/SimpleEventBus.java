package br.com.archbase.event.driven.bus.event;

import br.com.archbase.event.driven.spec.event.contracts.Event;
import br.com.archbase.event.driven.spec.event.contracts.EventBus;
import br.com.archbase.event.driven.spec.event.contracts.Subscriber;

import java.util.HashSet;
import java.util.Set;


public class SimpleEventBus implements EventBus {

    private final Set<Subscriber> mSubscribers = new HashSet<>();


    @Override
    public <R> R subscribe(Subscriber subscriber) {
        mSubscribers.add(subscriber);
        return null;
    }

    @Override
    public <R> R unsubscribe(Subscriber subscriber) {
        mSubscribers.remove(subscriber);
        return null;
    }


    @Override
    public <R> R publish(Event<R> event) {
        for (Subscriber subscriber : mSubscribers) {
            subscriber.onEvent(event);
        }
        return null;
    }
}