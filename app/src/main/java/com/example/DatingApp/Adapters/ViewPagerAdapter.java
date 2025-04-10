package com.example.DatingApp.Adapters;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;


import com.example.DatingApp.ImagesFragment;
import com.example.DatingApp.R;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    private Context context;
    private Integer[] imgs = new Integer[]{
            R.drawable.ic_online,
            R.drawable.ic_online,
            R.drawable.ic_online,
            R.drawable.ic_online,
            R.drawable.ic_online,
            R.drawable.ic_online,
            R.drawable.ic_online
    };

    public ViewPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    @Override
    public int getCount() {
        return imgs.length;
    }
/*

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }
*/

    @Override
    public Fragment getItem(int i) {
        ImagesFragment imagesFragment = new ImagesFragment();
        Bundle bundle = new Bundle();
        bundle.putString("Message", "Position is: "+i);
        bundle.putInt("Images",imgs[i]);
        i = i+1;
        imagesFragment.setArguments(bundle);
        return imagesFragment;
    }
/*
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        ImageView imageView = new ImageView(context);
        container.addView(imageView);

        return imageView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }*/
}