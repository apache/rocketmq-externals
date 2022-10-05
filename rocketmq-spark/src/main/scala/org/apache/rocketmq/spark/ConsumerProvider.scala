package org.apache.rocketmq.spark

import org.apache.rocketmq.spark.streaming.{MQPullConsumerProvider, MQPullConsumerProviderFactory}

import java.security.InvalidParameterException
import java.util.ServiceLoader
import scala.collection.mutable

/**
 * @Description TODO
 * @Author zhaorongsheng
 * @Date 2022/10/5 22:35
 * @Version 1.0
 */
object ConsumerProvider extends Logging {
    private val pullConsumerProviderFactories = loadPullConsumerProviderFactories()

    private def loadPullConsumerProviderFactories(): Seq[MQPullConsumerProviderFactory] = {
        val loader = ServiceLoader.load(classOf[MQPullConsumerProviderFactory])
        val providerFactories = mutable.ArrayBuffer[MQPullConsumerProviderFactory]()
        val iterator = loader.iterator
        while (iterator.hasNext) {
            try {
                val providerFactory = iterator.next()
                providerFactories += providerFactory
                logDebug(s"Loaded consumer provider: ${providerFactory.getName}")
            } catch {
                case e: Throwable =>
                    logError("Failed to load MQPullConsumerProvider", e)
            }
        }
        providerFactories.toSeq
    }

    def getPullConsumerProviderByFactoryName(factoryName: String): MQPullConsumerProvider = {
        val providerFactory = pullConsumerProviderFactories.filter(_.getName.equals(factoryName))
        if (providerFactory.isEmpty || providerFactory.length > 1) {
            val providerFactoriesStr = providerFactory.map(_.getName).mkString(", ")
            logError(s"Failed to get MQPullConsumerProviderFactory because of " +
                s"ambiguous or no satisfied factories: ${providerFactoriesStr}")
            throw new InvalidParameterException(s"Ambiguous or no satisfied provider factories: " +
                s"${providerFactoriesStr}")
        }
        logDebug(s"Get satisfied MQPullConsumerProverFactory: ${factoryName}")
        providerFactory.head.build()
    }

}
