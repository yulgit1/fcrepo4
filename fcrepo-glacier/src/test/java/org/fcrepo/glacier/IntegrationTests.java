package org.fcrepo.glacier;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;
import static org.modeshape.jcr.api.observation.Event.Sequencing.NODE_SEQUENCED;
import static org.modeshape.jcr.api.observation.Event.Sequencing.NODE_SEQUENCING_FAILURE;
import static org.modeshape.jcr.api.observation.Event.Sequencing.OUTPUT_PATH;
import static org.modeshape.jcr.api.observation.Event.Sequencing.SELECTED_PATH;
import static org.modeshape.jcr.api.observation.Event.Sequencing.SEQUENCED_NODE_ID;
import static org.modeshape.jcr.api.observation.Event.Sequencing.SEQUENCED_NODE_PATH;
import static org.modeshape.jcr.api.observation.Event.Sequencing.SEQUENCER_NAME;
import static org.modeshape.jcr.api.observation.Event.Sequencing.USER_ID;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.fcrepo.events.SequencingTestListener;
import org.junit.Assert;
import org.junit.Test;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.RunningStateProxy;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.observation.Event;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.TreeHashGenerator;
import com.amazonaws.services.glacier.model.UploadArchiveRequest;
import com.amazonaws.services.glacier.model.UploadArchiveResult;

public class IntegrationTests extends AbstractSequencerTest {

	private static Logger LOG = LoggerFactory.getLogger(IntegrationTests.class);
    /**
     * A [sequenced node path, event] map which will hold all the received sequencing events, both in failure and non-failure
     * cases, using the path of the sequenced node as key.
     */
    private final ConcurrentHashMap<String, Event> sequencingEvents = new ConcurrentHashMap<String, Event>();

    protected InputStream getRepositoryConfigStream() {
        return resourceStream("config/glacier-repo-config.json");
    }
	
	protected Node createNodeWithContentFromFile( String nodeRelativePath,
			String filePath ) throws RepositoryException {
		JcrSession session = repository.login();
		Node parent = session.getRootNode();
		for (String pathSegment : nodeRelativePath.split("/")) {
			parent = parent.addNode(pathSegment);
		}
		//parent.setPrimaryType(JcrConstants.NT_FILE);
		Node content = parent.addNode(JcrConstants.JCR_CONTENT);
		content.setProperty(JcrConstants.JCR_DATA,
				((javax.jcr.Session)session).getValueFactory().createBinary(resourceStream(filePath)));
		session.save();
		return parent;
	}
	
	protected Node getNodeContent( String nodeRelativePath) throws RepositoryException {
		JcrSession session = repository.login();		
		Node parent = session.getRootNode();
		for (String pathSegment : nodeRelativePath.split("/")) {
			parent = parent.getNode(pathSegment);
		}
		return parent;
	}

	private Map<?, ?> getSequencingEventInfo( Node sequencedNode ) throws RepositoryException {
        Event receivedEvent = sequencingEvents.get(sequencedNode.getPath());
        assertTrue(receivedEvent != null);
        return receivedEvent.getInfo();
    }

	
	private GlacierBackupSequencer getFirstSequencer() {
		RunningStateProxy rs = new RunningStateProxy(repository);
		List<GlacierBackupSequencer> sequencers = rs.getSequencersByType(GlacierBackupSequencer.class);
		return sequencers.get(0);
	}
	
    protected void addSequencingListeners( JcrSession session ) throws RepositoryException {
    	super.addSequencingListeners(session);
        ObservationManager observationManager = ((Workspace)session.getWorkspace()).getObservationManager();
        observationManager.addEventListener(new SequencingTestListener(sequencingEvents), NODE_SEQUENCED, null, true, null, null, false);
    }

	
	protected Map<?, ?> assertSequencingEventInfo( Node sequencedNode,
			String expectedUserId,
			String expectedSequencerName,
			String expectedSelectedPath,
			String expectedOutputPath ) throws RepositoryException {
		Map<?, ?> sequencingEventInfo = getSequencingEventInfo(sequencedNode);
		Assert.assertEquals(expectedUserId, sequencingEventInfo.get(Event.Sequencing.USER_ID));
		Assert.assertEquals(expectedSequencerName, sequencingEventInfo.get(Event.Sequencing.SEQUENCER_NAME));
		Assert.assertEquals(sequencedNode.getIdentifier(), sequencingEventInfo.get(Event.Sequencing.SEQUENCED_NODE_ID));

		Assert.assertEquals(sequencedNode.getPath(), sequencingEventInfo.get(Event.Sequencing.SEQUENCED_NODE_PATH));
		Assert.assertEquals(expectedSelectedPath, sequencingEventInfo.get(Event.Sequencing.SELECTED_PATH));
		Assert.assertEquals(expectedOutputPath, sequencingEventInfo.get(Event.Sequencing.OUTPUT_PATH));
		return sequencingEventInfo;
	}

	

	@Test
	public void shouldCreateSequencer() throws Exception {
		RunningStateProxy rs = new RunningStateProxy(repository);
		List<GlacierBackupSequencer> sequencers = rs.getSequencersByType(GlacierBackupSequencer.class);
		assertTrue(sequencers.size() == 1);
	}
	
	@Test
	public void shouldCreateAWSClient() throws Exception {
		GlacierBackupSequencer test = getFirstSequencer();
		AmazonGlacierClient orig = test.client;
		Field f = AmazonWebServiceClient.class.getDeclaredField("endpoint");
		f.setAccessible(true);
		
		assertEquals(URI.create("http://localhost:8000"), f.get(orig));
	}
	
	@Test
	public void shouldAssignNodeProperties() throws Exception {
		GlacierBackupSequencer test = getFirstSequencer();
		AmazonGlacierClient mockClient = mock(AmazonGlacierClient.class);
		UploadArchiveResult mockResult = mock(UploadArchiveResult.class);
		when(mockResult.getLocation()).thenReturn("http://localhost:8000/foo/bar");
		when(mockResult.getArchiveId()).thenReturn("bar");
		when(mockClient.uploadArchive(any(UploadArchiveRequest.class))).thenReturn(mockResult);
		test.client = mockClient;
		createNodeWithContentFromFile("glacier-backup.cnd", "config/glacier-backup.cnd");
		Thread.sleep(10000);
        // node is a path node, with a jcr:content childnode that has a jcr:data property
		// properties cannot be set on the jcr:content node, which is like a pseudo-property
		Node contentNode = getNodeContent("glacier-backup.cnd");
		PropertyIterator props = contentNode.getProperties();

		assertTrue(contentNode.hasProperty(GlacierBackupSequencer.GLACIER_CHECKSUM_PROPERTY));
		String expectedChecksum = TreeHashGenerator.calculateTreeHash(resourceStream("config/glacier-backup.cnd"));
		assertEquals(expectedChecksum, contentNode.getProperty(GlacierBackupSequencer.GLACIER_CHECKSUM_PROPERTY).getString());
		for (Event e:sequencingEvents.values()) {
			Map info = e.getInfo();
			for (Object key:info.keySet()) {
				System.out.println(e.getIdentifier() + " " + key + " => " + info.get(key));
			}
		}
        
	}
	
	public int timesSequenced(String path) {
		return sequencingEvents.containsKey(path) ? 1 : 0;
	}
	
	public int timesExecuted(String sequencerName) {
		int result = 0;
		for (Event event : sequencingEvents.values()){
			try {
				if (sequencerName.equals(event.getInfo().get(SEQUENCER_NAME))) result++;
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	
    protected final class SequencingListener implements EventListener {
    	private ConcurrentHashMap<String, Event> sequencingEvents;
        private final Map<String, Node> sequencedNodes = new HashMap<String, Node>();
        private final ConcurrentHashMap<String, CountDownLatch> nodeSequencedLatches = new ConcurrentHashMap<String, CountDownLatch>();

    	SequencingListener(ConcurrentHashMap<String, Event> sequencingEvents){
    		this.sequencingEvents = sequencingEvents;
    	}
        
        @SuppressWarnings( "synthetic-access" )
        @Override
        public void onEvent( EventIterator events ) {
            while (events.hasNext()) {
                try {
                    Event event = (Event)events.nextEvent();
                    smokeCheckSequencingEvent(event,
                                              NODE_SEQUENCED,
                                              SEQUENCED_NODE_ID,
                                              SEQUENCED_NODE_PATH,
                                              OUTPUT_PATH,
                                              SELECTED_PATH,
                                              SEQUENCER_NAME,
                                              USER_ID);
                    sequencingEvents.putIfAbsent((String)event.getInfo().get(SEQUENCED_NODE_PATH), event);

                    String nodePath = event.getPath();
                    sequencedNodes.put(nodePath, session.getNode(nodePath));

                    // signal the node is available
                    createWaitingLatchIfNecessary(nodePath, nodeSequencedLatches);
                    nodeSequencedLatches.get(nodePath).countDown();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
