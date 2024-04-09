package br.com.archbase.ddd.infraestructure.events;

import br.com.archbase.ddd.domain.contracts.Event;
import br.com.archbase.ddd.domain.contracts.EventHandler;
import br.com.archbase.ddd.domain.contracts.EventPublisher;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Log4j2
@Component("eventPublisher")
public class SimpleEventPublisher implements EventPublisher {

    private Set<EventHandler> eventHandlers = new HashSet<>();

    public void registerEventHandler(EventHandler handler) {
        eventHandlers.add(handler);
    }

    @Override
    public void publish(Event event) {
        doPublish(event);
    }

    protected void doPublish(Object event) {
        for (EventHandler handler : new ArrayList<EventHandler>(eventHandlers)) {
            if (handler.canHandle(event)) {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    log.error("event handling error", e);
                }
            }
        }
    }
}
