package org.apache.rocketmq.connect.es.model;

import org.apache.rocketmq.connect.es.config.MapperConfig;
import org.apache.rocketmq.connect.es.config.SyncMetadata;

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
		for(MapperConfig oneWays : syncMetadata.getMapperConfig().getOneWaysMapperConfig()) {
			super.update(new SyncMetadata(syncMetadata, oneWays));
		}
	}
}
