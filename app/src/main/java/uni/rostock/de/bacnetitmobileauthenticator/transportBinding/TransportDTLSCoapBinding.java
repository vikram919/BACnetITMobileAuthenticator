package uni.rostock.de.bacnetitmobileauthenticator.transportBinding;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;

import ch.fhnw.bacnetit.ase.application.service.api.TransportBindingService;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.TPDU;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataRequest;
import ch.fhnw.bacnetit.ase.transportbinding.service.api.ASEService;

public class TransportDTLSCoapBinding implements ASEService {

    private static String resource = "/transport";
    public static final String CLIENT_NAME = "client";
    public static final String SERVER_NAME = "server";

    private TransportBindingService transportBindingService;

    private CoapClient client;
    private CoapServer server;

    public void init() {
        this.server.start();
    }

    public void destroyCoapServer() {
        this.server.destroy();
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

    public void createSecureCoapServer(int portNumber) {
        server = new CoapServer();
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
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder();
        dtlsConfig.setAddress(new InetSocketAddress(portNumber));
        ConfigureDTLS.loadCredentials(dtlsConfig, SERVER_NAME);
        DTLSConnector connector = new DTLSConnector(dtlsConfig.build());
        CoapEndpoint.CoapEndpointBuilder builder = new CoapEndpoint.CoapEndpointBuilder();
        builder.setConnector(connector);
        server.addEndpoint(builder.build());
        server.start();
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

    public void createSecureCoapClient() {
        this.client = new CoapClient();
        CoapEndpoint.CoapEndpointBuilder endpointBuilder = new CoapEndpoint.CoapEndpointBuilder();
        DtlsConnectorConfig.Builder dtlsBuilder = new DtlsConnectorConfig.Builder();
        dtlsBuilder.setClientOnly();
        ConfigureDTLS.loadCredentials(dtlsBuilder, CLIENT_NAME);
        DTLSConnector dtlsConnector = new DTLSConnector(dtlsBuilder.build());
        endpointBuilder.setConnector(dtlsConnector);
        client.setEndpoint(endpointBuilder.build());
    }
}
