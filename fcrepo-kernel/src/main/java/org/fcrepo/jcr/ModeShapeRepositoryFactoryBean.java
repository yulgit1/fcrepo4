
package org.fcrepo.jcr;

import static java.util.Collections.singletonMap;
import static org.modeshape.jcr.api.RepositoryFactory.URL;

import java.net.URL;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrRepositoryFactory;

public class ModeShapeRepositoryFactoryBean {

    @Inject
    private JcrRepositoryFactory jcrRepositoryFactory;

    @Inject
    @ModeShapeRepositoryConfiguration
    private URL repositoryConfiguration;

    private JcrRepository repository;

    @PostConstruct
    public void buildRepository() {
        try {
            repository =
                    (JcrRepository) jcrRepositoryFactory
                            .getRepository(singletonMap(URL,
                                    repositoryConfiguration));
        } catch (RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    @Produces
    @Singleton
    public Repository getRepository() {
        if (repository == null) buildRepository();
        return repository;
    }

    @Default
    public static class FedoraRepositoryFactory extends JcrRepositoryFactory {
    }

}
