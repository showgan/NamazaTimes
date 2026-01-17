package com.mastegoane.namazatimes;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.mastegoane.namazatimes.databinding.FragmentDailyTimesBBinding;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DailyTimesFragmentB#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DailyTimesFragmentB extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public DailyTimesFragmentB() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DailyTimesFragmentB.
     */
    // TODO: Rename and change types and number of parameters
    public static DailyTimesFragmentB newInstance(String param1, String param2) {
        DailyTimesFragmentB fragment = new DailyTimesFragmentB();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = FragmentDailyTimesBBinding.inflate(inflater, container, false);
        View view = mBinding.getRoot();
        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        LiveData<Boolean> updateViewLD = mMainViewModel.getUpdateViews();
        updateViewLD.observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                updateViews();
            }
        });
    }

    private void updateViews() {
        final PrayerTimes.DailyTimes dailyTimes = mMainViewModel.getTodaysTimes();
        if (dailyTimes != null) {
            mBinding.textViewMainSabahTime.setText(dailyTimes.getTimeOf("fajr", true));
            mBinding.textViewMainShurooqTime.setText(dailyTimes.getTimeOf("shurooq", true));
            mBinding.textViewMainDuhrTime.setText(dailyTimes.getTimeOf("duhr", true));
            mBinding.textViewMainAsrTime.setText(dailyTimes.getTimeOf("asr", true));
            mBinding.textViewMainMagribTime.setText(dailyTimes.getTimeOf("magrib", true));
            mBinding.textViewMainIshaTime.setText(dailyTimes.getTimeOf("isha", true));
            final int prayerIndex = mMainViewModel.getCurrentPrayerIndex();
            switch (prayerIndex) {
                case 1:
                    mBinding.linearLayoutMainTimeFajr.setEnabled(true);
                    mBinding.linearLayoutMainTimeShurooq.setEnabled(false);
                    mBinding.linearLayoutMainTimeDuhr.setEnabled(false);
                    mBinding.linearLayoutMainTimeAsr.setEnabled(false);
                    mBinding.linearLayoutMainTimeMagrib.setEnabled(false);
                    mBinding.linearLayoutMainTimeIsha.setEnabled(false);
                    break;
                case 2:
                    mBinding.linearLayoutMainTimeFajr.setEnabled(false);
                    mBinding.linearLayoutMainTimeShurooq.setEnabled(true);
                    mBinding.linearLayoutMainTimeDuhr.setEnabled(false);
                    mBinding.linearLayoutMainTimeAsr.setEnabled(false);
                    mBinding.linearLayoutMainTimeMagrib.setEnabled(false);
                    mBinding.linearLayoutMainTimeIsha.setEnabled(false);
                    break;
                case 3:
                    mBinding.linearLayoutMainTimeFajr.setEnabled(false);
                    mBinding.linearLayoutMainTimeShurooq.setEnabled(false);
                    mBinding.linearLayoutMainTimeDuhr.setEnabled(true);
                    mBinding.linearLayoutMainTimeAsr.setEnabled(false);
                    mBinding.linearLayoutMainTimeMagrib.setEnabled(false);
                    mBinding.linearLayoutMainTimeIsha.setEnabled(false);
                    break;
                case 4:
                    mBinding.linearLayoutMainTimeFajr.setEnabled(false);
                    mBinding.linearLayoutMainTimeShurooq.setEnabled(false);
                    mBinding.linearLayoutMainTimeDuhr.setEnabled(false);
                    mBinding.linearLayoutMainTimeAsr.setEnabled(true);
                    mBinding.linearLayoutMainTimeMagrib.setEnabled(false);
                    mBinding.linearLayoutMainTimeIsha.setEnabled(false);
                    break;
                case 5:
                    mBinding.linearLayoutMainTimeFajr.setEnabled(false);
                    mBinding.linearLayoutMainTimeShurooq.setEnabled(false);
                    mBinding.linearLayoutMainTimeDuhr.setEnabled(false);
                    mBinding.linearLayoutMainTimeAsr.setEnabled(false);
                    mBinding.linearLayoutMainTimeMagrib.setEnabled(true);
                    mBinding.linearLayoutMainTimeIsha.setEnabled(false);
                    break;
                case 6:
                    mBinding.linearLayoutMainTimeFajr.setEnabled(false);
                    mBinding.linearLayoutMainTimeShurooq.setEnabled(false);
                    mBinding.linearLayoutMainTimeDuhr.setEnabled(false);
                    mBinding.linearLayoutMainTimeAsr.setEnabled(false);
                    mBinding.linearLayoutMainTimeMagrib.setEnabled(false);
                    mBinding.linearLayoutMainTimeIsha.setEnabled(true);
                    break;
                default:
                    mBinding.linearLayoutMainTimeFajr.setEnabled(false);
                    mBinding.linearLayoutMainTimeShurooq.setEnabled(false);
                    mBinding.linearLayoutMainTimeDuhr.setEnabled(false);
                    mBinding.linearLayoutMainTimeAsr.setEnabled(false);
                    mBinding.linearLayoutMainTimeMagrib.setEnabled(false);
                    mBinding.linearLayoutMainTimeIsha.setEnabled(false);
                    break;
            }
            updateTextColors(prayerIndex);

        }
    }

    private void updateTextColors(int prayerIndex) {
        final Context ctx = requireContext();
        final int colorNotSelected = ContextCompat.getColor(ctx, R.color.mainText);
        final int colorSelected = ContextCompat.getColor(ctx, R.color.mainTextSelected);
        mBinding.textViewMainSabahAdiga.setTextColor(colorNotSelected);
        mBinding.textViewMainSabahArabic.setTextColor(colorNotSelected);
        mBinding.textViewMainSabahTime.setTextColor(colorNotSelected);

        mBinding.textViewMainShurooqAdiga.setTextColor(colorNotSelected);
        mBinding.textViewMainShurooqArabic.setTextColor(colorNotSelected);
        mBinding.textViewMainShurooqTime.setTextColor(colorNotSelected);

        mBinding.textViewMainDuhrAdiga.setTextColor(colorNotSelected);
        mBinding.textViewMainDuhrArabic.setTextColor(colorNotSelected);
        mBinding.textViewMainDuhrTime.setTextColor(colorNotSelected);

        mBinding.textViewMainAsrAdiga.setTextColor(colorNotSelected);
        mBinding.textViewMainAsrArabic.setTextColor(colorNotSelected);
        mBinding.textViewMainAsrTime.setTextColor(colorNotSelected);

        mBinding.textViewMainMagribAdiga.setTextColor(colorNotSelected);
        mBinding.textViewMainMagribArabic.setTextColor(colorNotSelected);
        mBinding.textViewMainMagribTime.setTextColor(colorNotSelected);

        mBinding.textViewMainIshaAdiga.setTextColor(colorNotSelected);
        mBinding.textViewMainIshaArabic.setTextColor(colorNotSelected);
        mBinding.textViewMainIshaTime.setTextColor(colorNotSelected);

        switch (prayerIndex) {
            case 1:
                mBinding.textViewMainSabahAdiga.setTextColor(colorSelected);
                mBinding.textViewMainSabahArabic.setTextColor(colorSelected);
                mBinding.textViewMainSabahTime.setTextColor(colorSelected);
                break;
            case 2:
                mBinding.textViewMainShurooqAdiga.setTextColor(colorSelected);
                mBinding.textViewMainShurooqArabic.setTextColor(colorSelected);
                mBinding.textViewMainShurooqTime.setTextColor(colorSelected);
                break;
            case 3:
                mBinding.textViewMainDuhrAdiga.setTextColor(colorSelected);
                mBinding.textViewMainDuhrArabic.setTextColor(colorSelected);
                mBinding.textViewMainDuhrTime.setTextColor(colorSelected);
                break;
            case 4:
                mBinding.textViewMainAsrAdiga.setTextColor(colorSelected);
                mBinding.textViewMainAsrArabic.setTextColor(colorSelected);
                mBinding.textViewMainAsrTime.setTextColor(colorSelected);
                break;
            case 5:
                mBinding.textViewMainMagribAdiga.setTextColor(colorSelected);
                mBinding.textViewMainMagribArabic.setTextColor(colorSelected);
                mBinding.textViewMainMagribTime.setTextColor(colorSelected);
                break;
            case 6:
                mBinding.textViewMainIshaAdiga.setTextColor(colorSelected);
                mBinding.textViewMainIshaArabic.setTextColor(colorSelected);
                mBinding.textViewMainIshaTime.setTextColor(colorSelected);
                break;
            default:
                break;
        }
    }

    private MainViewModel mMainViewModel;
    private FragmentDailyTimesBBinding mBinding;

    private static final String TAG = DailyTimesFragmentB.class.getSimpleName();
}
