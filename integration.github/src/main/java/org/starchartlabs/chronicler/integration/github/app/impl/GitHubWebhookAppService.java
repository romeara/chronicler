/*
 * Copyright 2017 StarChart Labs Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.starchartlabs.chronicler.integration.github.app.impl;

import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starchartlabs.chronicler.integration.github.app.api.IGitHubWebhookAppService;
import org.starchartlabs.chronicler.integration.github.app.model.InstallationEvent;
import org.starchartlabs.chronicler.integration.github.app.model.PingEvent;
import org.starchartlabs.chronicler.integration.github.webhook.WebhookEvents;
import org.starchartlabs.chronicler.integration.github.webhook.WebhookVerifier;

import com.fasterxml.jackson.databind.ObjectMapper;

//TODO romeara test
/**
 * Implementation of {@link IGitHubWebhookAppService} which dispatches internal application events based on webhook data
 * provided by GitHub
 *
 * <p>
 * Clients should not reference this implementation directly - use dependency injection to obtain the application's
 * selected implementation of {@link IGitHubWebhookAppService}
 *
 * @author romeara
 * @since 0.1.0
 */
public class GitHubWebhookAppService implements IGitHubWebhookAppService {

    /** Logger reference to output information to the application log files */
    private static final Logger logger = LoggerFactory.getLogger(GitHubWebhookAppService.class);

    private final WebhookVerifier webhookVerifier;

    /**
     * @param webhookVerifier
     *            Represents checks used to verify that payloads come from GitHub and not another party
     * @since 0.1.0
     */
    public GitHubWebhookAppService(WebhookVerifier webhookVerifier) {
        this.webhookVerifier = Objects.requireNonNull(webhookVerifier);
    }

    @Override
    public boolean acceptPayload(String securityKey, String eventType, String payload) throws IOException {
        boolean validPayload = webhookVerifier.isPayloadLegitimate(securityKey, payload);

        ObjectMapper mapper = new ObjectMapper();
        mapper = mapper.findAndRegisterModules();

        if (validPayload) {
            if (Objects.equals(eventType, WebhookEvents.PING)) {
                PingEvent event = mapper.readValue(payload, PingEvent.class);

                logger.info("Received ping event. GitHub imparts wisdom: {}", event.getZen());
            } else if (Objects.equals(eventType, WebhookEvents.PULL_REQUEST)) {
                // TODO romeara implement
                logger.info("Received pull request event");
            } else if (Objects.equals(eventType, WebhookEvents.REPOSITORY)) {
                // TODO romeara implement
                logger.info("Received repository event");
            } else if (Objects.equals(eventType, WebhookEvents.INSTALLATION)) {
                InstallationEvent event = mapper.readValue(payload, InstallationEvent.class);

                logger.info("Received installation event ({}:{})", event.getAction(),
                        event.getInstallation().getAccount().getLogin());
            } else if (Objects.equals(eventType, WebhookEvents.INSTALLATION_REPOSITORIES)) {
                // TODO romeara implement
                logger.info("Received installation_repositories event");
            } else {
                logger.warn("Unrecognized event type {}", eventType);
            }
        } else {
            logger.error("Insecure payload delivered!");
        }

        return validPayload;
    }

}
