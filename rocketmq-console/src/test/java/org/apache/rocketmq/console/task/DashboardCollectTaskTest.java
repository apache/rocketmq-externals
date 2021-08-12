package org.apache.rocketmq.console.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.protocol.body.BrokerStatsData;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.body.KVTable;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.console.BaseTest;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.service.impl.DashboardCollectServiceImpl;
import org.apache.rocketmq.console.util.JsonUtil;
import org.apache.rocketmq.console.util.MockObjectUtil;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class DashboardCollectTaskTest extends BaseTest {

    @Spy
    private DashboardCollectTask dashboardCollectTask;

    @Spy
    private DashboardCollectServiceImpl dashboardCollectService;

    @Mock
    private MQAdminExt mqAdminExt;

    @Mock
    private RMQConfigure rmqConfigure;

    private int taskExecuteNum = 10;

    private File brokerFile;

    private File topicFile;

    @Before
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(rmqConfigure.getConsoleCollectData()).thenReturn("/tmp/rocketmq-console/test/data");
        ClusterInfo clusterInfo = MockObjectUtil.createClusterInfo();
        when(mqAdminExt.examineBrokerClusterInfo()).thenReturn(clusterInfo);
        String dataLocationPath = rmqConfigure.getConsoleCollectData();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String nowDateStr = format.format(new Date());
        brokerFile = new File(dataLocationPath + nowDateStr + ".json");
        topicFile = new File(dataLocationPath + nowDateStr + "_topic" + ".json");
        autoInjection();
    }

    @Test
    public void testCollectTopic() throws Exception {
        // enableDashBoardCollect = false
        when(rmqConfigure.isEnableDashBoardCollect()).thenReturn(false);
        dashboardCollectTask.collectTopic();
        {
            TopicList topicList = new TopicList();
            Set<String> topicSet = new HashSet<>();
            topicSet.add("topic_test");
            topicSet.add("%RETRY%group_test");
            topicSet.add("%DLQ%group_test");
            topicList.setTopicList(topicSet);
            when(mqAdminExt.fetchAllTopicList())
                .thenThrow(new RuntimeException("fetchAllTopicList exception"))
                .thenReturn(topicList);
            TopicRouteData topicRouteData = MockObjectUtil.createTopicRouteData();
            when(mqAdminExt.examineTopicRouteInfo(anyString())).thenReturn(topicRouteData);
            GroupList list = new GroupList();
            list.setGroupList(Sets.newHashSet("group_test"));
            when(mqAdminExt.queryTopicConsumeByWho(anyString())).thenReturn(list);
            BrokerStatsData brokerStatsData = MockObjectUtil.createBrokerStatsData();
            when(mqAdminExt.viewBrokerStatsData(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("viewBrokerStatsData TOPIC_PUT_NUMS exception"))
                .thenThrow(new RuntimeException("viewBrokerStatsData GROUP_GET_NUMS exception"))
                .thenReturn(brokerStatsData);
            when(rmqConfigure.isEnableDashBoardCollect()).thenReturn(true);
        }
        // fetchAllTopicList exception
        try {
            dashboardCollectTask.collectTopic();
        } catch (Exception e) {
            Assert.assertEquals(e.getMessage(), "fetchAllTopicList exception");
        }
        for (int i = 0; i < taskExecuteNum; i++) {
            dashboardCollectTask.collectTopic();
        }
        LoadingCache<String, List<String>> map = dashboardCollectService.getTopicMap();
        Assert.assertEquals(map.size(), 1);
        Assert.assertEquals(map.get("topic_test").size(), taskExecuteNum);
        dashboardCollectTask.saveData();
        Assert.assertEquals(topicFile.exists(), true);
        Map<String, List<String>> topicData =
            JsonUtil.string2Obj(MixAll.file2String(topicFile),
                new TypeReference<Map<String, List<String>>>() {
                });
        Assert.assertEquals(topicData.get("topic_test").size(), taskExecuteNum);
    }

    @Test
    public void testCollectBroker() throws Exception {
        // enableDashBoardCollect = false
        when(rmqConfigure.isEnableDashBoardCollect()).thenReturn(false);
        dashboardCollectTask.collectBroker();
        {
            HashMap<String, String> result = new HashMap<>();
            result.put("getTotalTps", "0.0 0.033330000333300004 0.03332972261338355");
            result.put("commitLogMinOffset", "0");
            KVTable kvTable = new KVTable();
            kvTable.setTable(result);
            when(mqAdminExt.fetchBrokerRuntimeStats(anyString()))
                .thenThrow(new RuntimeException("fetchBrokerRuntimeStats exception"))
                .thenReturn(kvTable);
            when(rmqConfigure.isEnableDashBoardCollect()).thenReturn(true);
        }
        // fetchBrokerRuntimeStats exception
        try {
            dashboardCollectTask.collectBroker();
        } catch (Exception e) {
            Assert.assertEquals(e.getMessage(), "fetchBrokerRuntimeStats exception");
        }

        for (int i = 0; i < taskExecuteNum; i++) {
            dashboardCollectTask.collectBroker();
        }
        LoadingCache<String, List<String>> map = dashboardCollectService.getBrokerMap();
        Assert.assertEquals(map.size(), 1);
        Assert.assertEquals(map.get("broker-a" + ":" + MixAll.MASTER_ID).size(), taskExecuteNum);
        mockBrokerFileExistBeforeSaveData();
        dashboardCollectTask.saveData();
        Assert.assertEquals(brokerFile.exists(), true);
        Map<String, List<String>> brokerData =
            JsonUtil.string2Obj(MixAll.file2String(brokerFile),
                new TypeReference<Map<String, List<String>>>() {
                });
        Assert.assertEquals(brokerData.get("broker-a" + ":" + MixAll.MASTER_ID).size(), taskExecuteNum + 2);
    }

    @After
    public void after() {
        if (brokerFile != null && brokerFile.exists()) {
            brokerFile.delete();
        }
        if (topicFile != null && topicFile.exists()) {
            topicFile.delete();
        }
    }

    private void mockBrokerFileExistBeforeSaveData() throws Exception {
        Map<String, List<String>> map = new HashMap<>();
        map.put("broker-a" + ":" + MixAll.MASTER_ID,  Lists.asList("1000", new String[] {"1000"}));
        map.put("broker-b" + ":" + MixAll.MASTER_ID,  Lists.asList("1000", new String[] {"1000"}));
        MixAll.string2File(JsonUtil.obj2String(map), brokerFile.getAbsolutePath());
    }
}
