package uni.rostock.de.bacnetitmobileauthenticator;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;

import ch.fhnw.bacnetit.ase.application.configuration.api.DiscoveryConfig;
import ch.fhnw.bacnetit.ase.application.service.api.ASEServices;
import ch.fhnw.bacnetit.ase.application.service.api.BACnetEntityListener;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelConfiguration;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelFactory;
import ch.fhnw.bacnetit.ase.application.transaction.api.ChannelListener;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.TPDU;
import ch.fhnw.bacnetit.ase.encoding.api.T_ReportIndication;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataIndication;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataRequest;
import ch.fhnw.bacnetit.ase.network.directory.api.DirectoryService;
import ch.fhnw.bacnetit.ase.transportbinding.service.api.ASEService;
import ch.fhnw.bacnetit.directorybinding.dnssd.api.DNSSD;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetObjectIdentifier;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetObjectType;
import ch.fhnw.bacnetit.samplesandtests.api.deviceobjects.BACnetPropertyIdentifier;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ASDU;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ConfirmedRequest;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.IncomingRequestParser;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.exception.BACnetException;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.SequenceOf;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.constructed.ServicesSupported;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive.CharacterString;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive.OctetString;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.type.primitive.UnsignedInteger;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.util.ByteQueue;
import ch.fhnw.bacnetit.samplesandtests.api.service.confirmed.AddListElementRequest;
import ch.fhnw.bacnetit.samplesandtests.api.service.confirmed.WritePropertyRequest;
import uni.rostock.de.bacnet.it.coap.messageType.ConfirmAddDeviceRequest;
import uni.rostock.de.bacnet.it.coap.messageType.OOBProtocol;
import uni.rostock.de.bacnet.it.coap.transportbinding.TransportDTLSCoapBinding;

public class BACnetIntentService extends IntentService {

    protected static final String TAG = BACnetIntentService.class.getName();
    private static final int AUTH_ID = 1;
    private static final int MOBILE_ID = 2;
    private static final int DTLS_SOCKET = 5684;
    private ASEServices aseServiceChannel;
    private static final String AUTH_IP = "139.30.202.56:";
    private String hostAddress;
    private static final String SECURE_SCHEME = "coaps://";
    TransportDTLSCoapBinding bindingConfiguration;
    private boolean signal = false;
    protected static final String SERVICE_ERROR_MESSAGE = "serviceErrorMessage";
    protected static final String SERVICE_ERROR_PAYLOAD = "serviceErrorPayload";
    protected static final String SERVICE_AUTH_SUCCESS = "serviceAuthSuccess";
    protected static final String SERVICE_AUTH_SUCCESS_PAYLOAD = "serviceAuthSuccessPayload";
    protected static final String SERVICE_AUTH_FAILURE = "serviceAuthError";
    protected static final String SERVICE_AUTH_FAILURE_PAYLOAD = "serviceAuthErrorPayload";


    public BACnetIntentService() {
        super(TAG);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        hostAddress();
        start();
        try {

            final DiscoveryConfig ds = new DiscoveryConfig("DNSSD", "1.1.1.1", "itb.bacnet.ch.",
                    "bds._sub._bacnet._udp.", "authen._sub._bacnet._udp.", "authenservice._sub._bacnet._udp.", false);

            DirectoryService.init();
            DirectoryService.getInstance().setDNSBinding(new DNSSD(ds));
            new RegisterMessageThread().execute("");
            Log.d("FlashAuthActivity", "Bacnet Register message sent to Authorizer!!");

//            new SendAddDeviceRequest().execute("");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }


    public void start() {
        aseServiceChannel = ChannelFactory.getInstance();
        ChannelConfiguration channelConfiguration = aseServiceChannel;
        channelConfiguration.setEntityListener(new BACnetEntityListener() {
            @Override
            public void onRemoteAdded(BACnetEID baCnetEID, URI uri) {
                DirectoryService.getInstance().register(baCnetEID, uri, false, true);
            }

            @Override
            public void onRemoteRemove(BACnetEID baCnetEID) {

            }

            @Override
            public void onLocalRequested(BACnetEID baCnetEID) {

            }
        });

        // configure transport binding to coap dtls
        bindingConfiguration = new TransportDTLSCoapBinding();
        bindingConfiguration.setCertificateMode();
        bindingConfiguration.createSecureCoapClient();
        bindingConfiguration.createSecureCoapServer(DTLS_SOCKET);
        bindingConfiguration.init();
        channelConfiguration.setASEService((ASEService) bindingConfiguration);
        channelConfiguration.registerChannelListener(new ChannelListener(new BACnetEID(MOBILE_ID)) {
            @Override
            public void onIndication(T_UnitDataIndication t_unitDataIndication, Object object) {
                // Parse the incoming message
                ASDU incoming = getServiceFromBody(t_unitDataIndication.getData().getBody());
                if (incoming instanceof ConfirmedRequest
                        && ((ConfirmedRequest) incoming).getServiceRequest() instanceof WritePropertyRequest) {
                    // FIXME: dirtyhack, get propertyvalue using wrightproperty
                    ByteQueue queue = new ByteQueue(t_unitDataIndication.getData().getBody());
                    byte[] msg = queue.peek(15, queue.size() - 21);
                    if (msg[0] == OOBProtocol.CONFIRM_ADD_DEVICE_REQUEST.getValue()) {
                        Log.d(TAG, "Confrim add device request received from BDS");
                        ConfirmAddDeviceRequest confirmAddDeviceRequest = new ConfirmAddDeviceRequest(msg);
                    }
                }
            }

            @Override
            public void onError(T_ReportIndication t_reportIndication, String s) {

            }
        });

        Log.d("PBAuthActivity", "Device started.....!");
    }

    public static byte[] performRegisterOverBds(BACnetEID who, URI location, BACnetEID bds) {
        final SequenceOf <CharacterString> uriChars = new SequenceOf <CharacterString>();
        uriChars.add(new CharacterString(location.toString()));
        final AddListElementRequest request = new AddListElementRequest(
                new BACnetObjectIdentifier(BACnetObjectType.multiStateInput, 1), BACnetPropertyIdentifier.stateText,
                null, uriChars);
        final ByteQueue byteQueue = new ByteQueue();
        request.write(byteQueue);
        return byteQueue.popAll();
    }

    class RegisterMessageThread extends AsyncTask <String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            try {
                ByteQueue queue = new ByteQueue();

                queue = new ByteQueue(performRegisterOverBds(new BACnetEID(MOBILE_ID),
                        new URI(SECURE_SCHEME + getHostAddress() + DTLS_SOCKET), new BACnetEID(AUTH_ID)));

                final TPDU tpdu = new TPDU(new BACnetEID(MOBILE_ID), new BACnetEID(AUTH_ID), queue.popAll());

                final T_UnitDataRequest unitDataRequest = new T_UnitDataRequest(new URI(SECURE_SCHEME + AUTH_IP + DTLS_SOCKET),
                        tpdu, 1, null);

                aseServiceChannel.doRequest(unitDataRequest);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            return "Directory Service Request is sent to BDS";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "Directory Service Request is sent to BDS");
        }

    }

    public ASDU getServiceFromBody(byte[] body) {
        ASDU request = null;
        try {
            ByteQueue queue = new ByteQueue(body);
            ServicesSupported servicesSupported = new ServicesSupported();
            servicesSupported.setAll(true);
            IncomingRequestParser requestParser = new IncomingRequestParser(servicesSupported, queue);
            request = requestParser.parse();
        } catch (BACnetException e) {
            e.printStackTrace();
        }
        return request;
    }

    public class SendAddDeviceRequest extends AsyncTask <String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "Sent Add device request to BDS";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "Sent Add device request to BDS");
        }
    }

    public void sendWriteProprtyRequest(byte[] message) {
        try {

            WritePropertyRequest writePropertyRequest = new WritePropertyRequest(
                    new BACnetObjectIdentifier(BACnetObjectType.analogValue, 1), BACnetPropertyIdentifier.presentValue,
                    new UnsignedInteger(55), new OctetString(message), new UnsignedInteger(1));
            final ByteQueue byteQueue = new ByteQueue();
            writePropertyRequest.write(byteQueue);

            final TPDU tpdu = new TPDU(new BACnetEID(MOBILE_ID), new BACnetEID(AUTH_ID), byteQueue.popAll());

            final T_UnitDataRequest unitDataRequest = new T_UnitDataRequest(new URI(SECURE_SCHEME + AUTH_IP + DTLS_SOCKET),
                    tpdu, 1, null);

            aseServiceChannel.doRequest(unitDataRequest);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        bindingConfiguration.destroyCoapServer();
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "service started");
    }

    public void hostAddress() {
        try {
            Enumeration <NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface i = interfaces.nextElement();
                if (i != null) {
                    Enumeration <InetAddress> addresses = i.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        String hostAddr = address.getHostAddress();
                        if (hostAddr.indexOf("139.") == 0) {
                            setHostAddress(hostAddr);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public String getHostAddress() {
        return this.hostAddress + ":";
    }

    public void broadcastMessageToUI(Intent messageIntent) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.sendBroadcast(messageIntent);
    }
}
