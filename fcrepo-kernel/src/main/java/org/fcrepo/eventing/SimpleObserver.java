
package org.fcrepo.eventing;

import static com.google.common.collect.Collections2.filter;
import static org.fcrepo.FedoraResourceType.Datastream;
import static org.fcrepo.FedoraResourceType.Object;
import static org.fcrepo.utils.FedoraTypesUtils.isFedoraDatastream;
import static org.fcrepo.utils.FedoraTypesUtils.isFedoraObject;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.fcrepo.FedoraResourceType;
import org.fcrepo.eventing.impl.FedoraEventImpl;
import org.modeshape.jcr.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList.Builder;

@Default
public class SimpleObserver implements EventListener {

    final private Integer eventTypes = Event.NODE_ADDED + Event.NODE_REMOVED +
            Event.NODE_MOVED + Event.PROPERTY_ADDED + Event.PROPERTY_CHANGED +
            Event.PROPERTY_REMOVED;

    @Inject
    private Repository repository;

    @Inject
    public FedoraEventBus eventBus;

    @Inject
    private EventFilter eventFilter;

    private Session session;

    final private Logger logger = LoggerFactory.getLogger(SimpleObserver.class);

    @PostConstruct
    public void buildListener() {
        logger.debug("Initializing " + this.getClass().getCanonicalName());
        try {
            session = repository.login();
            session.getWorkspace().getObservationManager().addEventListener(
                    this, eventTypes, "/", true, null, null, false);
            session.save();
        } catch (RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    // it's okay to suppress type-safety warning here,
    // because we know that EventIterator only produces
    // Events, like an Iterator<Event>
    @SuppressWarnings("unchecked")
    @Override
    public void onEvent(EventIterator events) {
        logger.debug("Received " + events.getSize() + " JCR events.");
        for (Event e : filter(new Builder<Event>().addAll(events).build(),
                eventFilter)) {
            logger.debug("Putting event: " + e.toString() + " on the bus.");
            FedoraResourceType type = null;
            try {
                Node n = session.getNode(e.getPath());
                if (isFedoraObject.apply(n)) {
                    type = Object;
                }
                if (isFedoraDatastream.apply(n)) {
                    type = Datastream;
                }
            } catch (RepositoryException e1) {
                throw new IllegalStateException(e1);
            }
            FedoraEvent fe = new FedoraEventImpl(e, type);
            eventBus.post(fe);
        }
    }

    @PreDestroy
    public void logoutSession() {
        session.logout();
    }

}
