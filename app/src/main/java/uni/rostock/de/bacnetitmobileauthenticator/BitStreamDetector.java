package uni.rostock.de.bacnetitmobileauthenticator;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

public class BitStreamDetector implements BitStreamDetectorCallback {

    public static final int STATE_IDLE = 01; /* Nothing happens - it should be dark */
    public static final int STATE_LEARNING = 02; /* detected first edge - learning symbol width now */
    public static final int STATE_TRANSMISSION = 04; /* READING KEY */

    private int mCurrentState;

    public static final int THRESHOLD = 240;
    public static final int KEY_LENGTH = 20; /* Number of symbols */
    public static final int TIMEOUT = 2000; /* in ms - maximum symbol width */
    public static final int LEARN_LENGTH = 6;

    private long mTimestamp1;
    private long mTimestamp2;

    private int mLearnCount;
    private short mLastLuminosity;

    private int[] mLearnedSymbolWidth = new int[LEARN_LENGTH + 1]; /* 1..4 will contain measured widths, 0 will contain calculated width */
    private Boolean condition;

    private BitStreamDetectorKeyReadCallback mBitStreamDetectorKeyReadCallback = null;

    private Context ctx = null;

    public BitStreamDetector(Context ct) {
        mCurrentState = STATE_IDLE;
        mLearnCount = 0;
        mLastLuminosity = -1;
        ctx = ct;
        if (mBitStreamDetectorKeyReadCallback != null)
            mBitStreamDetectorKeyReadCallback.onReset();
    }

    /* This one is called by CameraViewer for EVERY SINGLE FRAME */
    @Override
    public void onLuminosityMeasured(short relativeLuminosity) {
        switch (mCurrentState) {
            case STATE_IDLE:
                if (relativeLuminosity >= THRESHOLD) {
                    Log.d("BSD", "Switching to state LEARNING");
                    Log.d("BSD", "Rising Edge detected");
                    mTimestamp1 = System.currentTimeMillis();
                    mCurrentState = STATE_LEARNING;
                    if (mBitStreamDetectorKeyReadCallback != null)
                        mBitStreamDetectorKeyReadCallback.onStateChanged(STATE_LEARNING);
                }
                break;
            case STATE_LEARNING:
                switch (mLearnCount % 2) {
                    case 0:
                        if (relativeLuminosity < THRESHOLD) {
                            mLearnCount++;
                            Log.d("BSD", "Falling Edge detected");
                            if (mLearnCount == 1) {
                                mTimestamp1 = System.currentTimeMillis(); /* to discard the first symbol (very erroneous) */
                            }
                            mTimestamp2 = System.currentTimeMillis();
                            if ((mTimestamp2 - mTimestamp1) < (TIMEOUT * mLearnCount)) {
                                mLearnedSymbolWidth[mLearnCount] = (int) (mTimestamp2 - mTimestamp1);
                            } else {
                                Log.d("BSD", "Time out! Switching back to state IDLE");
                                mCurrentState = STATE_IDLE;
                                if (mBitStreamDetectorKeyReadCallback != null)
                                    mBitStreamDetectorKeyReadCallback.onReset();
                                mLearnCount = 0;
                            }
                        }
                        break;
                    case 1:
                        if (relativeLuminosity >= THRESHOLD) {
                            mLearnCount++;
                            Log.d("BSD", "Rising Edge detected");
                            mTimestamp2 = System.currentTimeMillis();
                            if ((mTimestamp2 - mTimestamp1) < (TIMEOUT * mLearnCount)) {
                                mLearnedSymbolWidth[mLearnCount] = (int) (mTimestamp2 - mTimestamp1);
                            } else {
                                Log.d("BSD", "Time out! Switching back to state IDLE");
                                mCurrentState = STATE_IDLE;
                                if (mBitStreamDetectorKeyReadCallback != null)
                                    mBitStreamDetectorKeyReadCallback.onReset();
                                mLearnCount = 0;
                            }
                        }
                        break;
                }
                if (mLearnCount == LEARN_LENGTH) {
                    mLearnCount = 0;
                    Log.d("BSD", "Learned " + LEARN_LENGTH + " symbols. Switching to state TRANSMISSION");
                    mCurrentState = STATE_TRANSMISSION;
                    if (mBitStreamDetectorKeyReadCallback != null)
                        mBitStreamDetectorKeyReadCallback.onStateChanged(STATE_TRANSMISSION);
                    mLearnedSymbolWidth[0] = 0;
                    for (int i = 2; i <= LEARN_LENGTH; i++) {
                        mLearnedSymbolWidth[0] += mLearnedSymbolWidth[i] / (i - 1);
                        Log.d("BSD", "Learned Symbol Width (" + i + "): " + mLearnedSymbolWidth[i]);
                    }
                    /* using the alternative b/c it is more accourate */
                    boolean fixedWidth = PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("settings_flicker_fixed_symbols", true);
                    if (fixedWidth) {
                        try {
                            mLearnedSymbolWidth[0] = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(ctx).getString("settings_flicker_fixed_symbol_width", "200"));
                        } catch (NumberFormatException e) {
                            mLearnedSymbolWidth[0] = 200;
                        }
                    } else {
                        mLearnedSymbolWidth[0] = mLearnedSymbolWidth[LEARN_LENGTH] / (LEARN_LENGTH - 1);
                    }

                    Log.d("BSD", ((fixedWidth) ? "Fixed" : "Learned") + " Symbol width: " + mLearnedSymbolWidth[0]);
                    Log.d("BSD", "Alternative: " + mLearnedSymbolWidth[LEARN_LENGTH] / (LEARN_LENGTH - 1));
                    Thread sampleThread = new Thread(mSampleThread, "SamplingThread");
                    sampleThread.start();
                }
                break;
            case STATE_TRANSMISSION:
                mLastLuminosity = relativeLuminosity;
                break;
        }
    }

    public BitStreamDetectorKeyReadCallback getBitStreamDetectorKeyReadCallback() {
        return mBitStreamDetectorKeyReadCallback;
    }

    public void setBitStreamDetectorKeyReadCallback(
            BitStreamDetectorKeyReadCallback bitStreamDetectorKeyReadCallback) {
        this.mBitStreamDetectorKeyReadCallback = bitStreamDetectorKeyReadCallback;
    }

    private Runnable mSampleThread = new Runnable() {
        @Override
        public void run() {
            String key = "";
            /* let the last callibration symbol pass, then wait for half a symbol width */
            try {
                Thread.sleep((long) (mLearnedSymbolWidth[0] * 0.5));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < KEY_LENGTH; i++) {
                condition = mLastLuminosity >= THRESHOLD;
                if (mBitStreamDetectorKeyReadCallback != null) {
                    key += condition ? "1" : "0";
                }
                if (i < KEY_LENGTH - 1) {
                    try {
                        Thread.sleep(mLearnedSymbolWidth[0]);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        Thread.sleep((long) (mLearnedSymbolWidth[0] * 0.5));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            mCurrentState = STATE_IDLE;
            Log.d("BSD", "Switching to state IDLE");
            if (mBitStreamDetectorKeyReadCallback != null) {
                mBitStreamDetectorKeyReadCallback.onKeyRead(key);
                mBitStreamDetectorKeyReadCallback.onReset();
            }
        }
    };


}