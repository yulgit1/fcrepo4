
package org.fcrepo;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

import static org.ops4j.pax.exam.CoreOptions.junitBundles;

@RunWith(PaxExam.class)
public abstract class AbstractTest {

    protected Logger logger;

    @Before
    public void setLogger() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Configuration
    public Option[] config() {
        Option[] config = {junitBundles()};
        return config;
    }

}
