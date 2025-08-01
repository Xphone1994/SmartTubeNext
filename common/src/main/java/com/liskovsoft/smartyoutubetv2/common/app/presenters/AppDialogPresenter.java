package com.liskovsoft.smartyoutubetv2.common.app.presenters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.liskovsoft.sharedutils.misc.WeakHashSet;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.base.BasePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.AppDialogView;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class AppDialogPresenter extends BasePresenter<AppDialogView> {
    @SuppressLint("StaticFieldLeak")
    private static AppDialogPresenter sInstance;
    private final Handler mHandler;
    private final Runnable mCloseDialog = this::closeDialog;
    private final WeakHashSet<Runnable> mOnStart = new WeakHashSet<>();
    private final WeakHashSet<Runnable> mOnFinish = new WeakHashSet<>();
    private String mTitle;
    private long mTimeoutMs;
    private boolean mIsTransparent;
    private boolean mIsOverlay;
    private List<OptionCategory> mCategories;
    private boolean mIsExpandable = true;
    private int mId;

    private String mBackupTitle;
    private List<OptionCategory> mBackupCategories;
    private int mBackupId;
    private boolean mBackupIsTransparent;
    private boolean mBackupIsOverlay;
    private boolean mBackupIsExpandable;

    public AppDialogPresenter(Context context) {
        super(context);
        mCategories = new ArrayList<>();
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static AppDialogPresenter instance(Context context) {
        if (sInstance == null) {
            sInstance = new AppDialogPresenter(context);
        }

        sInstance.setContext(context);

        return sInstance;
    }

    /**
     * Called when user pressed back button.
     */
    @Override
    public void onFinish() {
        super.onFinish();
        clear();

        Utils.runMyCallbacks(mOnFinish);
    }

    private void clear() {
        mTimeoutMs = 0;
        mHandler.removeCallbacks(mCloseDialog);
        resetData();
    }

    /**
     * Doubled items fix / Empty dialog fix (multiple overlapped dialogs)
     */
    private void backupData() {
        mBackupCategories = mCategories;
        mBackupTitle = mTitle;
        mBackupId = mId;
        mBackupIsExpandable = mIsExpandable;
        mBackupIsTransparent = mIsTransparent;
        mBackupIsOverlay = mIsOverlay;
    }

    private void resetData() {
        mCategories = new ArrayList<>();
        mIsExpandable = true;
        mIsTransparent = false;
        mIsOverlay = false;
        mId = 0;
        mTitle = null;
    }

    @Override
    public void onViewInitialized() {
        getView().show(mBackupCategories, mBackupTitle, mBackupIsExpandable, mBackupIsTransparent, mBackupIsOverlay, mBackupId);
        Utils.runMyCallbacks(mOnStart);
    }

    /**
     * Called after {@link #onFinish}
     */
    @Override
    public void onViewDestroyed() {
        super.onViewDestroyed();
        clear();
    }

    public void showDialog() {
        showDialog(null, null);
    }

    public void showDialog(String dialogTitle) {
        showDialog(dialogTitle, null);
    }

    public void showDialog(Runnable onFinish) {
        showDialog(null, onFinish);
    }

    public void showDialog(String dialogTitle, Runnable onFinish) {
        mTitle = dialogTitle;
        mOnFinish.add(onFinish);
        
        backupData(); // overlapped dialog fix
        resetData(); // prepare to new call

        if (getView() != null) {
            onViewInitialized();
        }

        getViewManager().startView(AppDialogView.class, true);

        setupTimeout();
    }

    public void closeDialog() {
        if (getView() != null) {
            getView().finish();
        }
    }

    public void goBack() {
        if (getView() != null) {
            getView().goBack();
        }
    }

    public void clearBackstack() {
        if (getView() != null) {
            getView().clearBackstack();
        }
    }

    public boolean isDialogShown() {
        // Also check that current dialog almost closed (new view start is pending from a menu item)
        // Hmm. Maybe current dialog is pending. Check that view is null.
        // Also check that we aren't started the same view (nested dialog).
        return (getViewManager().isVisible(getView()) && getView() != null && !getView().isPaused()) ||
                getViewManager().isViewPending(AppDialogView.class);
    }

    public boolean isCommentsDialogShown() {
        return isDialogShown() && isTypeComments();
    }

    public void appendCategory(OptionCategory category) {
        mCategories.add(category);
    }

    public void appendRadioCategory(String categoryTitle, List<OptionItem> items) {
        mCategories.add(OptionCategory.radioList(categoryTitle, items));
    }

    public void appendCheckedCategory(String categoryTitle, List<OptionItem> items) {
        mCategories.add(OptionCategory.checkedList(categoryTitle, items));
    }

    public void appendStringsCategory(String categoryTitle, List<OptionItem> items) {
        mCategories.add(OptionCategory.stringList(categoryTitle, items));
    }

    public void appendLongTextCategory(String categoryTitle, OptionItem item) {
        mCategories.add(OptionCategory.longText(categoryTitle, item));
    }

    public void appendChatCategory(String categoryTitle, OptionItem item) {
        mCategories.add(OptionCategory.chat(categoryTitle, item));
    }

    public void appendCommentsCategory(String categoryTitle, OptionItem item) {
        mCategories.add(OptionCategory.comments(categoryTitle, item));
    }

    public void appendSingleSwitch(OptionItem optionItem) {
        mCategories.add(OptionCategory.singleSwitch(optionItem));
    }

    public void appendSingleButton(OptionItem optionItem) {
        mCategories.add(OptionCategory.singleButton(optionItem));
    }

    public void showDialogMessage(String dialogTitle, Runnable onClose, int timeoutMs) {
        showDialog(dialogTitle, onClose);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (getView() != null) {
                getView().finish();
            }
        }, timeoutMs);
    }

    public void setCloseTimeoutMs(long timeoutMs) {
        mTimeoutMs = timeoutMs;
    }

    public void setOnStart(Runnable onStart) {
        Utils.addMyCallback(mOnStart, onStart);
    }

    public void setOnFinish(Runnable onFinish) {
        Utils.addMyCallback(mOnFinish, onFinish);
    }

    public void enableTransparent(boolean enable) {
        mIsTransparent = enable;
    }

    /**
     * Close the dialog on movements keys
     */
    public void enableOverlay(boolean enable) {
        mIsOverlay = enable;
    }

    public boolean isTransparent() {
        return getView() != null && getView().isTransparent();
    }

    public boolean isOverlay() {
        return getView() != null && getView().isOverlay();
    }

    public boolean isComments() {
        if (mBackupCategories == null || mBackupCategories.isEmpty()) {
            return false;
        }

        OptionCategory optionCategory = mBackupCategories.get(0);

        return optionCategory.type == OptionCategory.TYPE_COMMENTS;
    }

    /**
     * Show a category contents instead of title if a single category has been added
     */
    public void enableExpandable(boolean enable) {
        mIsExpandable = enable;
    }

    public void setId(int id) {
        mId = id;
    }

    public int getId() {
        return getView() != null ? getView().getViewId() : mId;
    }

    public boolean isEmpty() {
        return mCategories == null || mCategories.isEmpty();
    }

    private void setupTimeout() {
        mHandler.removeCallbacks(mCloseDialog);

        if (mTimeoutMs > 0) {
            mHandler.postDelayed(mCloseDialog, mTimeoutMs);
        }
    }

    private boolean isTypeComments() {
        if (mBackupCategories == null) {
            return false;
        }

        boolean isComments = false;

        for (OptionCategory category : mBackupCategories) {
            if (category.type == OptionCategory.TYPE_COMMENTS) {
                isComments = true;
                break;
            }
        }

        return isComments;
    }
}
