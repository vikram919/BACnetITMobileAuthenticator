package uni.rostock.de.bacnetitmobileauthenticator.transportBinding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

import ch.fhnw.bacnetit.ase.application.service.api.TransportBindingService;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.TPDU;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataRequest;
import ch.fhnw.bacnetit.ase.transportbinding.service.api.ASEService;

public class TransportCoapBinding implements ASEService {

    private TransportBindingService transportBindingService;

    private CoapClient client;
    private CoapServer server;
    private String resource = "/transport";

    public void init() {
	this.server.start();
    }

    public void destroyCoapServer() {
	this.server.stop();
    }

    public void destroyCoapClient() {
	this.client.shutdown();
    }

    @Override
    public void doCancel(BACnetEID destination, BACnetEID source) {

    }

    @Override
    public void doRequest(T_UnitDataRequest t_unitDataRequest) {
	client.setURI(t_unitDataRequest.getDestinationAddress().toString() + resource);
	sendRequest(t_unitDataRequest.getData());
    }

    @Override
    public void setTransportBindingService(TransportBindingService transportBindingService) {
	this.transportBindingService = transportBindingService;
    }

    public void createCoapServer(int portNumber) {
	server = new CoapServer(portNumber);
	server.add(new CoapResource("transport") {
	    @Override
	    public void handlePOST(CoapExchange exchange) {
		byte[] msg = exchange.getRequestPayload();
		ByteArrayInputStream bis = new ByteArrayInputStream(msg);
		ObjectInput in = null;
		try {
		    in = new ObjectInputStream(bis);
		    TPDU tpdu = (TPDU) in.readObject();
		    transportBindingService.onIndication(tpdu,
			    new InetSocketAddress(exchange.getSourceAddress(), exchange.getSourcePort()));
		    exchange.respond(ResponseCode.CHANGED);

		} catch (IOException | ClassNotFoundException e) {
		    e.printStackTrace();
		} finally {
		    try {
			if (in != null) {
			    in.close();
			}
		    } catch (IOException ex) {
		    }
		}
	    }
	});
    }

    /**
     * This method can be used by ApplicationLayer to send a OOB hanshake messages
     * to an particular endpoint directly over "CoAP" without using Bacnet4j
     * methods.
     * 
     * @param uri
     * @param message
     */
    public void sendMessage(String uri, byte[] message) {
	client.setURI(uri + resource);
	client.post(message, 0);
    }

    public void sendRequest(TPDU payload) {
	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	ObjectOutput out = null;
	try {
	    out = new ObjectOutputStream(bos);
	    out.writeObject(payload);
	    out.flush();
	    byte[] payloadBytes = bos.toByteArray();
	    client.post(payloadBytes, 0);
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    try {
		bos.close();
	    } catch (IOException ex) {
	    }
	}
    }

    public void createCoapClient() {
	this.client = new CoapClient();
    }
}
