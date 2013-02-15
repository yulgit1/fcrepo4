
package org.fcrepo.observer;

import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_DATASTREAM;
import static org.fcrepo.utils.FedoraJcrTypes.FEDORA_OBJECT;
import static org.junit.Assert.assertEquals;
import static org.modeshape.jcr.api.JcrConstants.JCR_DATA;
import static org.modeshape.jcr.api.JcrConstants.NT_RESOURCE;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.fcrepo.AbstractTest;
import org.fcrepo.eventing.FedoraEvent;
import org.fcrepo.eventing.FedoraEventBus;
import org.fcrepo.eventing.SimpleObserver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.Subscribe;

public class SimpleObserverTest extends AbstractTest {

    private Integer eventBusMessageCount;

    @Inject
    private SimpleObserver observer;

    @Inject
    private Repository repository;

    @Inject
    private FedoraEventBus eventBus;

    @Test
    public void TestEventBusPublishing() throws RepositoryException {

        Session se = repository.login();
        final Node obj = se.getRootNode().addNode("object1");
        obj.addMixin(FEDORA_OBJECT);
        final Node ds = obj.addNode("datastream");
        ds.addMixin(FEDORA_DATASTREAM);
        ds.addNode("jcr:content", NT_RESOURCE).setProperty(JCR_DATA,
                "test datastream");
        se.save();
        se.logout();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Should be two messages, for each time
        // each node becomes a Fedora object

        assertEquals("Where are my messages!?", (Integer) 2,
                eventBusMessageCount);

    }

    @Subscribe
    public void countMessages(FedoraEvent e) {
        eventBusMessageCount++;
    }

    @Before
    public void acquireConnections() {
        eventBusMessageCount = 0;
        eventBus.register(this);

    }

    @After
    public void releaseConnections() throws Exception {
        eventBus.unregister(this);
    }

}
