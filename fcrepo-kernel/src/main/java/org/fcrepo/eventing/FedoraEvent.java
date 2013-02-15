
package org.fcrepo.eventing;

import javax.jcr.observation.Event;

import org.fcrepo.FedoraResourceType;

public interface FedoraEvent extends Event {

    public FedoraResourceType getResourceType();

}
