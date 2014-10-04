package com.example.photogallery;

import android.support.v4.app.Fragment;

public class PhotoPageActivity extends SingleFragmentActtivity {

	@Override
	protected Fragment createFragment() {
		return new PhotoPageFragment();
	}
}
