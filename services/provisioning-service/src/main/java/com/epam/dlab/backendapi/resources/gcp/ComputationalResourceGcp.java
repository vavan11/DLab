package com.epam.dlab.backendapi.resources.gcp;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.handlers.ComputationalCallbackHandler;
import com.epam.dlab.backendapi.core.response.handlers.ComputationalConfigure;
import com.epam.dlab.backendapi.service.DockerService;
import com.epam.dlab.backendapi.service.SparkClusterService;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.dto.gcp.computational.ComputationalCreateGcp;
import com.epam.dlab.dto.gcp.computational.GcpComputationalTerminateDTO;
import com.epam.dlab.dto.gcp.computational.SparkComputationalCreateGcp;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.contracts.ComputationalAPI;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static com.epam.dlab.backendapi.core.commands.DockerAction.CREATE;
import static com.epam.dlab.backendapi.core.commands.DockerAction.TERMINATE;


@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ComputationalResourceGcp extends DockerService implements DockerCommands {

    @Inject
    private ComputationalConfigure computationalConfigure;
    @Inject
    private SparkClusterService sparkClusterService;

    @POST
    @Path(ComputationalAPI.COMPUTATIONAL_CREATE_CLOUD_SPECIFIC)
    public String create(@Auth UserInfo ui, ComputationalCreateGcp dto) {
        log.debug("Create computational resources {} for user {}: {}", dto.getComputationalName(), ui.getName(), dto);
        String uuid = DockerCommands.generateUUID();
        folderListenerExecutor.start(configuration.getImagesDirectory(),
                configuration.getResourceStatusPollTimeout(),
                getFileHandlerCallback(CREATE, uuid, dto));
        try {
            commandExecutor.executeAsync(
                    ui.getName(),
                    uuid,
                    commandBuilder.buildCommand(
                            new RunDockerCommand()
                                    .withInteractive()
                                    .withName(nameContainer(dto.getEdgeUserName(), CREATE, dto.getComputationalName()))
                                    .withVolumeForRootKeys(configuration.getKeyDirectory())
                                    .withVolumeForResponse(configuration.getImagesDirectory())
                                    .withVolumeForLog(configuration.getDockerLogDirectory(), DataEngineType.CLOUD_SERVICE.getName())
                                    .withResource(DataEngineType.CLOUD_SERVICE.getName())
                                    .withRequestId(uuid)
                                    .withConfKeyName(configuration.getAdminKey())
                                    .withActionCreate(DataEngineType.getDockerImageName(DataEngineType.CLOUD_SERVICE)),
                            dto
                    )
            );
        } catch (Exception t) {
            throw new DlabException("Could not create computational resource cluster", t);
        }
        return uuid;
    }

    @POST
    @Path(ComputationalAPI.COMPUTATIONAL_TERMINATE_CLOUD_SPECIFIC)
    public String terminate(@Auth UserInfo ui, GcpComputationalTerminateDTO dto) {

        log.debug("Terminate computational resources {} for user {}: {}", dto.getComputationalName(), ui.getName(), dto);
        String uuid = DockerCommands.generateUUID();
        folderListenerExecutor.start(configuration.getImagesDirectory(),
                configuration.getResourceStatusPollTimeout(),
                getFileHandlerCallback(TERMINATE, uuid, dto));
        try {
            commandExecutor.executeAsync(
                    ui.getName(),
                    uuid,
                    commandBuilder.buildCommand(
                            new RunDockerCommand()
                                    .withInteractive()
                                    .withName(nameContainer(dto.getEdgeUserName(), TERMINATE, dto.getComputationalName()))
                                    .withVolumeForRootKeys(configuration.getKeyDirectory())
                                    .withVolumeForResponse(configuration.getImagesDirectory())
                                    .withVolumeForLog(configuration.getDockerLogDirectory(), DataEngineType.CLOUD_SERVICE.getName())
                                    .withResource(DataEngineType.CLOUD_SERVICE.getName())
                                    .withRequestId(uuid)
                                    .withConfKeyName(configuration.getAdminKey())
                                    .withActionTerminate(DataEngineType.getDockerImageName(DataEngineType.CLOUD_SERVICE)),
                            dto
                    )
            );
        } catch (JsonProcessingException t) {
            throw new DlabException("Could not terminate computational resources cluster", t);
        }

        return uuid;
    }

    @POST
    @Path(ComputationalAPI.COMPUTATIONAL_CREATE_SPARK)
    public String createSparkCluster(@Auth UserInfo ui, SparkComputationalCreateGcp dto) {
        log.debug("Create computational Spark resources {} for user {}: {}", dto.getComputationalName(), ui.getName(), dto);

        return sparkClusterService.create(ui, dto);
    }


    @POST
    @Path(ComputationalAPI.COMPUTATIONAL_TERMINATE_SPARK)
    public String terminateSparkCluster(@Auth UserInfo ui, GcpComputationalTerminateDTO dto) {
        log.debug("Terminate computational Spark resources {} for user {}: {}", dto.getComputationalName(), ui.getName(), dto);

        return sparkClusterService.terminate(ui, dto);
    }

    private FileHandlerCallback getFileHandlerCallback(DockerAction action, String uuid, ComputationalBase<?> dto) {
        return new ComputationalCallbackHandler(computationalConfigure, selfService, action, uuid, dto);
    }

    private String nameContainer(String user, DockerAction action, String name) {
        return nameContainer(user, action.toString(), "computational", name);
    }

    @Override
    public String getResourceType() {
        return Directories.DATA_ENGINE_SERVICE_LOG_DIRECTORY;
    }
}
