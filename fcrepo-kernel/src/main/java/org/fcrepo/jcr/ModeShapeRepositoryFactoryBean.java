
package org.fcrepo.jcr;

import java.net.URL;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.RepositoryException;

import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.modeshape.jcr.api.RepositoryFactory;

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
                            .getRepository(Collections.singletonMap(
                                    RepositoryFactory.URL,
                                    repositoryConfiguration));
        } catch (RepositoryException e) {
            throw new IllegalStateException(e);
        }
    }

    public JcrRepository getRepository() {
        if (repository == null) buildRepository();
        return repository;
    }

}
