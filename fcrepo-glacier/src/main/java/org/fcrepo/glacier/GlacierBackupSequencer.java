package org.fcrepo.glacier;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.Property;

import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.sequencer.Sequencer;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.model.UploadArchiveRequest;
import com.amazonaws.services.glacier.model.UploadArchiveResult;

public class GlacierBackupSequencer extends Sequencer {
	
	public static String GLACIER_URI_PROPERTY = "fcrepo:glacier:location";
	public static String GLACIER_ARCHIVE_ID_PROPERTY = "fcrepo:glacier:archive-id";
	
    private AmazonGlacierClient m_client;
    private String m_defaultArchive;
    
    public GlacierBackupSequencer(AmazonGlacierClient client, String defaultArchive) {
    	m_client = client;
    	m_defaultArchive = defaultArchive;
    }
	@Override
	public boolean execute(Property inputProperty, Node outputNode, Context context)
			throws Exception {
        if (JcrConstants.JCR_CONTENT.equals(inputProperty.getName())) {
        	InputStream in = inputProperty.getBinary().getStream();
        	UploadArchiveRequest request = getRequest(context);
        	request.setBody(in);
        	request.setContentLength(inputProperty.getBinary().getSize());
        	//TODO Need to calculate the checksum!
        	request.setChecksum("foo");
        	UploadArchiveResult result = m_client.uploadArchive(request);
        	outputNode.setProperty(GLACIER_URI_PROPERTY, result.getLocation());
        	outputNode.setProperty(GLACIER_ARCHIVE_ID_PROPERTY, result.getArchiveId());
        	return true;
        } else {
            return false;
        }
	}
	
	public UploadArchiveRequest getRequest(Context context) {
		UploadArchiveRequest result = new UploadArchiveRequest();
		//TODO we should look for a property specifying the archive
		result.setRequestCredentials(getCredentials(context));
		return result;
	}
	
	public AWSCredentials getCredentials(Context context) {
		return new BasicAWSCredentials("","");
	}

}
