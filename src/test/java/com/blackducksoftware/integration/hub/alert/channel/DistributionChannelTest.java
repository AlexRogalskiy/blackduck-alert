/*
 * Copyright (C) 2018 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.alert.channel;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;

import org.junit.Test;
import org.mockito.Mockito;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.alert.TestGlobalProperties;
import com.blackducksoftware.integration.hub.alert.audit.repository.AuditEntryEntity;
import com.blackducksoftware.integration.hub.alert.audit.repository.AuditEntryRepositoryWrapper;
import com.blackducksoftware.integration.hub.alert.channel.email.EmailGroupChannel;
import com.blackducksoftware.integration.hub.alert.channel.email.mock.MockEmailGlobalEntity;
import com.blackducksoftware.integration.hub.alert.channel.email.repository.distribution.EmailGroupDistributionConfigEntity;
import com.blackducksoftware.integration.hub.alert.channel.email.repository.distribution.EmailGroupDistributionRepositoryWrapper;
import com.blackducksoftware.integration.hub.alert.channel.email.repository.global.GlobalEmailConfigEntity;
import com.blackducksoftware.integration.hub.alert.channel.email.repository.global.GlobalEmailRepositoryWrapper;
import com.blackducksoftware.integration.hub.alert.channel.slack.SlackChannel;
import com.blackducksoftware.integration.hub.alert.channel.slack.repository.global.GlobalSlackConfigEntity;
import com.blackducksoftware.integration.hub.alert.config.GlobalProperties;
import com.blackducksoftware.integration.hub.alert.datasource.entity.CommonDistributionConfigEntity;
import com.blackducksoftware.integration.hub.alert.datasource.entity.repository.CommonDistributionRepositoryWrapper;
import com.blackducksoftware.integration.hub.alert.digest.model.DigestModel;
import com.blackducksoftware.integration.hub.alert.enumeration.DigestTypeEnum;
import com.blackducksoftware.integration.hub.alert.enumeration.StatusEnum;
import com.blackducksoftware.integration.hub.alert.event.ChannelEvent;

public class DistributionChannelTest extends ChannelTest {
    @Test
    public void setAuditEntrySuccessCatchExceptionTest() {
        final EmailGroupChannel channel = new EmailGroupChannel(gson, null, null, null, null, null, contentConverter);
        channel.setAuditEntrySuccess(1L);
    }

    @Test
    public void setAuditEntrySuccessTest() {
        final AuditEntryRepositoryWrapper auditEntryRepository = Mockito.mock(AuditEntryRepositoryWrapper.class);
        final EmailGroupChannel channel = new EmailGroupChannel(gson, null, auditEntryRepository, null, null, null, contentConverter);

        final AuditEntryEntity entity = new AuditEntryEntity(1L, new Date(System.currentTimeMillis() - 1000), new Date(System.currentTimeMillis()), StatusEnum.SUCCESS, null, null);
        entity.setId(1L);
        Mockito.when(auditEntryRepository.findById(Mockito.anyLong())).thenReturn(entity);
        Mockito.when(auditEntryRepository.save(entity)).thenReturn(entity);

        channel.setAuditEntrySuccess(null);
        channel.setAuditEntrySuccess(entity.getId());
    }

    @Test
    public void setAuditEntryFailureCatchExceptionTest() {
        final EmailGroupChannel channel = new EmailGroupChannel(gson, null, null, null, null, null, contentConverter);
        channel.setAuditEntryFailure(1L, null, null);
    }

    @Test
    public void setAuditEntryFailureTest() {
        final AuditEntryRepositoryWrapper auditEntryRepository = Mockito.mock(AuditEntryRepositoryWrapper.class);
        final EmailGroupChannel channel = new EmailGroupChannel(gson, null, auditEntryRepository, null, null, null, contentConverter);

        final AuditEntryEntity entity = new AuditEntryEntity(1L, new Date(System.currentTimeMillis() - 1000), new Date(System.currentTimeMillis()), StatusEnum.FAILURE, null, null);
        entity.setId(1L);
        Mockito.when(auditEntryRepository.findById(Mockito.anyLong())).thenReturn(entity);
        Mockito.when(auditEntryRepository.save(entity)).thenReturn(entity);

        channel.setAuditEntryFailure(null, null, null);
        channel.setAuditEntryFailure(entity.getId(), "error", new Exception());
    }

    @Test
    public void getGlobalConfigEntityTest() {
        final GlobalEmailRepositoryWrapper globalEmailRepositoryWrapper = Mockito.mock(GlobalEmailRepositoryWrapper.class);
        final EmailGroupChannel channel = new EmailGroupChannel(gson, null, null, globalEmailRepositoryWrapper, null, null, contentConverter);

        final MockEmailGlobalEntity mockEntity = new MockEmailGlobalEntity();
        final GlobalEmailConfigEntity entity = mockEntity.createGlobalEntity();
        Mockito.when(globalEmailRepositoryWrapper.findAll()).thenReturn(Arrays.asList(entity));

        final GlobalEmailConfigEntity globalEntity = channel.getGlobalConfigEntity();
        assertEquals(entity, globalEntity);
    }

    @Test
    public void receiveMessageTest() {
        final GlobalProperties globalProperties = new TestGlobalProperties();
        final AuditEntryRepositoryWrapper auditEntryRepository = Mockito.mock(AuditEntryRepositoryWrapper.class);
        final GlobalEmailRepositoryWrapper globalEmailRepositoryWrapper = Mockito.mock(GlobalEmailRepositoryWrapper.class);
        final EmailGroupDistributionRepositoryWrapper emailGroupRepositoryWrapper = Mockito.mock(EmailGroupDistributionRepositoryWrapper.class);
        final CommonDistributionRepositoryWrapper commonRepositoryWrapper = Mockito.mock(CommonDistributionRepositoryWrapper.class);

        final EmailGroupChannel channel = new EmailGroupChannel(gson, globalProperties, auditEntryRepository, globalEmailRepositoryWrapper, emailGroupRepositoryWrapper, commonRepositoryWrapper, contentConverter);

        final Long commonId = 1L;
        final DigestModel digestModel = new DigestModel(createProjectData("Distribution Channel Test"));
        final ChannelEvent event = new ChannelEvent(SupportedChannels.EMAIL_GROUP, contentConverter.convertToString(digestModel), commonId);

        final CommonDistributionConfigEntity commonEntity = new CommonDistributionConfigEntity(commonId, SupportedChannels.EMAIL_GROUP, "Email Config", DigestTypeEnum.REAL_TIME, false);
        Mockito.when(commonRepositoryWrapper.findById(Mockito.anyLong())).thenReturn(commonEntity);

        final EmailGroupDistributionConfigEntity specificEntity = new EmailGroupDistributionConfigEntity("admins", "", "TEST SUBJECT LINE");
        Mockito.when(emailGroupRepositoryWrapper.findById(Mockito.anyLong())).thenReturn(specificEntity);

        channel.handleEvent(event);
    }

    //    @Test
    //    public void receiveMessageCatchExceptionTest() {
    //        final EmailGroupChannel channel = new EmailGroupChannel(null, null, null, null, null);
    //
    //        channel.handleEvent(null);
    //    }

    @Test
    public void handleEventWrongTypeTest() {
        final GlobalProperties globalProperties = new TestGlobalProperties();
        final CommonDistributionRepositoryWrapper commonRepositoryWrapper = Mockito.mock(CommonDistributionRepositoryWrapper.class);

        final EmailGroupChannel channel = new EmailGroupChannel(gson, globalProperties, null, null, null, commonRepositoryWrapper, contentConverter);

        final Long commonId = 1L;
        final DigestModel digestModel = new DigestModel(createProjectData("Distribution Channel Test"));
        final ChannelEvent event = new ChannelEvent(SupportedChannels.EMAIL_GROUP, contentConverter.convertToString(digestModel), commonId);

        final CommonDistributionConfigEntity commonEntity = new CommonDistributionConfigEntity(commonId, SupportedChannels.SLACK, "Other Config", DigestTypeEnum.REAL_TIME, false);
        Mockito.when(commonRepositoryWrapper.findById(Mockito.anyLong())).thenReturn(commonEntity);

        channel.handleEvent(event);
    }

    @Test
    public void testGlobalConfigTest() throws IntegrationException {
        // Slack has no global config, so we use it to test the default method.
        final SlackChannel slackChannel = new SlackChannel(gson, null, null, null, null, contentConverter);

        final String nullMessage = slackChannel.testGlobalConfig(null);
        assertEquals("The provided entity was null.", nullMessage);

        final String validEntityMessage = slackChannel.testGlobalConfig(new GlobalSlackConfigEntity());
        assertEquals("Not implemented.", validEntityMessage);
    }

}
