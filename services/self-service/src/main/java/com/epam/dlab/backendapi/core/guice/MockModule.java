package com.epam.dlab.backendapi.core.guice;

import com.epam.dlab.auth.SecurityAPI;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.client.mongo.MongoService;
import com.epam.dlab.backendapi.client.rest.DockerAPI;
import com.epam.dlab.dto.ImageMetadataDTO;
import com.epam.dlab.restclient.RESTService;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.mongodb.client.MongoCollection;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import static com.epam.dlab.auth.SecurityRestAuthenticator.SECURITY_SERVICE;
import static com.epam.dlab.backendapi.SelfServiceApplicationConfiguration.PROVISIONING_SERVICE;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by Alexey Suprun
 */
public class MockModule extends AbstractModule implements SecurityAPI, DockerAPI {
    @Override
    protected void configure() {
        bind(MongoService.class).toInstance(createMongoService());
        bind(RESTService.class).annotatedWith(Names.named(SECURITY_SERVICE))
                .toInstance(createAuthenticationService());
        bind(RESTService.class).annotatedWith(Names.named(PROVISIONING_SERVICE))
                .toInstance(createProvisioningService());
    }

    private MongoService createMongoService() {
        MongoService result = mock(MongoService.class);
        when(result.getCollection(anyString())).thenReturn(mock(MongoCollection.class));
        return result;
    }

    private RESTService createAuthenticationService() {
        RESTService result = mock(RESTService.class);
        when(result.post(eq(LOGIN), any(), any())).thenReturn("token123");
        when(result.post(eq(GET_USER_INFO), eq("token123"), eq(UserInfo.class))).thenReturn(new UserInfo("test", "token123"));
        return result;
    }

    private RESTService createProvisioningService() {
        RESTService result = mock(RESTService.class);
        when(result.get(eq(DOCKER), any()))
                .thenReturn(new HashSet<>(Arrays.asList(new ImageMetadataDTO("test image", "template", "decription", "request_id"))));
        return result;
    }
}
