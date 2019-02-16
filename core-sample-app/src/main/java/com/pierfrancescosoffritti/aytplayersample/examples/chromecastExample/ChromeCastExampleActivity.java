package com.pierfrancescosoffritti.aytplayersample.examples.chromecastExample;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.cast.framework.CastContext;
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.ChromecastYouTubePlayerContext;
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.io.infrastructure.ChromecastConnectionListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.chromecast.chromecastsender.utils.PlayServicesUtils;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import com.pierfrancescosoffritti.aytplayersample.R;
import com.pierfrancescosoffritti.aytplayersample.examples.chromecastExample.notifications.NotificationManager;
import com.pierfrancescosoffritti.aytplayersample.examples.chromecastExample.notifications.PlaybackControllerBroadcastReceiver;
import com.pierfrancescosoffritti.aytplayersample.examples.chromecastExample.utils.MediaRouteButtonUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.mediarouter.app.MediaRouteButton;

/**
 * Example Activity used to showcase how to use the chromecast-youtube-library extension to cast videos to a Chromecast device.
 * See documentation here: <a href="https://github.com/PierfrancescoSoffritti/chromecast-youtube-player">chromecast-youtube-player</a>
 */
public class ChromeCastExampleActivity extends AppCompatActivity implements YouTubePlayersManager.LocalYouTubePlayerInitListener, ChromecastConnectionListener {

    private int googlePlayServicesAvailabilityRequestCode = 1;

    private YouTubePlayersManager youTubePlayersManager;
    private MediaRouteButton mediaRouteButton;

    private NotificationManager notificationManager;
    private PlaybackControllerBroadcastReceiver playbackControllerBroadcastReceiver;

    private YouTubePlayerView youTubePlayerView;
    private View chromeCastControlsRoot;

    private boolean connectedToChromeCast = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chromecast_example);

        youTubePlayerView = findViewById(R.id.youtube_player_view);
        chromeCastControlsRoot = findViewById(R.id.chromecast_controls_root);

        getLifecycle().addObserver(youTubePlayerView);

        notificationManager = new NotificationManager(this, ChromeCastExampleActivity.class);

        youTubePlayersManager = new YouTubePlayersManager(this, youTubePlayerView, chromeCastControlsRoot, notificationManager, getLifecycle());
        mediaRouteButton = MediaRouteButtonUtils.initMediaRouteButton(this);

        registerBroadcastReceiver();

        // can't use CastContext until I'm sure the user has GooglePlayServices
        PlayServicesUtils.checkGooglePlayServicesAvailability(this, googlePlayServicesAvailabilityRequestCode, this::initChromeCast);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getApplicationContext().unregisterReceiver(playbackControllerBroadcastReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // can't use CastContext until I'm sure the user has GooglePlayServices
        if(requestCode == googlePlayServicesAvailabilityRequestCode)
            PlayServicesUtils.checkGooglePlayServicesAvailability(this, googlePlayServicesAvailabilityRequestCode, this::initChromeCast);
    }

    private void initChromeCast() {
        new ChromecastYouTubePlayerContext(
                CastContext.getSharedInstance(this).getSessionManager(),
                this, playbackControllerBroadcastReceiver, youTubePlayersManager
        );
    }

    @Override
    public void onChromecastConnecting() {
    }

    @Override
    public void onChromecastConnected(@NonNull ChromecastYouTubePlayerContext chromecastYouTubePlayerContext) {
        connectedToChromeCast = true;

        updateUi(true);
        notificationManager.showNotification();
    }

    @Override
    public void onChromecastDisconnected() {
        connectedToChromeCast = false;

        updateUi(false);
        notificationManager.dismissNotification();
    }

    @Override
    public void onLocalYouTubePlayerInit() {
        if(connectedToChromeCast)
            return;

        MediaRouteButtonUtils.addMediaRouteButtonToPlayerUi(
                mediaRouteButton, android.R.color.white,
                null, localPlayerUiMediaRouteButtonContainer
        );
    }

    private void registerBroadcastReceiver() {
        playbackControllerBroadcastReceiver = new PlaybackControllerBroadcastReceiver(youTubePlayersManager::togglePlayback);
        IntentFilter filter = new IntentFilter(PlaybackControllerBroadcastReceiver.TOGGLE_PLAYBACK);
        filter.addAction(PlaybackControllerBroadcastReceiver.STOP_CAST_SESSION);
        getApplicationContext().registerReceiver(playbackControllerBroadcastReceiver, filter);
    }

    private void updateUi(boolean connected) {
        MediaRouteButtonContainer disabledContainer = connected ? localPlayerUiMediaRouteButtonContainer : chromecastPlayerUiMediaRouteButtonContainer;
        MediaRouteButtonContainer enabledContainer = connected ? chromecastPlayerUiMediaRouteButtonContainer : localPlayerUiMediaRouteButtonContainer;
        int mediaRouteButtonColor = connected ? android.R.color.black : android.R.color.white;

        // the media route button has a single instance.
        // therefore it has to be moved from the local YouTube player Ui to the chromecast YouTube player Ui, and vice versa.
        MediaRouteButtonUtils.addMediaRouteButtonToPlayerUi(
                mediaRouteButton, mediaRouteButtonColor,
                disabledContainer, enabledContainer
        );

        youTubePlayerView.setVisibility(connected ? View.GONE : View.VISIBLE);
        chromeCastControlsRoot.setVisibility(connected ? View.VISIBLE : View.GONE);
    }

    private MediaRouteButtonContainer chromecastPlayerUiMediaRouteButtonContainer = new MediaRouteButtonContainer() {
        public void addMediaRouteButton(MediaRouteButton mediaRouteButton) { youTubePlayersManager.getChromecastUiController().addView(mediaRouteButton); }
        public void removeMediaRouteButton(MediaRouteButton mediaRouteButton) { youTubePlayersManager.getChromecastUiController().removeView(mediaRouteButton); }
    };

    private MediaRouteButtonContainer localPlayerUiMediaRouteButtonContainer = new MediaRouteButtonContainer() {
        public void addMediaRouteButton(MediaRouteButton mediaRouteButton) { youTubePlayerView.getPlayerUiController().addView(mediaRouteButton); }
        public void removeMediaRouteButton(MediaRouteButton mediaRouteButton) { youTubePlayerView.getPlayerUiController().removeView(mediaRouteButton); }
    };

    public interface MediaRouteButtonContainer {
        void addMediaRouteButton(MediaRouteButton mediaRouteButton);
        void removeMediaRouteButton(MediaRouteButton mediaRouteButton);
    }
}