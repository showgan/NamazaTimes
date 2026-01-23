
package com.mastegoane.namazatimes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.widget.CompoundButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import android.Manifest;
import android.app.DatePickerDialog;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.Toast;
import com.mastegoane.namazatimes.databinding.ActivityMainBinding;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.provider.Settings;

public class MainActivity extends AppCompatActivity {

	public void pickDateClicked(View view) {
		new DatePickerDialog(this, mDatePickerDateSetListener,
				mCalendar.get(Calendar.YEAR),
				mCalendar.get(Calendar.MONTH),
				mCalendar.get(Calendar.DAY_OF_MONTH))
				.show();
	}

	public void incrementDateClicked(View view) {
		if (view.getTag() != null && view.getTag().toString().equals("increment")) {
			mCalendar.add(Calendar.DATE, 1);
		} else {
			mCalendar.add(Calendar.DATE, -1);
		}
		try {
			mMainViewModel.updateViews();
		} catch (Exception ignored) {}
		updateViews();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Apply saved theme preference before inflating views
		mSharedPreferences = this.getSharedPreferences("namazatimes", MODE_PRIVATE);
		int nightModePref = mSharedPreferences.getInt("pref_night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
		AppCompatDelegate.setDefaultNightMode(nightModePref);
		super.onCreate(savedInstanceState);
		mMainViewModel = new ViewModelProvider(this,
				(ViewModelProvider.Factory) ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication()))
				.get(MainViewModel.class);
		mBinding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(mBinding.getRoot());
		final DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
		mDp2pxFactor = displayMetrics.density;
		mCalendar = mMainViewModel.getCalendar().getValue();

		mFragmentManager = getSupportFragmentManager();
		mDailyTimesFragmentA = new DailyTimesFragmentA();
		mDailyTimesFragmentB = new DailyTimesFragmentB();
		mDailyTimesFragmentC = new DailyTimesFragmentC();
		mCurrentFragment = mSharedPreferences.getInt("sp_current_fragment", 1);
		if (mCurrentFragment < 1 || mCurrentFragment > 3) {
			mCurrentFragment = 1;
		}
		selectFragment(mCurrentFragment);

		mBinding.bottomnavigationView.setOnNavigationItemSelectedListener(item -> {
			int id = item.getItemId();
			if (id == R.id.navShareToWhatsapp) {
				shareScreenshot(false);
				return true;
			} else if (id == R.id.navSaveToGallery) {
				shareScreenshot(true);
				return true;
			} else if (id == R.id.navCalendar) {
				// Open the date picker when calendar nav item is tapped
				try {
					pickDateClicked(mBinding.textViewMainDate);
				} catch (Exception e) { e.printStackTrace(); }
				return true;
			} else if (id == R.id.navSettings) {
				// Open the SettingsActivity (settings fragment housed there)
				try {
					startActivity(new Intent(MainActivity.this, SettingsActivity.class));
				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			}
			return true;
		});

		mDatePickerDateSetListener = new DatePickerDialog.OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
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

	@Override
	protected void onResume() {
		super.onResume();
		mIsResumed = true;
		try {
			int prefFragment = mSharedPreferences.getInt("sp_current_fragment", 1);
			if (prefFragment < 1 || prefFragment > 3) prefFragment = 1;
			if (prefFragment != mCurrentFragment) {
				selectFragment(prefFragment);
			}
		} catch (Exception ignored) {}
		// First, ensure we don't show both permission prompts at the same time.
		boolean notifRequested = requestNotificationPermissionIfNeeded();
		if (notifRequested) {
			// we'll request exact-alarm after notification permission result
			return;
		}
		// If we just returned from the exact-alarm settings flow, give the system
		// a moment to update and then check the status instead of immediately
		// re-showing the dialog.
		boolean pendingExactAlarm = mSharedPreferences.getBoolean(KEY_PENDING_EXACT_ALARM_REQUEST, false);
		if (pendingExactAlarm) {
			// Clear the flag immediately to prevent re-triggering
			mSharedPreferences.edit().putBoolean(KEY_PENDING_EXACT_ALARM_REQUEST, false).apply();
			// Dismiss the dialog if it's still showing
			dismissExactAlarmDialogIfShowing();
			// Cancel any pending runnable before posting a new one
			cancelPendingExactAlarmRunnable();
			if (mHandler == null) {
				mHandler = new Handler(Looper.getMainLooper());
			}
			mPendingExactAlarmRunnable = () -> {
				// Check activity state before showing toast
				if (!isActivityValid()) {
					return;
				}
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
					AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
					if (am != null && am.canScheduleExactAlarms()) {
						Toast.makeText(this, "Exact alarms allowed", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(this, "Exact alarms not enabled", Toast.LENGTH_LONG).show();
					}
				}
			};
			mHandler.postDelayed(mPendingExactAlarmRunnable, 800);
		} else {
			// Check if we already showed the dialog in this session
			boolean alreadyShown = mSharedPreferences.getBoolean(KEY_EXACT_ALARM_DIALOG_SHOWN, false);
			if (!alreadyShown) {
				requestExactAlarmPermissionIfNeeded();
			}
		}
	}

	@Override
	protected void onPause() {
		mIsResumed = false;
		// Dismiss dialog when activity is paused to prevent window leak
		dismissExactAlarmDialogIfShowing();
		// Cancel any pending runnables
		cancelPendingExactAlarmRunnable();
		super.onPause();
	}
	private boolean requestNotificationPermissionIfNeeded() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			// Don't request if already in progress or already requested this session
			if (mNotificationPermissionInProgress) {
				return true; // Return true to indicate permission flow is active
			}
			// Check if we already requested (and user responded) in this app install
			boolean alreadyRequested = mSharedPreferences.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false);
			if (alreadyRequested) {
				return false; // Don't request again
			}
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
				mNotificationPermissionInProgress = true;
				mSharedPreferences.edit().putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true).apply();
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS);
				return true;
			}
		}
		return false;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQ_POST_NOTIFICATIONS) {
			// Clear the in-progress flag
			mNotificationPermissionInProgress = false;
			boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
			if (granted) {
				// notification permission granted - proceed with exact alarm flow
				// But only if activity is still in a valid state
				if (isActivityValid()) {
					// Post with a small delay to allow activity state to settle
					if (mHandler == null) {
						mHandler = new Handler(Looper.getMainLooper());
					}
					mHandler.postDelayed(() -> {
						if (isActivityValid()) {
							requestExactAlarmPermissionIfNeeded();
						}
					}, 500);
				}
			} else {
				// notification permission denied - don't show exact alarm dialog
				// to avoid annoying the user with multiple permission dialogs
				// Mark as shown so we don't ask again in onResume
				mSharedPreferences.edit().putBoolean(KEY_EXACT_ALARM_DIALOG_SHOWN, true).apply();
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQ_SCHEDULE_EXACT_ALARM) {
			mSharedPreferences.edit().putBoolean(KEY_PENDING_EXACT_ALARM_REQUEST, false).apply();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
				if (am != null && am.canScheduleExactAlarms()) {
					Toast.makeText(this, "Exact alarms allowed", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, "Exact alarms not enabled", Toast.LENGTH_LONG).show();
				}
			}
		}
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
		try { mMainViewModel.updateViews(); } catch (Exception ignored) {}
		updateViews();
	}

	private void updateViews() {
		String dateFormat = "d\nMMMM\nyyyy";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
		try {
			mBinding.textViewMainDate.setText(simpleDateFormat.format(mCalendar.getTime()));
		} catch (Exception ignored) {}
		final int dayOfWeek = mCalendar.get(Calendar.DAY_OF_WEEK) - 1;
		switch (mCurrentFragment) {
			case 1:
				try {
					mBinding.textViewMainDay.setText(mDayNamesAdiga[dayOfWeek] + "\n" + mDayNamesHebrew[dayOfWeek] + "\n" + mDayNamesArabic[dayOfWeek]);
				} catch (Exception ignored) {}
				break;
			case 2:
				try { mBinding.textViewMainDay.setText(mDayNamesAdiga[dayOfWeek] + "\n" + mDayNamesArabic[dayOfWeek]); } catch (Exception ignored) {}
				break;
			case 3:
				try { mBinding.textViewMainDay.setText(mDayNamesAdiga[dayOfWeek] + "\n"); } catch (Exception ignored) {}
				break;
			default:
				try { mBinding.textViewMainDay.setText(mDayNamesAdiga[dayOfWeek] + "\n" + mDayNamesHebrew[dayOfWeek] + "\n" + mDayNamesArabic[dayOfWeek]); } catch (Exception ignored) {}
				break;
		}
	}

	private void shareScreenshot(boolean shareToGallery) {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
		}

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
		String mediaPath;
		try {
			File outputFile = new File(outputFilePath);
			OutputStream outputStream = new FileOutputStream(outputFile);
			// Use WEBP_LOSSY on Android R+ to avoid deprecation
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 100, outputStream);
			} else {
				bitmap.compress(Bitmap.CompressFormat.WEBP, 100, outputStream);
			}
			outputStream.flush();
			outputStream.close();
		} catch (Exception exception) {
			exception.printStackTrace();
			Toast.makeText(this, "COULD NOT OPEN FILE FOR SAVING SCREENSHOT", Toast.LENGTH_SHORT).show();
			return;
		}

		try {
			Intent intent = getIntent();
			intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			// Use modern MediaStore API on Android Q+ to avoid deprecated insertImage
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				ContentValues values = new ContentValues();
				values.put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_" + System.currentTimeMillis() + ".webp");
				values.put(MediaStore.Images.Media.MIME_TYPE, "image/webp");
				values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
				Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
				if (imageUri != null) {
					try (OutputStream os = getContentResolver().openOutputStream(imageUri)) {
						if (os != null) {
							bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 100, os);
						}
					}
					mediaPath = imageUri.toString();
				} else {
					mediaPath = null;
				}
			} else {
				mediaPath = MediaStore.Images.Media.insertImage(this.getContentResolver(), outputFilePath, "IMG_" + System.currentTimeMillis(), null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, "COULD NOT SAVE SCREENSHOT TO GALLERY", Toast.LENGTH_SHORT).show();
			return;
		}

		if (mediaPath == null || mediaPath.equals("")) {
			Toast.makeText(this, "SOMETHING WENT WRONG WHILE TRYING TO SAVE SCREENSHOT TO GALLERY", Toast.LENGTH_SHORT).show();
			return;
		}

		if (shareToGallery) {
			final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			// Use modern MediaStore API on Android Q+
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				try {
					ContentValues values = new ContentValues();
					values.put(MediaStore.Images.Media.DISPLAY_NAME, "AdigaQuiz_" + timeStamp + ".webp");
					values.put(MediaStore.Images.Media.MIME_TYPE, "image/webp");
					values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
					Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
					if (imageUri != null) {
						try (OutputStream os = getContentResolver().openOutputStream(imageUri)) {
							if (os != null) {
								bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 100, os);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "AdigaQuiz_" + timeStamp, "");
			}
			Toast.makeText(this, "SAVED THE IMAGE TO THE GALLERY", Toast.LENGTH_SHORT).show();
		} else {
			Intent intentShare = new Intent(Intent.ACTION_SEND);
			intentShare.setType("image/*");
			intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			intentShare.putExtra(Intent.EXTRA_STREAM, Uri.parse(mediaPath));
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

	private static final int REQ_POST_NOTIFICATIONS = 2;
	private static final int REQ_SCHEDULE_EXACT_ALARM = 3;
	private static final String KEY_PENDING_EXACT_ALARM_REQUEST = "pending_exact_alarm_request";
	private static final String KEY_EXACT_ALARM_DIALOG_SHOWN = "exact_alarm_dialog_shown";
	private static final String KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested";
	private androidx.appcompat.app.AlertDialog mExactAlarmDialog = null;
	private Handler mHandler = null;
	private Runnable mPendingExactAlarmRunnable = null;
	private boolean mIsResumed = false;
	private boolean mNotificationPermissionInProgress = false;

	private void requestExactAlarmPermissionIfNeeded() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
			if (am != null && !am.canScheduleExactAlarms()) {
				// Don't show if activity is not in valid state
				if (!isActivityValid()) {
					return;
				}
				// Don't show if dialog is already showing
				if (mExactAlarmDialog != null && mExactAlarmDialog.isShowing()) {
					return;
				}
				// Mark as shown so we don't repeatedly show on each onResume
				mSharedPreferences.edit().putBoolean(KEY_EXACT_ALARM_DIALOG_SHOWN, true).apply();
				try {
					mExactAlarmDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
							.setTitle("Allow exact alarms")
							.setMessage("To ensure the widget updates exactly when prayer times change, please allow the app to schedule exact alarms in system settings.")
							.setPositiveButton("Open Settings", (dialog, which) -> {
								try {
									Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
									intent.setData(Uri.parse("package:" + getPackageName()));
									if (intent.resolveActivity(getPackageManager()) != null) {
										mSharedPreferences.edit().putBoolean(KEY_PENDING_EXACT_ALARM_REQUEST, true).apply();
										startActivity(intent);
									} else {
										Toast.makeText(this, "Please allow exact alarms in system settings.", Toast.LENGTH_LONG).show();
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							})
							.setNegativeButton("Cancel", null)
							.create();
					mExactAlarmDialog.show();
				} catch (Exception e) {
					// Catch any window-related exceptions
					e.printStackTrace();
					mExactAlarmDialog = null;
				}
			}
		}
	}

	private boolean isActivityValid() {
		return mIsResumed && !isFinishing() && !isDestroyed();
	}

	private void dismissExactAlarmDialogIfShowing() {
		try {
			if (mExactAlarmDialog != null && mExactAlarmDialog.isShowing()) {
				mExactAlarmDialog.dismiss();
			}
		} catch (Exception e) {
			// Ignore any exceptions during dismiss
		}
		mExactAlarmDialog = null;
	}

	private void cancelPendingExactAlarmRunnable() {
		if (mHandler != null && mPendingExactAlarmRunnable != null) {
			mHandler.removeCallbacks(mPendingExactAlarmRunnable);
			mPendingExactAlarmRunnable = null;
		}
	}

	@Override
	protected void onDestroy() {
		dismissExactAlarmDialogIfShowing();
		cancelPendingExactAlarmRunnable();
		if (mHandler != null) {
			mHandler.removeCallbacksAndMessages(null);
			mHandler = null;
		}
		super.onDestroy();
	}
}
