package org.fcrepo.glacier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.nodetype.ConstraintViolationException;

import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.TreeHashGenerator;
import com.amazonaws.services.glacier.internal.TreeHashInputStream;
import com.amazonaws.services.glacier.model.UploadArchiveRequest;
import com.amazonaws.services.glacier.model.UploadArchiveResult;

public class GlacierBackupSequencer extends Sequencer {
	
	public static String GLACIER_BACKUP_MIXIN = "org.fcrepo:glacier-backup";
	public static String GLACIER_LOCATION_PROPERTY = "org.fcrepo.glacier:location";
	public static String GLACIER_ARCHIVE_ID_PROPERTY = "org.fcrepo.glacier:archive-id";
	public static String GLACIER_CHECKSUM_PROPERTY = "org.fcrepo.glacier:checksum";
	
	private static Logger LOG = LoggerFactory.getLogger(GlacierBackupSequencer.class);
	
    private AmazonGlacierClient m_client;
    private String m_defaultArchive;
    
    public GlacierBackupSequencer(AmazonGlacierClient client, String defaultArchive) {
    	m_client = client;
    	m_defaultArchive = defaultArchive;
    }
	@Override
	public boolean execute(Property inputProperty, Node outputNode, Context context)
			throws Exception {
        if (JcrConstants.JCR_DATA.equals(inputProperty.getName())) {
        	if (!outputNode.canAddMixin(GLACIER_BACKUP_MIXIN)) {
        		throw new ConstraintViolationException("Cannot add mixin \"" + GLACIER_BACKUP_MIXIN + "\" to this node");
        	}
        	Binary inputBinary = inputProperty.getBinary();
        	InputStream in = inputBinary.getStream();
        	long inputSize = inputBinary.getSize();
        	UploadArchiveRequest request = getRequest(context);
        	File outFile = null;
        	
            // read input to get tree hash, cache it in a temporary file
        	TreeHashInputStream treeIn = new TreeHashInputStream(in);
        	byte [] buf = new byte[1024];
        	outFile = File.createTempFile("fcrepo", null);
        	OutputStream out = new FileOutputStream(outFile);
        	int len = -1;
        	long read = 0;
        	while ((len = treeIn.read(buf)) > -1) {
        		out.write(buf,0,len);
        		read += len;
        	}
        	treeIn.close();
        	out.flush();
        	out.close();
        	if (inputSize != read) {
        		LOG.warn("input Binary size did not match bytes read: {} != {}", inputSize, read);
        	}
        	request.setContentLength(read);
        	request.setChecksum(TreeHashGenerator.calculateTreeHash(treeIn.getChecksums()));
        	request.setBody(new FileInputStream(outFile));

        	request.setContentLength(inputBinary.getSize());
        	//TODO Need to calculate the checksum!
        	request.setChecksum("foo");
        	UploadArchiveResult result = m_client.uploadArchive(request);

        	outputNode.addMixin(GLACIER_BACKUP_MIXIN);
        	outputNode.setProperty(GLACIER_LOCATION_PROPERTY, result.getLocation());
        	outputNode.setProperty(GLACIER_ARCHIVE_ID_PROPERTY, result.getArchiveId());
        	if (outFile != null) {
        		outFile.delete();
        	}
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
