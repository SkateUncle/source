package com.example.skate_uncle.skateuncle;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class SoundManager {

  SoundPool soundPool_;
  Map<Integer, Integer> ids = new HashMap<Integer, Integer>();
  Context context_;
  MediaPlayer background_player_;
  boolean is_pausing = false;

  public SoundManager(Context context) {
    context_ = context;
    soundPool_ = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);

    ids.put(2, soundPool_.load(context_, R.raw.change, 0));
    ids.put(3, soundPool_.load(context_, R.raw.die1, 0));
    ids.put(4, soundPool_.load(context_, R.raw.die2, 0));
    Log.d("loadmusic", "start loading music");
    soundPool_.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
      public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        Log.d("loadmusic", "complete" + sampleId + " " + status);
      }
    });
    background_player_ = MediaPlayer.create(context_, R.raw.background);
    background_player_.setLooping(true);
  }

  public void onStart() {
    background_player_.start();
  }

  public void onDirectionChange() {
    soundPool_.play(ids.get(2), 1, 1, 1, 0, 0);
  }

  public void onCollision() {
    if (background_player_.isPlaying()) {
      background_player_.pause();
      background_player_.seekTo(0);
    }
    soundPool_.play(ids.get(3), 1, 1, 1, 0, 0);
  }

  public void onDie() {
    soundPool_.play(ids.get(4), 1, 1, 1, 0, 0);
  }

  public void onPause() {
      if (background_player_.isPlaying()) {
          background_player_.pause();
          is_pausing = true;
      }
  }

  public void onResume() {
      if (is_pausing)
          background_player_.start();
  }
}
