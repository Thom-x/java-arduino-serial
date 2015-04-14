package application.serial;

/**
 * Created by Thomas on 12/04/2015.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.Enumeration;

@Component
public class Serial implements SerialPortEventListener {
    SerialPort serialPort;
    /** The port we're normally going to use. */
    private static final String PORT_NAMES[] = {
            "/dev/tty.usbserial-A9007UX1", // Mac OS X
            "/dev/ttyACM0", // Raspberry Pi
            "/dev/ttyUSB0", // Linux
            "COM3", // Windows
    };
    /**
     * A BufferedReader which will be fed by a InputStreamReader
     * converting the bytes into characters
     * making the displayed results codepage independent
     */
    private BufferedReader input;
    private String response = null;
    private volatile boolean responseAvailable = false;
    private final Object responseSync = new Object();
    /** The output stream to the port */
    private OutputStream output;
    /** Milliseconds to block while waiting for port open */
    private static final int TIME_OUT = 2000;
    /** Default bits per second for COM port. */
    private static final int DATA_RATE = 115200;

    public void initialize() {
        // the next line is for Raspberry Pi and
        // gets us into the while loop and was suggested here was suggested http://www.raspberrypi.org/phpBB3/viewtopic.php?f=81&t=32186
        //System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/ttyACM0");

        CommPortIdentifier portId = null;
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

        //First, Find an instance of serial port as set in PORT_NAMES.
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
            for (String portName : PORT_NAMES) {
                if (currPortId.getName().equals(portName)) {
                    portId = currPortId;
                    break;
                }
            }
        }
        if (portId == null) {
            System.out.println("Could not find COM port.");
            return;
        }

        try {
            // open serial port, and use class name for the appName.
            serialPort = (SerialPort) portId.open(this.getClass().getName(),TIME_OUT);

            // set port parameters
            serialPort.setSerialPortParams(DATA_RATE,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            // open the streams
            input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
            output = serialPort.getOutputStream();

            // add event listeners
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    /**
     *  Connect to the specified port with the given baud rate
     */
    public ObjectNode connect(String portName, int baudrate) {
        if(serialPort != null) {
            JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
            ObjectNode node = nodeFactory.objectNode();
            node.put("code", "error");
            node.put("message", "Already connected to a port");
            return node;
        }

        // the next line is for Raspberry Pi and
        // gets us into the while loop and was suggested here was suggested http://www.raspberrypi.org/phpBB3/viewtopic.php?f=81&t=32186
        //System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/ttyACM0");

        CommPortIdentifier portId = null;
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

        //First, Find an instance of serial port as set in PORT_NAMES.
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
                if (currPortId.getName().equals(portName)) {
                    portId = currPortId;
                    break;
                }
        }
        if (portId == null) {
            JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
            ObjectNode node = nodeFactory.objectNode();
            node.put("code","error");
            node.put("message","Could not find COM port.");
            return node;
        }

        try {
            // open serial port, and use class name for the appName.
            serialPort = (SerialPort) portId.open(this.getClass().getName(),TIME_OUT);

            // set port parameters
            serialPort.setSerialPortParams(baudrate,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            // open the streams
            input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
            output = serialPort.getOutputStream();

            // add event listeners
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
            JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
            ObjectNode node = nodeFactory.objectNode();
            node.put("code", "ok");
            node.put("message", "Connected to COM port.");
            return node;
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode node = nodeFactory.objectNode();
        node.put("code","error");
        node.put("message","Could not connect to COM port.");
        return node;
    }

    /**
     *  List all available port
     */
    public ObjectNode list()
    {
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode node = nodeFactory.objectNode();
        node.put("code","ok");
        ArrayNode ports = node.putArray("ports");
        CommPortIdentifier portId = null;
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
        //First, Find an instance of serial port as set in PORT_NAMES.
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
            ports.add(currPortId.getName());
        }
        return node;
    }

    /**
     *  Write data to the serial port
     */
    public Object writeln(String data) {
        if(serialPort == null)
        {
            JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
            ObjectNode node = nodeFactory.objectNode();
            node.put("code", "error");
            node.put("message", "Not connected.");
            return node;
        }
        synchronized (responseSync) {
            if (output != null) {
                try {
                    output.write(data.getBytes(Charset.forName("UTF-8")));
                    responseSync.wait();
                    if (response != null)
                    {
                        return response;
                    }
                    else
                    {
                        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
                        ObjectNode node = nodeFactory.objectNode();
                        node.put("code", "error");
                        node.put("message", "Fail to communicate with arduino.");
                        return node;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
                    ObjectNode node = nodeFactory.objectNode();
                    node.put("code", "error");
                    node.put("message", "Fail to communicate with arduino.");
                    return node;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
                    ObjectNode node = nodeFactory.objectNode();
                    node.put("code", "error");
                    node.put("message", "Fail to communicate with arduino.");
                    return node;
                }
            }
            JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
            ObjectNode node = nodeFactory.objectNode();
            node.put("code", "error");
            node.put("message", "Fail to communicate with arduino.");
            return node;        }
    }

    /**
     * This should be called when you stop using the port.
     * This will prevent port locking on platforms like Linux.
     */
    public ObjectNode disconnect() {
        if (serialPort != null) {
            serialPort.removeEventListener();
            serialPort.close();
            serialPort = null;
            JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
            ObjectNode node = nodeFactory.objectNode();
            node.put("code", "ok");
            node.put("message", "Disconnected.");
            return node;
        }
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode node = nodeFactory.objectNode();
        node.put("code", "error");
        node.put("message", "Not connected.");
        return node;
    }

    /**
     * This return the connection status
     */
    public ObjectNode getStatus() {
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode node = nodeFactory.objectNode();
        if (serialPort != null) {

            node.put("CONNECTED","true");
            node.put("DISCONNECTED","false");
        }
        else
        {
            node.put("CONNECTED","false");
            node.put("DISCONNECTED","true");
        }
        return node;
    }


    /**
     * Handle an event on the serial port. Read the data and print it.
     */
    public void serialEvent(SerialPortEvent oEvent) {
        synchronized (responseSync) {
            if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                try {
                    String inputLine = null;
                    inputLine = input.readLine();
                    response = inputLine;
                    responseAvailable = true;
                    responseSync.notifyAll();
                } catch (IOException e) {
                    System.err.println(e.toString());
                    response = null;
                    responseAvailable = true;
                    responseSync.notifyAll();
                }
            } else {
                response = null;
                responseAvailable = true;
                responseSync.notifyAll();
            }
            // Ignore all the other eventTypes, but you should consider the other ones.
        }
    }
}
