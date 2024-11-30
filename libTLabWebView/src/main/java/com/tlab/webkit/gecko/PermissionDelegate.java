package com.tlab.webkit.gecko;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Fragment;
import android.net.Uri;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import com.tlab.webkit.R;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.WebExtensionController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PermissionDelegate implements GeckoSession.PermissionDelegate {

    private static final String TAG = "PermissionDelegate";

    private final Activity mActivity;
    private final Fragment mFragment;
    private final GeckoRuntime mRuntime;
    private final GeckoSession mSession;

    private Callback mCallback;

    public PermissionDelegate(Activity activity, Fragment fragment, GeckoRuntime runtime, GeckoSession session) {
        mActivity = activity;
        mFragment = fragment;
        mRuntime = runtime;
        mSession = session;
    }

    private final List<Setting<?>> SETTINGS = new ArrayList<>();

    private abstract class Setting<T> {
        private final int mKey;
        private final int mDefaultKey;
        private final boolean mReloadCurrentSession;
        private T mValue;

        public Setting(final int key, final int defaultValueKey, final boolean reloadCurrentSession) {
            mKey = key;
            mDefaultKey = defaultValueKey;
            mReloadCurrentSession = reloadCurrentSession;

            SETTINGS.add(this);
        }

        public void onPrefChange(SharedPreferences pref) {
            Activity activity = mActivity;
            if (activity == null) return;

            final T defaultValue = getDefaultValue(mDefaultKey, activity.getResources());
            final String key = activity.getResources().getString(this.mKey);
            final T value = getValue(key, defaultValue, pref);
            if (!value().equals(value)) {
                setValue(value);
            }
        }

        private void setValue(final T newValue) {
            mValue = newValue;

            if (mSession != null) setValue(mSession.getSettings(), value());

            if (mRuntime != null) {
                setValue(mRuntime.getSettings(), value());
                setValue(mRuntime.getWebExtensionController(), value());
            }

            if (mReloadCurrentSession && mSession != null) {
                mSession.reload();
            }
        }

        public T value() {
            Activity activity = mActivity;
            if (activity == null) return null;
            return mValue == null ? getDefaultValue(mDefaultKey, activity.getResources()) : mValue;
        }

        protected abstract T getDefaultValue(final int key, final Resources res);

        protected abstract T getValue(final String key, final T defaultValue, final SharedPreferences preferences);

        /**
         * Override one of these to define the behavior when this setting changes.
         */
        protected void setValue(final GeckoSessionSettings settings, final T value) {
        }

        protected void setValue(final GeckoRuntimeSettings settings, final T value) {
        }

        protected void setValue(final WebExtensionController controller, final T value) {
        }
    }

    private class BooleanSetting extends Setting<Boolean> {
        public BooleanSetting(final int key, final int defaultValueKey) {
            this(key, defaultValueKey, false);
        }

        public BooleanSetting(final int key, final int defaultValueKey, final boolean reloadCurrentSession) {
            super(key, defaultValueKey, reloadCurrentSession);
        }

        @Override
        protected Boolean getDefaultValue(int key, Resources res) {
            return res.getBoolean(key);
        }

        @Override
        public Boolean getValue(final String key, final Boolean defaultValue, final SharedPreferences preferences) {
            return preferences.getBoolean(key, defaultValue);
        }
    }

    private final BooleanSetting mAllowAutoplay = new BooleanSetting(R.string.key_autoplay, R.bool.autoplay_default, /* reloadCurrentSession */ true);

    private boolean mShowNotificationsRejected;
    private final ArrayList<String> mAcceptedPersistentStorage = new ArrayList<String>();

    class NotificationCallback implements GeckoSession.PermissionDelegate.Callback {
        private final GeckoSession.PermissionDelegate.Callback mCallback;

        NotificationCallback(final GeckoSession.PermissionDelegate.Callback callback) {
            mCallback = callback;
        }

        @Override
        public void reject() {
            mShowNotificationsRejected = true;
            mCallback.reject();
        }

        @Override
        public void grant() {
            mShowNotificationsRejected = false;
            mCallback.grant();
        }
    }

    class PersistentStorageCallback implements GeckoSession.PermissionDelegate.Callback {
        private final GeckoSession.PermissionDelegate.Callback mCallback;
        private final String mUri;

        PersistentStorageCallback(final GeckoSession.PermissionDelegate.Callback callback, String uri) {
            mCallback = callback;
            mUri = uri;
        }

        @Override
        public void reject() {
            mCallback.reject();
        }

        @Override
        public void grant() {
            mAcceptedPersistentStorage.add(mUri);
            mCallback.grant();
        }
    }

    public void onRequestPermissionsResult(final String[] permissions, final int[] grantResults) {
        if (mCallback == null) {
            return;
        }

        final Callback cb = mCallback;
        mCallback = null;
        for (final int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                // At least one permission was not granted.
                cb.reject();
                return;
            }
        }
        cb.grant();
    }

    @Override
    public void onAndroidPermissionsRequest(@NonNull final GeckoSession session, final String[] permissions, @NonNull final Callback callback) {
        // requestPermissions was introduced in API 23.
        mCallback = callback;
        mFragment.requestPermissions(permissions, UnityConnect.REQUEST_PERMISSIONS);
    }

    @Override
    public GeckoResult<Integer> onContentPermissionRequest(@NonNull final GeckoSession session, final ContentPermission perm) {
        final int resId;
        switch (perm.permission) {
            case PERMISSION_GEOLOCATION:
                resId = R.string.request_geolocation;
                break;
            case PERMISSION_DESKTOP_NOTIFICATION:
                resId = R.string.request_notification;
                break;
            case PERMISSION_PERSISTENT_STORAGE:
                resId = R.string.request_storage;
                break;
            case PERMISSION_XR:
                resId = R.string.request_xr;
                break;
            case PERMISSION_AUTOPLAY_AUDIBLE:
            case PERMISSION_AUTOPLAY_INAUDIBLE:
                if (!mAllowAutoplay.value()) {
                    return GeckoResult.fromValue(ContentPermission.VALUE_DENY);
                } else {
                    return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW);
                }
            case PERMISSION_MEDIA_KEY_SYSTEM_ACCESS:
                resId = R.string.request_media_key_system_access;
                break;
            case PERMISSION_STORAGE_ACCESS:
                resId = R.string.request_storage_access;
                break;
            default:
                return GeckoResult.fromValue(ContentPermission.VALUE_DENY);
        }

        final String title = mActivity.getString(resId, Uri.parse(perm.uri).getAuthority());
        final PromptDelegate prompt = (PromptDelegate) mSession.getPromptDelegate();
        assert prompt != null;
        return prompt.onPermissionPrompt(session, title, perm);
    }

    private String[] normalizeMediaName(final MediaSource[] sources) {
        if (sources == null) {
            return null;
        }

        String[] res = new String[sources.length];
        for (int i = 0; i < sources.length; i++) {
            final int mediaSource = sources[i].source;
            final String name = sources[i].name;
            if (MediaSource.SOURCE_CAMERA == mediaSource) {
                assert name != null;
                if (name.toLowerCase(Locale.ROOT).contains("front")) {
                    res[i] = mActivity.getString(R.string.media_front_camera);
                } else {
                    res[i] = mActivity.getString(R.string.media_back_camera);
                }
            } else {
                assert name != null;
                if (!name.isEmpty()) {
                    res[i] = name;
                } else if (MediaSource.SOURCE_MICROPHONE == mediaSource) {
                    res[i] = mActivity.getString(R.string.media_microphone);
                } else {
                    res[i] = mActivity.getString(R.string.media_other);
                }
            }
        }

        return res;
    }

    @Override
    public void onMediaPermissionRequest(@NonNull final GeckoSession session, @NonNull final String uri, final MediaSource[] video, final MediaSource[] audio, @NonNull final MediaCallback callback) {
        // If we don't have device permissions at this point, just automatically reject the request
        // as we will have already have requested device permissions before getting to this point
        // and if we've reached here and we don't have permissions then that means that the user
        // denied them.
        if ((audio != null && ContextCompat.checkSelfPermission(mActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) || (video != null && ContextCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            callback.reject();
            return;
        }

        callback.grant(video != null ? video[0] : null, audio != null ? audio[0] : null);
    }
}