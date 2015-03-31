/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.impl.observer;

import com.codahale.metrics.Counter;
import com.hp.hpl.jena.graph.Triple;
import org.fcrepo.kernel.utils.iterators.RdfStream;
import org.fcrepo.metrics.RegistryService;
import org.slf4j.Logger;

import javax.inject.Inject;

import java.util.Hashtable;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Throwables.propagate;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.eventbus.EventBus;
import org.springframework.stereotype.Component;

/**
 * Created by ermadmix on 3/30/15.
 * @author ermadmix
 */
@Component
public class FedoraObserver {
    private static final Logger LOGGER = getLogger(FedoraObserver.class);

    /**
     * A simple counter of events that pass through this observer
     */
    static final Counter EVENT_COUNTER =
            RegistryService.getInstance().getMetrics().counter(name(FedoraObserver.class, "createFixityEvent"));

    @Inject
    private EventBus eventBus;

    /**
     * constructor
     */
    public FedoraObserver() { }
    /**
     * Filter the fixity rdf and create fedora fixity events
     *
     * @param fixityRdf - the Rdf to create events
     */
    public void createFixityEvent(final RdfStream fixityRdf) {
        try {
            final Hashtable fixityEvents = fixityRdfToEvent(fixityRdf);
            eventBus.post(fixityEvents);
            EVENT_COUNTER.inc();
        } catch (final Exception ex) {
            throw propagate(ex);
        }
    }

    private Hashtable<String,String> fixityRdfToEvent(final RdfStream fixityRdf) {
        final Hashtable fixityEvents = new Hashtable<String,String>();
        while (fixityRdf.hasNext()) {
            final Triple t = fixityRdf.next();
            fixityEvents.put(t.getPredicate().toString(),t.getObject().toString());
            LOGGER.debug ("testing triples here");
        }
        return fixityEvents;
    }

    /**
     * test method
     */
    public void onFixityEvent() {
        LOGGER.debug("in onFixityEvent");
    }

}
