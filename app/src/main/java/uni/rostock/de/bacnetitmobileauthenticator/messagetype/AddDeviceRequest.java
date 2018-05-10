package uni.rostock.de.bacnetitmobileauthenticator.messagetype;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ch.fhnw.bacnetit.ase.encoding._ByteQueue;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;

public class AddDeviceRequest {
    private static int MESSAGE_ID = OOBProtocol.ADD_DEVICE_REQUEST.getValue();
    private BACnetEID mobileEID;
    private BACnetEID authorizerEID;
    private byte[] finalMessage;

    public AddDeviceRequest(BACnetEID mobileEID, BACnetEID AuthorizerEID) {
        this.mobileEID = mobileEID;
        this.authorizerEID = AuthorizerEID;
        generateByteArray();

    }

    private void generateByteArray() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(MESSAGE_ID);
        bos.write((byte) mobileEID.bytes().length);
        try {
            bos.write(mobileEID.bytes());
            bos.write((byte) authorizerEID.bytes().length);
            bos.write(authorizerEID.bytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.finalMessage = bos.toByteArray();
    }

    public byte[] getBA() {
        return this.finalMessage;
    }

    public AddDeviceRequest(byte[] finalMessage) {
        this.finalMessage = finalMessage;
        int count = 1;
        byte[] mobileEIDBA = new byte[finalMessage[count]];
        count += 1;
        System.arraycopy(finalMessage, count, mobileEIDBA, 0, mobileEIDBA.length);
        count += mobileEIDBA.length;
        byte[] authorizerEIDBA = new byte[finalMessage[count]];
        count += 1;
        System.arraycopy(finalMessage, count, authorizerEIDBA, 0, authorizerEIDBA.length);
        count += authorizerEIDBA.length;
        _ByteQueue queue = new _ByteQueue(mobileEIDBA);
        try {
            this.mobileEID = new BACnetEID(queue);
        } catch (Exception e) {
            e.printStackTrace();
        }

        _ByteQueue queue2 = new _ByteQueue(authorizerEIDBA);
        try {
            this.authorizerEID = new BACnetEID(queue2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BACnetEID getMobileEID() {
        return this.mobileEID;
    }

    public BACnetEID getAuthorizerEID() {
        return this.authorizerEID;
    }
}
