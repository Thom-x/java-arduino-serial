package application.command;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Created by Thomas on 13/04/2015.
 */
public class Connect {
    @NotNull
    String portName;
    @NotNull
    @Min(0)
    @Max(115200)
    int baudRate;

    public String getPortName() {
        return portName;
    }

    public void setPortName(String port) {
        this.portName = port;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    @Override
    public String toString() {
        return "Connect{" +
                "port='" + portName + '\'' +
                ", baudRate=" + baudRate +
                '}';
    }
}
