package com.example.testexoplayer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity implements PlayerControlView.VisibilityListener, PlaybackPreparer {

    private final String TAG = getClass().getSimpleName();

    // Saved instance state keys.
    private static final String KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters";
    public static final String KEY_INDEX = "index";
    private static final String KEY_WINDOW = "window";
    private static final String KEY_POSITION = "position";
    private static final String KEY_AUTO_PLAY = "auto_play";
    private final String drmLicenseUrl = "";

    private PlayerView mPlayerView;
    private ExoPlayer mPlayer;
    private int startIndex;
    private boolean startAutoPlay;
    private int startWindow;
    private long startPosition;
    private MediaSource mediaSource;
    private FrameworkMediaDrm mediaDrm;
    private View mBtnLoad;
    private View mProgress;
    private boolean mFirst = true;

    private final int MSG_FAILURE = 0;
    private final int MSG_SUCCESS = 1;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_FAILURE:
                    mProgress.setVisibility(View.GONE);
                    showToast("Request failed!");
                    break;
                case MSG_SUCCESS:
                    if(MyApp.getPlaySourceBeans() == null || MyApp.getPlaySourceBeans().size() == 0){
                        showToast("No channel list");
                        return;
                    }
                    mFirst = false;
                    initPlayer();
                    mPlayerView.onResume();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        setContentView(R.layout.activity_main);


        mBtnLoad = findViewById(R.id.btn_load);
        mBtnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: ");
                mBtnLoad.setVisibility(View.GONE);
                mPlayerView.setVisibility(View.VISIBLE);
                mProgress.setVisibility(View.VISIBLE);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        OkHttpClient okHttpClient = new OkHttpClient();
                        Request request = new Request.Builder().url("http://192.168.180.15:18003/CMS-GATEWAY/cms/api/oklist?pageNum=1&pageSize=20").get().build();
                        okHttpClient.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                Log.d(TAG, "onFailure: "+e.getMessage());
                                mHandler.sendEmptyMessage(MSG_FAILURE);
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                String result = response.body().string();
                                Log.d(TAG, "onResponse: "+result);
                                try {
                                    Log.d(TAG, "onResponse: rows = "+ new JSONObject(result).getJSONObject("body").getJSONArray("rows").toString());
                                    MyApp.setPlaySource(new Gson().fromJson( new JSONObject(result).getJSONObject("body").getJSONArray("rows").toString(),new TypeToken<List<PlaySourceBean>>(){}.getType()));
                                    mHandler.sendEmptyMessage(MSG_SUCCESS);
                                } catch (JSONException e) {
                                    mHandler.sendEmptyMessage(MSG_FAILURE);
                                    e.printStackTrace();
                                }

                            }
                        });
                    }
                }).start();
            }
        });

        mProgress = findViewById(R.id.progress);
        mPlayerView = findViewById(R.id.player_view);
        mPlayerView.setControllerVisibilityListener(this);
        mPlayerView.setPlaybackPreparer(this);
        mPlayerView.setErrorMessageProvider(new PlayerErrorMessageProvider());

        if (savedInstanceState != null) {
            startIndex = savedInstanceState.getInt(KEY_INDEX);
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
            startWindow = savedInstanceState.getInt(KEY_WINDOW);
            startPosition = savedInstanceState.getLong(KEY_POSITION);
            Log.d(TAG, "onCreate: savedInstanceState startAutoPlay = "+startAutoPlay+" startWindow = "+startWindow+" startPosition = "+startPosition+" startIndex = "+startIndex);
        } else {
            Log.d(TAG, "onCreate: savedInstanceState == null");
            clearStartPosition();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
        if(mFirst){
            return;
        }
        initPlayer();
        mPlayerView.onResume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBtnLoad.requestFocus();
        Log.d(TAG, "onResume: getCurrentFocus "+getCurrentFocus());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ");
        mPlayerView.onPause();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState: ");
        updateStartPosition();
        outState.putInt(KEY_INDEX, startIndex);
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay);
        outState.putInt(KEY_WINDOW, startWindow);
        outState.putLong(KEY_POSITION, startPosition);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState: ");
    }

    private void initPlayer(){
        if(mPlayer == null){
            DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
            PlaySourceBean playSourceBean = MyApp.getPlaySourceBeans().get(startIndex);
//            if (playSourceBean.getEncryptionType() == 1) {
//                int errorStringId = R.string.error_drm_unknown;
//                if (Util.SDK_INT < 18) {
//                    errorStringId = R.string.error_drm_not_supported;
//                } else {
//                    try {
////                        UUID drmSchemeUuid = Util.getDrmUuid(playSourceBean.getDrm_scheme());
////                        if (drmSchemeUuid == null) {
////                            errorStringId = R.string.error_drm_unsupported_scheme;
////                        } else {
//                            drmSessionManager = buildDrmSessionManagerV18(C.WIDEVINE_UUID, drmLicenseUrl, null, false);
////                        }
//                    } catch (UnsupportedDrmException e) {
//                        errorStringId = e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
//                                ? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown;
//                    }
//                }
//                if (drmSessionManager == null) {
//                    showToast(errorStringId);
////                    finish();
//                    return;
//                }
//            }

            mPlayer = ExoPlayerFactory.newSimpleInstance(this,new DefaultRenderersFactory(this),new DefaultTrackSelector(),drmSessionManager);
            mPlayerView.setPlayer(mPlayer);
            mPlayer.setPlayWhenReady(true);
            mPlayer.addListener(new PlayerEventListener());
            String playUrl = MyApp.getPlaySourceBeans().get(startIndex).getPlayUrl();
            Log.d(TAG, "initPlayer: playUrl = "+playUrl);
            mediaSource = new DashMediaSource.Factory(new DefaultDataSourceFactory(this, new DefaultHttpDataSourceFactory(TAG)))
                    .createMediaSource(Uri.parse(playUrl));
        }
        boolean haveStartPosition = startWindow != C.INDEX_UNSET;
        if (haveStartPosition) {
            mPlayer.seekTo(startWindow, startPosition);
        }
        Log.d(TAG, "initPlayer: startWindow = "+startWindow+" startPosition = "+startPosition+" startIndex = "+startIndex);
        mPlayer.prepare(mediaSource, !haveStartPosition, false);
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            updateStartPosition();
            mPlayer.release();
            mPlayer = null;
            mediaSource = null;
        }
        releaseMediaDrm();
    }

    private void clearStartPosition() {
        startIndex = 0;
        startAutoPlay = true;
        startWindow = C.INDEX_UNSET;
        startPosition = C.TIME_UNSET;
    }

    private void updateStartPosition() {
        if (mPlayer != null) {
            startAutoPlay = mPlayer.getPlayWhenReady();
            startWindow = mPlayer.getCurrentWindowIndex();
            startPosition = Math.max(0, mPlayer.getContentPosition());
            Log.d(TAG, "updateStartPosition: startAutoPlay = "+startAutoPlay+" startWindow = "+startWindow+" startPosition = "+startPosition+" startIndex = "+startIndex);
        }
    }

    @Override
    public void onVisibilityChange(int visibility) {
        Log.d(TAG, "onVisibilityChange: "+visibility);
    }

    @Override
    public void preparePlayback() {
        Log.d(TAG, "preparePlayback: ");
    }

    private class PlayerEventListener implements Player.EventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Log.d(TAG, "onPlayerStateChanged: playWhenReady = "+playWhenReady+" playbackState = "+playbackState);
            switch (playbackState){
                case Player.STATE_IDLE:

                    break;
                case Player.STATE_BUFFERING:
                    if(mPlayerView.getVisibility() == View.VISIBLE)
                        mProgress.setVisibility(View.VISIBLE);
                    break;
                case Player.STATE_READY:
                case Player.STATE_ENDED:
                    mProgress.setVisibility(View.GONE);
                    break;
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {
            Log.d(TAG, "onPlayerError: "+e.getMessage());
            showToast("play error : "+e.getMessage());
            mProgress.setVisibility(View.GONE);
        }

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            Log.d(TAG, "onTracksChanged: trackGroups = "+trackGroups+" trackSelections = "+trackSelections);
        }
    }

    private class PlayerErrorMessageProvider implements ErrorMessageProvider<ExoPlaybackException> {

        @Override
        public Pair<Integer, String> getErrorMessage(ExoPlaybackException e) {
            String errorString = getString(R.string.error_generic);
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                Exception cause = e.getRendererException();
                if (cause instanceof DecoderInitializationException) {
                    // Special case for decoder initialization failures.
                    DecoderInitializationException decoderInitializationException =
                            (DecoderInitializationException) cause;
                    if (decoderInitializationException.decoderName == null) {
                        if (decoderInitializationException.getCause() instanceof DecoderQueryException) {
                            errorString = getString(R.string.error_querying_decoders);
                        } else if (decoderInitializationException.secureDecoderRequired) {
                            errorString =
                                    getString(
                                            R.string.error_no_secure_decoder, decoderInitializationException.mimeType);
                        } else {
                            errorString =
                                    getString(R.string.error_no_decoder, decoderInitializationException.mimeType);
                        }
                    } else {
                        errorString =
                                getString(
                                        R.string.error_instantiating_decoder,
                                        decoderInitializationException.decoderName);
                    }
                }
            }
            return Pair.create(0, errorString);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown: keycode = "+keyCode);
        if((keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) && !mFirst){
            startActivityForResult(new Intent(this,ListActivity.class),Activity.RESULT_FIRST_USER);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "dispatchKeyEvent: keycode = "+event.getKeyCode()+" action = "+event.getAction());
        // See whether the player view wants to handle media or DPAD keys events.
        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK && mPlayerView.isControllerVisible()){
            mPlayerView.hideController();
            return true;
        }
        if(mFirst)
            return super.dispatchKeyEvent(event);
        return mPlayerView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode = "+requestCode+" resultCode = "+resultCode+" data = "+data);
        if(resultCode == Activity.RESULT_FIRST_USER && data != null){
            releasePlayer();
            clearStartPosition();
            startIndex = data.getIntExtra(KEY_INDEX,0);
            initPlayer();
        }
    }

    private DefaultDrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManagerV18(
            UUID uuid, String licenseUrl, String[] keyRequestPropertiesArray, boolean multiSession)
            throws UnsupportedDrmException {
        HttpDataSource.Factory licenseDataSourceFactory = new DefaultHttpDataSourceFactory(TAG);
        HttpMediaDrmCallback drmCallback =
                new HttpMediaDrmCallback(licenseUrl, licenseDataSourceFactory);
        if (keyRequestPropertiesArray != null) {
            for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
                drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
                        keyRequestPropertiesArray[i + 1]);
            }
        }
        releaseMediaDrm();
        mediaDrm = FrameworkMediaDrm.newInstance(uuid);
        return new DefaultDrmSessionManager<>(uuid, mediaDrm, drmCallback, null, multiSession);
    }

    private void releaseMediaDrm() {
        if (mediaDrm != null) {
            mediaDrm.release();
            mediaDrm = null;
        }
    }

    private void showToast(int messageId) {
        showToast(getString(messageId));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}

