package com.sorbonne.atom_d.entities.custom_queries

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query

@Dao
interface CustomQueriesDao {
//
//    @Query(
//        "SELECT " +
//                "  message_table.experiment_name," +
//                "  message_table.message_size," +
//                "  message_table.message_attempts," +
//                "  messageDataExperimentLogs_table.echo_id," +
//                "  messageDataExperimentLogs_table.echo_request_by," +
//                "  messageDataExperimentLogs_table.echo_reply_by," +
//                "  messageDataExperimentLogs_table.echo_seq_number," +
//                "  messageDataExperimentLogs_table.received_at," +
//                "  messageDataExperimentLogs_table.receiver_latitude," +
//                "  messageDataExperimentLogs_table.receiver_longitude," +
//                "  messageDataExperimentLogs_table.transmitter_latitude, " +
//                "  messageDataExperimentLogs_table.transmitter_longitude," +
//                "  messageDataExperimentLogs_table.log_timer " +
//                "FROM message_table, messageDataExperimentLogs_table " +
//                "WHERE message_table.experiment_name = messageDataExperimentLogs_table.experiment_name"
//    )
//    fun getChunksToExport(): List<ChunksToExport?>?
//
//    class ChunksToExport() {
//        var experiment_name: String? = null
//        var message_size: String? = null
//        var message_attempts: String? = null
//        var echo_id: String? = null
//        var echo_request_by: String? = null
//        var echo_reply_by: String? = null
//        var echo_seq_number: String? = null
//        var received_at: String? = null
//        var receiver_latitude: String? = null
//        var receiver_longitude: String? = null
//        var transmitter_latitude: String? = null
//        var transmitter_longitude: String? = null
//        var log_timer: String? = null
//    }
//
//    @Query(
//        ("SELECT " +
//                "   files_table.experiment_name," +
//                "   files_table.file_size," +
//                "   fileExperimentLogs_table.sent_from," +
//                "   fileExperimentLogs_table.received_by," +
//                "   fileExperimentLogs_table.transmitted_at," +
//                "   fileExperimentLogs_table.received_at," +
//                "   fileExperimentLogs_table.wifi_frequency," +
//                "   fileExperimentLogs_table.wifi_speed," +
//                "   fileExperimentLogs_table.wifi_rssi," +
//                "   fileExperimentLogs_table.receiver_latitude," +
//                "   fileExperimentLogs_table.receiver_longitude," +
//                "   fileExperimentLogs_table.transmitter_latitude," +
//                "   fileExperimentLogs_table.transmitter_longitude," +
//                "   fileExperimentLogs_table.log_timer," +
//                "   chunksFromFileLogs_table.chunk_size," +
//                "   chunksFromFileLogs_table.payload_id," +
//                "   chunksFromFileLogs_table.transferred_time " +
//                "FROM files_table, fileExperimentLogs_table, chunksFromFileLogs_table " +
//                "WHERE " +
//                "   files_table.experiment_name = fileExperimentLogs_table.experiment_name AND " +
//                "   fileExperimentLogs_table.id = chunksFromFileLogs_table.experiment_id " +
//                "LIMIT :from, :until")
//    )
//    fun getFilesToExport(from: Long, until: Long): List<FilesLogsToExport?>?


    class FilesLogsToExport() {
        var experiment_name: String? = null
        var file_size: String? = null
        var sent_from: String? = null
        var received_by: String? = null
        var transmitted_at: String? = null
        var received_at: String? = null
        var wifi_frequency: String? = null
        var wifi_speed: String? = null
        var wifi_rssi: String? = null
        var receiver_latitude: String? = null
        var receiver_longitude: String? = null
        var transmitter_latitude: String? = null
        var transmitter_longitude: String? = null
        var log_timer: String? = null
        var chunk_size: String? = null
        var payload_id: String? = null
        var transferred_time: String? = null
    }


    @Query(
        ("SELECT " +
                "   experiment_name, " +
                "   \"CHUNK\" AS type," +
                "   message_attempts AS attempts, " +
                "   message_size as size " +
                "FROM chunk_experiments " +
                "   UNION " +
//                "SELECT " +
//                "   experiment_name, " +
//                "   \"FILE\"  AS type," +
//                "   number_of_tries AS attempts, " +
//                "   file_size as size " +
//                "FROM files_table" +
//                "   UNION " +
                "SELECT " +
                "   experiment_name," +
                "   \"DISCOVERY\" AS type, " +
                "   number_of_repetitions AS attempts," +
                "   0 AS size " +
                "FROM connection_attempts")
    )
    fun getAllExperimentsName(): LiveData<List<AllExperimentsName>>

    data class AllExperimentsName(
        var experiment_name: String,
        var type: String,
        var attempts: Int,
        var size: Long
    )
}