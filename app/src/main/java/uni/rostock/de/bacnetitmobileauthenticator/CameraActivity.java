package uni.rostock.de.bacnetitmobileauthenticator;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION_RESOLVED = 0;
    private static final String TAG = "CameraActivity";

    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    private String mCameraId;
    private TextureView textureView;
    private CameraDevice mCameraDevice;
    private TextView textViewDisplayPin;

    private ImageReader mImageReader;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private BitStreamDetector bitStreamDetector;

    private Handler mHandler;
    private HandlerThread mHandlerThread;

    protected static final String ADD_DEVICE_REQUEST_SIGNAL = "addDeviceRequestSignal";
    protected static final String ADD_DEVICE_REQUEST_SIGNAL_PAYLOAD = "addDeviceRequestSignalPayload";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(400, 300);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, null);
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    Toast.makeText(this, "Camera needs permissions", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESOLVED);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        //Get the texture view
        textureView = findViewById(R.id.textureView);
        textViewDisplayPin = findViewById(R.id.displayPinTextView);
        startBackgroundThread();
        bitStreamDetector = new BitStreamDetector(getApplicationContext());
        bitStreamDetector.setBitStreamDetectorKeyReadCallback(bitStreamDetectorKeyReadCallback);

    }

    @Override
    public void onPause() {
        super.onPause();
        closeCamera();
        stopBackgroundThread();
    }


    @Override
    public void onResume() {
        super.onResume();

        if (textureView.isAvailable()) {
            setupCamera(textureView.getWidth(), textureView.getHeight());
            connectCamera();
        } else {
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_RESOLVED) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "Application needs camers permissions", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }


    private void setupCamera(int width, int height) {
        CameraManager mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
                // get the back side camera lens
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                // adjust camera according to device rotation
                int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                int totalRotation = cameraToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if (swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private static int cameraToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int cameraOrientation = cameraCharacteristics.get(cameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (cameraOrientation + deviceOrientation + 360) % 360;
    }

    private void startPreview() {

        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(400, 300);
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mImageReader = ImageReader.newInstance(400, 300, ImageFormat.YUV_420_888, 16);
            mImageReader.setOnImageAvailableListener(imageReaderListener, mHandler);
            Surface readerSurface = mImageReader.getSurface();
            mCaptureRequestBuilder.addTarget(readerSurface);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, readerSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(), "Unable to setup preview", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    ImageReader.OnImageAvailableListener imageReaderListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            /*we can exactly get the dimension of the frame received*/
            int mPreviewHeight = image.getHeight();
            int mPreviewWidth = image.getWidth();

            if (image != null) {
                // with we extract the Y channel of the image
                ByteBuffer bb = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[bb.capacity()];
                bb.get(bytes);
                long totalLuminosity = 0;
                long c = 0;
                for (int row = (mPreviewHeight*1)/3; row < (mPreviewHeight*2)/3; row++) {
                    for (int col = (mPreviewWidth*1)/3; col < (mPreviewWidth *2)/3; col++) {
                        int v = (int) bytes[row * mPreviewWidth + col];
                        totalLuminosity += (v < 0) ? 256 + v : v;
                        c++;
                    }
                }
                short averageLuminosity = (short) (totalLuminosity / c);
                Log.d(TAG, "luminosity Value: " + averageLuminosity);
                bitStreamDetector.onLuminosityMeasured(averageLuminosity);
                image.close();
            }
        }
    };

    private void startBackgroundThread() {
        mHandlerThread = new HandlerThread("CameraBackground");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        mHandlerThread = new HandlerThread("CameraBackground");
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    BitStreamDetectorKeyReadCallback bitStreamDetectorKeyReadCallback = new BitStreamDetectorKeyReadCallback() {
        @Override
        public void onKeyRead(String key) {
            Log.d(TAG, key);
            Log.d(TAG, "key value reached");
            closeCamera();
            stopBackgroundThread();
            Log.d(TAG, "sending ADD_DEVICE_REQUEST_SIGNAL");
            sendMessage(key);
        }

        @Override
        public void onSymbolRead(Boolean symbol) {

        }

        @Override
        public void onStateChanged(int state) {

        }

        @Override
        public void onReset() {

        }
    };

    private void sendMessage(String key) {
        Intent messageIntent = new Intent(ADD_DEVICE_REQUEST_SIGNAL);
        messageIntent.putExtra(ADD_DEVICE_REQUEST_SIGNAL_PAYLOAD, key);
        Log.d(TAG, "ADD_DEVICE_REQUEST_SIGNAL with payload is sent to BACnetIntentService");
        LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        Intent statusActivity = new Intent(getApplicationContext(), StatusActivity.class);
        startActivity(statusActivity);
    }
}