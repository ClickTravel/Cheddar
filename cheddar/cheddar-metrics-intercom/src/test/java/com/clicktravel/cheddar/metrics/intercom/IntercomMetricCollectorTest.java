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
package com.clicktravel.cheddar.metrics.intercom;

import static com.clicktravel.cheddar.metrics.intercom.random.data.RandomIntercomDataGenerator.randomIntercomUser;
import static com.clicktravel.cheddar.metrics.intercom.random.data.RandomMetricDataGenerator.randomMetricOrganisation;
import static com.clicktravel.cheddar.metrics.intercom.random.data.RandomMetricDataGenerator.randomMetricOrganisationWithCreatedAt;
import static com.clicktravel.cheddar.metrics.intercom.random.data.RandomMetricDataGenerator.randomMetricUser;
import static com.clicktravel.common.random.Randoms.randomId;
import static com.clicktravel.common.random.Randoms.randomString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.clicktravel.cheddar.metrics.MetricException;
import com.clicktravel.cheddar.metrics.MetricOrganisation;
import com.clicktravel.cheddar.metrics.MetricUser;
import com.clicktravel.cheddar.metrics.MetricUserNotFoundException;
import com.clicktravel.common.validation.ValidationException;

import io.intercom.api.*;

@SuppressWarnings({ "unchecked", "rawtypes" })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ User.class, Company.class, IntercomMetricCollector.class, Tag.class, Contact.class })
public class IntercomMetricCollectorTest {

    private MetricCustomAttributeToIntercomCustomAttributeMapper metricToIntercomMapper;
    private IntercomCustomAttributeToMetricCustomAttributeMapper intercomToMetricMapper;
    private IntercomMetricCollector intercomMetricCollector;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(User.class);
        PowerMockito.mockStatic(Company.class);
        PowerMockito.mockStatic(Tag.class);
        PowerMockito.mockStatic(Contact.class);
        final String personalAccessToken = randomString();
        metricToIntercomMapper = mock(MetricCustomAttributeToIntercomCustomAttributeMapper.class);
        intercomToMetricMapper = mock(IntercomCustomAttributeToMetricCustomAttributeMapper.class);
        intercomMetricCollector = new IntercomMetricCollector(personalAccessToken);
        intercomMetricCollector.setMetricToIntercomCustomAttributeMapper(metricToIntercomMapper);
        intercomMetricCollector.setIntercomToMetricCustomAttributeMapper(intercomToMetricMapper);
    }

    @Test
    public void shouldCreateIntercomCompany_withMetricOrganisation() throws Exception {
        // Given
        final MetricOrganisation metricOrganisation = randomMetricOrganisationWithCreatedAt();
        final long expectedRemoteCreatedAt = metricOrganisation.createdAt().getMillis() / 1000;

        // When
        intercomMetricCollector.createOrganisation(metricOrganisation);

        // Then
        final ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verifyStatic(Company.class);
        Company.create(companyCaptor.capture());
        final Company company = companyCaptor.getValue();
        assertThat(company.getCompanyID(), is(metricOrganisation.id()));
        assertThat(company.getName(), is(metricOrganisation.name()));
        assertThat(company.getRemoteCreatedAt(), is(expectedRemoteCreatedAt));
    }

    @Test
    public void shouldUpdateIntercomCompany_withMetricOrganisation() throws Exception {
        // Given
        final MetricOrganisation metricOrganisation = randomMetricOrganisation();
        final String metricOrganisationId = metricOrganisation.id();
        final Map<String, String> expectedFindParams = new HashMap<>();
        expectedFindParams.put("company_id", metricOrganisationId);
        final Company mockCompany = mock(Company.class);

        mockStatic(Company.class);
        when(Company.find(expectedFindParams)).thenReturn(mockCompany);

        // When
        intercomMetricCollector.updateOrganisation(metricOrganisation);

        // Then
        verify(mockCompany).setName(metricOrganisation.name());
        verifyStatic(Company.class);
        Company.update(mockCompany);
    }

    @Test
    public void shouldTagIntercomOrganisation_withMetricOrganisation() throws Exception {
        // Given
        final String tagName = randomString();
        final MetricOrganisation metricOrganisation = randomMetricOrganisation();

        // When
        intercomMetricCollector.tagOrganisation(tagName, metricOrganisation);

        // Then
        final ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        final ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verifyStatic(Tag.class);
        Tag.tag(tagCaptor.capture(), companyCaptor.capture());
        assertThat(tagCaptor.getValue().getName(), is(tagName));
        assertThat(companyCaptor.getValue().getCompanyID(), is(metricOrganisation.id()));
    }

    @Test
    public void shouldNotTagIntercomOrganisation_withExceptionThrownDuringTagging() throws Exception {
        // Given
        final String tagName = randomString();
        final MetricOrganisation metricOrganisation = randomMetricOrganisation();

        when(Tag.tag(any(Tag.class), any(Company.class))).thenThrow(IntercomException.class);

        // When
        MetricException thrownException = null;
        try {
            intercomMetricCollector.tagOrganisation(tagName, metricOrganisation);
        } catch (final MetricException e) {
            thrownException = e;
        }

        // Then
        assertNotNull(thrownException);
    }

    @Test
    public void shouldCreateUser_withMetricUser() throws Exception {
        // Given
        final MetricUser metricUser = randomMetricUser();
        final Map<String, CustomAttribute> customAttributes = mock(Map.class);
        when(metricToIntercomMapper.apply(metricUser.customAttributes())).thenReturn(customAttributes);
        DateTimeUtils.setCurrentMillisFixed(DateTime.now().getMillis());
        // When
        intercomMetricCollector.createUser(metricUser);

        // Then
        final ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verifyStatic(User.class);
        User.create(userCaptor.capture());
        final User intercomUser = userCaptor.getValue();
        assertThat(intercomUser.getId(), is(metricUser.id()));
        assertThat(intercomUser.getUserId(), is(metricUser.id()));
        assertThat(intercomUser.getCustomAttributes(), is(customAttributes));
        assertThat(intercomUser.getCompanyCollection().getPage().size(), is(metricUser.organisationIds().size()));
        assertThat(intercomUser.getSignedUpAt(), is(DateTime.now().getMillis() / 1000));
        intercomUser.getCompanyCollection().getPage()
                .forEach(company -> assertTrue(metricUser.organisationIds().contains(company.getCompanyID())));
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldUpdateUser_withMetricUser() throws Exception {
        // Given
        final MetricUser metricUser = randomMetricUser();
        final Map<String, CustomAttribute> customAttributes = mock(Map.class);
        when(metricToIntercomMapper.apply(metricUser.customAttributes())).thenReturn(customAttributes);
        // When
        intercomMetricCollector.updateUser(metricUser);

        // Then
        final ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verifyStatic(User.class);
        User.update(userCaptor.capture());
        final User intercomUser = userCaptor.getValue();
        assertThat(intercomUser.getId(), is(metricUser.id()));
        assertThat(intercomUser.getUserId(), is(metricUser.id()));
        assertThat(intercomUser.getUserId(), is(metricUser.id()));
        assertThat(intercomUser.getCustomAttributes(), is(customAttributes));
        assertThat(intercomUser.getCompanyCollection().getPage().size(), is(metricUser.organisationIds().size()));
        intercomUser.getCompanyCollection().getPage()
                .forEach(company -> assertTrue(metricUser.organisationIds().contains(company.getCompanyID())));
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldAddCustomAttributesToUser_withUserIdAndCustomAttributes() {
        // Given
        final String userId = randomId();
        final User mockUser = mock(User.class);
        final Map<String, Object> customAttributes = mock(Map.class);
        final Map<String, String> params = new HashMap<>();
        params.put("user_id", userId);
        when(User.find(params)).thenReturn(mockUser);
        final Map<String, CustomAttribute> mockCustomAttributes = mock(Map.class);
        when(metricToIntercomMapper.apply(customAttributes)).thenReturn(mockCustomAttributes);

        // When
        intercomMetricCollector.addCustomAttributesToUser(userId, customAttributes);

        // Then
        final ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verifyStatic(User.class);
        User.update(userCaptor.capture());
        assertThat(userCaptor.getValue().getCustomAttributes(), is(mockCustomAttributes));
    }

    @Test
    public void shouldNotAddCustomAttributesToUser_withIntercomFindUserException() {
        // Given
        final String userId = randomId();
        final Map<String, String> params = new HashMap<>();
        params.put("user_id", userId);
        when(User.find(params)).thenThrow(IntercomException.class);
        final Map<String, Object> customAttributes = mock(Map.class);

        // When
        intercomMetricCollector.addCustomAttributesToUser(userId, customAttributes);

        // Then
        verifyStatic(User.class, never());
        User.update(any(User.class));
    }

    @Test
    public void shouldAddOrganisationToUser_withUserIdAndOrganisationId() throws Exception {
        // Given
        final String userId = randomId();
        final Map<String, String> params = new HashMap<>();
        params.put("user_id", userId);
        final String organisationId = randomId();
        final User mockUser = mock(User.class);
        when(User.find(params)).thenReturn(mockUser);

        // When
        intercomMetricCollector.addOrganisationToUser(userId, organisationId);

        // Then
        final ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(mockUser).addCompany(companyCaptor.capture());
        assertThat(companyCaptor.getValue().getCompanyID(), is(organisationId));
        verifyStatic(User.class);
        User.update(mockUser);
    }

    @Test
    public void shouldNotAddOrganisationToUser_withUserIdAndOrganisationIdAndIntercomFindUserException() {
        // Given
        final String userId = randomId();
        final Map<String, String> params = new HashMap<>();
        params.put("user_id", userId);
        final String organisationId = randomId();
        when(User.find(params)).thenThrow(IntercomException.class);

        // When
        MetricException actualException = null;
        try {
            intercomMetricCollector.addOrganisationToUser(userId, organisationId);
        } catch (final MetricException e) {
            actualException = e;
        }

        // Then
        assertNotNull(actualException);
        verifyStatic(User.class, never());
        User.update(any(User.class));
    }

    @Test
    public void shouldRemoveOrganisationFromUser_withUserIdAndOrganisationId() throws Exception {
        // Given
        final String userId = randomId();
        final Map<String, String> params = new HashMap<>();
        params.put("user_id", userId);
        final String organisationId = randomId();
        final User mockUser = mock(User.class);
        when(User.find(params)).thenReturn(mockUser);
        final Company mockCompany = mock(Company.class);
        when(Company.find(organisationId)).thenReturn(mockCompany);

        // When
        intercomMetricCollector.removeOrganisationFromUser(userId, organisationId);

        // Then
        final ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(mockUser).removeCompany(companyCaptor.capture());
        assertThat(companyCaptor.getValue().getCompanyID(), is(organisationId));
        verifyStatic(User.class);
        User.update(mockUser);
    }

    @Test
    public void shouldNotRemoveOrganisationFromUser_withUserIdAndOrganisationIdAndIntercomFindUserException() {
        // Given
        final String userId = randomId();
        final Map<String, String> params = new HashMap<>();
        params.put("user_id", userId);
        final String organisationId = randomId();
        when(User.find(params)).thenThrow(IntercomException.class);

        // When
        intercomMetricCollector.removeOrganisationFromUser(userId, organisationId);

        // Then
        verifyStatic(User.class, never());
        User.update(any(User.class));
    }

    @Test
    public void shouldReturnUser_withUserId() {
        // Given
        final String userId = randomId();
        final Map<String, String> params = new HashMap<>();
        params.put("user_id", userId);
        final User user = randomIntercomUser();
        when(User.find(params)).thenReturn(user);
        final Map<String, CustomAttribute> mockCustomAttributes = mock(Map.class);
        user.setCustomAttributes(mockCustomAttributes);

        // When
        final MetricUser result = intercomMetricCollector.getUser(userId);

        // Then
        assertNotNull(result);
        assertThat(result.id(), is(user.getUserId()));
        user.getCompanyCollection().getPage().forEach(company -> {
            assertTrue(result.organisationIds().contains(company.getCompanyID()));
        });
        assertThat(result.name(), is(user.getName()));
        assertThat(result.emailAddress(), is(user.getEmail()));
        assertThat(result.customAttributes(), is(mockCustomAttributes));
    }

    @Test
    public void shouldNotReturnUser_withNullUserId() {
        // Given
        final String userId = null;

        // When
        ValidationException thrownException = null;
        try {
            intercomMetricCollector.getUser(userId);
        } catch (final ValidationException e) {
            thrownException = e;
        }

        // Then
        assertNotNull(thrownException);
    }

    @Test
    public void shouldNotReturnUser_withExceptionThrownByIntercom() {
        // Given
        final String userId = randomId();
        final Map<String, String> params = new HashMap<>();
        params.put("user_id", userId);

        when(User.find(params)).thenThrow(IntercomException.class);

        // When
        MetricUserNotFoundException thrownException = null;
        try {
            intercomMetricCollector.getUser(userId);
        } catch (final MetricUserNotFoundException e) {
            thrownException = e;
        }

        // Then
        assertNotNull(thrownException);
    }

    @Test
    public void shouldNotReturnUser_withNullUserReturnedByIntercom() {
        // Given
        final String userId = randomId();
        final Map<String, String> params = new HashMap<>();
        params.put("user_id", userId);
        final User user = null;

        when(User.find(params)).thenReturn(user);

        // When
        MetricUserNotFoundException thrownException = null;
        try {
            intercomMetricCollector.getUser(userId);
        } catch (final MetricUserNotFoundException e) {
            thrownException = e;
        }

        // Then
        assertNotNull(thrownException);
    }

    @Test
    public void shouldConvertExistingContactToUser_withContactIdMetricUser() throws Exception {
        // Given
        final String contactId = randomId();
        final MetricUser metricUser = randomMetricUser();
        final Contact mockContact = mock(Contact.class);

        when(Contact.findByUserID(contactId)).thenReturn(mockContact);

        // When
        intercomMetricCollector.convertExistingContactToUser(contactId, metricUser);

        // Then
        final ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verifyStatic(Contact.class);
        Contact.convert(eq(mockContact), userCaptor.capture());
        final User intercomUser = userCaptor.getValue();
        assertThat(intercomUser.getId(), is(metricUser.id()));
        assertThat(intercomUser.getUserId(), is(metricUser.id()));
        assertThat(intercomUser.getCompanyCollection().getPage().size(), is(metricUser.organisationIds().size()));
        intercomUser.getCompanyCollection().getPage()
                .forEach(company -> assertTrue(metricUser.organisationIds().contains(company.getCompanyID())));
    }

    @Test
    public void shouldNotConvertExistingContactToUser_withFindContactException() {
        // Given
        final String contactId = randomId();
        final MetricUser metricUser = randomMetricUser();

        when(Contact.findByUserID(contactId)).thenThrow(IntercomException.class);

        // When
        MetricException actualException = null;
        try {
            intercomMetricCollector.convertExistingContactToUser(contactId, metricUser);
        } catch (final MetricException e) {
            actualException = e;
        }

        // Then
        assertNotNull(actualException);
        verifyStatic(Contact.class, never());
        Contact.convert(any(Contact.class), any(User.class));

    }
}
