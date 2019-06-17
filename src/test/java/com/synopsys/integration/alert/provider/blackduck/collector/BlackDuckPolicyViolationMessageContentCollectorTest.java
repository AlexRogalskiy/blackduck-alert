package com.synopsys.integration.alert.provider.blackduck.collector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;

import com.google.gson.Gson;
import com.synopsys.integration.alert.TestConstants;
import com.synopsys.integration.alert.common.enumeration.FormatType;
import com.synopsys.integration.alert.common.enumeration.ItemOperation;
import com.synopsys.integration.alert.common.message.model.AggregateMessageContent;
import com.synopsys.integration.alert.common.message.model.CategoryItem;
import com.synopsys.integration.alert.common.message.model2.MessageContentGroup;
import com.synopsys.integration.alert.common.workflow.filter.field.JsonExtractor;
import com.synopsys.integration.alert.common.workflow.processor2.DefaultMessageContentProcessor;
import com.synopsys.integration.alert.common.workflow.processor2.DigestMessageContentProcessor;
import com.synopsys.integration.alert.common.workflow.processor2.MessageContentCollapser;
import com.synopsys.integration.alert.common.workflow.processor2.MessageContentProcessor;
import com.synopsys.integration.alert.database.notification.NotificationContent;
import com.synopsys.integration.alert.provider.blackduck.BlackDuckProperties;
import com.synopsys.integration.alert.provider.blackduck.BlackDuckProvider;
import com.synopsys.integration.blackduck.api.generated.enumeration.NotificationType;

public class BlackDuckPolicyViolationMessageContentCollectorTest {
    private final JsonExtractor jsonExtractor = new JsonExtractor(new Gson());
    private final List<MessageContentProcessor> messageContentProcessorList = Arrays.asList(new DefaultMessageContentProcessor(), new DigestMessageContentProcessor(new DefaultMessageContentProcessor(), new MessageContentCollapser()));

    public static final void insertAndAssertCountsOnTopic(final BlackDuckPolicyCollector collector, final NotificationContent notification, final String topicName, final int expectedCategoryItemsCount,
        final int expectedLinkableItemsCount) {
        collector.insert(notification);
        final AggregateMessageContent content = collector.collect(FormatType.DEFAULT)
                                                    .stream()
                                                    .filter(contentGroup -> topicName.equals(contentGroup.getCommonTopic().getValue()))
                                                    .map(MessageContentGroup::getSubContent)
                                                    .flatMap(List::stream)
                                                    .findFirst()
                                                    .orElse(null);
        final SortedSet<CategoryItem> items = content.getCategoryItems();
        Assert.assertEquals(expectedCategoryItemsCount, items.size());
        Assert.assertEquals(expectedLinkableItemsCount, getCategoryItemLinkableItemsCount(items));
    }

    public static int getCategoryItemLinkableItemsCount(final SortedSet<CategoryItem> items) {
        int count = 0;
        for (final CategoryItem item : items) {
            count += item.getItems().size();
        }
        return count;
    }

    @Test
    public void insertRuleViolationClearedNotificationTest() throws Exception {
        final BlackDuckPolicyCollector collector = createPolicyViolationCollector();
        runSingleTest(collector, TestConstants.POLICY_CLEARED_NOTIFICATION_JSON_PATH, NotificationType.RULE_VIOLATION_CLEARED);
    }

    @Test
    public void insertRuleViolationNotificationTest() throws Exception {
        final BlackDuckPolicyCollector collector = createPolicyViolationCollector();
        runSingleTest(collector, TestConstants.POLICY_CLEARED_NOTIFICATION_JSON_PATH, NotificationType.RULE_VIOLATION);
    }

    // FIXME the test is geared towards a very specific format. Since it's now more flexible, we'll have to think of a new standard to measure
    // @Test
    public void insertMultipleAndVerifyCorrectNumberOfCategoryItemsTest() throws Exception {
        final String topicName = "example";
        final int numberOfRulesCleared = 4;

        // there are 3 possible linkable items per notification in the test data
        // 1- policy rule
        // 2- component
        // 3- component version or policy override user
        final int linkableItemsPerCategory = 3;

        final String ruleContent = getNotificationContentFromFile(TestConstants.POLICY_CLEARED_NOTIFICATION_JSON_PATH);

        final NotificationContent n0 = createNotification(ruleContent, NotificationType.RULE_VIOLATION_CLEARED);
        final NotificationContent n1 = createNotification(ruleContent, NotificationType.RULE_VIOLATION_CLEARED);

        final BlackDuckPolicyCollector collector = createPolicyViolationCollector();

        //Rules are now being combined to fix size
        int categoryCount = linkableItemsPerCategory;
        // add 1 item for the policy override name linkable items
        int linkableItemsCount = categoryCount * linkableItemsPerCategory;
        insertAndAssertCountsOnTopic(collector, n0, topicName, categoryCount, linkableItemsCount);

        categoryCount = numberOfRulesCleared;
        linkableItemsCount = categoryCount * linkableItemsPerCategory;
        insertAndAssertCountsOnTopic(collector, n1, topicName, categoryCount, linkableItemsCount);

        Assert.assertEquals(1, collector.collect(FormatType.DEFAULT).size());
    }

    @Test
    public void testOperationCircuitBreaker() throws Exception {
        final String ruleContent = getNotificationContentFromFile(TestConstants.NOTIFICATION_JSON_PATH);
        final NotificationContent n0 = createNotification(ruleContent, NotificationType.BOM_EDIT);
        final BlackDuckPolicyCollector collector = createPolicyViolationCollector();
        collector.insert(n0);
        Assert.assertEquals(0, collector.collect(FormatType.DEFAULT).size());
    }

    @Test
    public void insertionExceptionTest() throws Exception {
        final BlackDuckPolicyViolationCollector collector = createPolicyViolationCollector();
        final BlackDuckPolicyViolationCollector spiedCollector = Mockito.spy(collector);
        final String overrideContent = getNotificationContentFromFile(TestConstants.POLICY_OVERRIDE_NOTIFICATION_JSON_PATH);
        final NotificationContent n0 = createNotification(overrideContent, NotificationType.POLICY_OVERRIDE);
        Mockito.doThrow(new IllegalArgumentException("Insertion Error Exception Test")).when(spiedCollector)
            .addApplicableItems(Mockito.any(SortedSet.class), Mockito.anyLong(), Mockito.any(Set.class), Mockito.any(ItemOperation.class), Mockito.any(Set.class));
        spiedCollector.insert(n0);
        final List<MessageContentGroup> contentList = spiedCollector.collect(FormatType.DEFAULT);
        assertTrue(contentList.isEmpty());
    }

    @Test
    public void collectEmptyMapTest() {
        final BlackDuckPolicyCollector collector = createPolicyViolationCollector();
        final BlackDuckPolicyCollector spiedCollector = Mockito.spy(collector);
        final List<MessageContentGroup> contentList = spiedCollector.collect(FormatType.DEFAULT);
        assertTrue(contentList.isEmpty());
    }

    private void runSingleTest(final BlackDuckPolicyCollector collector, final String notificationJsonFileName, final NotificationType notificationType) throws Exception {
        final String content = getNotificationContentFromFile(TestConstants.POLICY_CLEARED_NOTIFICATION_JSON_PATH);
        final NotificationContent notificationContent = createNotification(content, notificationType);
        test(collector, notificationContent);
    }

    private BlackDuckPolicyViolationCollector createPolicyViolationCollector() {
        final BlackDuckProperties blackDuckProperties = Mockito.mock(BlackDuckProperties.class);
        Mockito.when(blackDuckProperties.createBlackDuckHttpClientAndLogErrors(Mockito.any(Logger.class))).thenReturn(Optional.empty());
        return new BlackDuckPolicyViolationCollector(jsonExtractor, messageContentProcessorList, blackDuckProperties);
    }

    private String getNotificationContentFromFile(final String notificationJsonFileName) throws Exception {
        final ClassPathResource classPathResource = new ClassPathResource(notificationJsonFileName);
        final File jsonFile = classPathResource.getFile();
        return FileUtils.readFileToString(jsonFile, Charset.defaultCharset());
    }

    private NotificationContent createNotification(final String notificationContent, final NotificationType type) {
        final Date creationDate = Date.from(Instant.now());
        return new NotificationContent(creationDate, BlackDuckProvider.COMPONENT_NAME, creationDate, type.name(), notificationContent);
    }

    private void test(final BlackDuckPolicyCollector collector, final NotificationContent notification) {
        collector.insert(notification);
        final List<MessageContentGroup> aggregateMessageContentList = collector.collect(FormatType.DEFAULT);
        assertFalse(aggregateMessageContentList.isEmpty());
    }
}
