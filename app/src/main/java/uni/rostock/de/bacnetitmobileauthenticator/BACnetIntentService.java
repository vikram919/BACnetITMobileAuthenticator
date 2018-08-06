package uni.rostock.de.bacnetitmobileauthenticator;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.eclipse.californium.core.coap.CoAP;

import java.net.URI;

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
import uni.rostock.de.bacnet.it.coap.oobAuth.AddDeviceRequest;
import uni.rostock.de.bacnet.it.coap.oobAuth.ApplicationMessages;
import uni.rostock.de.bacnet.it.coap.oobAuth.OobProtocol;
import uni.rostock.de.bacnet.it.coap.oobAuth.OobStatus;
import uni.rostock.de.bacnet.it.coap.transportbinding.TransportDTLSCoapBinding;

public class BACnetIntentService extends IntentService {

    protected static final String TAG = BACnetIntentService.class.getName();
    private static final int AUTH_ID = 1;
    private static final int MOBILE_ID = 2;
    private static final int DTLS_SOCKET = CoAP.DEFAULT_COAP_SECURE_PORT;
    private ASEServices aseServiceChannel;
    private static final String AUTH_IP = "139.30.202.56:";
    private static final String SECURE_SCHEME = "coaps://";
    private String oobPswdString;
    protected static final String ADD_DEVICE_REQUEST_STATUS = "addDeviceRequestAckStatus";
    protected static final String ADD_DEVICE_REQUEST_ACK_ACTION = "addDeviceRequestAckAction";
    protected static final String ADD_DEVICE_REQUEST_CONFIRM_ACTION = "addDeviceRequestConfirmAction";

    public BACnetIntentService() {
        super(TAG);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(CameraActivity.ADD_DEVICE_REQUEST_SIGNAL));
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
        TransportDTLSCoapBinding bindingConfiguration = new TransportDTLSCoapBinding();
        bindingConfiguration.setCertificateMode();
        bindingConfiguration.createSecureCoapClient();
        bindingConfiguration.createSecureCoapServer(CoAP.DEFAULT_COAP_SECURE_PORT);
        bindingConfiguration.init();
        channelConfiguration.setASEService((ASEService) bindingConfiguration);
        channelConfiguration.registerChannelListener(new ChannelListener(new BACnetEID(MOBILE_ID)) {
            @Override
            public void onIndication(T_UnitDataIndication t_unitDataIndication, Object object) {
                // Parse the incoming message
                ASDU incoming = ApplicationMessages.getServiceFromBody(t_unitDataIndication.getData().getBody());
                if (incoming instanceof ConfirmedRequest
                        && ((ConfirmedRequest) incoming).getServiceRequest() instanceof WritePropertyRequest) {
                    // FIXME: dirtyhack, get propertyvalue using wrightproperty
                    ByteQueue queue = new ByteQueue(t_unitDataIndication.getData().getBody());
                    byte[] msg = queue.peek(15, queue.size() - 21);
                    if(msg[0] >> 5 == OobProtocol.OOB_STATUS){
                        Log.d(TAG, "mobile received OobStatus message");
                        OobStatus oobStatus = new OobStatus(msg);
                        boolean status = oobStatus.getOobStatus();
                        Intent messageIntent = new Intent(ADD_DEVICE_REQUEST_STATUS);
                        messageIntent.setAction(ADD_DEVICE_REQUEST_CONFIRM_ACTION);
                        messageIntent.putExtra("status", status);
                        broadcastMessageToUI(messageIntent);
                    }
                }
                if (incoming instanceof SimpleACK) {
                    Log.d(TAG, "mobile received ack for AddDeviceRequest");
                    Intent messageIntent = new Intent(ADD_DEVICE_REQUEST_STATUS);
                    messageIntent.setAction(ADD_DEVICE_REQUEST_ACK_ACTION);
                    broadcastMessageToUI(messageIntent);
                }
            }

            @Override
            public void onError(T_ReportIndication t_reportIndication, String s) {

            }
        });
        Log.d(TAG, "Device started.....!");

    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "service started");
        try {
            final DiscoveryConfig ds = new DiscoveryConfig("DNSSD", "1.1.1.1", "itb.bacnet.ch.",
                    "bds._sub._bacnet._udp.", "authen._sub._bacnet._udp.", "authenservice._sub._bacnet._udp.", false);

            DirectoryService.init();
            DirectoryService.getInstance().setDNSBinding(new DNSSD(ds));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        start();
    }

    public void broadcastMessageToUI(Intent messageIntent) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.sendBroadcast(messageIntent);
    }

    private void setOobPswdString(String value) {
        Log.d(TAG, "received oob password string: "+value);
        this.oobPswdString = value;
    }

    private String getOobPswdString() {
        return oobPswdString;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "broadcastReceiver received some signal");
            /*
            Listens for error messages if BACnetService fails,
            starts status intent early inform user about the error.
            */
            if (intent.getAction().contentEquals(CameraActivity.ADD_DEVICE_REQUEST_SIGNAL)) {
                Log.d(TAG, "received AddDeviceRequest action signal");
                String key = intent.getStringExtra(CameraActivity.ADD_DEVICE_REQUEST_SIGNAL_PAYLOAD).substring(1);
                setOobPswdString(key);
                Log.d(TAG, "mobile sending AddDeviceRequest message to BDS with key: "+key);
                LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(this);
                new SendAddDeviceRequest().execute("");
            }
        }
    };

    private class SendAddDeviceRequest extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            AddDeviceRequest addDeviceRequest = new AddDeviceRequest(getOobPswdString());
            ApplicationMessages.sendWritePropertyRequest(aseServiceChannel, addDeviceRequest.getBA(),
                    new BACnetEID(MOBILE_ID), new BACnetEID(AUTH_ID), SECURE_SCHEME + AUTH_IP + DTLS_SOCKET);
            return "Sent Add device request to BDS";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "Sent Add device request to BDS");
        }
    }
}
