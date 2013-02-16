package org.fcrepo.eventing;

import javax.enterprise.inject.Alternative;
import javax.jcr.observation.Event;

@Alternative
public class NOOPFilter implements EventFilter {
	@Override
	public boolean apply(Event event) {
		return true;
	}
}
