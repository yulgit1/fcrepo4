
package org.fcrepo.eventing.impl;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.fcrepo.FedoraResourceType;
import org.fcrepo.eventing.FedoraEvent;

public class FedoraEventImpl implements FedoraEvent {

    Event event;

    FedoraResourceType type;

    public FedoraEventImpl(Event event, FedoraResourceType type) {
        this.event = event;
        this.type = type;
    }

    @Override
    public int getType() {
        return event.getType();
    }

    @Override
    public String getPath() throws RepositoryException {
        return event.getPath();
    }

    @Override
    public String getUserID() {
        return event.getUserID();
    }

    @Override
    public String getIdentifier() throws RepositoryException {
        return event.getIdentifier();
    }

    @Override
    public Map getInfo() throws RepositoryException {
        return event.getInfo();
    }

    @Override
    public String getUserData() throws RepositoryException {
        return event.getUserData();
    }

    @Override
    public long getDate() throws RepositoryException {
        return event.getDate();
    }

    @Override
    public FedoraResourceType getResourceType() {

        return null;
    }

}
