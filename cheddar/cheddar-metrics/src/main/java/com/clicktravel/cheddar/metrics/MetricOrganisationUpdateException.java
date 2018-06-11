/*
 * Copyright 2014 Click Travel Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.clicktravel.cheddar.metrics;

public class MetricOrganisationUpdateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String organisationId;
    private final String organisationName;

    public MetricOrganisationUpdateException(final String organisationId, final String organisationName) {
        this.organisationId = organisationId;
        this.organisationName = organisationName;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("MetricOrganisationUpdateException [organisationId=");
        builder.append(organisationId);
        builder.append(", organisationName=");
        builder.append(organisationName);
        builder.append("]");
        return builder.toString();
    }

}
