package rassus.dz2;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class MeasurementStorage {

    private final ArrayList<Datagram> measurements;

    public MeasurementStorage() {
        this.measurements = new ArrayList<>();
    }

    public synchronized void storeMeasurement(Datagram d) {
        measurements.add(d);
    }

    public synchronized boolean checkIfStored(int hashCode) {
        for(Datagram d : this.measurements) {
            if(d.getHashCode() == hashCode) return true;
        }
        return false;
    }

    public synchronized String getReport() {
        StringBuffer sb = new StringBuffer();
        sb.append("\nReport for last 5 seconds:\n--------------------------\nMean value - ");
        sb.append(meanValue());
        sb.append("\nMeasurements sorted by SCALAR stamp\nNODE NAME - VALUE - SCALAR STAMP - VECTOR STAMP\n");
        sb.append(sortByScalar());
        sb.append("\nMeasurements sorted by VECTOR stamp\nNODE NAME - VALUE - SCALAR STAMP - VECTOR STAMP\n");
        sb.append(sortByVector());
        sb.append("--------------------------------------\n\n");
        clearStorage();
        return sb.toString();
    }

    private boolean vector1Tovector2(AtomicIntegerArray vector1, AtomicIntegerArray vector2) {
        int greaterThan = 0;
        int lessThan = 0;
        for(int i = 0; i < vector1.length(); i++) {
            if(vector1.get(i) < vector2.get(i)) lessThan++;
            else if(vector1.get(i) > vector2.get(i)) greaterThan++;
        }
        if(lessThan > 0 && greaterThan == 0) return true;
        else return false;
    }

    private synchronized String sortByVector() {
        ArrayList<Datagram> sortedList = new ArrayList<>();
        sortedList.add(measurements.get(0));
        boolean inserted = false;
        for(Datagram measurement : measurements) {
            for(int i = 0; i < sortedList.size(); i++) {
                if(vector1Tovector2(measurement.getVectorStamp(), sortedList.get(i).getVectorStamp())) {
                    sortedList.add(i, measurement);
                    inserted = true;
                    break;
                }
            }
            if(!inserted) sortedList.add(measurement);
            else inserted = false;
        }
        return measurementsToString(sortedList);
    }

    private synchronized String sortByScalar() {
        ArrayList<Datagram> sortedList = new ArrayList<>();
        sortedList.add(measurements.get(0));
        boolean inserted = false;
        for(Datagram measurement : measurements) {
            for(int i = 0; i < sortedList.size(); i++) {
                if(measurement.getScalarStamp() < sortedList.get(i).getScalarStamp()) {
                    sortedList.add(i, measurement);
                    inserted = true;
                    break;
                }
            }
            if(!inserted) sortedList.add(measurement);
            else inserted = false;
        }
        return measurementsToString(sortedList);
    }

    private String measurementsToString(ArrayList<Datagram> datagrams) {
        StringBuffer sb = new StringBuffer();
        for(Datagram d : datagrams) {
            sb.append(d.getNodeName()+" - "+d.getMeasurement()+" - "+d.getScalarStamp()+" - "+d.getVectorStamp()+"\n");
        }
        return sb.toString();
    }

    private float meanValue() {
        float sum = 0;
        for(Datagram measurement : this.measurements) {
            sum += (float) measurement.getMeasurement();
        }
        return sum / (float) this.measurements.size();
    }

    private void clearStorage() {
        measurements.clear();
    }
}
