package ch.rmy.android.http_shortcuts;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.farbod.labelledspinner.LabelledSpinner;

import net.dinglisch.ipack.IpackKeys;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import ch.rmy.android.http_shortcuts.http.HttpRequester;
import ch.rmy.android.http_shortcuts.icons.IconSelector;
import ch.rmy.android.http_shortcuts.listeners.OnIconSelectedListener;
import ch.rmy.android.http_shortcuts.shortcuts.Header;
import ch.rmy.android.http_shortcuts.shortcuts.HeaderAdapter;
import ch.rmy.android.http_shortcuts.shortcuts.PostParameter;
import ch.rmy.android.http_shortcuts.shortcuts.PostParameterAdapter;
import ch.rmy.android.http_shortcuts.shortcuts.Shortcut;
import ch.rmy.android.http_shortcuts.shortcuts.ShortcutStorage;

/**
 * The activity to create/edit shortcuts.
 *
 * @author Roland Meyer
 */
@SuppressLint("InflateParams")
public class EditorActivity extends BaseActivity implements OnClickListener, LabelledSpinner.OnItemChosenListener, OnItemClickListener, TextWatcher {

    public final static String EXTRA_SHORTCUT_ID = "shortcut_id";
    private final static int SELECT_ICON = 1;
    private final static int SELECT_IPACK_ICON = 3;
    public final static int EDIT_SHORTCUT = 2;

    private ShortcutStorage shortcutStorage;
    private Shortcut shortcut;
    private PostParameterAdapter postParameterAdapter;
    private HeaderAdapter customHeaderAdapter;

    @Bind(R.id.input_method)
    LabelledSpinner methodView;
    @Bind(R.id.input_feedback)
    LabelledSpinner feedbackView;
    @Bind(R.id.input_timeout)
    LabelledSpinner timeoutView;
    @Bind(R.id.input_retry_policy)
    LabelledSpinner retryPolicyView;
    @Bind(R.id.input_shortcut_name)
    EditText nameView;
    @Bind(R.id.input_description)
    EditText descriptionView;
    @Bind(R.id.input_url)
    EditText urlView;
    @Bind(R.id.input_username)
    EditText usernameView;
    @Bind(R.id.input_password)
    EditText passwordView;
    @Bind(R.id.input_icon)
    ImageView iconView;
    @Bind(R.id.post_params_container)
    LinearLayout postParamsContainer;
    @Bind(R.id.post_parameter_list)
    ListView postParameterList;
    @Bind(R.id.button_add_post_param)
    Button postParameterAddButton;
    @Bind(R.id.custom_headers_list)
    ListView customHeaderList;
    @Bind(R.id.button_add_custom_header)
    Button customHeaderAddButton;
    @Bind(R.id.input_custom_body)
    EditText customBodyView;
    @Bind(R.id.custom_body_container)
    LinearLayout customBodyContainer;

    private String selectedMethod;
    private int selectedFeedback;
    private String selectedIcon;
    private int selectedTimeout;
    private int selectedRetryPolicy;

    private boolean hasChanges;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        shortcutStorage = new ShortcutStorage(this);
        long shortcutID = getIntent().getLongExtra(EXTRA_SHORTCUT_ID, 0);
        if (shortcutID == 0) {
            shortcut = shortcutStorage.createShortcut();
        } else {
            shortcut = shortcutStorage.getShortcutByID(shortcutID);
        }

        nameView.setText(shortcut.getName());
        descriptionView.setText(shortcut.getDescription());
        urlView.setText(shortcut.getProtocol() + "://" + shortcut.getURL());
        usernameView.setText(shortcut.getUsername());
        passwordView.setText(shortcut.getPassword());
        customBodyView.setText(shortcut.getBodyContent());

        nameView.addTextChangedListener(this);
        descriptionView.addTextChangedListener(this);
        urlView.addTextChangedListener(this);
        usernameView.addTextChangedListener(this);
        passwordView.addTextChangedListener(this);
        customBodyView.addTextChangedListener(this);

        selectedMethod = shortcut.getMethod();
        methodView.setItemsArray(Shortcut.METHODS);
        hideErrorLabel(methodView);
        for (int i = 0; i < Shortcut.METHODS.length; i++) {
            if (Shortcut.METHODS[i].equals(shortcut.getMethod())) {
                methodView.setSelection(i);
                break;
            }
        }
        methodView.setOnItemChosenListener(this);

        if (selectedMethod.equals(Shortcut.METHOD_GET)) {
            postParamsContainer.setVisibility(View.GONE);
        } else {
            postParamsContainer.setVisibility(View.VISIBLE);
        }
        postParameterAdapter = new PostParameterAdapter(this);
        postParameterList.setAdapter(postParameterAdapter);
        postParameterAdapter.addAll(shortcutStorage.getPostParametersByID(shortcutID));
        postParameterAddButton.setOnClickListener(this);
        postParameterList.setOnItemClickListener(this);
        if (postParameterAdapter.getCount() == 0) {
            customBodyContainer.setVisibility(View.VISIBLE);
        } else {
            customBodyContainer.setVisibility(View.GONE);
        }

        customHeaderAdapter = new HeaderAdapter(this);
        customHeaderList.setAdapter(customHeaderAdapter);
        customHeaderAdapter.addAll(shortcutStorage.getHeadersByID(shortcutID));
        customHeaderAddButton.setOnClickListener(this);
        customHeaderList.setOnItemClickListener(this);

        String[] feedbackStrings = new String[Shortcut.FEEDBACK_OPTIONS.length];
        for (int i = 0; i < Shortcut.FEEDBACK_OPTIONS.length; i++) {
            feedbackStrings[i] = getText(Shortcut.FEEDBACK_RESOURCES[i]).toString();
        }
        feedbackView.setOnItemChosenListener(this);
        feedbackView.setItemsArray(feedbackStrings);
        hideErrorLabel(feedbackView);
        for (int i = 0; i < Shortcut.FEEDBACK_OPTIONS.length; i++) {
            if (Shortcut.FEEDBACK_OPTIONS[i] == shortcut.getFeedback()) {
                feedbackView.setSelection(i);
                break;
            }
        }
        selectedFeedback = shortcut.getFeedback();

        String[] timeoutStrings = new String[Shortcut.TIMEOUT_OPTIONS.length];
        for (int i = 0; i < Shortcut.TIMEOUT_OPTIONS.length; i++) {
            timeoutStrings[i] = String.format(getText(Shortcut.TIMEOUT_RESOURCES[i]).toString(), Shortcut.TIMEOUT_OPTIONS[i] / 1000);
        }
        timeoutView.setOnItemChosenListener(this);
        timeoutView.setItemsArray(timeoutStrings);
        hideErrorLabel(timeoutView);
        for (int i = 0; i < Shortcut.TIMEOUT_OPTIONS.length; i++) {
            if (Shortcut.TIMEOUT_OPTIONS[i] == shortcut.getTimeout()) {
                timeoutView.setSelection(i);
                break;
            }
        }
        selectedTimeout = shortcut.getTimeout();

        String[] retryPolicyStrings = new String[Shortcut.RETRY_POLICY_OPTIONS.length];
        for (int i = 0; i < Shortcut.RETRY_POLICY_OPTIONS.length; i++) {
            retryPolicyStrings[i] = getText(Shortcut.RETRY_POLICY_RESOURCES[i]).toString();
        }
        retryPolicyView.setOnItemChosenListener(this);
        retryPolicyView.setItemsArray(retryPolicyStrings);
        hideErrorLabel(retryPolicyView);
        for (int i = 0; i < Shortcut.RETRY_POLICY_OPTIONS.length; i++) {
            if (Shortcut.RETRY_POLICY_OPTIONS[i] == shortcut.getRetryPolicy()) {
                retryPolicyView.setSelection(i);
                break;
            }
        }
        selectedRetryPolicy = shortcut.getRetryPolicy();

        iconView.setImageURI(shortcut.getIconURI(this));
        if (shortcut.getIconName() != null && shortcut.getIconName().startsWith("white_")) {
            iconView.setBackgroundColor(0xFF000000);
        } else {
            iconView.setBackgroundColor(0);
        }
        iconView.setOnClickListener(this);
        selectedIcon = shortcut.getIconName();

        if (shortcut.isNew()) {
            setTitle(R.string.create_shortcut);
        } else {
            setTitle(R.string.edit_shortcut);
        }

        hasChanges = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        setListViewHeightBasedOnChildren(postParameterList);
        setListViewHeightBasedOnChildren(customHeaderList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected int getNavigateUpIcon() {
        return R.drawable.ic_clear;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                confirmClose();
                return true;
            case R.id.action_save_shortcut:
                compileShortcut(false);
                return true;
            case R.id.action_test_shortcut:
                compileShortcut(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(iconView)) {
            openIconSelectionDialog();
        } else if (v.equals(postParameterAddButton)) {


            (new MaterialDialog.Builder(this))
                    .customView(R.layout.dialog_edit_post_parameter, false)
                    .title(R.string.title_post_param_edit)
                    .positiveText(R.string.dialog_ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            EditText keyField = (EditText) dialog.findViewById(R.id.input_post_param_key);
                            EditText valueField = (EditText) dialog.findViewById(R.id.input_post_param_value);
                            if (!keyField.getText().toString().isEmpty()) {
                                PostParameter parameter = new PostParameter(0, keyField.getText().toString(), valueField.getText().toString());
                                postParameterAdapter.add(parameter);
                                setListViewHeightBasedOnChildren(postParameterList);
                                customBodyContainer.setVisibility(View.GONE);
                            }
                        }
                    })
                    .negativeText(R.string.dialog_cancel)
                    .show();
        } else if (v.equals(customHeaderAddButton)) {

            (new MaterialDialog.Builder(this))
                    .customView(R.layout.dialog_edit_custom_header, false)
                    .title(R.string.title_custom_header_edit)
                    .positiveText(R.string.dialog_ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            EditText keyField = (EditText) dialog.findViewById(R.id.input_custom_header_key);
                            EditText valueField = (EditText) dialog.findViewById(R.id.input_custom_header_value);
                            if (!keyField.getText().toString().isEmpty()) {
                                Header header = new Header(0, keyField.getText().toString(), valueField.getText().toString());
                                customHeaderAdapter.add(header);
                                setListViewHeightBasedOnChildren(customHeaderList);
                            }
                        }
                    })
                    .negativeText(R.string.dialog_cancel)
                    .show();
        }
    }

    private void openIconSelectionDialog() {
        (new MaterialDialog.Builder(this))
                .title(R.string.change_icon)
                .items(R.array.context_menu_choose_icon)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                        switch (which) {
                            case 0:
                                openBuiltInIconSelectionDialog();
                                return;
                            case 1:
                                openImagePicker();
                                return;
                            case 2:
                                openIpackPicker();
                                return;
                        }
                    }
                })
                .show();
    }

    private void openBuiltInIconSelectionDialog() {
        IconSelector iconSelector = new IconSelector(this, new OnIconSelectedListener() {

            @Override
            public void onIconSelected(String resourceName) {
                iconView.setImageResource(getResources().getIdentifier(resourceName, "drawable", getPackageName()));

                if (resourceName.startsWith("white_")) {
                    iconView.setBackgroundColor(Color.BLACK);
                } else {
                    iconView.setBackgroundColor(Color.TRANSPARENT);
                }

                selectedIcon = resourceName;
                hasChanges = true;
            }

        });
        iconSelector.show();
    }

    private void openImagePicker() {
        // Workaround for Kitkat (thanks to http://stackoverflow.com/a/20186938/1082111)
        Intent imageIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imageIntent.setType("image/*");
        startActivityForResult(imageIntent, SELECT_ICON);
    }

    private void openIpackPicker() {
        Intent iconIntent = Intent.createChooser(new Intent(IpackKeys.Actions.ICON_SELECT), getText(R.string.choose_ipack));
        startActivityForResult(iconIntent, SELECT_IPACK_ICON);
    }

    @Override
    public void onBackPressed() {
        confirmClose();
    }

    private void confirmClose() {
        if (hasChanges) {
            (new MaterialDialog.Builder(this))
                    .content(R.string.confirm_discard_changes_message)
                    .positiveText(R.string.dialog_discard)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            cancelAndClose();
                        }
                    })
                    .negativeText(R.string.dialog_cancel)
                    .show();
        } else {
            cancelAndClose();
        }
    }

    private void compileShortcut(boolean testOnly) {

        // Validation
        if (nameView.getText().toString().matches("^\\s*$")) {
            if (testOnly) {
                shortcut.setName("Shortcut");
            } else {
                nameView.setError(getText(R.string.validation_name_not_empty));
                nameView.requestFocus();
                return;
            }
        } else {
            shortcut.setName(nameView.getText().toString().trim());
        }

        String url = urlView.getText().toString();
        if (urlView.getText().length() == 0 || url.equalsIgnoreCase("http://") || url.equalsIgnoreCase("https://") || !(URLUtil.isHttpUrl(url) || URLUtil.isHttpsUrl(url))) {
            urlView.setError(getText(R.string.validation_url_invalid));
            urlView.requestFocus();
            return;
        }

        String protocol;
        if (URLUtil.isHttpsUrl(url)) {
            protocol = Shortcut.PROTOCOL_HTTPS;
            url = url.substring(8);
        } else {
            protocol = Shortcut.PROTOCOL_HTTP;
            url = url.substring(7);
        }

        shortcut.setURL(url);
        shortcut.setProtocol(protocol);
        shortcut.setMethod(selectedMethod);
        shortcut.setDescription(descriptionView.getText().toString().trim());
        shortcut.setPassword(passwordView.getText().toString());
        shortcut.setUsername(usernameView.getText().toString());
        shortcut.setIconName(selectedIcon);
        shortcut.setBodyContent(customBodyView.getText().toString());
        shortcut.setFeedback(selectedFeedback);
        shortcut.setTimeout(selectedTimeout);
        shortcut.setRetryPolicy(selectedRetryPolicy);

        List<PostParameter> parameters = new ArrayList<PostParameter>();
        for (int i = 0; i < postParameterAdapter.getCount(); i++) {
            parameters.add(postParameterAdapter.getItem(i));
        }
        List<Header> headers = new ArrayList<Header>();
        for (int i = 0; i < customHeaderAdapter.getCount(); i++) {
            headers.add(customHeaderAdapter.getItem(i));
        }

        if (testOnly) {
            HttpRequester.executeShortcut(this, shortcut, parameters, headers);

        } else {
            long shortcutID = shortcutStorage.storeShortcut(shortcut);

            shortcutStorage.storePostParameters(shortcutID, parameters);
            shortcutStorage.storeHeaders(shortcutID, headers);

            Intent returnIntent = new Intent();
            returnIntent.putExtra(EXTRA_SHORTCUT_ID, shortcutID);
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    }

    private void cancelAndClose() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_ICON) {

                String iconName = Integer.toHexString((int) Math.floor(Math.random() * 1000000)) + ".png";

                InputStream in = null;
                OutputStream out = null;
                try {
                    in = getContentResolver().openInputStream(intent.getData());
                    Bitmap bitmap = BitmapFactory.decodeStream(in);
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 96, 96, false);
                    if (bitmap != resizedBitmap) {
                        bitmap.recycle();
                    }

                    out = openFileOutput(iconName, 0);
                    resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    iconView.setImageBitmap(resizedBitmap);
                    iconView.setBackgroundColor(0);
                    out.flush();

                    selectedIcon = iconName;
                    hasChanges = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    iconView.setImageResource(Shortcut.DEFAULT_ICON);
                    iconView.setBackgroundColor(0);
                    selectedIcon = null;
                    hasChanges = true;
                    showSnackbar(getString(R.string.error_set_image));
                } finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException e) {
                    }
                }
            } else if (requestCode == SELECT_IPACK_ICON) {
                String ipackageName = intent.getData().getAuthority();
                int id = intent.getIntExtra(IpackKeys.Extras.ICON_ID, -1);
                Uri uri = Uri.parse("android.resource://" + ipackageName + "/" + id);
                iconView.setImageURI(uri);
                iconView.setBackgroundColor(0);

                selectedIcon = uri.toString();
                hasChanges = true;
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (parent.equals(postParameterList)) {
            final PostParameter parameter = postParameterAdapter.getItem(position);

            Dialog dialog = (new MaterialDialog.Builder(this))
                    .customView(R.layout.dialog_edit_post_parameter, false)
                    .title(R.string.title_post_param_edit)
                    .positiveText(R.string.dialog_ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            EditText keyField = (EditText) dialog.findViewById(R.id.input_post_param_key);
                            EditText valueField = (EditText) dialog.findViewById(R.id.input_post_param_value);
                            if (!keyField.getText().toString().isEmpty()) {
                                parameter.setKey(keyField.getText().toString());
                                parameter.setValue(valueField.getText().toString());
                                postParameterAdapter.notifyDataSetChanged();
                                hasChanges = true;
                            }
                        }
                    })
                    .neutralText(R.string.dialog_remove)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            postParameterAdapter.remove(parameter);
                            setListViewHeightBasedOnChildren(postParameterList);
                            hasChanges = true;
                            if (postParameterAdapter.getCount() == 0) {
                                customBodyContainer.setVisibility(View.VISIBLE);
                            } else {
                                customBodyContainer.setVisibility(View.GONE);
                            }
                        }
                    })
                    .negativeText(R.string.dialog_cancel)
                    .build();

            EditText keyField = (EditText) dialog.findViewById(R.id.input_post_param_key);
            keyField.setText(parameter.getKey());

            EditText valueField = (EditText) dialog.findViewById(R.id.input_post_param_value);
            valueField.setText(parameter.getValue());

            dialog.show();

        } else if (parent.equals(customHeaderList)) {

            final Header header = customHeaderAdapter.getItem(position);

            Dialog dialog = (new MaterialDialog.Builder(this))
                    .customView(R.layout.dialog_edit_custom_header, false)
                    .title(R.string.title_custom_header_edit)
                    .positiveText(R.string.dialog_ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            EditText keyField = (EditText) dialog.findViewById(R.id.input_custom_header_key);
                            EditText valueField = (EditText) dialog.findViewById(R.id.input_custom_header_value);
                            if (!keyField.getText().toString().isEmpty()) {
                                header.setKey(keyField.getText().toString());
                                header.setValue(valueField.getText().toString());
                                customHeaderAdapter.notifyDataSetChanged();
                                hasChanges = true;
                            }
                        }
                    }).neutralText(R.string.dialog_remove)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            customHeaderAdapter.remove(header);
                            setListViewHeightBasedOnChildren(customHeaderList);
                            hasChanges = true;
                        }
                    })
                    .negativeText(R.string.dialog_cancel)
                    .build();

            EditText keyField = (EditText) dialog.findViewById(R.id.input_custom_header_key);
            keyField.setText(header.getKey());

            EditText valueField = (EditText) dialog.findViewById(R.id.input_custom_header_value);
            valueField.setText(header.getValue());

            dialog.show();

        }
    }

    @Override
    public void onItemChosen(View view, AdapterView<?> parent, View itemView, int position, long id) {
        switch (view.getId()) {
            case R.id.input_method:
                selectedMethod = Shortcut.METHODS[position];
                if (!selectedMethod.equals(shortcut.getMethod())) {
                    hasChanges = true;
                }

                if (selectedMethod.equals(Shortcut.METHOD_GET)) {
                    postParamsContainer.setVisibility(View.GONE);
                } else {
                    postParamsContainer.setVisibility(View.VISIBLE);
                }

                break;
            case R.id.input_feedback:
                selectedFeedback = Shortcut.FEEDBACK_OPTIONS[position];
                if (selectedFeedback != shortcut.getFeedback()) {
                    hasChanges = true;
                }
                break;
            case R.id.input_timeout:
                selectedTimeout = Shortcut.TIMEOUT_OPTIONS[position];
                if (selectedTimeout != shortcut.getTimeout()) {
                    hasChanges = true;
                }
                break;
            case R.id.input_retry_policy:
                selectedRetryPolicy = Shortcut.RETRY_POLICY_OPTIONS[position];
                if (selectedRetryPolicy != shortcut.getRetryPolicy()) {
                    hasChanges = true;
                }
                break;
        }
    }

    @Override
    public void onNothingChosen(View labelledSpinner, AdapterView<?> adapterView) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        hasChanges = true;
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    /**
     * Method for Setting the Height of the ListView dynamically. Hack to fix the issue of not showing all the items of the ListView when placed inside a ScrollView.
     *
     * @param listView
     */
    private void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = MeasureSpec.makeMeasureSpec(listView.getWidth(), MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    private void hideErrorLabel(LabelledSpinner spinner) {
        spinner.getChildAt(3).setVisibility(View.GONE);
    }

}
