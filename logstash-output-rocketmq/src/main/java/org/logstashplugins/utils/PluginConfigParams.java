package org.logstashplugins.utils;

import co.elastic.logstash.api.PluginConfigSpec;

import java.util.Arrays;
import java.util.List;

public class PluginConfigParams {

    public static final PluginConfigSpec<String> PREFIX_CONFIG =
            PluginConfigSpec.stringSetting("prefix", "");

    public static final PluginConfigSpec<String> NAMESRV_CONFIG
            = PluginConfigSpec.stringSetting("namesrvAddr", "127.0.0.1:9876");

    public static final PluginConfigSpec<String> PRODUCER_GROUP_CONFIG
            = PluginConfigSpec.stringSetting("producerGroup", "PRODUCER_FROM_LOGSTASH");

    public static final PluginConfigSpec<String> MAMESPACE_CONFIG
            = PluginConfigSpec.stringSetting("namespace", "");

    public static final PluginConfigSpec<Boolean> ENABLE_MSG_TRACE_CONFIG
            = PluginConfigSpec.booleanSetting("enableMsgTrace", false);

    public static final PluginConfigSpec<String> TOPIC_CONFIG
            = PluginConfigSpec.stringSetting("topic", "TopicTest");

    public static final PluginConfigSpec<String> FLAG_CONFIG
            = PluginConfigSpec.stringSetting("flag", "0");

    public static final PluginConfigSpec<String> KEY_CONFIG
            = PluginConfigSpec.stringSetting("tag", "TEST_TAG");

    public static final PluginConfigSpec<String> TAG_CONFIG
            = PluginConfigSpec.stringSetting("key", "TEST_KEY");

    public static final PluginConfigSpec<String> DEFAULT_TOPIC_QUEUE_NUMS_CONFIG
            = PluginConfigSpec.stringSetting("defaultTopicQueueNums", "4");

    public static final PluginConfigSpec<String> SEND_MSG_TIMEOUT_CONFIG
            = PluginConfigSpec.stringSetting("sendMsgTimeout", "3000");

    public static final PluginConfigSpec<String> COMPRESS_MSG_BODY_OVER_HOWMUCH_GONFIG
            = PluginConfigSpec.stringSetting("compressMsgBodyOverHowmuch", "4096");

    public static final PluginConfigSpec<String> RETRY_TIMES_WHEN_SEND_FAILD_CONFIG
            = PluginConfigSpec.stringSetting("retryTimesWhenSendFailed", "2");

    public static final PluginConfigSpec<String> RETRY_TIMES_WHEN_SEND_ASYHCFAILD_CONFIG
            = PluginConfigSpec.stringSetting("retryTimesWhenSendAsyncFailed", "2");

    public static final PluginConfigSpec<Boolean> RETRY_ANOTHER_BROKER_WHEN_NOT_STRRE_OK
            = PluginConfigSpec.booleanSetting("retryAnotherBrokerWhenNotStoreOK", false);

    public static final PluginConfigSpec<String> MAX_MESSAGE_SIZE
            = PluginConfigSpec.stringSetting("maxMessageSize", "4194304");

    public static final PluginConfigSpec<String> SEND_MODE
            = PluginConfigSpec.stringSetting("sendMode", "SYNC");

    public static final PluginConfigSpec<Boolean> SEND_EMAIL_FAILED_CONFIG
            = PluginConfigSpec.booleanSetting("sendEmailFailed", false);

    public static final PluginConfigSpec<String> MY_EMAIL_ACCOUNT_CONFIG
            = PluginConfigSpec.stringSetting("myEmailAccount", null);

    public static final PluginConfigSpec<String> MY_EMAIL_PASSWORD_CONFIG
            = PluginConfigSpec.stringSetting("myEmailPassword", null);

    public static final PluginConfigSpec<String> MY_EMAIL_SMTPHOST_CONFIG
            = PluginConfigSpec.stringSetting("myEmailSMTPHost", null);

    public static final PluginConfigSpec<String> RECEIVE_MAIL_ACCOUNT_CONFIG
            = PluginConfigSpec.stringSetting("receiveMailAccount", null);

    public static PluginConfigSpec<?>[] paramsArray = new PluginConfigSpec[]{
            PREFIX_CONFIG, NAMESRV_CONFIG, PRODUCER_GROUP_CONFIG, SEND_EMAIL_FAILED_CONFIG, MAMESPACE_CONFIG, ENABLE_MSG_TRACE_CONFIG, TOPIC_CONFIG,
            DEFAULT_TOPIC_QUEUE_NUMS_CONFIG, SEND_MSG_TIMEOUT_CONFIG, COMPRESS_MSG_BODY_OVER_HOWMUCH_GONFIG, RETRY_TIMES_WHEN_SEND_FAILD_CONFIG,
            RETRY_TIMES_WHEN_SEND_ASYHCFAILD_CONFIG, RETRY_ANOTHER_BROKER_WHEN_NOT_STRRE_OK, MAX_MESSAGE_SIZE, SEND_MODE, FLAG_CONFIG,
            KEY_CONFIG, TAG_CONFIG, MY_EMAIL_ACCOUNT_CONFIG, MY_EMAIL_PASSWORD_CONFIG, MY_EMAIL_SMTPHOST_CONFIG, RECEIVE_MAIL_ACCOUNT_CONFIG
    };

    public static List<PluginConfigSpec<?>> paramsList = Arrays.asList(paramsArray);


    public static final int SYNC = 0;
    public static final int ASYNC = 1;
    public static final int ONEWAY = 2;
    public static final int BATCH = 3;

    public static int transeferMode(String mode) {
        if (mode.equals("ASYNC")) {
            return PluginConfigParams.ASYNC;
        }
        if (mode.equals("ONEWAY")) {
            return PluginConfigParams.ONEWAY;
        }
        if (mode.equals("BATCH")) {
            return PluginConfigParams.BATCH;
        }
        return PluginConfigParams.SYNC;
    }
}
