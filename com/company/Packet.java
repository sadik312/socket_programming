package company;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class Packet {

    private final Header header;
    private final byte[] data;


    //For outgoing packages
    public Packet(Header header){
        this.header = header;
        this.data = null;
    }

    public Packet(Header header, byte[] data){
        this.header = header;
        this.data = data;
    }

    //for incoming using dataInputStream
    public Packet(byte[] incoming, DataInputStream in) throws IOException {
        this.header = new Header(incoming);
        if(header.getLength() != 0){
            this.data = new byte[header.getLength()];
            in.read(data);
        }else{
            this.data = null;
        }
    }
    //header data(payload)| (header+payload)
    //out going packages
    public byte[] bytePackage() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(header.buildHeader());
        if(this.data != null){
            out.write(this.data);
        }
        return out.toByteArray();
    }

    public Header getHeader(){ return this.header;}
    public byte[] getData() { return data;}
}

class Header{
    private final byte opcode;
    private final short length;
    public final int headerLength = 3;

    //Constructor for making a header
    public Header(byte opcode, short length){
        this.opcode = opcode;
        this.length = length;
    }

    public Header(byte opcode){
        this.opcode = opcode;
        this.length = 0;
    }

    //constructor for parsing a header
    public Header(byte[] header){
        this.opcode = header[0];
        this.length = (short)((((short)header[1])<<8) | header[2]);
    }

    public byte getOpcode() { return opcode; }
    public short getLength() { return length; }

    //building a header
    public byte[] buildHeader(){
        byte[] headerr = new byte[3];
        headerr[0] = opcode;
        headerr[1] = (byte)((length>>8) & 0xff);
        headerr[2] = (byte)(length&0xff);

        return headerr;
    }
}