package com.epam.dlab.backendapi.modules;

import com.epam.dlab.auth.SecurityFactory;
import com.epam.dlab.backendapi.SelfServiceApplication;
import com.epam.dlab.backendapi.auth.SelfServiceSecurityAuthenticator;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.gcp.GcpKeyDao;
import com.epam.dlab.backendapi.resources.callback.gcp.EdgeCallbackGcp;
import com.epam.dlab.backendapi.resources.callback.gcp.KeyUploaderCallbackGcp;
import com.epam.dlab.backendapi.resources.gcp.ComputationalResourceGcp;
import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.backendapi.service.InfrastructureTemplatesService;
import com.epam.dlab.backendapi.service.gcp.GcpInfrastructureInfoService;
import com.epam.dlab.backendapi.service.gcp.GcpInfrastructureTemplatesService;
import com.epam.dlab.cloud.CloudModule;
import com.fiestacabin.dropwizard.quartz.SchedulerConfiguration;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.setup.Environment;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

public class GcpSelfServiceModule extends CloudModule {
    @Override
    public void init(Environment environment, Injector injector) {

        environment.jersey().register(injector.getInstance(EdgeCallbackGcp.class));
        environment.jersey().register(injector.getInstance(KeyUploaderCallbackGcp.class));
        environment.jersey().register(injector.getInstance(ComputationalResourceGcp.class));

        injector.getInstance(SecurityFactory.class).configure(injector, environment,
				SelfServiceSecurityAuthenticator.class, injector.getInstance(Authorizer.class));

    }

    @Override
    protected void configure() {
        bind((KeyDAO.class)).to(GcpKeyDao.class);
        bind(InfrastructureInfoService.class).to(GcpInfrastructureInfoService.class);
        bind(InfrastructureTemplatesService.class).to(GcpInfrastructureTemplatesService.class);
		bind(SchedulerConfiguration.class).toInstance(
				new SchedulerConfiguration(SelfServiceApplication.class.getPackage().getName()));
    }

    @Provides
    @Singleton
    Scheduler provideScheduler() throws SchedulerException {
        return StdSchedulerFactory.getDefaultScheduler();
    }
}
