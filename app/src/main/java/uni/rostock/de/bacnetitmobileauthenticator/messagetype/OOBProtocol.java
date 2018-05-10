package uni.rostock.de.bacnetitmobileauthenticator.messagetype;

public enum OOBProtocol {


    /* A byte is allocated to inform the type of Message id */
    ADD_DEVICE_REQUEST(0),

    CONFIRM_ADD_DEVICE_REQUEST(1),

    INIT_MESSAGE_ID(2),

    REPLY_INIT_MESSAGE_ID(3),

    REQUEST_TO_MOBILE_DH1_ID(4),

    MOBILE_TO_AUTH_DH1_ID(5),

    AUTH_TO_MOBILE_DH2_ID(6),

    MOBILE_TO_DEVICE_DH2_ID(7),

    DEVICE_TO_MOBILE_FINAL_ID(8),

    MOBILE_TO_AUTH_FINAL_ID(9),

    /* Default as spcecified in IEEE 802.15.6-2012 */
    CURVE_secp256r1(1),

    X25519(2),

    PRIVATE_KEY_BYTES(32),

    PUBLIC_KEY_BYTES(2),

    SECRET_KEY_BYTES(16),

    MAC_KEY_BYTES(16),

    IV_KEY_BYTES(16),

    OOB_PSWD_KEY_LENGTH(16),

    DERIVED_KEYS_LENGTH(48);

    private final int value;

    private OOBProtocol(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

}
