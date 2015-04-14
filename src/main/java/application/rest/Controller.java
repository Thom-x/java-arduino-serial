package application.rest;

import application.command.Connect;
import application.serial.Serial;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.*;

/**
 * Created by Thomas on 12/04/2015.
 */

@RestController
@RequestMapping(value="/", headers="Accept=application/json")
public class Controller {

    @Autowired
    Serial serial;

    @RequestMapping(value="data")
    public Object data(@RequestBody String s) {
        return serial.writeln(s + "\n\0");
    }

    @RequestMapping(value="list")
    public ObjectNode list() {
        return serial.list();
    }

    @RequestMapping(value="connect")
    public ObjectNode connect(@RequestBody @Valid Connect c) {
        return serial.connect(c.getPortName(), c.getBaudRate());
    }

    @RequestMapping(value="disconnect")
    public ObjectNode disconnect() {
        return serial.disconnect();
    }

    @RequestMapping(value="getStatus")
    public ObjectNode getStatus() {
        return serial.getStatus();
    }


}
