/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fer.rassus.dz1.sw;

import java.io.Serializable;

/**
 *
 * @author nameless
 */
public class Measurement implements Serializable {
    private final int temperature;
    private final int pressure;
    private final int humidity;
    private final int CO;
    private final int NO2;
    private final int SO2;

    public Measurement(int temperature, int pressure, int humidity, int CO, int NO2, int SO2) {
        this.temperature = temperature;
        this.pressure = pressure;
        this.humidity = humidity;
        this.CO = CO;
        this.NO2 = NO2;
        this.SO2 = SO2;
    }

    public int getTemperature() {
        return temperature;
    }

    public int getPressure() {
        return pressure;
    }

    public int getHumidity() {
        return humidity;
    }

    public int getCO() {
        return CO;
    }

    public int getNO2() {
        return NO2;
    }

    public int getSO2() {
        return SO2;
    }
 
    @Override
    public String toString() {
        String s = "Temperature\tPressure\tHumidity\tCO\tNO2\tSO2";
        String s1 = Integer.toString(temperature)+"\t\t"+Integer.toString(pressure)+"\t\t"+Integer.toString(humidity)+"\t\t"+Integer.toString(CO)+"\t"+Integer.toString(NO2)+"\t"+Integer.toString(SO2);
        return s + "\n" + s1;
    }
}
