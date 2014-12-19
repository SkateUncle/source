package com.example.skate_uncle.skateuncle;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import java.util.Random;

public class SkateUncleView extends View {

    static final boolean kDebug = false;

    private SoundManager soundManager_;

    class Spirit {

        public Spirit(int screen_width, int screen_height) {
            bitmap_right_ = (BitmapDrawable) getContext().getResources()
                    .getDrawable(R.drawable.santa_right);
            bitmap_left_ = (BitmapDrawable) getContext().getResources()
                    .getDrawable(R.drawable.santa_left);
            bitmap_die_1_ = (BitmapDrawable) getContext().getResources()
                    .getDrawable(R.drawable.santa_fall_0);
            bitmap_die_2_ = (BitmapDrawable) getContext().getResources()
                    .getDrawable(R.drawable.santa_fall_1);
            screen_width_ = screen_width;
            screen_height_ = screen_height;
            width_x_ = screen_width_ / PORTRAIT_DIVIDER;
            width_y_ = width_x_;
            Reset();
        }

        public void Reset() {
            x_ = screen_width_ / 2;
            // Note: adjust score calculation when adjust y_
            y_ = screen_height_ * 2 / 3;
        }

        public void Draw(Canvas canvas) {
            BitmapDrawable bitmap_show;
            if (state_ == State.DYING) {
                if (SystemClock.uptimeMillis() / 700 % 2 == 1) {
                    bitmap_show = bitmap_die_1_;
                } else {
                    bitmap_show = bitmap_die_2_;
                }
            } else if (x_speed_ >= 0) {
                bitmap_show = bitmap_right_;
            } else {
                bitmap_show = bitmap_left_;
            }
            rect_ = new Rect(x_ - width_x_, y_ - width_y_, x_ + width_x_, y_ + width_y_);
            bitmap_show.setBounds(rect_);
            bitmap_show.draw(canvas);
            if (kDebug) {
                Paint p = new Paint();
                p.setColor(Color.RED);
                p.setStyle(Style.STROKE);
                canvas.drawRect(GetCollisionRect(), p);
            }
        }

        public void UpdatePos(long last_time, long cur_time) {
            if (state_ != State.PLAYING) {
                x_speed_ = 0;
                return;
            }

            float new_x_speed_ = (MainActivity.x_acceleration * (screen_width_ / BASE_MOVE_DIVIDER));
            if (new_x_speed_ * x_speed_ < 0) {
                soundManager_.onDirectionChange();
            }
            x_speed_ = new_x_speed_;

            // set min time to avoid sudden delay in the game.
            long delta_time = Math.min(200, cur_time - last_time);
            float x_delta = x_speed_ * delta_time;
            if (x_ + x_delta < width_x_) {
                x_ = width_x_;
            } else if (x_ + x_delta + width_x_ >= screen_width_) {
                x_ = screen_width_ - width_x_;
            } else {
                x_ += x_delta;
            }
        }

        public Rect GetCollisionRect() {
            Rect r = rect_;
            int cx = r.centerX();
            if (x_speed_ >= 0) {
                cx += r.width() / 10;
            } else {
                cx -= r.width() / 10;
            }
            int cy = r.centerY() - r.height() / 10;
            int dx = (int) (r.width() / 2 * 0.65);
            int dy = (int) (r.height() / 2 * 0.8);
            return new Rect(cx - dx, cy - dy, cx + dx, cy + dy);
        }

        // positive to right, negative to left
        private float x_speed_ = 0;
        private Rect rect_;
        private int x_;
        private int y_;
        private BitmapDrawable bitmap_left_;
        private BitmapDrawable bitmap_right_;
        private BitmapDrawable bitmap_die_1_;
        private BitmapDrawable bitmap_die_2_;

        private int width_x_;
        private int width_y_;

        // Width and height of screen
        private int screen_width_;
        private int screen_height_;

        private static final float BASE_MOVE_DIVIDER = 250;
        private static final int PORTRAIT_DIVIDER = 10;
    }

    class Scene {

        public Scene() {
            rock_image_ = BitmapFactory.decodeResource(getResources(), R.drawable.rock3);
            // TODO: use another resource
            border_image_ = BitmapFactory.decodeResource(getResources(), R.drawable.rock3);
            ice_image_ = BitmapFactory.decodeResource(getResources(), R.drawable.ice3);
            width_x_ = getWidth() / kScreenX;
            width_y_ = getHeight() / kScreenY;
            rand_ = new Random();
            Reset();
        }

        public void Reset() {
            rocks_ = new int[kRockY][];
            for (int i = 0; i < rocks_.length; i++) {
                rocks_[i] = kRockTemplate[0];
            }
            offset_pixel_y_ = 0;
            num_line_ = 0;
        }

        public void Move(int y) {
            for (offset_pixel_y_ += y; offset_pixel_y_ >= width_y_; offset_pixel_y_ -= width_y_) {
                for (int i = 1; i < kRockY; i++) {
                    rocks_[i - 1] = rocks_[i];
                }
                num_line_++;
                rocks_[kRockY - 1] = kRockTemplate[
                        num_line_ % kNumLinePerRock == 0 ? rand_.nextInt(kRockTemplate.length - 1) + 1 : 0];
            }
        }

        public void Draw(Canvas canvas) {
            // left border
            for (int i = 0; i <= kScreenY; i++) {
                Rect r = GetRect(0, i);
                canvas.drawBitmap(ice_image_, null, r, null);
                canvas.drawBitmap(border_image_, null, r, null);
            }
            // right border
            for (int i = 0; i <= kScreenY; i++) {
                Rect r = GetRect(kScreenX - 1, i);
                canvas.drawBitmap(ice_image_, null, r, null);
                canvas.drawBitmap(border_image_, null, r, null);
            }
            // middle
            for (int i = 0; i < rocks_.length; i++) {
                for (int j = 0; j < rocks_[i].length; j++) {
                    Rect r = GetRect(j + 1, i);
                    canvas.drawBitmap(ice_image_, null, r, null);
                    if (rocks_[i][j] == 1) {
                        canvas.drawBitmap(rock_image_, null, r, null);
                    }
                }
            }
        }

        public int Score() {
            return Math.max(0, (num_line_ - kRockY * 2 / 3 - 2) / kNumLinePerRock);
        }

        public boolean HasCollision(Rect r) {
            for (int i = 0; i < kScreenY; i++) {
                if (GetRect(0, i).intersect(r)) {
                    return true;
                }
                if (GetRect(kScreenX - 1, i).intersect(r)) {
                    return true;
                }
            }
            for (int i = 0; i < rocks_.length; i++) {
                for (int j = 0; j < rocks_[i].length; j++) {
                    if (rocks_[i][j] == 1 && GetRect(j + 1, i).intersect(r)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private Rect GetRect(int x, int y) {
            int pixel_x = x * width_x_;
            int pixel_y = getHeight() - (y + 1) * width_y_ + offset_pixel_y_;
            return new Rect(pixel_x, pixel_y, pixel_x + width_x_, pixel_y + width_y_);
        }

        private int[][] rocks_;
        private int offset_pixel_y_;
        private int num_line_;

        private Bitmap rock_image_;
        private Bitmap border_image_;
        private Bitmap ice_image_;

        Random rand_;

        final int[][] kRockTemplate = {
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
                {0, 0, 0, 1, 1, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 1, 1, 0, 0, 0},
                {1, 0, 0, 1, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 1, 0, 0, 1},
                {0, 1, 1, 1, 1, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 1, 1, 1, 1, 0},
                {1, 1, 1, 0, 0, 0, 0, 1, 0, 0},
                {0, 0, 1, 0, 0, 0, 1, 1, 1, 1},
                {1, 0, 1, 0, 1, 0, 1, 0, 0, 0},
                {0, 0, 0, 1, 0, 1, 0, 1, 0, 1},
                {1, 0, 0, 0, 1, 1, 0, 0, 0, 1},
        };
        final private int kScreenX = 12;
        final private int kScreenY = 16;
        // additional 2 lines for left and right borders
        final private int kRockX = kScreenX - 2;
        // additional one line to show part of rocks at both bottom and top borders.
        final private int kRockY = kScreenY + 1;

        private int width_x_;
        private int width_y_;

        private int kNumLinePerRock = 5;
    }

    class Scorer {
        public Scorer() {
            MainActivity activity = (MainActivity) getContext();
            shared_pref_ = activity.getPreferences(Context.MODE_PRIVATE);
            key_ = activity.getString(R.string.saved_high_score);
            highest_score_ = shared_pref_.getInt(key_, 0);
        }

        public void MaybeWriteScore(int score) {
            if (score > highest_score_) {
                highest_score_ = score;
                SharedPreferences.Editor editor = shared_pref_.edit();
                editor.putInt(key_, score);
                editor.commit();
            }
        }

        public int highest_score() {
            return highest_score_;
        }

        private String key_;
        private SharedPreferences shared_pref_;
        private int highest_score_;
    }

    enum State {
        INIT, WAIT_START, PLAYING, DYING, OVER,
    }

    private State state_;

    private static final String LOG_TAG = "FlappyBirdView";

    public SkateUncleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SkateUncleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void initDrawables() {
        soundManager_ = new SoundManager(getContext());
        lastTime_ = SystemClock.uptimeMillis();
        final int height = getHeight();
        final int width = getWidth();

        spirit_ = new Spirit(width, height);
        scene_ = new Scene();
        scorer_ = new Scorer();
        state_ = State.WAIT_START;
    }

    private void updateAndDrawFPS(long curTime, Canvas canvas) {
        lastTime_ = curTime;
        ++refreshCounter_;

        if (refreshCounter_ == 100) {
            fps_ = 1000 * refreshCounter_ / (curTime - startTime_);
            startTime_ = curTime;
            refreshCounter_ = 0;
        }

        // Show fps on top right corner.
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(30);
        paint.setColor(Color.RED);
        paint.setTypeface(Typeface.MONOSPACE);
        String text = "Score: " + scene_.Score();
        text += "  Highest: " + scorer_.highest_score();
        if (kDebug) {
            text += "  FPS: " + new Long(fps_).toString();
        }
        canvas.drawText(text, 40, 40, paint);
    }

    boolean dying_sound_played = false;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (spirit_ == null) {
            initDrawables();
            startTime_ = SystemClock.uptimeMillis();
        }
        canvas.drawColor(0xff112233);
        scene_.Draw(canvas);
        final long curTime = SystemClock.uptimeMillis();
        spirit_.UpdatePos(lastTime_, curTime);
        spirit_.Draw(canvas);

        switch (state_) {
            case INIT:
                scene_.Reset();
                spirit_.Reset();
                ChangeState(State.WAIT_START);
                break;
            case WAIT_START: {
                DrawImage(canvas, getWidth() / 2, (int) (getHeight() * 0.37), 0.3, R.drawable.title);
                if (curTime / 700 % 2 == 0) {
                    DrawImage(canvas, getWidth() / 2, (int) (getHeight() * 0.63), 0.3,
                            R.drawable.tap_to_start);
                }
                if (MainActivity.ReceiveTapEvent()) {
                    ((MainActivity) getContext()).ResetAngles();
                    soundManager_.onStart();
                    ChangeState(State.PLAYING);
                }
                break;
            }
            case PLAYING:
                if (scene_.HasCollision(spirit_.GetCollisionRect())) {
                    ChangeState(State.DYING);
                    soundManager_.onCollision();
                }
                int wallOffset = (int) ((curTime - lastTime_) * WALL_SPEED);
                scene_.Move(wallOffset);
                break;
            case DYING: {
                if (curTime - start_time_in_current_state_ > 1000 && !dying_sound_played) {
                    soundManager_.onDie();
                    dying_sound_played = true;
                } else if (curTime - start_time_in_current_state_ > 4000) {
                    ChangeState(State.OVER);
                    dying_sound_played = false;
                }
                break;
            }
            case OVER: {
                //DrawImage(canvas, getWidth() / 2, (int)(getHeight() * 0.37), 0.3, R.drawable.game_over);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setTextSize(100);
                paint.setColor(Color.RED);
                paint.setTextAlign(Align.CENTER);
                paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
                canvas.drawText("" + scene_.Score(),
                        getWidth() / 2, (int) (getHeight() * 0.5), paint);
                scorer_.MaybeWriteScore(scene_.Score());
                int share_center_x = getWidth() / 2;
                int share_center_y = (int) (getHeight() * 0.9);
                DrawImage(canvas, share_center_x, share_center_y, 0.4, R.drawable.share);
                if (MainActivity.ReceiveTapEvent()) {
                    if (Math.abs(MainActivity.last_tap_x_ - share_center_x) < 245 * 0.5 &&
                            Math.abs(MainActivity.last_tap_y_ - share_center_y) < 85 * 0.5) {
                        share();
                    } else {
                        scene_.Reset();
                        spirit_.Reset();
                        ((MainActivity) getContext()).ResetAngles();
                        soundManager_.onStart();
                        ChangeState(State.PLAYING);
                    }
                }
                break;
            }
        }
        updateAndDrawFPS(curTime, canvas);
        postInvalidate();
    }

    private void ChangeState(State s) {
        if (state_ != s) {
            MainActivity.ReceiveTapEvent();
            start_time_in_current_state_ = SystemClock.uptimeMillis();
            state_ = s;
        }
    }

    private void DrawImage(Canvas canvas, int cx, int cy, double factor, int id) {
        final Bitmap image = BitmapFactory.decodeResource(getResources(), id);
        int width = (int) (factor * image.getWidth());
        int height = (int) (factor * image.getHeight());
        canvas.drawBitmap(
                image, null,
                new Rect(cx - width / 2, cy - height / 2, cx + width / 2, cy + height / 2),
                null);
    }

    private void share() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        draw(c);
        String url = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), bitmap,
                "Uncle Santa", "Uncle Santa");

        if (url != null) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/jpeg");
            share.putExtra(Intent.EXTRA_STREAM, Uri.parse(url));
            getContext().startActivity(
                    Intent.createChooser(share, getContext().getString(R.string.share_via)));
        } else {
            Toast.makeText(getContext(), "Error sharing the score!", Toast.LENGTH_LONG).show();
        }
    }

    static final float WALL_SPEED = 0.2f;
    static final int WALL_COUNT = 5;
    static final int WALL_HEIGHT = 15;
    static final int BIRD_RADIUS = 20;

    private Spirit spirit_;
    private Scene scene_;
    private Scorer scorer_;

    private long lastTime_;
    private long start_time_in_current_state_;
    private long refreshCounter_;
    private long startTime_;
    private long fps_;
}
