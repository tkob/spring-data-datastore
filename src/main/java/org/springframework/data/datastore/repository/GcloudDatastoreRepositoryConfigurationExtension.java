package org.springframework.data.datastore.repository;

import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;

public class GcloudDatastoreRepositoryConfigurationExtension
    extends RepositoryConfigurationExtensionSupport {

    @Override
    public String getRepositoryFactoryClassName() {
        return GcloudDatastoreRepositoryFactory.class.getName();
    }

    @Override
    protected String getModulePrefix() {
        return "gcloudds";
    }
}
