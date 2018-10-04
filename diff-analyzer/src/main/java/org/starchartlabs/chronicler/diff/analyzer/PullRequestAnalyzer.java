/*
 * Copyright (c) Sep 26, 2018 StarChart Labs Authors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    romeara - initial API and implementation and/or initial documentation
 */
package org.starchartlabs.chronicler.diff.analyzer;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.starchartlabs.chronicler.calamari.core.auth.InstallationAccessToken;
import org.starchartlabs.chronicler.events.GitHubPullRequestEvent;
import org.starchartlabs.chronicler.github.model.PageReader;
import org.starchartlabs.chronicler.github.model.Requests;
import org.starchartlabs.chronicler.github.model.pullrequest.StatusHandler;

import com.google.gson.JsonElement;

import okhttp3.HttpUrl;

//https://developer.github.com/v3/pulls/#list-pull-requests-files
public class PullRequestAnalyzer {

    /** Logger reference to output information to the application log files */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Supplier<String> applicationKey;

    public PullRequestAnalyzer(Supplier<String> applicationKey) {
        this.applicationKey = Objects.requireNonNull(applicationKey);
    }

    public void analyze(GitHubPullRequestEvent event) {
        logger.info("Processing pull request for {}", event.getLoggableRepositoryName());

        InstallationAccessToken accessToken = InstallationAccessToken.forRepository(event.getBaseRepositoryUrl(),
                applicationKey, Requests.USER_AGENT);

        StatusHandler statusHandler = new StatusHandler("doc/chronicler", event.getPullRequestStatusesUrl(),
                accessToken);

        // Set pending status
        statusHandler.sendPending("Analysis in progress");

        AnalysisSettings settings = AnalysisSettings.defaultSettings();

        HttpUrl url = HttpUrl.get(event.getPullRequestUrl()).newBuilder()
                .addPathSegment("files")
                .addQueryParameter("page", "1")
                .addQueryParameter("per_page", "30")
                .build();

        try {
            // TODO test new paging
            FilePathAnalysis pathAnalysis = new PageReader(accessToken).page(url.toString())
                    .map(element -> toPathAnalysis(element, settings))
                    .until(Collectors.reducing(FilePathAnalysis::new), this::reduce,
                            o -> o.map(FilePathAnalysis::isComplete).orElse(false))
                    .orElse(new FilePathAnalysis());

            AnalysisResults results = new AnalysisResults(pathAnalysis.containsProductionFiles(),
                    pathAnalysis.containsReleaseNoteFiles());

            logger.info("Analysis results: prod: {}, rel: {}", results.isModifyingProductionFiles(),
                    results.isModifyingReleaseNotes());

            processResult(results, statusHandler);
        } catch (Exception e) {
            statusHandler.sendError("Error processing pull request files");

            throw new RuntimeException("Error processing pull request files", e);
        }
    }

    private void processResult(AnalysisResults results, StatusHandler statusHandler) {
        String description = null;

        if (results.isDocumented()) {
            if (results.isModifyingProductionFiles()) {
                description = "Release notes updated as required";
            } else {
                description = "No production files modified";
            }
        } else {
            description = "Production files modified without release notes";
        }

        if (results.isDocumented()) {
            statusHandler.sendSuccess(description);
        } else {
            statusHandler.sendFailure(description);
        }
    }

    private FilePathAnalysis toPathAnalysis(JsonElement element, AnalysisSettings settings) {
        String path = element.getAsJsonObject().get("filename").getAsString();
        path = path.startsWith("/") ? path : "/" + path;

        return new FilePathAnalysis(settings, path);
    }

    private Optional<FilePathAnalysis> reduce(Optional<FilePathAnalysis> a, Optional<FilePathAnalysis> b) {
        return Stream.of(a, b)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.reducing(FilePathAnalysis::new));
    }

    private static final class FilePathAnalysis {

        private final boolean production;

        private final boolean releaseNote;

        public FilePathAnalysis() {
            this(false, false);
        }

        public FilePathAnalysis(AnalysisSettings settings, String path) {
            Objects.requireNonNull(settings);
            Objects.requireNonNull(path);

            this.production = settings.isProductionFile(path);
            this.releaseNote = settings.isReleaseNoteFile(path);
        }

        public FilePathAnalysis(FilePathAnalysis a, FilePathAnalysis b) {
            this(a.containsProductionFiles() || b.containsProductionFiles(),
                    a.containsReleaseNoteFiles() || b.containsReleaseNoteFiles());
        }

        private FilePathAnalysis(boolean production, boolean releaseNote) {
            this.production = production;
            this.releaseNote = releaseNote;
        }

        public boolean containsProductionFiles() {
            return production;
        }

        public boolean containsReleaseNoteFiles() {
            return releaseNote;
        }

        // Complete because the end-state of "success" is now guaranteed
        public boolean isComplete() {
            return containsProductionFiles() && containsReleaseNoteFiles();
        }

    }

}