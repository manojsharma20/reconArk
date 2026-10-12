package io.reconark.manager.clients;

import io.reconark.common.model.ReconDefinition;
import io.reconark.manager.ManagerProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/** Calls config-service and ingestion-service over REST. Used only when a run starts. */
@Component
public class ServiceClients {

    private final RestClient configService;
    private final RestClient ingestionService;

    public ServiceClients(ManagerProperties properties) {
        this.configService = RestClient.create(properties.configServiceUrl());
        this.ingestionService = RestClient.create(properties.ingestionServiceUrl());
    }

    public ReconDefinition definition(String definitionId) {
        try {
            return configService.get().uri("/api/definitions/{id}", definitionId).retrieve().body(ReconDefinition.class);
        } catch (HttpClientErrorException.NotFound notFound) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recon definition not found: " + definitionId);
        }
    }

    public FileInfo file(String fileId) {
        try {
            return ingestionService.get().uri("/api/files/{id}", fileId).retrieve().body(FileInfo.class);
        } catch (HttpClientErrorException.NotFound notFound) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file not found: " + fileId);
        }
    }
}
