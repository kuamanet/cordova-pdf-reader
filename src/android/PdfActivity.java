package net.kuama.pdf;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.watermark.androidwm_light.WatermarkBuilder;
import com.watermark.androidwm_light.bean.WatermarkText;

import net.kuama.pdf.viewer.MuPDFCore;
import net.kuama.pdf.viewer.OutlineActivity;
import net.kuama.pdf.viewer.PageAdapter;
import net.kuama.pdf.viewer.PageView;
import net.kuama.pdf.viewer.ReaderView;
import net.kuama.pdf.viewer.SearchTask;
import net.kuama.pdf.viewer.SearchTaskResult;

import java.util.ArrayList;
import java.util.Locale;

public class PdfActivity extends AppCompatActivity {

    private String mWatermarkFromExtras;
    private String mTitle;

    /* The core rendering instance */
    enum TopBarMode {
        Main, Search, More
    }

    private final int OUTLINE_REQUEST = 0;
    private MuPDFCore core;
    private String mFileName;
    private ReaderView mDocView;
    private View mButtonsView;
    private boolean mButtonsVisible;
    private TextView mFilenameView;
    private SeekBar mPageSlider;
    private int mPageSliderRes;
    private TextView mPageNumberView;
    private ImageButton mSearchButton;
    private ImageButton mOutlineButton;
    private ViewAnimator mTopBarSwitcher;
    private PdfActivity.TopBarMode mTopBarMode = PdfActivity.TopBarMode.Main;
    private ImageButton mSearchBack;
    private ImageButton mSearchFwd;
    private ImageButton mSearchClose;
    private EditText mSearchText;
    private SearchTask mSearchTask;
    private AlertDialog.Builder mAlertBuilder;
    private ArrayList<OutlineActivity.Item> mFlatOutline;


    private MuPDFCore openBuffer(byte buffer[], String magic) {
        try {
            core = new MuPDFCore(buffer, magic);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
        return core;
    }

    public static class Extras {
        public static final String watermark_extra = PdfActivity.class.getName() + "_watermark";
        public static final String base_64_pdf = PdfActivity.class.getName() + "_base_64_pdf";
        public static final String activity_title = PdfActivity.class.getName() + "_activity_title";
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();

        String base64pdf = null;

        if (extras == null) {
            Log.e(PdfActivity.class.getSimpleName(), "Missing extras");
            return;
        } else {
            base64pdf = extras.getString(Extras.base_64_pdf);

            if (base64pdf == null && PdfReader.BASE_64_DATA != null) {
                base64pdf = PdfReader.BASE_64_DATA;
            }

            if (base64pdf == null) {
                Log.e(PdfActivity.class.getSimpleName(), "Missing " + Extras.base_64_pdf + " extra");
                return;
            }

            mWatermarkFromExtras = extras.getString(Extras.watermark_extra, null);
            mTitle = extras.getString(Extras.activity_title, null);
        }


        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mAlertBuilder = new AlertDialog.Builder(this);

        if (core == null) {

            try {
                byte buffer[] = Base64.decode(base64pdf, Base64.DEFAULT);
                core = openBuffer(buffer, "application/pdf");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (core == null) {
            AlertDialog alert = mAlertBuilder.create();
            int cannotOpenDocumentIdentifier = getResources().getIdentifier("cannot_open_document", "string", getPackageName());
            alert.setTitle(cannotOpenDocumentIdentifier);

            int dismissIdentifier = getResources().getIdentifier("dismiss", "string", getPackageName());
            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(dismissIdentifier),
                    (dialog, which) -> finish());
            alert.setOnCancelListener(dialog -> finish());
            alert.show();
            return;
        }

        createUI(savedInstanceState);
    }

    public void createUI(Bundle savedInstanceState) {
        if (core == null)
            return;

        // Now create the UI.
        // First create the document view
        mDocView = new ReaderView(this) {
            @Override
            protected void onMoveToChild(int i) {
                if (core == null)
                    return;

                mPageNumberView.setText(String.format(Locale.ROOT, "%d / %d", i + 1, core.countPages()));
                mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
                mPageSlider.setProgress(i * mPageSliderRes);
                super.onMoveToChild(i);
            }

            @Override
            protected void onTapMainDocArea() {
                if (!mButtonsVisible) {
                    showButtons();
                } else {
                    if (mTopBarMode == PdfActivity.TopBarMode.Main)
                        hideButtons();
                }
            }

            @Override
            protected void onDocMotion() {
                hideButtons();
            }
        };

        if (mWatermarkFromExtras != null) {
            mDocView.setAdapter(new WatermarkAdapter(this, core, mWatermarkFromExtras));
        } else {
            mDocView.setAdapter(new PageAdapter(this, core));
        }

        mSearchTask = new SearchTask(this, core) {
            @Override
            protected void onTextFound(SearchTaskResult result) {
                SearchTaskResult.set(result);
                // Ask the ReaderView to move to the resulting page
                mDocView.setDisplayedViewIndex(result.pageNumber);
                // Make the ReaderView act on the change to SearchTaskResult
                // via overridden onChildSetup method.
                mDocView.resetupChildren();
            }
        };

        // Make the buttons overlay, and store all its
        // controls in variables
        makeButtonsView();

        // Set up the page slider
        int smax = Math.max(core.countPages() - 1, 1);
        mPageSliderRes = ((10 + smax - 1) / smax) * 2;

        // Set the file-name text
        String docTitle = mTitle;

        if (docTitle == null) {
            docTitle = core.getTitle();
        }

        if (docTitle != null)
            mFilenameView.setText(docTitle);
        else
            mFilenameView.setText(mFileName);

        // Activate the seekbar
        mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                mDocView.pushHistory();
                mDocView.setDisplayedViewIndex((seekBar.getProgress() + mPageSliderRes / 2) / mPageSliderRes);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                updatePageNumView((progress + mPageSliderRes / 2) / mPageSliderRes);
            }
        });

        // Activate the search-preparing button
        mSearchButton.setOnClickListener(v -> searchModeOn());

        mSearchClose.setOnClickListener(v -> searchModeOff());

        // Search invoking buttons are disabled while there is no text specified
        mSearchBack.setEnabled(false);
        mSearchFwd.setEnabled(false);
        mSearchBack.setColorFilter(Color.argb(255, 128, 128, 128));
        mSearchFwd.setColorFilter(Color.argb(255, 128, 128, 128));

        // React to interaction with the text widget
        mSearchText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                boolean haveText = s.toString().length() > 0;
                setButtonEnabled(mSearchBack, haveText);
                setButtonEnabled(mSearchFwd, haveText);

                // Remove any previous search results
                if (SearchTaskResult.get() != null && !mSearchText.getText().toString().equals(SearchTaskResult.get().txt)) {
                    SearchTaskResult.set(null);
                    mDocView.resetupChildren();
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }
        });

        //React to Done button on keyboard
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                    search(1);
                return false;
            }
        });

        mSearchText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)
                    search(1);
                return false;
            }
        });

        // Activate search invoking buttons
        mSearchBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                search(-1);
            }
        });
        mSearchFwd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                search(1);
            }
        });

        if (core.hasOutline()) {
            mOutlineButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mFlatOutline == null)
                        mFlatOutline = core.getOutline();
                    if (mFlatOutline != null) {
                        Intent intent = new Intent(PdfActivity.this, OutlineActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putInt("POSITION", mDocView.getDisplayedViewIndex());
                        bundle.putSerializable("OUTLINE", mFlatOutline);
                        intent.putExtras(bundle);
                        startActivityForResult(intent, OUTLINE_REQUEST);
                    }
                }
            });
        } else {
            mOutlineButton.setVisibility(View.GONE);
        }

        // Reenstate last state if it was recorded
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        mDocView.setDisplayedViewIndex(prefs.getInt("page" + mFileName, 0));

        if (savedInstanceState == null || !savedInstanceState.getBoolean("ButtonsHidden", false))
            showButtons();

        if (savedInstanceState != null && savedInstanceState.getBoolean("SearchMode", false))
            searchModeOn();

        // Stick the document view and the buttons overlay into a parent view
        RelativeLayout layout = new RelativeLayout(this);
        layout.setBackgroundColor(Color.DKGRAY);
        layout.addView(mDocView);
        layout.addView(mButtonsView);
        setContentView(layout);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OUTLINE_REQUEST:
                if (resultCode >= RESULT_FIRST_USER) {
                    mDocView.pushHistory();
                    mDocView.setDisplayedViewIndex(resultCode - RESULT_FIRST_USER);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mFileName != null && mDocView != null) {
            outState.putString("FileName", mFileName);

            // Store current page in the prefs against the file name,
            // so that we can pick it up each time the file is loaded
            // Other info is needed only for screen-orientation change,
            // so it can go in the bundle
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("page" + mFileName, mDocView.getDisplayedViewIndex());
            edit.apply();
        }

        if (!mButtonsVisible)
            outState.putBoolean("ButtonsHidden", true);

        if (mTopBarMode == PdfActivity.TopBarMode.Search)
            outState.putBoolean("SearchMode", true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mSearchTask != null)
            mSearchTask.stop();

        if (mFileName != null && mDocView != null) {
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("page" + mFileName, mDocView.getDisplayedViewIndex());
            edit.apply();
        }
    }

    public void onDestroy() {
        if (mDocView != null) {
            mDocView.applyToChildren(new ReaderView.ViewMapper() {
                public void applyToView(View view) {
                    ((PageView) view).releaseBitmaps();
                }
            });
        }
        if (core != null)
            core.onDestroy();
        core = null;
        super.onDestroy();
    }

    private void setButtonEnabled(ImageButton button, boolean enabled) {
        button.setEnabled(enabled);
        button.setColorFilter(enabled ? Color.argb(255, 255, 255, 255) : Color.argb(255, 128, 128, 128));
    }

    private void showButtons() {
        if (core == null)
            return;
        if (!mButtonsVisible) {
            mButtonsVisible = true;
            // Update page number text and slider
            int index = mDocView.getDisplayedViewIndex();
            updatePageNumView(index);
            mPageSlider.setMax((core.countPages() - 1) * mPageSliderRes);
            mPageSlider.setProgress(index * mPageSliderRes);
            if (mTopBarMode == PdfActivity.TopBarMode.Search) {
                mSearchText.requestFocus();
                showKeyboard();
            }

            Animation anim = new TranslateAnimation(0, 0, -mTopBarSwitcher.getHeight(), 0);
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mTopBarSwitcher.setVisibility(View.VISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                }
            });
            mTopBarSwitcher.startAnimation(anim);

            anim = new TranslateAnimation(0, 0, mPageSlider.getHeight(), 0);
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mPageSlider.setVisibility(View.VISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mPageNumberView.setVisibility(View.VISIBLE);
                }
            });
            mPageSlider.startAnimation(anim);
        }
    }

    private void hideButtons() {
        if (mButtonsVisible) {
            mButtonsVisible = false;
            hideKeyboard();

            Animation anim = new TranslateAnimation(0, 0, 0, -mTopBarSwitcher.getHeight());
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mTopBarSwitcher.setVisibility(View.INVISIBLE);
                }
            });
            mTopBarSwitcher.startAnimation(anim);

            anim = new TranslateAnimation(0, 0, 0, mPageSlider.getHeight());
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mPageNumberView.setVisibility(View.INVISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mPageSlider.setVisibility(View.INVISIBLE);
                }
            });
            mPageSlider.startAnimation(anim);
        }
    }

    private void searchModeOn() {
        if (mTopBarMode != PdfActivity.TopBarMode.Search) {
            mTopBarMode = PdfActivity.TopBarMode.Search;
            //Focus on EditTextWidget
            mSearchText.requestFocus();
            showKeyboard();
            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        }
    }

    private void searchModeOff() {
        if (mTopBarMode == PdfActivity.TopBarMode.Search) {
            mTopBarMode = PdfActivity.TopBarMode.Main;
            hideKeyboard();
            mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
            SearchTaskResult.set(null);
            // Make the ReaderView act on the change to mSearchTaskResult
            // via overridden onChildSetup method.
            mDocView.resetupChildren();
        }
    }

    private void updatePageNumView(int index) {
        if (core == null)
            return;
        mPageNumberView.setText(String.format(Locale.ROOT, "%d / %d", index + 1, core.countPages()));
    }

    private void makeButtonsView() {
        int layoutIdentifier = getResources().getIdentifier("layout/document_activity", null, getPackageName());
        mButtonsView = getLayoutInflater().inflate(layoutIdentifier, null);

        int backButtonIdentifier = getResources().getIdentifier("id/backButton", null, getPackageName());
        ImageButton backBtn = mButtonsView.findViewById(backButtonIdentifier);

        backBtn.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        int docNameTextIdentifier = getResources().getIdentifier("id/docNameText", null, getPackageName());
        mFilenameView = mButtonsView.findViewById(docNameTextIdentifier);

        int pageSliderIdentifier = getResources().getIdentifier("id/pageSlider", null, getPackageName());
        mPageSlider = mButtonsView.findViewById(pageSliderIdentifier);

        int pageNumberIdentifier = getResources().getIdentifier("id/pageNumber", null, getPackageName());
        mPageNumberView = mButtonsView.findViewById(pageNumberIdentifier);

        int searchButtonIdentifier = getResources().getIdentifier("id/searchButton", null, getPackageName());
        mSearchButton = mButtonsView.findViewById(searchButtonIdentifier);

        int outlineButtonIdentifier = getResources().getIdentifier("id/outlineButton", null, getPackageName());
        mOutlineButton = mButtonsView.findViewById(outlineButtonIdentifier);

        int switcherIdentifier = getResources().getIdentifier("id/switcher", null, getPackageName());
        mTopBarSwitcher = mButtonsView.findViewById(switcherIdentifier);

        int searchBackIdentifier = getResources().getIdentifier("id/searchBack", null, getPackageName());
        mSearchBack = mButtonsView.findViewById(searchBackIdentifier);

        int searchForwardIdentifier = getResources().getIdentifier("id/searchForward", null, getPackageName());
        mSearchFwd = mButtonsView.findViewById(searchForwardIdentifier);

        int searchCloseIdentifier = getResources().getIdentifier("id/searchClose", null, getPackageName());
        mSearchClose = mButtonsView.findViewById(searchCloseIdentifier);

        int searchTextIdentifier = getResources().getIdentifier("id/searchText", null, getPackageName());
        mSearchText = mButtonsView.findViewById(searchTextIdentifier);

        mTopBarSwitcher.setVisibility(View.INVISIBLE);
        mPageNumberView.setVisibility(View.INVISIBLE);

        mPageSlider.setVisibility(View.INVISIBLE);
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.showSoftInput(mSearchText, 0);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
    }

    private void search(int direction) {
        hideKeyboard();
        int displayPage = mDocView.getDisplayedViewIndex();
        SearchTaskResult r = SearchTaskResult.get();
        int searchPage = r != null ? r.pageNumber : -1;
        mSearchTask.go(mSearchText.getText().toString(), direction, displayPage, searchPage);
    }

    @Override
    public boolean onSearchRequested() {
        if (mButtonsVisible && mTopBarMode == PdfActivity.TopBarMode.Search) {
            hideButtons();
        } else {
            showButtons();
            searchModeOn();
        }
        return super.onSearchRequested();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mButtonsVisible && mTopBarMode != PdfActivity.TopBarMode.Search) {
            hideButtons();
        } else {
            showButtons();
            searchModeOff();
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (!mDocView.popHistory())
            super.onBackPressed();
    }

    class WatermarkAdapter extends PageAdapter {
        private final Context mContext;
        private final MuPDFCore mCore;
        private final SparseArray<PointF> mPageSizes = new SparseArray<PointF>();
        private Bitmap mSharedHqBm;
        private WatermarkText mWatermarkText;

        public WatermarkAdapter(Context c, MuPDFCore core, String watermarkText) {
            super(c, core);
            mContext = c;
            mCore = core;

            mWatermarkText = new WatermarkText(watermarkText)
                    .setPositionX(0.5)
                    .setPositionY(0.5)
                    .setTextAlpha(60)
                    .setRotation(-30)
                    .setTextSize(20);
        }

        public int getCount() {
            return mCore.countPages();
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        public void releaseBitmaps() {
            //  recycle and release the shared bitmap.
            if (mSharedHqBm != null)
                mSharedHqBm.recycle();
            mSharedHqBm = null;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            final PageView pageView;
            if (convertView == null) {
                if (mSharedHqBm == null || mSharedHqBm.getWidth() != parent.getWidth() || mSharedHqBm.getHeight() != parent.getHeight()) {

                    mSharedHqBm = Bitmap.createBitmap(parent.getWidth(), parent.getHeight(), Bitmap.Config.ARGB_8888);
                }

                pageView = new PageView(mContext, mCore, new Point(parent.getWidth(), parent.getHeight()), mSharedHqBm);
                pageView.setmAfterViewRenderedHandler(new PageView.OnAfterImageRenderedListener() {
                    @Override
                    public void afterRender(ImageView mEntire) {
                        WatermarkBuilder
                                .create(mContext, pageView.getmEntireBm())
                                .loadWatermarkText(mWatermarkText)
                                .setTileMode(true)
                                .getWatermark()
                                .setToImageView(mEntire);
                    }
                });

            } else {
                pageView = (PageView) convertView;
            }

            PointF pageSize = mPageSizes.get(position);
            if (pageSize != null) {
                // We already know the page size. Set it up
                // immediately
                pageView.setPage(position, pageSize);
            } else {
                // Page size as yet unknown. Blank it for now, and
                // start a background task to find the size
                pageView.blank(position);
                AsyncTask<Void, Void, PointF> sizingTask = new AsyncTask<Void, Void, PointF>() {
                    @Override
                    protected PointF doInBackground(Void... arg0) {
                        return mCore.getPageSize(position);
                    }

                    @Override
                    protected void onPostExecute(PointF result) {
                        super.onPostExecute(result);
                        // We now know the page size
                        mPageSizes.put(position, result);
                        // Check that this view hasn't been reused for
                        // another page since we started
                        if (pageView.getPage() == position)
                            pageView.setPage(position, result);
                    }
                };

                sizingTask.execute((Void) null);
            }
            return pageView;
        }
    }

}
