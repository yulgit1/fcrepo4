
package org.fcrepo;

import static org.ops4j.pax.exam.CoreOptions.junitBundles;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
