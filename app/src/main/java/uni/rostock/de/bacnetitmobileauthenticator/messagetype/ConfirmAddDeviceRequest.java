package uni.rostock.de.bacnetitmobileauthenticator.messagetype;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ch.fhnw.bacnetit.ase.encoding._ByteQueue;
import ch.fhnw.bacnetit.ase.encoding.api.BACnetEID;

public class ConfirmAddDeviceRequest {
    private static int MESSAGE_ID = OOBProtocol.CONFIRM_ADD_DEVICE_REQUEST.getValue();
    private BACnetEID sourceEID;
    private BACnetEID destinationEID;
    private byte[] secretPswdBA;
    private byte[] finalMessage;

    public ConfirmAddDeviceRequest(BACnetEID sourceEID, BACnetEID destinationEID, byte[] secretPswdBA) {
        this.sourceEID = sourceEID;
        this.destinationEID = destinationEID;
        this.secretPswdBA = secretPswdBA;
        generateBA();
    }

    private void generateBA() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(MESSAGE_ID);
        bos.write(sourceEID.bytes().length);
        try {
            bos.write(sourceEID.bytes());
            bos.write(destinationEID.bytes().length);
            bos.write(destinationEID.bytes());
            bos.write(secretPswdBA);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.finalMessage = bos.toByteArray();

    }

    public byte[] getBA() {
        return this.finalMessage;
    }

    public ConfirmAddDeviceRequest(byte[] finalMessage) {
        this.finalMessage = finalMessage;
        int count = 1;
        byte[] sourceEIDBA = new byte[finalMessage[count]];
        count += 1;
        System.arraycopy(finalMessage, count, sourceEIDBA, 0, sourceEIDBA.length);
        count += sourceEIDBA.length;
        byte[] destinationEIDBA = new byte[finalMessage[count]];
        count += 1;
        System.arraycopy(finalMessage, count, destinationEIDBA, 0, destinationEIDBA.length);
        count += destinationEIDBA.length;
        try {
            _ByteQueue queue = new _ByteQueue(sourceEIDBA);
            _ByteQueue queue2 = new _ByteQueue(destinationEIDBA);
            this.destinationEID = new BACnetEID(queue2);
            this.sourceEID = new BACnetEID(queue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        secretPswdBA = new byte[finalMessage.length - count];
        System.arraycopy(finalMessage, count, secretPswdBA, 0, secretPswdBA.length);
    }

    public BACnetEID getSourceEID() {
        return this.sourceEID;
    }

    public BACnetEID getDestinationEID() {
        return this.destinationEID;
    }

    public byte[] getSecretPswdBA() {
        return this.secretPswdBA;
    }
}

