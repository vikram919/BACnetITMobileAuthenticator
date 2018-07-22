package uni.rostock.de.bacnetitmobileauthenticator;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import ch.fhnw.bacnetit.ase.application.configuration.api.DiscoveryConfig;
import ch.fhnw.bacnetit.ase.application.service.api.ASEServices;
import ch.fhnw.bacnetit.ase.application.service.api.BACnetEntityListener;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelConfiguration;
import ch.fhnw.bacnetit.ase.application.service.api.ChannelFactory;
import ch.fhnw.bacnetit.ase.application.transaction.api.ChannelListener;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;
import ch.fhnw.bacnetit.ase.encoding.api.T_ReportIndication;
import ch.fhnw.bacnetit.ase.encoding.api.T_UnitDataIndication;
import ch.fhnw.bacnetit.ase.network.directory.api.DirectoryService;
import ch.fhnw.bacnetit.ase.transportbinding.service.api.ASEService;
import ch.fhnw.bacnetit.directorybinding.dnssd.api.DNSSD;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ASDU;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.ConfirmedRequest;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.asdu.SimpleACK;
import ch.fhnw.bacnetit.samplesandtests.api.encoding.util.ByteQueue;
import ch.fhnw.bacnetit.samplesandtests.api.service.confirmed.WritePropertyRequest;
import uni.rostock.de.bacnet.it.coap.oobAuth.ApplicationMessages;
import uni.rostock.de.bacnet.it.coap.transportbinding.TransportDTLSCoapBinding;

public class BACnetIntentService extends IntentService {

    protected static final String TAG = BACnetIntentService.class.getName();
    private static final int AUTH_ID = 1;
    private static final int MOBILE_ID = 2;
    private static final int DTLS_SOCKET = 5684;
    private ASEServices aseServiceChannel;
    private static final String AUTH_IP = "139.30.202.56:";
    private static final String SECURE_SCHEME = "coaps://";
    TransportDTLSCoapBinding bindingConfiguration;
    private boolean signal = false;
    private String oobPswdString;
    protected static final String SERVICE_ERROR_MESSAGE = "serviceErrorMessage";
    protected static final String SERVICE_ERROR_PAYLOAD = "serviceErrorPayload";
    protected static final String SERVICE_AUTH_SUCCESS = "serviceAuthSuccess";
    protected static final String SERVICE_AUTH_SUCCESS_PAYLOAD = "serviceAuthSuccessPayload";
    protected static final String SERVICE_AUTH_FAILURE = "serviceAuthError";
    protected static final String SERVICE_AUTH_FAILURE_PAYLOAD = "serviceAuthErrorPayload";
    private ApplicationMessages applicationMessages;

    public BACnetIntentService() {
        super(TAG);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        try {

            final DiscoveryConfig ds = new DiscoveryConfig("DNSSD", "1.1.1.1", "itb.bacnet.ch.",
                    "bds._sub._bacnet._udp.", "authen._sub._bacnet._udp.", "authenservice._sub._bacnet._udp.", false);

            DirectoryService.init();
            DirectoryService.getInstance().setDNSBinding(new DNSSD(ds));
            start();
            new SendAddDeviceRequest().execute("");
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
                ASDU incoming = applicationMessages.getServiceFromBody(t_unitDataIndication.getData().getBody());
                if (incoming instanceof ConfirmedRequest
                        && ((ConfirmedRequest) incoming).getServiceRequest() instanceof WritePropertyRequest) {
                    // FIXME: dirtyhack, get propertyvalue using wrightproperty
                    ByteQueue queue = new ByteQueue(t_unitDataIndication.getData().getBody());
                    byte[] msg = queue.peek(15, queue.size() - 21);
//                    if (msg[0] == OOBProtocol.CONFIRM_ADD_DEVICE_REQUEST) {
//                        Log.d(TAG, "Confrim add device request received from BDS");
//                        ConfirmAddDeviceRequest confirmAddDeviceRequest = new ConfirmAddDeviceRequest(msg);
//                    }
                }
                if (incoming instanceof SimpleACK) {
                    Log.d(TAG, "mobile received ack for AddDeviceRequest");
                }
            }

            @Override
            public void onError(T_ReportIndication t_reportIndication, String s) {

            }
        });
        Log.d(TAG, "Device started.....!");
    }

    public class SendAddDeviceRequest extends AsyncTask <String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            byte[] message = getOobPswdString().getBytes(StandardCharsets.UTF_8);
            applicationMessages.sendWritePropertyRequest(aseServiceChannel, message,
                    new BACnetEID(MOBILE_ID), new BACnetEID(AUTH_ID), SECURE_SCHEME + AUTH_IP + DTLS_SOCKET);
            return "Sent Add device request to BDS";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "Sent Add device request to BDS");
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
        setOobPswdString(intent.getStringExtra(CameraActivity.ADD_DEVICE_REQUEST_SIGNAL_PAYLOAD));
    }

    public void broadcastMessageToUI(Intent messageIntent) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.sendBroadcast(messageIntent);
    }

    private void setOobPswdString(String value) {
        this.oobPswdString = value;
    }

    private String getOobPswdString() {
        return oobPswdString;
    }
}
