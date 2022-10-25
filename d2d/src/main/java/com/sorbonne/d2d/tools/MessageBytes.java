package com.sorbonne.d2d.tools;

import java.util.Arrays;

public class MessageBytes {

    public static byte ECHO_REQUEST = (byte) 0x80;
    public static byte INFO_PACKET = (byte) 0x81;
    public static byte INFO_PACKET_ACK = (byte) 0x82;
    public static byte METRIC = (byte) 0x83;

    /*Type: 128 -> echo request
    *       129 -> echo reply
    *
    * length: size of the total payload where the message is hosted
    *
    * Identifier: unique identifier for every ping process (16 bits)
    *
    * SequenceNumber: incremental sequence number per packet sent.
    * */

    private byte type;
    private byte tag;
    private int length;
    private int identifier;
    private int sequenceNumber;
    private  byte [] payload;
    private byte [] buffer;

    private static final String TAG = MessageBytes.class.getSimpleName();

    public MessageBytes(){
        setAllParametersToZero();
    }

    public MessageBytes(byte[] newBuffer){
        getPacketValues(newBuffer);
    }

    private void setAllParametersToZero(){
        this.type = 0;
        this.tag = 0;
        this.length = 0;
        this.identifier = 0;
        this.sequenceNumber = 0;
        this.payload = new byte[0];
    }

    private void setGeneralMessage(byte type, byte tag, byte[] payloadData){
        this.type = type;
        this.tag = tag;
        this.length = 10;
        if(payloadData != null){
            this.payload = payloadData;
            this.length = this.length + payloadData.length;
        }
    }

    private void getPacketValues(byte[] packet_buffer){
        setAllParametersToZero();
        this.type       = packet_buffer[0];
        this.tag       = packet_buffer[1];
        this.length     |= ((packet_buffer[2] & 0xff) << 8);
        this.length     |= ((packet_buffer[3] & 0xff));
        this.identifier |= ((packet_buffer[4] & 0xff) << 8);
        this.identifier |= ((packet_buffer[5] & 0xff));
        this.sequenceNumber |= ((packet_buffer[6] & 0xff) << 24);
        this.sequenceNumber |= ((packet_buffer[7] & 0xff) << 16);
        this.sequenceNumber |= ((packet_buffer[8] & 0xff) << 8);
        this.sequenceNumber |= ((packet_buffer[9] & 0xff));
        if(packet_buffer.length > 10) {
            this.payload = Arrays.copyOfRange(packet_buffer, 10, this.length);
        }
    }

    /*
    * Function that computes the packet to be sent
     * */

    public void buildRegularPacket(byte chunkType, byte chunkTag, byte[] payloadData){

        setGeneralMessage(chunkType, chunkTag, payloadData);

        this.buffer = new byte[this.length];

        /* Buffer values are set */
        buffer[0] = this.type;
        buffer[1] = this.tag;

        buffer[2] = (byte) ((this.length >> 8) & 0xff);
        buffer[3] = (byte) (this.length & 0xff);

        buffer[4] = (byte) ((this.identifier >> 8) & 0xff);
        buffer[5] = (byte) (this.identifier & 0xff);

        buffer[6] = (byte) ((this.sequenceNumber >> 24) & 0xff);
        buffer[7] = (byte) ((this.sequenceNumber >> 16) & 0xff);
        buffer[8] = (byte) ((this.sequenceNumber >> 8) & 0xff);
        buffer[9] = (byte) (this.sequenceNumber & 0xff);
        if(this.length - 10 > 0){
            System.arraycopy(this.payload, 0, buffer, 10, this.length - 10);
        }
    }

    public void updateEchoCounter(int counter){
        buffer[5] = (byte) ((counter >> 24) & 0xff);
        buffer[6] = (byte) ((counter >> 16) & 0xff);
        buffer[7] = (byte) ((counter >> 8) & 0xff);
        buffer[8] = (byte) (counter & 0xff);
    }

    public void buildEchoPacket(int ChunkSize, int packetId,int SetSequenceNumber){
        buffer = new byte[ChunkSize];

        buffer[0] = ECHO_REQUEST;
        buffer[1] = (byte) ((ChunkSize >> 8) & 0xff);
        buffer[2] = (byte) (ChunkSize & 0xff);
        buffer[3] = (byte) ((packetId >> 8) & 0xff);
        buffer[4] = (byte) (packetId & 0xff);
        buffer[5] = (byte) ((SetSequenceNumber >> 24) & 0xff);
        buffer[6] = (byte) ((SetSequenceNumber >> 16) & 0xff);
        buffer[7] = (byte) ((SetSequenceNumber >> 8) & 0xff);
        buffer[8] = (byte) (SetSequenceNumber & 0xff);
    }

    public byte getType(){
        return this.type;
    }

    public byte getTag(){
        return this.tag;
    }

    public int getLength(){
        return this.length;
    }

    public int getIdentifier(){
        return this.identifier;
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
    }

    public byte[] getPayload(){
        return  this.payload;
    }

    public byte[] getBuffer(){
        return  this.buffer;
    }
}
