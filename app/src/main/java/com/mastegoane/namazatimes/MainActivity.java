package com.mastegoane.namazatimes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.Toast;
import com.mastegoane.namazatimes.databinding.ActivityMainBinding;
import com.orhanobut.dialogplus.DialogPlus;
import com.orhanobut.dialogplus.DialogPlusBuilder;
import com.orhanobut.dialogplus.Holder;
import com.orhanobut.dialogplus.ViewHolder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public void pickDateClicked(View view) {
        new DatePickerDialog(this, mDatePickerDateSetListener,
                mCalendar.get(Calendar.YEAR),
                mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    public void incrementDateClicked(View view) {
        if (view.getTag().toString().equals("increment")) {
            mCalendar.add(Calendar.DATE, 1);
        } else {
            mCalendar.add(Calendar.DATE, -1);
        }
        mMainViewModel.updateViews();
        updateViews();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMainViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication()))
                .get(MainViewModel.class);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        final DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        mDp2pxFactor = displayMetrics.density;
        mSharedPreferences = this.getSharedPreferences("namazatimes", MODE_PRIVATE);
        mCalendar = mMainViewModel.getCalendar().getValue();

        mFragmentManager = getSupportFragmentManager();
        mDailyTimesFragmentA = new DailyTimesFragmentA();
        mDailyTimesFragmentB = new DailyTimesFragmentB();
        mDailyTimesFragmentC = new DailyTimesFragmentC();
        mCurrentFragment = mSharedPreferences.getInt("sp_current_fragment", 1);
        if (mCurrentFragment < 1 || mCurrentFragment > 3) {
            // BUG
            mCurrentFragment = 1;
        }
        selectFragment(mCurrentFragment);

        mBinding.bottomnavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navShareToWhatsapp:
                    shareScreenshot(false);
                    return true;
                case R.id.navSaveToGallery:
                    shareScreenshot(true);
                    return true;
                case R.id.navSettings:
                    showDialogPlusSettings();
                    return true;
            }
            return false;
        });

        mDatePickerDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // TODO Auto-generated method stub
                mCalendar.set(Calendar.YEAR, year);
                mCalendar.set(Calendar.MONTH, monthOfYear);
                mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                mMainViewModel.updateViews();
                updateViews();
            }
        };

        mMainViewModel.readPrayerTimes();
        updateViews();
    }

    private void showDialogPlusSettings() {
        final boolean expanded = true;
        final int gravity = Gravity.TOP;
        final Holder holder = new ViewHolder(R.layout.dialogplus_content_settings);
        final DialogPlusAdapter adapter = new DialogPlusAdapter(MainActivity.this);
        final DialogPlusBuilder builder = DialogPlus.newDialog(this)
                .setContentHolder(holder);
        builder.setHeader(R.layout.dialogplus_header_settings);
        builder.setFooter(R.layout.dialogplus_footer_settings);
        builder.setCancelable(true)
                .setGravity(gravity)
                .setAdapter(adapter)
                .setContentBackgroundResource(R.color.transparent)
                .setOnClickListener((dialog, view) -> {
                    final int viewId = view.getId();
                    final AppCompatButton buttonA = findViewById(R.id.dialogplus_settings_content_buttonA);
                    final AppCompatButton buttonB = findViewById(R.id.dialogplus_settings_content_buttonB);
                    final AppCompatButton buttonC = findViewById(R.id.dialogplus_settings_content_buttonC);
                    final AppCompatButton buttonSelect = findViewById(R.id.dialogplus_settings_footer_buttonSelect);
                    if (viewId == R.id.dialogplus_settings_content_buttonB) {
                        buttonA.setSelected(false);
                        buttonB.setSelected(true);
                        buttonC.setSelected(false);
                        buttonSelect.setEnabled(true);
                    } else if (viewId == R.id.dialogplus_settings_content_buttonC) {
                        buttonA.setSelected(false);
                        buttonB.setSelected(false);
                        buttonC.setSelected(true);
                        buttonSelect.setEnabled(true);
                    } else if (viewId == R.id.dialogplus_settings_content_buttonA) {
                        buttonA.setSelected(true);
                        buttonB.setSelected(false);
                        buttonC.setSelected(false);
                        buttonSelect.setEnabled(true);
                    } else if (viewId == R.id.dialogplus_settings_footer_buttonSelect) {
                        if (buttonSelect.isEnabled()) {
                            int selectedFragment = 1;
                            if (buttonB.isSelected()) {
                                selectedFragment = 2;
                            } else if (buttonC.isSelected()) {
                                selectedFragment = 3;
                            }
                            final SharedPreferences.Editor sharedPrefEditor = mSharedPreferences.edit();
                            sharedPrefEditor.putInt("sp_current_fragment", selectedFragment);
                            // Note: need to use commit() and not apply() below. Otherwise, the app restarts before data is written down.
                            sharedPrefEditor.commit();
                            selectFragment(selectedFragment);
                            dialog.dismiss();
                        }
                    } else if (viewId == R.id.dialogplus_settings_footer_buttonCancel) {
                        dialog.dismiss();
                    }
                })
                .setContentHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
//                .setContentHeight((int)(800*mDp2pxFactor))
//                .setExpanded(expanded)
                .setExpanded(expanded, (int)(360*mDp2pxFactor))
//                .setExpanded(expanded, 400)
//                .setOnCancelListener(dialog -> toast("cancelled"))
                .setOverlayBackgroundResource(android.R.color.transparent);
        builder.create().show();
    }


    private void selectFragment(int fragmentIndex) {
        final FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        switch (fragmentIndex) {
            case 1:
                fragmentTransaction.replace(R.id.frameLayoutMainDailyTimesFragment, mDailyTimesFragmentA, null);
                break;
            case 2:
                fragmentTransaction.replace(R.id.frameLayoutMainDailyTimesFragment, mDailyTimesFragmentB, null);
                break;
            case 3:
                fragmentTransaction.replace(R.id.frameLayoutMainDailyTimesFragment, mDailyTimesFragmentC, null);
                break;
            default:
                fragmentTransaction.replace(R.id.frameLayoutMainDailyTimesFragment, mDailyTimesFragmentA, null);
                break;

        }
        mCurrentFragment = fragmentIndex;
        fragmentTransaction.commit();
        mMainViewModel.updateViews();
        updateViews();
    }

    private void updateViews() {
        String dateFormat = "d\nMMMM\nyyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
        mBinding.textViewMainDate.setText(simpleDateFormat.format(mCalendar.getTime()));
        final int dayOfWeek = mCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        switch (mCurrentFragment) {
            case 1:
                mBinding.textViewMainDay.setText(mDayNamesAdiga[dayOfWeek] + "\n" + mDayNamesHebrew[dayOfWeek] + "\n" + mDayNamesArabic[dayOfWeek]);
                break;
            case 2:
                mBinding.textViewMainDay.setText(mDayNamesAdiga[dayOfWeek] + "\n" + mDayNamesArabic[dayOfWeek]);
                break;
            case 3:
                mBinding.textViewMainDay.setText(mDayNamesAdiga[dayOfWeek] + "\n");
                break;
            default:
                mBinding.textViewMainDay.setText(mDayNamesAdiga[dayOfWeek] + "\n" + mDayNamesHebrew[dayOfWeek] + "\n" + mDayNamesArabic[dayOfWeek]);
                break;
        }
    }

    private void shareScreenshot(boolean shareToGallery) {
        final ConstraintLayout view = mBinding.constraintLayoutMainFullScreen;
        int width = view.getWidth();
        int height = view.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        File outputDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                Log.e("DIRECTORY_PICTURES", "Failed to create output directory");
                return;
            }
        }
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        final String outputFilePath = outputDir.getAbsolutePath() + File.separator + "question2share.webp";
        Log.d("Writing file", outputFilePath);
        String mediaPath = "";
        try {
            File outputFile = new File(outputFilePath);
            OutputStream outputStream = new FileOutputStream(outputFile);
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        try {
            mediaPath = MediaStore.Images.Media.insertImage(this.getContentResolver(), outputFilePath, null, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (shareToGallery) {
            final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "AdigaQuiz_" + timeStamp, "");
            Toast.makeText(this, "SAVED THE IMAGE TO THE GALLERY", Toast.LENGTH_SHORT).show();
        } else {
            Intent intentShare = new Intent(Intent.ACTION_SEND);
//            intentShare.setType("image/webp");
            intentShare.setType("image/*");
            intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            intentShare.putExtra(Intent.EXTRA_STREAM, Uri.parse(outputFilePath));
            intentShare.putExtra(Intent.EXTRA_STREAM, Uri.parse(mediaPath));
            intentShare.setPackage("com.whatsapp");
            startActivity(Intent.createChooser(intentShare, "Share Image"));
        }
    }

    private DatePickerDialog.OnDateSetListener mDatePickerDateSetListener;
    private Calendar mCalendar;
    private String[] mDayNamesAdiga = new String[] {
            "Thawmaf",
            "Blıpe",
            "Ğubcı",
            "Beresḱéjiy",
            "Mefeḱu'",
            "Beresḱefu'",
            "Mefezaku'"
    };
    private String[] mDayNamesEnglish = new String[] {
            "Sunday",
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday"
    };
    private String[] mDayNamesTurkish = new String[] {
            "Pazar",
            "Pazartesi",
            "Salı",
            "Çarşamba",
            "Perşembe",
            "Cuma",
            "Cumartesi"
    };
    private String[] mDayNamesArabic = new String[] {
            "الأحد",
            "الإثنين",
            "الثلاثاء",
            "الأربعاء",
            "الخميس",
            "الجمعة",
            "السبت"
    };
    private String[] mDayNamesHebrew = new String[] {
            "ראשון",
            "שני",
            "שלישי",
            "רביעי",
            "חמישי",
            "שישי",
            "שבת"
    };

    private FragmentManager mFragmentManager;
    private DailyTimesFragmentA mDailyTimesFragmentA;
    private DailyTimesFragmentB mDailyTimesFragmentB;
    private DailyTimesFragmentC mDailyTimesFragmentC;

    private int mCurrentFragment = 1;

    protected float mDp2pxFactor = 0;
    private ActivityMainBinding mBinding;
    private MainViewModel mMainViewModel;
    private SharedPreferences mSharedPreferences = null;

    private static final String TAG = MainActivity.class.getSimpleName();
}
