package com.tlab.webkit.gecko;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.io.InputStream;
import java.util.Objects;

import com.tlab.webkit.R;
import com.tlab.widget.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.Autocomplete;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.SlowScriptResponse;

public class PromptDelegate implements GeckoSession.PromptDelegate {

    public interface ShowPromptDelegate {
        void show(AlertDialog.Init init);
    }

    protected static final String TAG = "BasicGeckoViewPrompt";

    private final Activity mActivity;
    private final Fragment mFragment;
    private final ShowPromptDelegate mShowPromptDelegate;

    private int mFileType;
    private GeckoResult<PromptResponse> mFileResponse;
    private FilePrompt mFilePrompt;

    public PromptDelegate(final Activity activity, final Fragment fragment, final ShowPromptDelegate showPromptDelegate) {
        mActivity = activity;
        mFragment = fragment;
        mShowPromptDelegate = showPromptDelegate;
    }

    @Override
    public GeckoResult<PromptResponse> onAlertPrompt(@NonNull final GeckoSession session, @NonNull final AlertPrompt prompt) {
        if (mFragment == null) return GeckoResult.fromValue(prompt.dismiss());
        final AlertDialog.Init init = new AlertDialog.Init(AlertDialog.Init.Reason.PROMPT, prompt.title, prompt.message);
        init.setPositive(mActivity.getResources().getString(android.R.string.ok), null);
        GeckoResult<PromptResponse> res = new GeckoResult<>();
        createStandardDialog(init, prompt, res);
        mShowPromptDelegate.show(init);
        return res;
    }

    @Override
    public GeckoResult<PromptResponse> onButtonPrompt(@NonNull final GeckoSession session, @NonNull final ButtonPrompt prompt) {
        if (mFragment == null) return GeckoResult.fromValue(prompt.dismiss());
        final AlertDialog.Init init = new AlertDialog.Init(AlertDialog.Init.Reason.PROMPT, prompt.title, prompt.message);

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        Resources resources = mActivity.getResources();
        init.setPositive(resources.getString(android.R.string.ok), (result) -> res.complete(prompt.confirm(ButtonPrompt.Type.POSITIVE)));
        init.setNegative(resources.getString(android.R.string.cancel), (result) -> res.complete(prompt.confirm(ButtonPrompt.Type.NEGATIVE)));

        createStandardDialog(init, prompt, res);
        mShowPromptDelegate.show(init);
        return res;
    }

    @Override
    public GeckoResult<PromptResponse> onSharePrompt(@NonNull final GeckoSession session, final SharePrompt prompt) {
        return GeckoResult.fromValue(prompt.dismiss());
    }

    @Nullable
    @Override
    public GeckoResult<PromptResponse> onRepostConfirmPrompt(@NonNull final GeckoSession session, @NonNull final RepostConfirmPrompt prompt) {
        if (mFragment == null) return GeckoResult.fromValue(prompt.dismiss());

        Resources resources = mActivity.getResources();
        final AlertDialog.Init init = new AlertDialog.Init(AlertDialog.Init.Reason.PROMPT, resources.getString(R.string.repost_confirm_title), resources.getString(R.string.repost_confirm_message));

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        init.setPositive(resources.getString(R.string.repost_confirm_resend), (result) -> res.complete(prompt.confirm(AllowOrDeny.ALLOW)));
        init.setNegative(resources.getString(R.string.repost_confirm_cancel), (result) -> res.complete(prompt.confirm(AllowOrDeny.DENY)));

        createStandardDialog(init, prompt, res);
        mShowPromptDelegate.show(init);
        return res;
    }

    @Nullable
    @Override
    public GeckoResult<PromptResponse> onCreditCardSave(@NonNull GeckoSession session, @NonNull AutocompleteRequest<Autocomplete.CreditCardSaveOption> request) {
        Log.i(TAG, "onCreditCardSave " + request.options[0].value);
        return null;
    }

    @Nullable
    @Override
    public GeckoResult<PromptResponse> onLoginSave(@NonNull GeckoSession session, @NonNull AutocompleteRequest<Autocomplete.LoginSaveOption> request) {
        Log.i(TAG, "onLoginSave");
        return GeckoResult.fromValue(request.confirm(request.options[0]));
    }

    @Nullable
    @Override
    public GeckoResult<PromptResponse> onBeforeUnloadPrompt(@NonNull final GeckoSession session, @NonNull final BeforeUnloadPrompt prompt) {
        if (mFragment == null) return GeckoResult.fromValue(prompt.dismiss());

        Resources resources = mActivity.getResources();
        final AlertDialog.Init init = new AlertDialog.Init(AlertDialog.Init.Reason.PROMPT, resources.getString(R.string.before_unload_title), resources.getString(R.string.before_unload_message));

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        init.setPositive(resources.getString(R.string.before_unload_leave_page), (result) -> res.complete(prompt.confirm(AllowOrDeny.ALLOW)));
        init.setNegative(resources.getString(R.string.before_unload_stay), (result) -> res.complete(prompt.confirm(AllowOrDeny.DENY)));

        createStandardDialog(init, prompt, res);
        mShowPromptDelegate.show(init);
        return res;
    }

    private void createStandardDialog(final AlertDialog.Init init, final BasePrompt prompt, final GeckoResult<PromptResponse> response) {
        init.setOnDismissListener(() -> {
            if (!prompt.isComplete()) response.complete(prompt.dismiss());
        });
    }

    @Override
    public GeckoResult<PromptResponse> onTextPrompt(@NonNull final GeckoSession session, @NonNull final TextPrompt prompt) {
        if (mFragment == null) return GeckoResult.fromValue(prompt.dismiss());

        final AlertDialog.Init init = new AlertDialog.Init(AlertDialog.Init.Reason.PROMPT);
        TextWidget.Init overlay = new TextWidget.Init(prompt.title, prompt.message);
        overlay.setText(prompt.defaultValue);
        init.setOverlay(overlay);

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        Resources resources = mActivity.getResources();
        init.setNegative(resources.getString(android.R.string.cancel), /* listener */ null);
        init.setPositive(resources.getString(android.R.string.ok), (result) -> {
            try {
                res.complete(prompt.confirm(result.getString("text")));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });

        createStandardDialog(init, prompt, res);
        mShowPromptDelegate.show(init);
        return res;
    }

    @Override
    public GeckoResult<PromptResponse> onAuthPrompt(@NonNull final GeckoSession session, @NonNull final AuthPrompt prompt) {
        if (mFragment == null) return GeckoResult.fromValue(prompt.dismiss());
        final AlertDialog.Init init = new AlertDialog.Init(AlertDialog.Init.Reason.PROMPT, prompt.title, prompt.message);
        final AuthWidget.Init overlay = new AuthWidget.Init();

        final int flags = prompt.authOptions.flags;
        if ((flags & AuthPrompt.AuthOptions.Flags.ONLY_PASSWORD) == 0) {
            overlay.setUsername(prompt.authOptions.username);
            overlay.setFlag(false);
        } else overlay.setFlag(true);

        overlay.setPassword(prompt.authOptions.password);
        overlay.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        init.setOverlay(overlay);

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        Resources resources = mActivity.getResources();
        init.setNegative(resources.getString(android.R.string.cancel), /* listener */ null);
        init.setPositive(resources.getString(android.R.string.ok), (result) -> {
            if ((flags & AuthPrompt.AuthOptions.Flags.ONLY_PASSWORD) == 0) {
                try {
                    res.complete(prompt.confirm(result.getString("username"), result.getString("password")));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    res.complete(prompt.confirm(result.getString("password")));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        createStandardDialog(init, prompt, res);
        mShowPromptDelegate.show(init);

        return res;
    }

    private void addChoiceItems(final int type, final ArrayList<SelectWidget.ModifiableChoice> list, final ChoicePrompt.Choice[] items, final String indent) {
        if (type == ChoicePrompt.Type.MENU) {
            for (final ChoicePrompt.Choice item : items)
                list.add(new SelectWidget.ModifiableChoice(item));
            return;
        }

        for (final ChoicePrompt.Choice item : items) {
            final SelectWidget.ModifiableChoice modItem = new SelectWidget.ModifiableChoice(item);

            final ChoicePrompt.Choice[] children = item.items;

            if (indent != null && children == null) modItem.label = indent + modItem.label;
            list.add(modItem);

            if (children != null) {
                final String newIndent;
                if (type == ChoicePrompt.Type.SINGLE || type == ChoicePrompt.Type.MULTIPLE) {
                    newIndent = (indent != null) ? indent + '\t' : "\t";
                } else {
                    newIndent = null;
                }
                addChoiceItems(type, list, children, newIndent);
            }
        }
    }

    private void onChoicePromptImpl(final GeckoSession session, final String title, final String message, final int type, final ChoicePrompt.Choice[] choices, final ChoicePrompt prompt, final GeckoResult<PromptResponse> res) {
        if (mFragment == null) {
            res.complete(prompt.dismiss());
            return;
        }
        final AlertDialog.Init init = new AlertDialog.Init(AlertDialog.Init.Reason.PROMPT, title, message);

        final ArrayList<SelectWidget.ModifiableChoice> list = new ArrayList<>();

        addChoiceItems(type, list, choices, /* indent */ null);

        if (type == ChoicePrompt.Type.SINGLE || type == ChoicePrompt.Type.MENU) {
            createStandardDialog(init, prompt, res);

            Resources resources = mActivity.getResources();
            init.setPositive(resources.getString(android.R.string.ok), result -> {
                try {
                    final SelectWidget.ModifiableChoice item = list.get(result.getJSONArray("positions").optInt(0));
                    if (type == ChoicePrompt.Type.MENU) {
                        final ChoicePrompt.Choice[] children = item.choice.items;
                        if (children != null) {
                            // Show sub-menu.
                            init.setOnDismissListener(null);
                            init.dismiss();
                            onChoicePromptImpl(session, item.label, /* message */ null, type, children, prompt, res);
                            return;
                        }
                    }
                    res.complete(prompt.confirm(item.choice));
                    init.dismiss();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            });
        } else if (type == ChoicePrompt.Type.MULTIPLE) {
            Resources resources = mActivity.getResources();
            init.setNegative(resources.getString(android.R.string.cancel), /* listener */ null);
            init.setPositive(resources.getString(android.R.string.ok), (result) -> {
                try {
                    final JSONArray positions = result.getJSONArray("positions");
                    ArrayList<String> items = new ArrayList<>(positions.length());
                    for (int i = 0; i < positions.length(); i++) {
                        final SelectWidget.ModifiableChoice item = list.get(positions.optInt(i));
                        items.add(item.choice.id);
                    }
                    res.complete(prompt.confirm(items.toArray(new String[0])));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            });
            createStandardDialog(init, prompt, res);
        } else {
            throw new UnsupportedOperationException();
        }

        SelectWidget.Init overlay = new SelectWidget.Init();
        ArrayList<JSONObject> options = new ArrayList<>();
        list.forEach(c -> options.add(c.toJSON()));
        overlay.set(type, options);
        init.setOverlay(overlay);

        mShowPromptDelegate.show(init); // Show

        prompt.setDelegate(new PromptInstanceDelegate() {
            @Override
            public void onPromptDismiss(@NonNull final BasePrompt prompt) {
                init.dismiss();
            }

            @Override
            public void onPromptUpdate(@NonNull final BasePrompt prompt) {
                init.setOnDismissListener(null);
                init.dismiss();
                final ChoicePrompt newPrompt = (ChoicePrompt) prompt;
                onChoicePromptImpl(session, newPrompt.title, newPrompt.message, newPrompt.type, newPrompt.choices, newPrompt, res);
            }
        });
    }

    @Override
    public GeckoResult<PromptResponse> onChoicePrompt(@NonNull final GeckoSession session, @NonNull final ChoicePrompt prompt) {
        final GeckoResult<PromptResponse> res = new GeckoResult<>();
        onChoicePromptImpl(session, prompt.title, prompt.message, prompt.type, prompt.choices, prompt, res);
        return res;
    }

    private static int parseColor(final String value) {
        try {
            return Color.parseColor(value);
        } catch (final IllegalArgumentException e) {
            return 0;
        }
    }

    @Override
    public GeckoResult<PromptResponse> onColorPrompt(@NonNull final GeckoSession session, @NonNull final ColorPrompt prompt) {
        if (mFragment == null) return GeckoResult.fromValue(prompt.dismiss());
        final AlertDialog.Init init = new AlertDialog.Init(AlertDialog.Init.Reason.PROMPT, prompt.title);

        final int initial = parseColor(prompt.defaultValue /* def */);

        final ColorWidget.Init overlay = new ColorWidget.Init();
        overlay.setColor(initial);
        init.setOverlay(overlay);

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        Resources resources = mActivity.getResources();
        init.setNegative(resources.getString(android.R.string.cancel), /* listener */ null);
        init.setPositive(resources.getString(android.R.string.ok), (result) -> {
            try {
                res.complete(prompt.confirm(String.format("#%06x", 0xffffff & result.getInt("color"))));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });

        mShowPromptDelegate.show(init);

        return res;
    }

    private static Date parseDate(final SimpleDateFormat formatter, final String value, final boolean defaultToNow) {
        try {
            if (value != null && !value.isEmpty()) {
                return formatter.parse(value);
            }
        } catch (final ParseException ignored) {
        }
        return defaultToNow ? new Date() : null;
    }

    @Override
    public GeckoResult<PromptResponse> onDateTimePrompt(@NonNull final GeckoSession session, @NonNull final DateTimePrompt prompt) {
        if (mFragment == null) return GeckoResult.fromValue(prompt.dismiss());
        final SimpleDateFormat formatter = getSimpleDateFormat(prompt);
        final Date minDate = parseDate(formatter, prompt.minValue, /* defaultToNow */ false);
        final Date maxDate = parseDate(formatter, prompt.maxValue, /* defaultToNow */ false);
        final Date date = parseDate(formatter, prompt.defaultValue, /* defaultToNow */ true);
        final Calendar cal = formatter.getCalendar();
        cal.setTime(date);

        final AlertDialog.Init init = new AlertDialog.Init(AlertDialog.Init.Reason.PROMPT);
        final DateTimeWidget.Init overlay = new DateTimeWidget.Init();

        boolean useDate;
        boolean useTime;
        if (prompt.type == DateTimePrompt.Type.DATE || prompt.type == DateTimePrompt.Type.MONTH || prompt.type == DateTimePrompt.Type.WEEK || prompt.type == DateTimePrompt.Type.DATETIME_LOCAL) {
            overlay.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
            if (minDate != null) overlay.setMinDate(minDate.getTime());
            if (maxDate != null) overlay.setMaxDate(maxDate.getTime());
            useDate = true;
        } else {
            useDate = false;
        }

        if (prompt.type == DateTimePrompt.Type.TIME || prompt.type == DateTimePrompt.Type.DATETIME_LOCAL) {
            overlay.setTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
            useTime = true;
        } else {
            useTime = false;
        }

        overlay.setType(prompt.type);

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        Resources resources = mActivity.getResources();
        init.setNegative(resources.getString(android.R.string.cancel), /* listener */ null);
        init.setNeutral(resources.getString(R.string.clear_field), (result) -> res.complete(prompt.confirm("")));
        init.setPositive(resources.getString(android.R.string.ok), (result) -> {
            try {
                if (useDate)
                    cal.set(result.getInt("year"), result.getInt("month") - 1, result.getInt("dayOfMonth"));

                if (useTime) {
                    cal.set(Calendar.HOUR_OF_DAY, result.getInt("hour"));
                    cal.set(Calendar.MINUTE, result.getInt("minutes"));
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            res.complete(prompt.confirm(formatter.format(cal.getTime())));
        });

        init.setOverlay(overlay);

        mShowPromptDelegate.show(init);

        return res;
    }

    private static @NonNull SimpleDateFormat getSimpleDateFormat(DateTimePrompt prompt) {
        final String format;
        if (prompt.type == DateTimePrompt.Type.DATE) {
            format = "yyyy-MM-dd";
        } else if (prompt.type == DateTimePrompt.Type.MONTH) {
            format = "yyyy-MM";
        } else if (prompt.type == DateTimePrompt.Type.WEEK) {
            format = "yyyy-'W'ww";
        } else if (prompt.type == DateTimePrompt.Type.TIME) {
            format = "HH:mm";
        } else if (prompt.type == DateTimePrompt.Type.DATETIME_LOCAL) {
            format = "yyyy-MM-dd'T'HH:mm";
        } else {
            throw new UnsupportedOperationException();
        }

        return new SimpleDateFormat(format, Locale.ROOT);
    }

    @Override
    public GeckoResult<PromptResponse> onFilePrompt(@NonNull GeckoSession session, @NonNull FilePrompt prompt) {
        final Fragment fragment = mFragment;
        if (fragment == null) {
            return GeckoResult.fromValue(prompt.dismiss());
        }

        // Merge all given MIME types into one, using wildcard if needed.
        final Intent intent = getIntent(prompt);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        if (prompt.type == FilePrompt.Type.MULTIPLE) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        assert prompt.mimeTypes != null;
        if (prompt.mimeTypes.length > 0) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, prompt.mimeTypes);
        }

        GeckoResult<PromptResponse> res = new GeckoResult<>();

        try {
            mFileResponse = res;
            mFilePrompt = prompt;
            fragment.startActivityForResult(intent, UnityConnect.REQUEST_FILE_PICKER);
        } catch (final ActivityNotFoundException e) {
            Log.e(TAG, "Cannot launch activity", e);
            return GeckoResult.fromValue(prompt.dismiss());
        }

        return res;
    }

    private static @NonNull Intent getIntent(@NonNull FilePrompt prompt) {
        String mimeType = null;
        String mimeSubtype = null;
        if (prompt.mimeTypes != null) {
            for (final String rawType : prompt.mimeTypes) {
                final String normalizedType = rawType.trim().toLowerCase(Locale.ROOT);
                final int len = normalizedType.length();
                int slash = normalizedType.indexOf('/');
                if (slash < 0) {
                    slash = len;
                }
                final String newType = normalizedType.substring(0, slash);
                final String newSubtype = normalizedType.substring(Math.min(slash + 1, len));
                if (mimeType == null) {
                    mimeType = newType;
                } else if (!mimeType.equals(newType)) {
                    mimeType = "*";
                }
                if (mimeSubtype == null) {
                    mimeSubtype = newSubtype;
                } else if (!mimeSubtype.equals(newSubtype)) {
                    mimeSubtype = "*";
                }
            }
        }

        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType((mimeType != null ? mimeType : "*") + '/' + (mimeSubtype != null ? mimeSubtype : "*"));
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        return intent;
    }

    /**
     * Get the file name from uri.
     * <a href="https://qiita.com/CUTBOSS/items/3476e164b86a63b02b2e">...</a>
     *
     * @param context context
     * @param uri     uri
     * @return file name
     */
    public static String getFileNameFromUri(@NonNull Context context, Uri uri) {
        if (null == uri) return null;

        String scheme = uri.getScheme();

        String fileName = null;
        switch (Objects.requireNonNull(scheme)) {
            case "content":
                String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
                Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        fileName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
                    }
                    cursor.close();
                }
                break;

            case "file":
                fileName = new File(uri.getPath()).getName();
                break;

            default:
                break;
        }
        return fileName;
    }

    public static File uri2Temp(@NonNull Context context, Uri uri) {
        try {
            final String dirToCopy = "tlabaltoh";
            File cacheDir = new File(context.getCacheDir(), dirToCopy);

            if (!cacheDir.exists()) {
                boolean result = cacheDir.mkdirs();
            }

            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            assert inputStream != null;

            final File tempFile = new File(context.getCacheDir(), dirToCopy + "/" + getFileNameFromUri(context, uri));
            tempFile.deleteOnExit();

            // https://developer.android.com/privacy-and-security/risks/untrustworthy-contentprovider-provided-filename?hl=ja#java
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }

            inputStream.close();

            return tempFile;
        } catch (IOException e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            throw new RuntimeException(e);
        }
    }

    public void onFileCallbackResult(final int resultCode, final Intent data) {
        Activity activity = mActivity;
        if ((activity == null) || (mFileResponse == null)) return;
        
        final GeckoResult<PromptResponse> res = mFileResponse;
        mFileResponse = null;

        final FilePrompt prompt = mFilePrompt;
        mFilePrompt = null;

        if (resultCode != Activity.RESULT_OK || data == null) {
            res.complete(prompt.dismiss());
            return;
        }

        final Uri uri = data.getData();
        final ClipData clip = data.getClipData();

        if (prompt.type == FilePrompt.Type.SINGLE || (prompt.type == FilePrompt.Type.MULTIPLE && clip == null)) {
            assert uri != null;
            res.complete(prompt.confirm(activity, Uri.fromFile(uri2Temp(activity, uri))));
        } else if (prompt.type == FilePrompt.Type.MULTIPLE) {
            final int count = clip.getItemCount();
            final ArrayList<Uri> uris = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                uris.add(Uri.fromFile(uri2Temp(activity, clip.getItemAt(i).getUri())));
            }
            res.complete(prompt.confirm(activity, uris.toArray(new Uri[0])));
        }
    }

    public GeckoResult<Integer> onPermissionPrompt(final GeckoSession session, final String title, final GeckoSession.PermissionDelegate.ContentPermission perm) {
        final GeckoResult<Integer> res = new GeckoResult<>();
        if (mFragment == null) {
            res.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_PROMPT);
            return res;
        }
        final AlertDialog.Init init = new AlertDialog.Init(AlertDialog.Init.Reason.PROMPT, title);
        Resources resources = mActivity.getResources();
        init.setNegative(resources.getString(android.R.string.cancel), (result) -> res.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY));
        init.setPositive(resources.getString(android.R.string.ok), (result) -> res.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW));
        mShowPromptDelegate.show(init);
        return res;
    }

    public void onSlowScriptPrompt(GeckoSession geckoSession, String title, GeckoResult<SlowScriptResponse> reportAction) {
        if (mFragment == null) return;
        final AlertDialog.Init init = new AlertDialog.Init(AlertDialog.Init.Reason.PROMPT, title);
        final String negative = "wait";
        final String positive = "stop";
        init.setNegative(negative, (result) -> reportAction.complete(SlowScriptResponse.CONTINUE));
        init.setPositive(positive, (result) -> reportAction.complete(SlowScriptResponse.STOP));
        mShowPromptDelegate.show(init);
    }

//    private Spinner addMediaSpinner(final Context context, final ViewGroup container, final MediaSource[] sources, final String[] sourceNames) {
//        final ArrayAdapter<MediaSource> adapter = new ArrayAdapter<MediaSource>(context, android.R.layout.simple_spinner_item) {
//            private View convertView(final int position, final View view) {
//                if (view != null) {
//                    final MediaSource item = getItem(position);
//                    ((TextView) view).setText(sourceNames != null ? sourceNames[position] : Objects.requireNonNull(item).name);
//                }
//                return view;
//            }
//
//            @NonNull
//            @Override
//            public View getView(final int position, View view, @NonNull final ViewGroup parent) {
//                return convertView(position, super.getView(position, view, parent));
//            }
//
//            @Override
//            public View getDropDownView(final int position, final View view, @NonNull final ViewGroup parent) {
//                return convertView(position, super.getDropDownView(position, view, parent));
//            }
//        };
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        adapter.addAll(sources);
//
//        final Spinner spinner = new Spinner(context);
//        spinner.setAdapter(adapter);
//        spinner.setSelection(0);
//        container.addView(spinner);
//        return spinner;
//    }
//
//    public void onMediaPrompt(final GeckoSession session, final String title, final MediaSource[] video, final MediaSource[] audio, final String[] videoNames, final String[] audioNames, final GeckoSession.PermissionDelegate.MediaCallback callback) {
//        if (mActivity == null || (video == null && audio == null)) {
//            callback.reject();
//            return;
//        }
//        final Alert init = new Alert();
//        final Widget widget = new Widget(title, "");
//
//        final Spinner videoSpinner;
//        if (video != null) {
//            videoSpinner = addMediaSpinner(init.getContext(), widget, video, videoNames);
//        } else {
//            videoSpinner = null;
//        }
//
//        final Spinner audioSpinner;
//        if (audio != null) {
//            audioSpinner = addMediaSpinner(init.getContext(), widget, audio, audioNames);
//        } else {
//            audioSpinner = null;
//        }
//
//        init.setNegative(CANCEL, /* listener */ null).setPositive(OK, () -> {
//            final MediaSource video1 = (videoSpinner != null) ? (MediaSource) videoSpinner.getSelectedItem() : null;
//            final MediaSource audio1 = (audioSpinner != null) ? (MediaSource) audioSpinner.getSelectedItem() : null;
//            callback.grant(video1, audio1);
//        });
//
//        init.setOnDismissListener(callback::reject);
//        // Show
//    }
//
//    public void onMediaPrompt(final GeckoSession session, final String title, final MediaSource[] video, final MediaSource[] audio, final GeckoSession.PermissionDelegate.MediaCallback callback) {
//        onMediaPrompt(session, title, video, audio, null, null, callback);
//    }

    @Override
    public GeckoResult<PromptResponse> onPopupPrompt(@NonNull final GeckoSession session, final PopupPrompt prompt) {
        return GeckoResult.fromValue(prompt.confirm(AllowOrDeny.ALLOW));
    }
}
