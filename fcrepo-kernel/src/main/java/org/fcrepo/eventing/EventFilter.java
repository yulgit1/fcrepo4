package org.fcrepo.eventing;

import javax.jcr.observation.Event;

import com.google.common.base.Predicate;

public interface EventFilter extends Predicate<Event> {

}
