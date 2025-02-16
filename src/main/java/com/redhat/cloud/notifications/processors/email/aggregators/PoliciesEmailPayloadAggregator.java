package com.redhat.cloud.notifications.processors.email.aggregators;

import com.redhat.cloud.notifications.models.EmailAggregation;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.HashSet;

public class PoliciesEmailPayloadAggregator extends AbstractEmailPayloadAggregator {

    private static final String POLICIES_KEY = "policies";
    private static final String HOST_KEY = "hosts";
    private static final String UNIQUE_SYSTEM_COUNT = "unique_system_count";
    private static final String CONTEXT_KEY = "context";
    private static final String EVENTS_KEY = "events";
    private static final String PAYLOAD_KEY = "payload";

    // Policy related
    private static final String POLICY_ID = "policy_id";
    private static final String POLICY_NAME = "policy_name";
    private static final String POLICY_DESCRIPTION = "policy_description";
    private static final String POLICY_CONDITION = "policy_condition";

    // Host
    private static final String DISPLAY_NAME = "display_name";
    private static final String INSIGHTS_ID = "insights_id";
    private static final String TAGS = "tags";

    private HashSet<String> uniqueHosts = new HashSet<>();
    private HashMap<String, HashSet<String>> uniqueHostPerPolicy = new HashMap<>();


    public PoliciesEmailPayloadAggregator() {
        context.put(POLICIES_KEY, new JsonObject());
    }

    public void processEmailAggregation(EmailAggregation notification) {
        JsonObject notificationJson = notification.getPayload();

        JsonObject policies = context.getJsonObject(POLICIES_KEY);
        JsonObject context = notificationJson.getJsonObject(CONTEXT_KEY);

        JsonObject host = new JsonObject();
        this.copyStringField(host, context, DISPLAY_NAME);
        this.copyStringField(host, context, INSIGHTS_ID);
        host.put(TAGS, context.getJsonArray(TAGS));

        notificationJson.getJsonArray(EVENTS_KEY).stream().forEach(eventObject -> {
            JsonObject event = (JsonObject) eventObject;
            JsonObject payload = event.getJsonObject(PAYLOAD_KEY);
            String policyId = payload.getString(POLICY_ID);

            if (!policies.containsKey(policyId)) {
                JsonObject newPolicy = new JsonObject();
                this.copyStringField(newPolicy, payload, POLICY_NAME);
                this.copyStringField(newPolicy, payload, POLICY_ID);
                this.copyStringField(newPolicy, payload, POLICY_DESCRIPTION);
                this.copyStringField(newPolicy, payload, POLICY_CONDITION);

                newPolicy.put(HOST_KEY, new JsonArray());

                policies.put(policyId, newPolicy);
                uniqueHostPerPolicy.put(policyId, new HashSet<>());
            }

            JsonObject policy = policies.getJsonObject(policyId);
            String insightsId = host.getString(INSIGHTS_ID);
            policy.getJsonArray(HOST_KEY).add(host);
            uniqueHostPerPolicy.get(policyId).add(insightsId);
            policy.put(UNIQUE_SYSTEM_COUNT, this.uniqueHostPerPolicy.get(policyId).size());
        });

        String insightsId = host.getString(INSIGHTS_ID);
        uniqueHosts.add(insightsId);
        this.context.put(UNIQUE_SYSTEM_COUNT, this.uniqueHosts.size());
    }

    public Integer getUniqueHostCount() {
        return this.uniqueHosts.size();
    }
}
