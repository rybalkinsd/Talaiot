package com.cdsap.talaiot.publisher

import com.cdsap.talaiot.configuration.InfluxDbPublisherConfiguration
import com.cdsap.talaiot.configuration.ThresholdConfiguration
import com.cdsap.talaiot.entities.TaskLength
import com.cdsap.talaiot.entities.TaskMeasurementAggregated
import com.cdsap.talaiot.logger.LogTracker
import com.cdsap.talaiot.request.Request
import java.util.concurrent.Executor


class InfluxDbPublisher(
    private val influxDbPublisherConfiguration: InfluxDbPublisherConfiguration,
    private val logTracker: LogTracker,
    private val requestPublisher: Request,
    private val executor: Executor
) : Publisher {

    override fun publish(taskMeasurementAggregated: TaskMeasurementAggregated) {
        logTracker.log("================")
        logTracker.log("InfluxDbPublisher")
        logTracker.log("================")
        if (influxDbPublisherConfiguration.url.isEmpty() ||
            influxDbPublisherConfiguration.dbName.isEmpty() ||
            influxDbPublisherConfiguration.urlMetric.isEmpty()
        ) {
            println(
                "InfluxDbPublisher not executed. Configuration requires url, dbName and urlMetrics: \n" +
                        "influxDbPublisher {\n" +
                        "            dbName = \"tracking\"\n" +
                        "            url = \"http://localhost:8086\"\n" +
                        "            urlMetric = \"tracking\"\n" +
                        "}\n" +
                        "Please update your configuration"
            )

        } else {
            val url = "${influxDbPublisherConfiguration.url}/write?db=${influxDbPublisherConfiguration.dbName}"
            var content = ""
            val thresholdConfiguration = influxDbPublisherConfiguration.threshold

            taskMeasurementAggregated.apply {
                var metrics = ""
                values.forEach {
                    val tag = formatToLineProtocol(it.key)
                    val tagValue = formatToLineProtocol(it.value)
                    metrics += "$tag=$tagValue,"
                }
                taskMeasurement
                    .filter { threshold(thresholdConfiguration, it) }
                    .forEach {
                        content += "${influxDbPublisherConfiguration.urlMetric},state=${it.state}" +
                                ",module=${it.module},rootNode=${it.rootNode},task=${it.taskPath},${metrics.dropLast(1)} value=${it.ms}\n"
                    }
                logTracker.log(content)
            }

            if (!content.isEmpty()) {
                executor.execute {
                    requestPublisher.send(url, content)
                }
            } else {
                logTracker.log("Empty content")
            }
        }
    }

    private fun threshold(thresholdConfiguration: ThresholdConfiguration?, task: TaskLength) =
        if (thresholdConfiguration == null) {
            true
        } else {
            task.ms in thresholdConfiguration.minExecutionTime..thresholdConfiguration.maxExecutionTime
        }

    private fun formatToLineProtocol(tag: String) = tag.replace(Regex("""[ ,=,\,]"""), "")

}
