package de.halbmarathon.runtracker;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import java.util.Locale;
import java.util.Set;

/**
 * Speaks short English announcements with the phone's own text-to-speech. Nothing is downloaded and no
 * audio files are shipped; an offline English voice is preferred.
 *
 * - Other audio (music, podcasts) is turned down while speaking and comes back afterwards.
 * - A short wake lock is held only while a sentence is spoken, so it works with the screen off without
 *   keeping the CPU awake for the whole run.
 * - If the engine or the English voice data is missing, {@link #status()} says so and nothing is spoken.
 *
 * The speech engine reports back on other threads, so the methods are synchronized.
 */
final class Announcer {

    static final String READY = "ready";
    static final String INIT = "init";
    static final String NO_ENGLISH = "no_english";
    static final String UNAVAILABLE = "unavailable";

    interface StatusListener {
        /** Called once, when the engine is ready or has failed. Runs on another thread. */
        void onStatus(String status);
    }

    private static final long LOCK_MS = 20000;

    private final Context ctx;
    private final boolean oneShot;
    private final StatusListener listener;
    private final AudioManager audio;
    private final PowerManager power;
    private final AudioAttributes attrs;
    private final AudioManager.OnAudioFocusChangeListener focusListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int change) {
            // Nothing to do: speech is short and Android turns the other audio down by itself.
        }
    };

    private TextToSpeech tts;
    private String status = INIT;
    private String pending = null;
    private String current = null;
    private int counter = 0;
    private boolean closed = false;
    private AudioFocusRequest focusRequest;
    private PowerManager.WakeLock lock;

    /**
     * @param oneShot if true the engine is released after the first sentence (used for the voice test)
     */
    Announcer(Context context, boolean oneShot, StatusListener listener) {
        this.ctx = context.getApplicationContext();
        this.oneShot = oneShot;
        this.listener = listener;
        this.audio = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        this.power = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        this.attrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build();
        try {
            tts = new TextToSpeech(ctx, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int result) {
                    initDone(result);
                }
            });
        } catch (Throwable t) {
            RunLog.add("voice:init-error " + RunLog.describe(t));
            finish(UNAVAILABLE);
        }
    }

    synchronized String status() {
        return status;
    }

    private synchronized void initDone(int result) {
        if (closed) {
            closeEngine();
            return;
        }
        if (result != TextToSpeech.SUCCESS || tts == null) {
            RunLog.add("voice:init-fail");
            finish(UNAVAILABLE);
            return;
        }
        try {
            Locale chosen = null;
            Locale[] wanted = new Locale[] { Locale.US, Locale.UK, Locale.ENGLISH };
            for (Locale l : wanted) {
                int r = tts.setLanguage(l);
                if (r >= TextToSpeech.LANG_AVAILABLE) {
                    chosen = l;
                    break;
                }
            }
            if (chosen == null) {
                RunLog.add("voice:no-english");
                finish(NO_ENGLISH);
                return;
            }
            tts.setAudioAttributes(attrs);
            preferOfflineVoice();
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String id) {}

                @Override
                public void onDone(String id) {
                    spoken(id);
                }

                @Override
                @SuppressWarnings("deprecation")
                public void onError(String id) {
                    spoken(id);
                }

                @Override
                public void onError(String id, int code) {
                    spoken(id);
                }
            });
            RunLog.add("voice:ready lang=" + chosen.toString());
        } catch (Throwable t) {
            RunLog.add("voice:setup-error " + RunLog.describe(t));
            finish(UNAVAILABLE);
            return;
        }
        status = READY;
        notifyListener(READY);
        if (pending != null) {
            String p = pending;
            pending = null;
            speak(p);
        }
    }

    /** Take an installed English voice that works without internet, if the engine offers a choice. */
    private void preferOfflineVoice() {
        try {
            Set<Voice> voices = tts.getVoices();
            if (voices == null) return;
            Voice best = null;
            int bestScore = -1;
            for (Voice v : voices) {
                Locale l = v.getLocale();
                if (l == null || !"en".equals(l.getLanguage())) continue;
                if (v.isNetworkConnectionRequired()) continue;
                if (v.getFeatures() != null && v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) continue;
                int score = "US".equals(l.getCountry()) ? 3 : "GB".equals(l.getCountry()) ? 2 : 1;
                if (score > bestScore) {
                    best = v;
                    bestScore = score;
                }
            }
            if (best != null) tts.setVoice(best);
        } catch (Throwable t) {
            // The default voice of the language is fine.
        }
    }

    /** Says the text. While the engine is still starting, the newest text waits for it. A running sentence is replaced. */
    synchronized void speak(String text) {
        if (closed || text == null || text.length() == 0) return;
        if (INIT.equals(status)) {
            pending = text;
            return;
        }
        if (!READY.equals(status) || tts == null) return;
        try {
            current = "hm" + (++counter);
            requestFocus();
            acquireLock();
            int r = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, current);
            if (r != TextToSpeech.SUCCESS) {
                RunLog.add("voice:speak-fail");
                spoken(current);
            }
        } catch (Throwable t) {
            RunLog.add("voice:speak-error " + RunLog.describe(t));
            spoken(current);
        }
    }

    private synchronized void spoken(String id) {
        if (id == null || !id.equals(current)) return;     // an older sentence that was replaced
        current = null;
        releaseAudio();
        if (oneShot) shutdown();
    }

    synchronized void shutdown() {
        closed = true;
        pending = null;
        current = null;
        releaseAudio();
        closeEngine();
    }

    private void closeEngine() {
        if (tts == null) return;
        try {
            tts.stop();
            tts.shutdown();
        } catch (Throwable ignored) {
        }
        tts = null;
    }

    private void finish(String s) {
        status = s;
        closeEngine();
        notifyListener(s);
    }

    private void notifyListener(String s) {
        if (listener == null) return;
        try {
            listener.onStatus(s);
        } catch (Throwable ignored) {
        }
    }

    private void requestFocus() {
        if (audio == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                if (focusRequest == null) {
                    focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setAudioAttributes(attrs)
                        .setOnAudioFocusChangeListener(focusListener)
                        .build();
                }
                audio.requestAudioFocus(focusRequest);
            } else {
                audio.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            }
        } catch (Throwable ignored) {
        }
    }

    private void acquireLock() {
        if (power == null) return;
        try {
            if (lock == null) {
                lock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "halbmarathon:stimme");
                lock.setReferenceCounted(false);
            }
            lock.acquire(LOCK_MS);
        } catch (Throwable ignored) {
        }
    }

    private void releaseAudio() {
        try {
            if (lock != null && lock.isHeld()) lock.release();
        } catch (Throwable ignored) {
        }
        if (audio == null) return;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                if (focusRequest != null) audio.abandonAudioFocusRequest(focusRequest);
            } else {
                audio.abandonAudioFocus(focusListener);
            }
        } catch (Throwable ignored) {
        }
    }
}
