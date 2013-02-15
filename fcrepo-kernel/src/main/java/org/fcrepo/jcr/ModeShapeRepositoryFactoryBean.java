
package org.fcrepo.jcr;

import static java.util.Collections.singletonMap;
import static org.modeshape.jcr.api.RepositoryFactory.URL;

import java.io.IOException;
import java.net.URL;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jcr.RepositoryException;

import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.modeshape.jcr.api.Repository;
import org.springframework.beans.factory.FactoryBean;

@ApplicationScoped
public class ModeShapeRepositoryFactoryBean implements
        FactoryBean<JcrRepository> {

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

    @Override
    @Produces
    @Singleton
    public JcrRepository getObject() throws RepositoryException, IOException {
        if (repository != null)
            return repository;
        else {
            buildRepository();
            return repository;
        }
    }

    @Override
    public Class<?> getObjectType() {
        return Repository.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Default
    public static class FedoraRepositoryFactory extends JcrRepositoryFactory {
    }

}
