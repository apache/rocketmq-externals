package org.apache.rocketmq.connect.es.model;

import org.apache.rocketmq.connect.es.SyncMetadata;
import org.apache.rocketmq.connect.es.config.MapperConfig;

/**
 * create 需要找到main
 * update  也要操作main ，
 * 删除自己 就行了
 * @author laohu
 *
 */
public class ManyWaysModel extends AbstractModel {

	@Override
	public void update(SyncMetadata syncMetadata) {
		for(MapperConfig manyWays : syncMetadata.getMapperConfig().getManyWaysMapperConfig()) {
			update(new SyncMetadata(syncMetadata, manyWays));
		}
	}
}
