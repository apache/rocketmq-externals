package org.apache.rocketmq.connect.es.model;

import org.apache.rocketmq.connect.es.config.MapperConfig;
import org.apache.rocketmq.connect.es.config.SyncMetadata;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHits;

/**
 * create 需要找到main update 也要操作main ， 删除自己 就行了
 * 
 * @author laohu
 *
 */
public class ManyWaysModel extends AbstractModel {

	@Override
	public void update(SyncMetadata syncMetadata) {
		MapperConfig mapperConfig = syncMetadata.getMapperConfig();
		for (MapperConfig manyWaysMapperConfig : mapperConfig.getManyWaysMapperConfig()) {
			SyncMetadata manyWaysSyncMetadata = new SyncMetadata(syncMetadata, manyWaysMapperConfig);
			// 这里到底使用scroll 滚动还是，分页
			syncMetadata.getClient().searchAsync(
					getSearchRequest(manyWaysMapperConfig.getIndex(), manyWaysMapperConfig.getMainRelationField(),
							manyWaysSyncMetadata.getUniqueValue()),
					RequestOptions.DEFAULT,
					new ScrollActionListener(new SyncMetadata(manyWaysSyncMetadata, manyWaysMapperConfig)));
		}
	}

	class ScrollActionListener extends ModelDefaultActionListener<SearchResponse> {

		public ScrollActionListener(SyncMetadata syncMetadata) {
			super(syncMetadata);

		}

		@Override
		public void onResponse(SearchResponse searchResponse) {
			SearchHits hits = searchResponse.getHits();
			if (hits == null || hits.getTotalHits().value == 0) {
				return;
			}
			syncMetadata.getClient().bulkAsync(getBulkRequest(hits.getHits(), syncMetadata), RequestOptions.DEFAULT,
					new ModelDefaultActionListener<BulkResponse>(syncMetadata));
			if (hits.getTotalHits().value == 100) {
				syncMetadata.getClient().scrollAsync(
						getSearchScrollRequest(searchResponse, new Scroll(TimeValue.timeValueSeconds(300))),
						RequestOptions.DEFAULT, new ScrollActionListener(syncMetadata));
			}
		}

	}
}
