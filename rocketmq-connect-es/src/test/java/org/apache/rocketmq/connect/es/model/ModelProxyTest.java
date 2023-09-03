package org.apache.rocketmq.connect.es.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ModelProxyTest {

	private ModelProxy modelProxy = new ModelProxy();

	private Map<ModelType, Model> modelMap = new HashMap<ModelType, Model>();

	@Mock
	private SimpleModel simpleModel;

	@Mock
	private OneWaysModel oneWaysModel;

	@Mock
	private ManyWaysModel manyWaysModel;

	@Mock
	private MainModel mainModel;

	@Before
	public void init() throws IllegalAccessException {
		modelMap.put(ModelType.SIMPLE, simpleModel);
		modelMap.put(ModelType.ONEWAYS, oneWaysModel);
		modelMap.put(ModelType.MANYWAYS, manyWaysModel);
		modelMap.put(ModelType.MAIN, mainModel);
		
		FieldUtils.writeDeclaredField(modelProxy, "modelMap", modelMap,true);
	}

	@Test
	public void createTest() {
		modelProxy.create(null);
		
		Mockito.verify(simpleModel, Mockito.only()).create(null);
		Mockito.verify(oneWaysModel, Mockito.never()).create(null);
		Mockito.verify(manyWaysModel, Mockito.never()).create(null);
		Mockito.verify(mainModel, Mockito.only()).create(null);
		
	}

	@Test
	public void updateTest() {
		modelProxy.update(null);
		Mockito.verify(simpleModel, Mockito.only()).update(null);
		Mockito.verify(oneWaysModel, Mockito.only()).update(null);
		Mockito.verify(manyWaysModel, Mockito.only()).update(null);
		Mockito.verify(mainModel, Mockito.only()).update(null);
	}

	@Test
	public void deleteTest() {
		modelProxy.delete(null);
		
		Mockito.verify(simpleModel, Mockito.only()).delete(null);
		Mockito.verify(oneWaysModel, Mockito.never()).delete(null);
		Mockito.verify(manyWaysModel, Mockito.never()).delete(null);
		Mockito.verify(mainModel, Mockito.only()).delete(null);
	}

}
