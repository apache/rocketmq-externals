package org.apache.rocketmq.connect.es.model;

import java.util.List;

import org.apache.rocketmq.connect.es.SyncMetadata;
import org.apache.rocketmq.connect.es.config.MapperConfig;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHits;

/**
 * <li>有一， 才有多 create是普通操作， 数据库层面是先有主，才有从</li>
 * <li>主修改，需要同步从</li>
 * <li>删除操作，值删除自己数据，在数据层面，应该是最后删除main数据</li>
 * 
 * @author laohu
 *
 */
public class OneWaysModel extends AbstractModel {

	@Override
	public void update(SyncMetadata syncMetadata) {
		MapperConfig mapperConfig = syncMetadata.getMapperConfig();
		List<MapperConfig> mapperConfigMapperConfig = mapperConfig.getManyWaysMapperConfig();
		String uniqueValue = syncMetadata.getUniqueValue();
		for (MapperConfig manyWaysMapperConfig : mapperConfigMapperConfig) {
			// 这里到底使用scroll 滚动还是，分页
			syncMetadata.getClient().searchAsync(getSearchRequest(manyWaysMapperConfig.getIndex(), null, uniqueValue), RequestOptions.DEFAULT,
					new ScrollActionListener(new SyncMetadata(syncMetadata,manyWaysMapperConfig)));
		}
	}

	class ScrollActionListener extends ModelDefaultActionListener<SearchResponse>{

		public ScrollActionListener(SyncMetadata syncMetadata) {
			super(syncMetadata);
			
		}
		@Override
		public void onResponse(SearchResponse searchResponse) {
			SearchHits hits = searchResponse.getHits();
			if (hits == null || hits.getTotalHits().value == 0) {
				return;
			}
			getBulkRequest(hits.getHits(), syncMetadata);
			if(hits.getTotalHits().value == 100) {
				syncMetadata.getMapperConfig().getRestHighLevelClient().scrollAsync(getSearchScrollRequest(searchResponse, new Scroll(TimeValue.timeValueSeconds(300))),
						RequestOptions.DEFAULT,
						new ScrollActionListener(syncMetadata));
			}
		}
		
	}
}
