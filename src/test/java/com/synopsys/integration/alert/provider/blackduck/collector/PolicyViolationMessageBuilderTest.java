package com.synopsys.integration.alert.provider.blackduck.collector;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.Gson;
import com.synopsys.integration.alert.TestConstants;
import com.synopsys.integration.alert.common.message.model.ProviderMessageContent;
import com.synopsys.integration.alert.common.persistence.model.ConfigurationJobModel;
import com.synopsys.integration.alert.provider.blackduck.collector.builder.BlackDuckMessageBuilder;
import com.synopsys.integration.alert.provider.blackduck.collector.builder.PolicyClearedMessageBuilder;
import com.synopsys.integration.alert.provider.blackduck.collector.builder.PolicyViolationMessageBuilder;
import com.synopsys.integration.alert.provider.blackduck.collector.util.VulnerabilityUtil;
import com.synopsys.integration.blackduck.api.generated.enumeration.NotificationType;
import com.synopsys.integration.blackduck.api.manual.view.NotificationView;
import com.synopsys.integration.blackduck.api.manual.view.RuleViolationClearedNotificationView;
import com.synopsys.integration.blackduck.api.manual.view.RuleViolationNotificationView;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.bucket.BlackDuckBucket;

public class PolicyViolationMessageBuilderTest {
    private Gson gson = new Gson();

    @Test
    public void insertRuleViolationClearedNotificationTest() throws Exception {
        PolicyClearedMessageBuilder policyViolationClearedMessageBuilder = new PolicyClearedMessageBuilder();
        runSingleTest(policyViolationClearedMessageBuilder, TestConstants.POLICY_CLEARED_NOTIFICATION_JSON_PATH, NotificationType.RULE_VIOLATION_CLEARED);
    }

    @Test
    public void insertRuleViolationNotificationTest() throws Exception {
        VulnerabilityUtil vulnUtil = Mockito.mock(VulnerabilityUtil.class);
        PolicyViolationMessageBuilder policyViolationMessageBuilder = new PolicyViolationMessageBuilder(vulnUtil);
        runSingleTest(policyViolationMessageBuilder, TestConstants.POLICY_CLEARED_NOTIFICATION_JSON_PATH, NotificationType.RULE_VIOLATION);
    }

    private void runSingleTest(BlackDuckMessageBuilder messageBuilder, String notificationJsonFileName, NotificationType notificationType) throws Exception {
        String content = getNotificationContentFromFile(notificationJsonFileName);
        NotificationView notificationView = createNotificationView(content, notificationType);
        test(messageBuilder, notificationView);
    }

    private String getNotificationContentFromFile(String notificationJsonFileName) throws Exception {
        ClassPathResource classPathResource = new ClassPathResource(notificationJsonFileName);
        File jsonFile = classPathResource.getFile();
        return FileUtils.readFileToString(jsonFile, Charset.defaultCharset());
    }

    private NotificationView createNotificationView(String notificationContent, NotificationType notificationType) {
        if (NotificationType.RULE_VIOLATION_CLEARED == notificationType) {
            return gson.fromJson(notificationContent, RuleViolationClearedNotificationView.class);
        }
        return gson.fromJson(notificationContent, RuleViolationNotificationView.class);
    }

    private void test(BlackDuckMessageBuilder messageBuilder, NotificationView notificationView) {
        BlackDuckBucket blackDuckBucket = new BlackDuckBucket();
        BlackDuckServicesFactory blackDuckServicesFactory = BlackDuckMessageBuilderTestHelper.mockServicesFactory();

        Mockito.when(blackDuckServicesFactory.getBlackDuckHttpClient()).thenReturn(BlackDuckMessageBuilderTestHelper.mockHttpClient());

        ConfigurationJobModel job = Mockito.mock(ConfigurationJobModel.class);
        List<ProviderMessageContent> aggregateMessageContentList = messageBuilder.buildMessageContents(1L, new Date(), job, notificationView, blackDuckBucket, blackDuckServicesFactory);
        assertFalse(aggregateMessageContentList.isEmpty());
    }

}
