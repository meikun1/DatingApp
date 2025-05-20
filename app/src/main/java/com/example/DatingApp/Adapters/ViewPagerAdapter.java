package com.example.DatingApp.Adapters;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.DatingApp.ImagesFragment;
import com.example.DatingApp.R;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private final Integer[] imgs = new Integer[]{
            R.drawable.ic_online,
            R.drawable.ic_online,
            R.drawable.ic_online,
            R.drawable.ic_online,
            R.drawable.ic_online,
            R.drawable.ic_online,
            R.drawable.ic_online
    };

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        ImagesFragment fragment = new ImagesFragment();
        Bundle bundle = new Bundle();
        bundle.putString("Message", "Position is: " + position);
        bundle.putInt("Images", imgs[position]);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return imgs.length;
    }
}
