package com.sorbonne.atom_d.tools

import android.util.Log
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class ScoreComputation {

    private val TAG = ScoreComputation::class.simpleName
    private val batteryManagerMetrics = mutableMapOf<String, MutableList<Double>>()
    private val throughputMetrics = mutableMapOf<String, MutableList<MutableList<Long>>>()

    fun insertBatteryManagerMetric(deviceId: String, value: Double){
        if(!batteryManagerMetrics.containsKey(deviceId)){
            batteryManagerMetrics[deviceId] = mutableListOf(value)
        } else {
            batteryManagerMetrics[deviceId]?.add(value)
        }
    }

    fun insertThroughputMetric(deviceId: String, timer: Long, bytes: Long){
        if(!throughputMetrics.containsKey(deviceId)){
            throughputMetrics[deviceId] = mutableListOf(mutableListOf(timer, bytes))
        } else {
            throughputMetrics[deviceId]?.add(mutableListOf(timer, bytes))
        }
    }

    fun getComputedScore(deviceId: String): Double {
        var avgBatteryManagerMetric = 0.0
        getAverageBatteryManagerMetric(deviceId)?.let {
            avgBatteryManagerMetric = it
        }

        var avgThroughputMetric = 0.0
        getAvgThroughputMetric(deviceId)?.let {
            avgThroughputMetric = it
        }

        var avgBatteryManagerMetricAux:Double = avgBatteryManagerMetric
        var avgBatteryManagerMetricCount = 0
        while (avgBatteryManagerMetricAux.toInt() > 0) {
            avgBatteryManagerMetricAux /= 10
            avgBatteryManagerMetricCount += 1
        }

        var avgThroughputMetricAux = avgThroughputMetric
        var avgThroughputMetricCount = 0
        while(avgThroughputMetricAux.toInt() > 0) {
            avgThroughputMetricAux /= 10
            avgThroughputMetricCount += 1
        }


        val batteryManagerRank = (avgBatteryManagerMetric/ (
                if(avgBatteryManagerMetricCount > 0)
                    (10.0.pow(avgBatteryManagerMetricCount.toDouble()) / avgBatteryManagerMetricCount)
                else 1.0)) * 0.4

        val throughputRank = (avgThroughputMetric/ (
                if(avgThroughputMetricCount > 0)
                    (10.0.pow(avgThroughputMetricCount.toDouble())/avgThroughputMetricCount)
                else 1.0)) * 0.6

        val rank = (batteryManagerRank + throughputRank) * 100
        Log.i(TAG, "BatteryManagerMetric: $avgBatteryManagerMetric - ThroughputMetric: $avgThroughputMetric - Score: $rank")
        return rank
    }

    private fun getAverageBatteryManagerMetric(deviceId: String): Double? {
        val values = batteryManagerMetrics[deviceId]
        batteryManagerMetrics.remove(deviceId)
        values?.let {
            val avg = values.average()
            val std = calculateSD(values.toDoubleArray())
            return  zScore(avg, std, values.toDoubleArray(), 3).average()
        }
        return null
    }

    private fun getAvgThroughputMetric(deviceId: String): Double? {

        val valuesList = throughputMetrics.remove(deviceId)
        var timing: Double
        var bytes: Double
        val sample = mutableListOf<Double>()

        valuesList?.forEachIndexed{ index, values ->
            if(index > 0){
                timing = (values[0] - valuesList[index-1][0]).toDouble()
                bytes = (values[1] - valuesList[index-1][1]).toDouble()
                sample.add((bytes/timing)*8000)
            }
        }
        if(sample.isNotEmpty()){
            val avg = sample.average()
            val std = calculateSD(sample.toDoubleArray())
            return zScore(avg, std, sample.toDoubleArray(), 3).average()
        }
        return null
    }


    private fun calculateSD(numArray: DoubleArray): Double {
        var standardDeviation = 0.0
        val mean = numArray.average()
        for (num in numArray) {
            standardDeviation += (num - mean).pow(2.0)
        }
        return sqrt(standardDeviation / numArray.size)
    }

    private fun zScore(avg: Double, std: Double, samples: DoubleArray, zStd: Int): MutableList<Double> {
        var zScore:Double
        val result = mutableListOf<Double>()
        for (sample in samples){
            zScore = abs((sample - avg)/std)
//            Log.e(TAG, "avg: $avg - std: $std - sample: $sample- score: $zScore")
            if(zScore <= zStd){
                result.add(sample)
            }
        }
        return result
    }

}