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
package org.fcrepo.kernel.impl.observer.eventmappings;

import static com.google.common.collect.Iterators.getLast;
import static com.google.common.collect.Iterators.size;
import static javax.jcr.observation.Event.NODE_ADDED;
import static javax.jcr.observation.Event.PROPERTY_ADDED;
import static javax.jcr.observation.Event.PROPERTY_CHANGED;
import static org.jgroups.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.modeshape.jcr.api.JcrConstants.JCR_CONTENT;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.fcrepo.kernel.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.observer.FedoraEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Iterator;

/**
 * <p>AllNodeEventsOneEventTest class.</p>
 *
 * @author ajs6f
 */
public class AllNodeEventsOneEventTest {

    private static final String TEST_IDENTIFIER1 = randomUUID().toString();

    private static final String TEST_PATH1 = "/test/node1";

    private static final String TEST_IDENTIFIER2 = TEST_IDENTIFIER1;

    private static final String TEST_PATH2 = TEST_PATH1 + "/dc:title";

    private static final String TEST_IDENTIFIER3 = randomUUID().toString();

    private static final String TEST_PATH3 = "/test/node2";

    private static final String TEST_IDENTIFIER4 = randomUUID().toString();

    private static final String TEST_PATH4 = "/test/node3";

    private static final String TEST_IDENTIFIER5 = randomUUID().toString();

    private static final String TEST_PATH5 = "/test/node3/" + JCR_CONTENT;

    private final AllNodeEventsOneEvent testMapping = new AllNodeEventsOneEvent();

    @Mock
    private Event mockEvent1;

    @Mock
    private Event mockEvent2;

    @Mock
    private Event mockEvent3;

    @Mock
    private Event mockEvent4;

    @Mock
    private Event mockEvent5;

    @Mock
    private Iterator<Event> mockIterator;

    @Mock
    private Iterator<Event> mockIterator2;

    @Mock
    private Iterator<Event> mockIterator3;

    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockEvent1.getIdentifier()).thenReturn(TEST_IDENTIFIER1);
        when(mockEvent1.getPath()).thenReturn(TEST_PATH1);
        when(mockEvent1.getType()).thenReturn(NODE_ADDED);
        when(mockEvent2.getIdentifier()).thenReturn(TEST_IDENTIFIER2);
        when(mockEvent2.getPath()).thenReturn(TEST_PATH2);
        when(mockEvent2.getType()).thenReturn(PROPERTY_ADDED);
        when(mockEvent3.getIdentifier()).thenReturn(TEST_IDENTIFIER3);
        when(mockEvent3.getPath()).thenReturn(TEST_PATH3);
        when(mockEvent3.getType()).thenReturn(PROPERTY_CHANGED);
        when(mockIterator.next()).thenReturn(mockEvent1, mockEvent2, mockEvent3);
        when(mockIterator.hasNext()).thenReturn(true, true, true, false);

        when(mockEvent4.getIdentifier()).thenReturn(TEST_IDENTIFIER4);
        when(mockEvent4.getPath()).thenReturn(TEST_PATH4);
        when(mockEvent4.getType()).thenReturn(NODE_ADDED);
        when(mockEvent5.getIdentifier()).thenReturn(TEST_IDENTIFIER5);
        when(mockEvent5.getPath()).thenReturn(TEST_PATH5);
        when(mockEvent5.getType()).thenReturn(NODE_ADDED);
        when(mockIterator2.next()).thenReturn(mockEvent4, mockEvent5);
        when(mockIterator2.hasNext()).thenReturn(true, true, false);

        when(mockIterator3.next()).thenReturn(mockEvent4, mockEvent5);
        when(mockIterator3.hasNext()).thenReturn(true, true, false);
    }

    @Test
    public void testCardinality() {
        assertEquals("Didn't get 2 FedoraEvents for 3 input JCR Events, two of which were on the same node!", 2,
                size(testMapping.apply(mockIterator)));
    }

    @Test
    public void testCollapseContentEvents() {
        assertEquals("Didn't collapse content node and fcr:content events!", 1, size(testMapping.apply(mockIterator2)));
    }

    @Test
    public void testFileEventProperties() {
        final FedoraEvent e = testMapping.apply(mockIterator3).next();
        assertTrue("Didn't add fedora:hasContent property to fcr:content events!: " + e.getProperties(),
                e.getProperties().contains("fedora:hasContent"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testBadOPeration() {
        testMapping.apply(mockIterator).remove();
    }

    @Test(expected = RuntimeException.class)
    public void testBadEvent() throws RepositoryException {
        reset(mockEvent1);
        when(mockEvent1.getIdentifier()).thenThrow(new RepositoryException("Expected."));
        testMapping.apply(mockIterator);
    }

    @Test
    public void testPropertyEvents() throws RepositoryException {
        final Iterator<FedoraEvent> iterator = testMapping.apply(mockIterator);
        assertNotNull(iterator);
        assertTrue("Iterator is empty!", iterator.hasNext());

        boolean found = false;
        while (iterator.hasNext()) {
            final FedoraEvent event = iterator.next();
            if (TEST_IDENTIFIER3.equals(event.getIdentifier())) {
                assertEquals("Expected one event property", 1, event.getProperties().size());
                found = true;
            }

        }
        assertTrue("Third mock event was not found!", found);
    }

    @Test(expected = RepositoryRuntimeException.class)
    public void testError() throws RepositoryException {
        when(mockEvent3.getPath()).thenThrow(new RepositoryException("expected"));

        final Iterator<FedoraEvent> iterator = testMapping.apply(mockIterator);
        assertNotNull(iterator);
        getLast(iterator);
    }

}
