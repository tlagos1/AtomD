# ![AtomD Icon](app/src/main/res/mipmap-hdpi/ic_atom_launcher.png) AtomD

## Overview

AtomD is an application designed to perform D2D measurements in real conditions by using the  [Google Nearby Connections](https://developers.google.com/nearby/connections/overview) API.


This tool allows you to do the following:
- Create and manipulate different experimental environments.
- Track the GSP location (latitude and longitude) for each generated action.
- Obtain the average throughput of the link through file transmission.
- Analyze the delay during the neighbor discovery process.

## Application support

This application is designed for Sdk 28 to Sdk 33 .
Additionally, it uses the following dependencies:

- location 
- nearby 
- room

## Quickstart


The Google Nearby Connection API requires at least Bluetooth, Wifi, and Location access [permissions](https://developers.google.com/nearby/connections/android/get-started).

Since the location and write, permissions are considered dangerous for the system, the user has to grant them.

<img src= "README_IMG/location.png" width="300"/> <img src= "README_IMG/write.png" width="300"/>

Once the permissions are given, AtomD will provide to the user a text box in order to set a readable name to the endPoint. From here, the user can get a random name or a user-defined name.

<img src= "README_IMG/endpoint_name.png" width="300"/>

Please note that after a name was submitted, this one cannot be changed until the application data is erased.01

Once a name has been selected, AtomD enables Google Nearby Connection to discover/advertise, thus giving us access to three different states.

- Idle: State where the endPoint is not running Google Nearby Connection. Here the discovery, advertise, and transfer functions are inactive. If any D2D instance is active in this state, it will be killed by AtomD.

- Discovery: State where the endPoint enables Google Nearby Connection to discover and advertise towards other endPoints of the same D2D instance. It should be noted that for each favorable advertising instance, the endPoint will be able to discover other endPoints.

- Connected: State where the endPoint has at least one successful connection with other endPoints. From here, AtomD enables the execution of the experiments defined in the "experiments" menu option.

<img src= "README_IMG/d2d_states.png" width="300"/>

As soon as the endPoint is in the discovery state, the user can click on the "players" button, which will navigate to another fragment that lists the endPoints that are in the same state. In addition, here the user can select the endPoints to interact with.

<img src= "README_IMG/players.png" width="300"/>

In case a connection to another endPoint has been established, AtomD will enable its experiment management. By default, AtomD already comes with an RTT experiment that consists of the timing measurement of a broadcast [message](https://developers.google.com/nearby/connections/android/exchange-data) that travels from the current endPoint to the other endPoints connected to it and vice versa. Other experiments can be created in the "experiments" menu tab.

<img src= "README_IMG/experiments.png" width="300"/>

Finally, the information collected from the experiment can be viewed in the "notifications" tab, where the user can also export the raw logs in a CSV file.

<img src= "README_IMG/notifications.png" width="300"/>

##  Experiments

To perform data transmission, Google Nearby Connection splits the data input into Chunks with a current maximum [capacity](https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient?hl=es#MAX_BYTES_DATA_SIZE) of 2^15 bytes.

Based on this, AtomD supports two types of payload transmission. First, we have the Single message which is intended to carry precise information within their payload. Then we have Chunk sets, which belong to the subdivision of a payload that exceeds the maximum size of a data message.

Focusing on the individual message, a logical header of 7 bytes is set within the payload with the following parameters.

| 1 Byte | 2 Bytes |  2 Bytes | 2 Bytes  | ... |
|--------|---------|----------|----------|------------|
|  Type  | Length  |Indetifier| Seq. Num.| Payload Data|

- Type: This field identifies the type of individual data message being sent. The following hexadecimal can be used within this field:
	- 0x80: Echo request data message: This data message is used to initiate an exchange between two or more endPoints (broadcast).
	- 0x81: Echo reply data message.  This data message is used to reply to the endPoint who transmitted the received Echo request data message (unicast).
	- 0x82: File information data message: This data message is sent before a file is transmitted and contains the information corresponding to the file.
- Length: This field corresponds to the total size of the payload to be transmitted plus the header.
- Identifier: It corresponds to the echo instance that is being exchanged. This field will be set to 0 if a file information data message is being used.
- Sequence Number: Corresponds to the request counter of an echo request being exchanged. In case an echo reply data message is being used, this field will repeat the value of its corresponding echo Request. This field will be set to 0 if a file information data message is being used.
- Payload Data: Data to be sent.

### Echo experiments

A data message experiment consists of exchanging echo messages between two or more endPoints. To achieve this, AtomD initializes an echo instance in the sending endPoint.  Then, it is indexed using an ID, which is sent inside the individual data message header in the "Identifier" field.

Message experiments can be set with a limited or unlimited number of attempts. Furthermore, the user can set a Transmission Time Interval (TTI) between each echo Request. However, the payload size is defined by the maximum size allowed by the data message.

From the data retrieved from each data message experiment, the time instant at which a given Echo Request was sent and the time instant at which the Echo Reply corresponding to that Echo Request was received are saved. This data is stored in the table "messageDataExperimentLogs_table" of the database "D2D_database".

### File experiments

A File experiment consists of the transmission of a binary file with a user-defined size.  To achieve this, AtomD initiates the exchange by sending a File Information Message in order to prepare the receiving endPoint about the file that it is about to receive. Then, the chunks corresponding to the file are sent.

When a new file experiment is created, AtomD produces a binary file with the specifications submitted by the user. It should be noted that these files are created in the application-specific directory located on the device's primary external/shared storage.

From the data gathered, the load rate in Mbps and the RSSI in dBm are measured. Additionally, the time instant from the first data message sent to the last data message delivered is saved. This data is stored in the table "FileExperimentLogs_table" of the database "D2D_database".

## Database

Since AtomD's goal is to analyze the behavior of D2D in real environments, it uses an SQLite database named "D2D_database" to record each experiment log.

The tables corresponding to the database are listed below.

- **message_table**: Table that contains the dataMessage experiments.
	-  experiment_name: Name that identifies the experiment. This value is unique and can be generated automatically or defined by the user.
	-  message_size: Total data message payload size. By default, this field is set to 2^15. In future updates, this may vary.
	-  message_tti: User-defined transmission time interval between each Echo Request (see **dataMessage experiments subsection**).
	-  message_attempts: Experiment lifetime set by attempts. If this value is 0, the experiment will run indefinitely.
	-  message_payload: Exchanged payload. This default value is filled with 0. In future implementations, this may vary.

- **files_table**: Table that contains the file experiments.
	- experiment_name: Name that identifies the experiment. This value is unique and can be generated automatically or defined by the user.
	- file_size: User-defined file size in bytes.

- **messageExperimentLogs_table**: Table that contains the logs generated during the course of a data message experiment.
	- experiment_name: Name of the related experiment.
	- echo_id: Id corresponding to the echo instance.
	- echo_seq_number: Echo request counter.
	- echo_request_by: Name of the endPoint that sent the Echo request.
	- echo_reply_by: Name of the endPoint that sent the Echo reply.
	- latitude: gps coordinates.
	- longitude: gps coordinates.
	- transmitted_at: Time instant in which the Echo request was sent.
	- received_at: Time instant when the Echo reply was received.
	- log_timer: Input log timer.

- **FileExperimentLogs_table**: Table that contains the logs generated during the course of a file experiment.
	- experiment_name: Name of the related experiment.
	- sent_from: Name of the transmitter endPoint.
	- received_by: Name of the receiver endPoint.
	- latitude: gps coordinates.
	- longitude: gps coordinates.
	- transmitted_at: Time instant at which the file was started to be sent.
	- received_at: Time instant when the file was received/transmitted.
	- wifi_frequency: Wifi-direct channel.
	- wifi_speed: Average transmission speed in Mbps.
	- wifi_rssi: Average received signal strength indicator in dBm.
	- log_timer: Input log timer.

