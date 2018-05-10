package uni.rostock.de.bacnetitmobileauthenticator.transportBinding;

import android.content.Context;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.interceptors.MessageTracer;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;

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

    private static final boolean PSK_MODE = true;
    private static final boolean CERTIFICATE_MODE = false;
    private static String resource = "/transport";
    public static final String PSK_IDENTITY = "password";
    public static final byte[] PSK_SECRET = "sesame".getBytes();
    private Context context;

    private TransportBindingService transportBindingService;

    private CoapClient client;
    private CoapServer server;

    public void init() {
        this.server.start();
    }

    public void destroyCoapServer() {
        this.server.stop();
    }

    public void destroyCoapClient() {
        this.client.shutdown();
    }

    public TransportDTLSCoapBinding(Context context) {
        this.context = context;
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
        DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder();
        config.setAddress(new InetSocketAddress(portNumber));
        if (PSK_MODE) {
            config.setPskStore(new StaticPskStore(PSK_IDENTITY, PSK_SECRET));
        } else {
            ConfigureDTLS.configureCredentials(config, ConfigureDTLS.SERVER_NAME, CERTIFICATE_MODE,context);
        }
        DTLSConnector connector = new DTLSConnector(config.build());
        CoapEndpoint.CoapEndpointBuilder builder = new CoapEndpoint.CoapEndpointBuilder();
        builder.setConnector(connector);
        server.addEndpoint(builder.build());
        server.start();

        // add special interceptor for message traces
        for (Endpoint ep : server.getEndpoints()) {
            ep.addInterceptor(new MessageTracer());
        }
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
        if (PSK_MODE) {
            dtlsBuilder.setPskStore(new StaticPskStore(PSK_IDENTITY, PSK_SECRET));
        } else {
            ConfigureDTLS.configureCredentials(dtlsBuilder, ConfigureDTLS.CLIENT_NAME, CERTIFICATE_MODE,context);
        }
        DTLSConnector dtlsConnector = new DTLSConnector(dtlsBuilder.build());
        endpointBuilder.setConnector(dtlsConnector);
        client.setEndpoint(endpointBuilder.build());
    }

}
