package org.fcrepo.jcr;

import java.net.MalformedURLException;
import java.net.URL;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

@ApplicationScoped
@Default
public class DefaultConfiguration {

    @Produces
    @ModeShapeRepositoryConfiguration
    public URL getConfiguration() throws MalformedURLException {
        return this.getClass().getResource("/repository.json");
    }

}