
package org.fcrepo.eventing;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

@Singleton
public class FedoraEventBus extends EventBus {

    final private Logger logger = LoggerFactory.getLogger(FedoraEventBus.class);

    public void post(FedoraEvent event) {
        logger.debug("Received posting: " + event.toString());
        super.post(event);
    }
    
    public FedoraEventBus() {
        super();
        logger.debug("Created Fedora event bus.");
    }
}
