package art.ourhyt.legacy2modern.advanced.infrastructure.adapters.out.aws;

import art.ourhyt.legacy2modern.advanced.domain.model.ConsumeQuotaResult;
import art.ourhyt.legacy2modern.advanced.domain.model.QuotaStatus;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.AdvancedConfigPort;
import art.ourhyt.legacy2modern.advanced.domain.ports.out.AdvancedQuotaRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class DynamoAdvancedQuotaRepositoryAdapter implements AdvancedQuotaRepositoryPort {
    private static final Logger LOG = Logger.getLogger(DynamoAdvancedQuotaRepositoryAdapter.class);
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final DynamoDbClient dynamoDbClient;
    private final AdvancedConfigPort config;

    @Inject
    public DynamoAdvancedQuotaRepositoryAdapter(DynamoDbClient dynamoDbClient, AdvancedConfigPort config) {
        this.dynamoDbClient = dynamoDbClient;
        this.config = config;
    }

    @Override
    public ConsumeQuotaResult consumeDaily(String userId, int defaultDailyLimit) {
        final LocalDate day = LocalDate.now(ZoneOffset.UTC);
        final String dayKey = day.format(DAY_FORMAT);
        final String resetAt = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toString();
        final long ttl = Instant.now().plusSeconds(8L * 24L * 3600L).getEpochSecond();
        final Map<String, AttributeValue> key = key(userId, dayKey);

        LOG.infov("userId={0} day={1} step=quota_consume_start table={2}", userId, dayKey, config.usageTable());
        final Map<String, String> names = Map.of("#limit", "limit", "#ttl", "ttl");
        final Map<String, AttributeValue> initValues = new HashMap<>();
        initValues.put(":limit", AttributeValue.builder().n(String.valueOf(defaultDailyLimit)).build());
        initValues.put(":ttl", AttributeValue.builder().n(String.valueOf(ttl)).build());

        final UpdateItemRequest initRequest = UpdateItemRequest.builder()
            .tableName(config.usageTable())
            .key(key)
            .updateExpression("SET #limit = if_not_exists(#limit, :limit), #ttl = :ttl")
            .expressionAttributeNames(names)
            .expressionAttributeValues(initValues)
            .build();
        dynamoDbClient.updateItem(initRequest);

        final Map<String, String> consumeNames = Map.of(
            "#count", "count",
            "#limit", "limit",
            "#ttl", "ttl"
        );
        final Map<String, AttributeValue> consumeValues = new HashMap<>();
        consumeValues.put(":one", AttributeValue.builder().n("1").build());
        consumeValues.put(":ttl", AttributeValue.builder().n(String.valueOf(ttl)).build());

        final UpdateItemRequest consumeRequest = UpdateItemRequest.builder()
            .tableName(config.usageTable())
            .key(key)
            .updateExpression("SET #ttl = :ttl ADD #count :one")
            .conditionExpression("attribute_not_exists(#count) OR #count < #limit")
            .expressionAttributeNames(consumeNames)
            .expressionAttributeValues(consumeValues)
            .build();

        try {
            dynamoDbClient.updateItem(consumeRequest);
        } catch (ConditionalCheckFailedException exception) {
            final QuotaStatus current = getToday(userId, defaultDailyLimit);
            LOG.infov("userId={0} day={1} step=quota_consume_denied used={2} limit={3}", userId, dayKey, current.used(), current.limit());
            return new ConsumeQuotaResult(false, new QuotaStatus(current.limit(), current.limit(), 0, current.resetAt()));
        }

        final QuotaStatus updated = getToday(userId, defaultDailyLimit);
        LOG.infov("userId={0} day={1} step=quota_consume_success used={2} limit={3}", userId, dayKey, updated.used(), updated.limit());
        return new ConsumeQuotaResult(true, updated);
    }

    @Override
    public QuotaStatus getToday(String userId, int defaultDailyLimit) {
        final LocalDate day = LocalDate.now(ZoneOffset.UTC);
        final String dayKey = day.format(DAY_FORMAT);
        final String resetAt = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toString();

        LOG.infov("userId={0} day={1} step=quota_read_start table={2}", userId, dayKey, config.usageTable());
        final GetItemRequest request = GetItemRequest.builder()
            .tableName(config.usageTable())
            .key(key(userId, dayKey))
            .build();

        final GetItemResponse response = dynamoDbClient.getItem(request);
        if (!response.hasItem() || response.item().isEmpty()) {
            LOG.infov("userId={0} day={1} step=quota_read_empty defaultLimit={2}", userId, dayKey, defaultDailyLimit);
            return new QuotaStatus(defaultDailyLimit, 0, defaultDailyLimit, resetAt);
        }

        final int limit = readInt(response.item(), "limit", defaultDailyLimit);
        final int used = readInt(response.item(), "count", 0);
        final int remaining = Math.max(0, limit - used);
        LOG.infov("userId={0} day={1} step=quota_read_success used={2} limit={3}", userId, dayKey, used, limit);
        return new QuotaStatus(limit, used, remaining, resetAt);
    }

    private Map<String, AttributeValue> key(String userId, String day) {
        return Map.of(
            "userId", AttributeValue.builder().s(userId).build(),
            "day", AttributeValue.builder().s(day).build()
        );
    }

    private int readInt(Map<String, AttributeValue> item, String key, int fallback) {
        final AttributeValue value = item.get(key);
        if (value == null || value.n() == null) {
            return fallback;
        }
        return Integer.parseInt(value.n());
    }
}
