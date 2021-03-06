package com.dailymotion.android.player.sdk;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.dailymotion.android.BuildConfig;
import com.dailymotion.android.player.sdk.events.PlayerEvent;
import com.dailymotion.android.player.sdk.events.PlayerEventFactory;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by hugo
 * on 6/13/17.
 */

public class PlayerWebView extends WebView {

    public static final String EVENT_APIREADY = "apiready";
    public static final String EVENT_TIMEUPDATE = "timeupdate";
    public static final String EVENT_DURATION_CHANGE = "durationchange";
    public static final String EVENT_PROGRESS = "progress";
    public static final String EVENT_SEEKED = "seeked";
    public static final String EVENT_SEEKING = "seeking";
    public static final String EVENT_GESTURE_START = "gesture_start";
    public static final String EVENT_GESTURE_END = "gesture_end";
    public static final String EVENT_MENU_DID_SHOW = "menu_did_show";
    public static final String EVENT_MENU_DID_HIDE = "menu_did_hide";
    public static final String EVENT_VIDEO_START = "video_start";
    public static final String EVENT_VIDEO_END = "video_end";
    public static final String EVENT_AD_START = "ad_start";
    public static final String EVENT_AD_PLAY = "ad_play";
    public static final String EVENT_AD_PAUSE = "ad_pause";
    public static final String EVENT_AD_END = "ad_end";
    public static final String EVENT_AD_TIME_UPDATE = "ad_timeupdate";
    public static final String EVENT_ADD_TO_COLLECTION_REQUESTED = "add_to_collection_requested";
    public static final String EVENT_LIKE_REQUESTED = "like_requested";
    public static final String EVENT_WATCH_LATER_REQUESTED = "watch_later_requested";
    public static final String EVENT_SHARE_REQUESTED = "share_requested";
    public static final String EVENT_FULLSCREEN_TOGGLE_REQUESTED = "fullscreen_toggle_requested";
    public static final String EVENT_PLAY = "play";
    public static final String EVENT_PAUSE = "pause";
    public static final String EVENT_LOADEDMETADATA = "loadedmetadata";
    public static final String EVENT_PLAYING = "playing";
    public static final String EVENT_START = "start";
    public static final String EVENT_END = "end";
    public static final String EVENT_CONTROLSCHANGE = "controlschange";
    public static final String EVENT_VOLUMECHANGE = "volumechange";
    /**
     * @deprecated Use EVENT_QUALITY_CHANGE instead
     */
    @Deprecated
    public static final String EVENT_QUALITY = "qualitychange";
    public static final String EVENT_QUALITY_CHANGE = "qualitychange";
    public static final String EVENT_QUALITIES_AVAILABLE = "qualitiesavailable";

    public static final String EVENT_PLAYBACK_READY = "playback_ready";
    public static final String EVENT_CHROME_CAST_REQUESTED = "chromecast_requested";
    public static final String EVENT_VIDEO_CHANGE = "videochange";

    private static final java.lang.String ASSETS_SCHEME = "asset://";

    public static final String COMMAND_NOTIFY_LIKECHANGED = "notifyLikeChanged";
    public static final String COMMAND_NOTIFY_WATCHLATERCHANGED = "notifyWatchLaterChanged";
    public static final String COMMAND_NOTIFYFULLSCREENCHANGED = "notifyFullscreenChanged";
    public static final String COMMAND_LOAD = "load";
    public static final String COMMAND_MUTE = "mute";
    public static final String COMMAND_CONTROLS = "controls";
    public static final String COMMAND_PLAY = "play";
    public static final String COMMAND_PAUSE = "pause";
    public static final String COMMAND_SEEK = "seek";
    public static final String COMMAND_SETPROP = "setProp";
    public static final String COMMAND_QUALITY = "quality";
    public static final String COMMAND_SUBTITLE = "subtitle";
    public static final String COMMAND_TOGGLE_CONTROLS = "toggle-controls";
    public static final String COMMAND_TOGGLE_PLAY = "toggle-play";
    public static final String COMMAND_VOLUME = "volume";

    private ArrayList<Command> mCommandList = new ArrayList<>();

    private final String mExtraUA = ";dailymotion-player-sdk-android " + BuildConfig.SDK_VERSION;

    static class Command {
        public String methodName;
        public Object[] params;
    }

    private Handler mHandler;
    private Gson mGson;
    private boolean mDisallowIntercept = false;
    private String mVideoId;
    private float mPosition;
    private boolean mPlayWhenReady = true;
    private boolean mVisible;
    private EventListener mEventListener;
    private PlayerEventListener mPlayerEventListener;
    @Nullable private WebViewErrorListener webViewErrorListener;
    private boolean mIsWebContentsDebuggingEnabled = false;

    private Runnable mControlsCommandRunnable;
    private Runnable mMuteCommandRunnable;
    private Runnable mLoadCommandRunnable;

    private boolean mVideoPaused = false;
    private String mQuality = "";
    private double mBufferedTime = 0;
    private double mDuration = 0;
    private boolean mIsSeeking = false;
    private boolean mIsEnded = false;
    private boolean mIsInitialized = false;
    private boolean mIsFullScreen = false;
    private float mVolume = 1f;

    private long mControlsLastTime;
    private long mMuteLastTime;
    private long mLoadLastTime;
    private PlayerEventFactory eventFactory;

    private boolean mApiReady;
    private boolean mHasMetadata;
    private boolean mHasPlaybackReady;

    public boolean isEnded() {
        return mIsEnded;
    }

    public boolean isSeeking() {
        return mIsSeeking;
    }

    public double getBufferedTime() {
        return mBufferedTime;
    }

    public double getDuration() {
        return mDuration;
    }

    public boolean getVideoPaused() {
        return mVideoPaused;
    }

    public String getQuality() {
        return mQuality;
    }

    public String getVideoId() {
        return mVideoId;
    }

    /**
     * @deprecated Use setVisible(visible, shouldPauseTimer, shouldResumeTimer) instead
     */
    @Deprecated
    public void setVisible(boolean visible) {
        setVisible(visible, true);
    }

    /**
     * Notify PlayerWebView of the view visibility.
     *
     * @param visible TRUE, view is visible. FALSE otherwise.
     * @param shouldHandleTimers if TRUE, will call resumeTimers() if visible is TRUE and pauseTimers() if visible is FALSE.
     *                           Otherwise, calls to resumeTimers() / pauseTimers() won't be made.
     *                           Beware pauseTimers() will pause timers for all your webviews. If you're using more than 2,
     *                           you might want to handle this separately.
     */
    public void setVisible(boolean visible, boolean shouldHandleTimers) {
        if (mVisible != visible) {
            mVisible = visible;

            if (!mVisible) {
                setPlayWhenReady(false);
                // when we resume, we don't want video to start automatically
            }
            if (!mVisible) {
                onPause();
                if (shouldHandleTimers) {
                    pauseTimers();
                }
            } else {
                onResume();
                if (shouldHandleTimers) {
                    resumeTimers();
                }
            }
        }
    }

    private void updatePlayState() {
        if (!mVisible) {
            pause();
        } else {
            if (mPlayWhenReady) {
                play();
            } else {
                pause();
            }
        }
    }

    public boolean getPlayWhenReady() {
        return mPlayWhenReady;
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        mPlayWhenReady = playWhenReady;
        updatePlayState();
    }

    public void setMinimizeProgress(float p) {
        showControls(!(p > 0));
    }

    public void setIsLiked(boolean isLiked) {
        queueCommand(COMMAND_NOTIFY_LIKECHANGED, isLiked);
    }

    public void setIsInWatchLater(boolean isInWatchLater) {
        queueCommand(COMMAND_NOTIFY_WATCHLATERCHANGED, isInWatchLater);
    }

    private class JavascriptBridge {
        @JavascriptInterface
        public void triggerEvent(final String e) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleEvent(e);
                }
            });
        }
    }

    private void sendCommand(Command command) {
        switch (command.methodName) {
            case COMMAND_MUTE:
                callPlayerMethod((Boolean) command.params[0] ? "mute" : "unmute");
                break;
            case COMMAND_CONTROLS:
                callPlayerMethod("api", "controls", (Boolean) command.params[0] ? "true" : "false");
                break;
            case COMMAND_QUALITY:
                callPlayerMethod("api", "quality", command.params[0]);
                break;
            case COMMAND_SUBTITLE:
                callPlayerMethod("api", "subtitle", command.params[0]);
                break;
            case COMMAND_TOGGLE_CONTROLS:
                callPlayerMethod("api", "toggle-controls", command.params);
                break;
            case COMMAND_TOGGLE_PLAY:
                callPlayerMethod("api", "toggle-play", command.params);
                break;
            case COMMAND_VOLUME:
                callPlayerMethod("api", "volume", command.params);
                break;
            default:
                callPlayerMethod(command.methodName, command.params);
                break;
        }
    }

    private void handleEvent(String e) {

        /*
         * the data we get from the api is a bit strange...
         */
        e = URLDecoder.decode(e);

        String p[] = e.split("&");
        HashMap<String, String> map = new HashMap<>();

        for (String s : p) {
            String s2[] = s.split("=");
            if (s2.length == 1) {
                map.put(s2[0], null);
            } else if (s2.length == 2) {
                map.put(s2[0], s2[1]);
            } else {
                Timber.e("bad param: " + s);
            }
        }

        String event = map.get("event");
        if (event == null) {
            Timber.e("bad event 2: " + e);
            return;
        }

        if (!event.equals("timeupdate")) {
            Timber.d("[%d] event %s", hashCode(), e);
        }

        PlayerEvent playerEvent = eventFactory.createPlayerEvent(event, map, e);
        switch (playerEvent.getName()) {
            case EVENT_APIREADY: {
                mApiReady = true;
                break;
            }
            case EVENT_START: {
                mIsEnded = false;
                mHandler.removeCallbacks(mLoadCommandRunnable);
                mLoadCommandRunnable = null;
                break;
            }
            case EVENT_END: {
                mIsEnded = true;
                break;
            }
            case EVENT_PROGRESS: {
                mBufferedTime = Float.parseFloat(map.get("time"));
                break;
            }
            case EVENT_TIMEUPDATE: {
                mPosition = Float.parseFloat(map.get("time"));
                break;
            }
            case EVENT_DURATION_CHANGE: {
                mDuration = Float.parseFloat(map.get("duration"));
                break;
            }
            case EVENT_GESTURE_START: {
                mDisallowIntercept = true;
                break;
            }
            case EVENT_MENU_DID_SHOW: {
                mDisallowIntercept = true;
                break;
            }
            case EVENT_GESTURE_END: {
                mDisallowIntercept = false;
                break;
            }
            case EVENT_MENU_DID_HIDE: {
                mDisallowIntercept = false;
                break;
            }
            case EVENT_PLAY: {
                mVideoPaused = false;
                mPlayWhenReady = true;
                break;
            }
            case EVENT_PAUSE: {
                mVideoPaused = true;
                mPlayWhenReady = false;
                break;
            }
            case EVENT_AD_PLAY: {
                mPlayWhenReady = true;
                break;
            }
            case EVENT_AD_PAUSE: {
                mPlayWhenReady = false;
                break;
            }
            case EVENT_CONTROLSCHANGE: {
                mHandler.removeCallbacks(mControlsCommandRunnable);
                mControlsCommandRunnable = null;
                break;
            }
            case EVENT_VOLUMECHANGE: {
                mVolume = Float.parseFloat(map.get("volume"));
                mHandler.removeCallbacks(mMuteCommandRunnable);
                mMuteCommandRunnable = null;
                break;
            }
            case EVENT_LOADEDMETADATA: {
                mHasMetadata = true;
                break;
            }
            case EVENT_QUALITY_CHANGE: {
                mQuality = map.get("quality");
                break;
            }
            case EVENT_SEEKED: {
                mIsSeeking = false;
                mPosition = Float.parseFloat(map.get("time"));
                break;
            }
            case EVENT_SEEKING: {
                mIsSeeking = true;
                mPosition = Float.parseFloat(map.get("time"));
                break;
            }
            case EVENT_PLAYBACK_READY: {
                mHasPlaybackReady = true;
                break;
            }
        }

        if (mEventListener != null) {
            mEventListener.onEvent(event, map);
        }

        /* Only expose the POJO events we are supporting */
        if (mPlayerEventListener != null) {
            mPlayerEventListener.onEvent(playerEvent);
        }

        tick();
    }

    private void tick() {

        if (!mApiReady) {
            return;
        }

        Iterator<Command> iterator = mCommandList.iterator();
        while (iterator.hasNext()) {
            final Command command = iterator.next();
            switch (command.methodName) {
                case COMMAND_PAUSE:
                case COMMAND_PLAY:
                    if (!mHasPlaybackReady) {
                        continue;
                    }
                    break;
                case COMMAND_NOTIFY_LIKECHANGED:
                    if (!mHasMetadata) {
                        continue;
                    }
                    break;
                case COMMAND_NOTIFY_WATCHLATERCHANGED:
                    if (!mHasMetadata) {
                        continue;
                    }
                    break;
                case COMMAND_MUTE:
                    if (System.currentTimeMillis() - mMuteLastTime < 1000) {
                        continue;
                    }
                    mMuteLastTime = System.currentTimeMillis();
                    break;
                case COMMAND_LOAD:
                    if (System.currentTimeMillis() - mLoadLastTime < 1000) {
                        continue;
                    }
                    mLoadLastTime = System.currentTimeMillis();
                    break;
                case COMMAND_CONTROLS:
                    if (System.currentTimeMillis() - mControlsLastTime < 1000) {
                        continue;
                    }
                    mControlsLastTime = System.currentTimeMillis();
                    break;
            }

            iterator.remove();
            sendCommand(command);
        }
    }


    Object mJavascriptBridge = new JavascriptBridge();

    @Override
    public void loadUrl(String url) {
        Timber.d("[%d] loadUrl %s", hashCode(), url);
        super.loadUrl(url);
    }

    public void queueCommand(String method, Object... params) {
        /*
         * remove duplicate commands
         */
        Iterator<Command> iterator = mCommandList.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().methodName.equals(method)) {
                iterator.remove();
            }
        }

        /*
         * if we're loading a new video, cancel the stuff from before
         */
        if (method.equals(COMMAND_LOAD)) {
            mPosition = 0;
            mDisallowIntercept = false;

            mVideoId = (String) params[0];

            mHasMetadata = false;
            mHasPlaybackReady = false;

            iterator = mCommandList.iterator();
            while (iterator.hasNext()) {
                switch (iterator.next().methodName) {
                    case COMMAND_NOTIFY_LIKECHANGED:
                    case COMMAND_NOTIFY_WATCHLATERCHANGED:
                    case COMMAND_SEEK:
                    case COMMAND_PAUSE:
                    case COMMAND_PLAY:
                        iterator.remove();
                        break;
                }
            }

        }
        Command command = new Command();
        command.methodName = method;
        command.params = params;
        mCommandList.add(command);
        tick();
    }

    public void callPlayerMethod(String method, Object... params) {
        StringBuilder builder = new StringBuilder();
        builder.append("javascript:player.");
        builder.append(method);
        builder.append('(');
        int count = 0;
        for (Object o : params) {
            count++;
            if (o instanceof String) {
                builder.append("'" + o + "'");
            } else if (o instanceof Number) {
                builder.append(o.toString());
            } else if (o instanceof Boolean) {
                builder.append(o.toString());
            } else {
                builder.append("JSON.parse('" + mGson.toJson(o) + "')");
            }
            if (count < params.length) {
                builder.append(",");
            }
        }
        builder.append(')');
        String js = builder.toString();

        loadUrl(js);
    }

    private void mute(boolean mute) {
        queueCommand(COMMAND_MUTE, mute);
    }

    public void mute() {
        mute(true);
    }

    public void unmute() {
        mute(false);
    }

    public void play() {
        queueCommand(COMMAND_PLAY);
    }

    public void pause() {
        queueCommand(COMMAND_PAUSE);
    }

    public void setQuality(String quality) {
        queueCommand(COMMAND_QUALITY, quality);
    }

    public void seek(double time) {
        queueCommand(COMMAND_SEEK, time);
    }

    public void showControls(boolean visible) {
        queueCommand(COMMAND_CONTROLS, visible);
    }

    public void setFullscreenButton(boolean fullScreen) {
        if (fullScreen != mIsFullScreen) {
            mIsFullScreen = fullScreen;
            queueCommand(COMMAND_NOTIFYFULLSCREENCHANGED);
        }
    }

    public PlayerWebView(Context context) {
        super(context);
    }

    public PlayerWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void initialize(final String baseUrl, final Map<String, String> queryParameters, final Map<String, String> httpHeaders) {
        mIsInitialized = true;
        eventFactory = new PlayerEventFactory();
        new AdIdTask(getContext(), new AdIdTask.AdIdTaskListener() {
            @Override
            public void onResult(AdvertisingIdClient.Info info) {
                finishInitialization(baseUrl, queryParameters, httpHeaders, info);
            }
        }).execute();
    }

    public void finishInitialization(final String baseUrl, final Map<String, String> queryParameters, final Map<String, String> httpHeaders, final AdvertisingIdClient.Info adInfo) {
        mGson = new Gson();
        WebSettings mWebSettings = getSettings();
        mWebSettings.setDomStorageEnabled(true);
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setUserAgentString(mWebSettings.getUserAgentString() + mExtraUA);
        mWebSettings.setPluginState(WebSettings.PluginState.ON);

        setBackgroundColor(Color.BLACK);
        if (Build.VERSION.SDK_INT >= 17) {
            mWebSettings.setMediaPlaybackRequiresUserGesture(false);
        }

        mHandler = new Handler();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(mIsWebContentsDebuggingEnabled);
        }

        WebChromeClient mChromeClient = new WebChromeClient() {

            /**
             * The view to be displayed while the fullscreen VideoView is buffering
             * @return the progress view
             */
            @Override
            public View getVideoLoadingProgressView() {
                ProgressBar pb = new ProgressBar(getContext());
                pb.setIndeterminate(true);
                return pb;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
            }

            @Override
            public Bitmap getDefaultVideoPoster() {
                int colors[] = new int[1];
                colors[0] = Color.TRANSPARENT;
                Bitmap bm = Bitmap.createBitmap(colors, 0, 1, 1, 1, Bitmap.Config.ARGB_8888);
                return bm;
            }

            @Override
            public void onHideCustomView() {
            }
        };

        addJavascriptInterface(mJavascriptBridge, "dmpNativeBridge");

        setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (url.startsWith(ASSETS_SCHEME)) {
                    String asset = url.substring(ASSETS_SCHEME.length());
                    if (asset.endsWith(".ttf") || asset.endsWith(".otf")) {
                        try {
                            InputStream inputStream = getContext().getAssets().open(asset);
                            WebResourceResponse response = null;
                            String encoding = "UTF-8";
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                int statusCode = 200;
                                String reasonPhase = "OK";
                                Map<String, String> responseHeaders = new HashMap<>();
                                responseHeaders.put("Access-Control-Allow-Origin", "*");
                                response = new WebResourceResponse("font/ttf", encoding, statusCode, reasonPhase, responseHeaders, inputStream);
                            } else {
                                response = new WebResourceResponse("font/ttf", encoding, inputStream);
                            }
                            return response;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Timber.e("webview redirect to %s", url);
                Intent httpIntent = new Intent(Intent.ACTION_VIEW);
                httpIntent.setData(Uri.parse(url));
                httpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                getContext().startActivity(httpIntent);
                return true;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (webViewErrorListener != null) {
                    webViewErrorListener.onErrorReceived(view, errorCode, description, failingUrl);
                }
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (webViewErrorListener != null) {
                    webViewErrorListener.onErrorReceived(view, request, error);
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
                if (webViewErrorListener != null) {
                    webViewErrorListener.onReceivedSslError(view, handler, error);
                }
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                if (webViewErrorListener != null) {
                    webViewErrorListener.onReceivedHttpError(view, request, errorResponse);
                }
            }
        });
        setWebChromeClient(mChromeClient);

        Map<String, String> parameters = new HashMap<>();
        // the following parameters below are compulsory, make sure they are always defined
        parameters.put("app", getContext().getPackageName());
        parameters.put("api", "nativeBridge");
        parameters.put("queue-enable", "0");

        if (Utils.hasFireTV(getContext())) {
            parameters.put("client_type", "firetv");
        } else if (Utils.hasLeanback(getContext())) {
            parameters.put("client_type", "androidtv");
        } else {
            parameters.put("client_type", "androidapp");
        }

        try {
            if (adInfo != null && adInfo.getId() != null && !adInfo.getId().isEmpty()) {
                parameters.put("ads_device_id", adInfo.getId());
                parameters.put("ads_device_tracking", adInfo.isLimitAdTrackingEnabled() ? "0" : "1");
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        parameters.putAll(queryParameters);

        StringBuilder builder = new StringBuilder();
        builder.append(baseUrl);
        boolean isFirstParameter = true;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (isFirstParameter) {
                isFirstParameter = false;
                builder.append('?');
            } else {
                builder.append('&');
            }

            String encodedParam;
            try {
                encodedParam = URLEncoder.encode(entry.getValue(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                encodedParam = entry.getValue();
            }

            String encodedKey;
            try {
                encodedKey = URLEncoder.encode(entry.getKey(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                encodedKey = entry.getKey();
            }

            builder.append(encodedKey);
            builder.append('=');
            builder.append(encodedParam);
        }

        loadUrl(builder.toString(), httpHeaders);
    }

    public interface WebViewErrorListener {
        void onErrorReceived(WebView webView, int errorCode, String description, String failingUrl);

        @RequiresApi(Build.VERSION_CODES.M)
        void onErrorReceived(WebView view, WebResourceRequest request, WebResourceError error);

        void onReceivedSslError (WebView view, SslErrorHandler handler, SslError error);

        @RequiresApi(Build.VERSION_CODES.M)
        void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse);
    }

    public void setWebViewErrorListener(@Nullable WebViewErrorListener errorListener) {
        this.webViewErrorListener = errorListener;
    }

    /**
     * @deprecated Implement PlayerEventListener instead
     */
    @Deprecated
    public interface EventListener {
        void onEvent(String event, HashMap<String, String> map);
    }

    /**
     * @param listener
     * @deprecated Use setPlayerEventListener() instead
     */
    @Deprecated
    public void setEventListener(EventListener listener) {
        mEventListener = listener;
    }

    public interface PlayerEventListener {
        void onEvent(@NonNull PlayerEvent event);
    }

    public void setPlayerEventListener(PlayerEventListener listener) {
        mPlayerEventListener = listener;
    }

    public void release() {
        loadUrl("about:blank");
        onPause();
    }

    public void load(String videoId) {
        load(videoId, null);
    }

    public void load(String videoId, Map<String, Object> loadParams) {
        if (!mIsInitialized) {
            Map<String, String> defaultQueryParameters = new HashMap<>();
            defaultQueryParameters.put("sharing-enable", "false");
            defaultQueryParameters.put("watchlater-enable", "false");
            defaultQueryParameters.put("like-enable", "false");
            defaultQueryParameters.put("collections-enable", "false");
            defaultQueryParameters.put("fullscreen-action", "trigger_event");
            defaultQueryParameters.put("locale", Locale.getDefault().getLanguage());

            initialize("https://www.dailymotion.com/embed/", defaultQueryParameters, new HashMap<String, String>());
        }
        queueCommand(COMMAND_LOAD, videoId, loadParams);
    }

    public void setSubtitle(String language_code) {
        queueCommand(COMMAND_SUBTITLE, language_code);
    }

    public void toggleControls() {
        queueCommand(COMMAND_TOGGLE_CONTROLS);
    }

    public void togglePlay() {
        queueCommand(COMMAND_TOGGLE_PLAY);
    }

    public long getPosition() {
        return (long) (mPosition * 1000);
    }

    public void setVolume(float volume) {
        if (volume >= 0f && volume <= 1f) {
            queueCommand(COMMAND_VOLUME, volume);
        }
    }

    public float getVolume() {
        return mVolume;
    }

    public void setIsWebContentsDebuggingEnabled(boolean isWebContentsDebuggingEnabled) {
        mIsWebContentsDebuggingEnabled = isWebContentsDebuggingEnabled;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mDisallowIntercept) {
            requestDisallowInterceptTouchEvent(true);
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

}
