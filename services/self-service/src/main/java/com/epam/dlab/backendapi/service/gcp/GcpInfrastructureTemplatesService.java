package com.epam.dlab.backendapi.service.gcp;

import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.resources.dto.gcp.GcpDataprocConfiguration;
import com.epam.dlab.backendapi.service.InfrastructureTemplatesServiceBase;
import com.epam.dlab.dto.base.computational.FullComputationalTemplate;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;

public class GcpInfrastructureTemplatesService extends InfrastructureTemplatesServiceBase {
    @Inject
    private SelfServiceApplicationConfiguration configuration;

    @Override
    protected FullComputationalTemplate getCloudFullComputationalTemplate(ComputationalMetadataDTO metadataDTO) {
        return new GcpFullComputationalTemplate(metadataDTO,
                GcpDataprocConfiguration.builder()
                        .dataprocAvailableMasterInstanceCount(configuration.getDataprocAvailableMasterInstanceCount())
                        .minDataprocSlaveInstanceCount(configuration.getMinDataprocSlaveInstanceCount())
                        .maxDataprocSlaveInstanceCount(configuration.getMaxDataprocSlaveInstanceCount())
                        .minDataprocPreemptibleInstanceCount(configuration.getMinDataprocPreemptibleCount())
                        .build());
    }


    private class GcpFullComputationalTemplate extends FullComputationalTemplate {
        @JsonProperty("limits")
        private GcpDataprocConfiguration gcpDataprocConfiguration;

        GcpFullComputationalTemplate(ComputationalMetadataDTO metadataDTO,
                                     GcpDataprocConfiguration gcpDataprocConfiguration) {
            super(metadataDTO);
            this.gcpDataprocConfiguration = gcpDataprocConfiguration;
        }
    }
}
