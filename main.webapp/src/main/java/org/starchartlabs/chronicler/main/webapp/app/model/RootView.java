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
package org.starchartlabs.chronicler.main.webapp.app.model;

import java.util.Objects;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

// TODO romeara doc, test, add meta-data view
public class RootView {

    @JsonProperty(value = "installationUrl", required = true)
    private final String installationUrl;

    @JsonProperty(value = "sourceUrl", required = true)
    private final String sourceUrl;

    public RootView(String installationUrl, String sourceUrl) {
        this.installationUrl = Objects.requireNonNull(installationUrl);
        this.sourceUrl = Objects.requireNonNull(sourceUrl);
    }

    public String getInstallationUrl() {
        return installationUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInstallationUrl(),
                getSourceUrl());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        boolean result = false;

        if (obj instanceof RootView) {
            RootView compare = (RootView) obj;

            result = Objects.equals(compare.getInstallationUrl(), getInstallationUrl())
                    && Objects.equals(compare.getSourceUrl(), getSourceUrl());
        }

        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("installationUrl", getInstallationUrl())
                .add("sourceUrl", getSourceUrl())
                .toString();
    }
}