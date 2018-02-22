package rassus.dz2;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class Datagram implements Serializable {

    private final String nodeName;
    private final String hostName;
    private final Integer PORT;
    private final String responseHostName;
    private final Integer responsePORT;

    private final boolean isConfirmation;
    private final int hashCode;
    private final Integer measurement;
    private AtomicIntegerArray vectorStamp;
    private Long scalarStamp;

    public Datagram(int measurement, String hostName, int PORT, String responseHostName, int responsePORT, AtomicIntegerArray vectorStamp, long scalarStamp, String nodeName) {
        this.isConfirmation = false;
        this.measurement = measurement;
        this.hashCode = this.hashCode();
        this.nodeName = nodeName;
        this.hostName = hostName;
        this.PORT = PORT;
        this.responseHostName = responseHostName;
        this.responsePORT = responsePORT;
        this.vectorStamp = vectorStamp;
        this.scalarStamp = scalarStamp;

    }

    public Datagram(boolean isConfirmation, int hashCode) {
        this.isConfirmation = isConfirmation;
        this.measurement = null;
        this.hashCode = hashCode;
        this.hostName = null;
        this.PORT = null;
        this.vectorStamp = null;
        this.scalarStamp = null;
        this.responseHostName = null;
        this.responsePORT = null;
        this.nodeName = null;
    }

    public int getMeasurement() {
        return measurement;
    }

    public boolean isConfirmation() {
        return isConfirmation;
    }

    public int getHashCode() {
        return hashCode;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPORT() {
        return PORT;
    }

    public AtomicIntegerArray getVectorStamp() {
        return vectorStamp;
    }

    public long getScalarStamp() {
        return scalarStamp;
    }

    public String getResponseHostName() {
        return responseHostName;
    }

    public int getResponsePORT() {
        return responsePORT;
    }

    public String getNodeName() {
        return nodeName;
    }

    /*
    @Override
    public int hashCode(){
        return Objects.hash(this.hostName, this.PORT, this.measurement, this.vectorStamp, this.scalarStamp, this.isConfirmation, this.responseHostName, this.responsePORT);
    }
    */
}
