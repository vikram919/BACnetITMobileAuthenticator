package uni.rostock.de.bacnetitmobileauthenticator;

public interface BitStreamDetectorKeyReadCallback {
    public void onKeyRead(String key);
    public void onSymbolRead(Boolean symbol);
    public void onStateChanged(int state);
    public void onReset();
}
