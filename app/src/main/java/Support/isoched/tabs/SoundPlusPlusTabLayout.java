/*
 * Copyright (C) 2015-2016 Adrian Ulrich <adrian@blinkenlights.ch>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>. 
 */

package Support.isoched.tabs;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Simple wrapper for SlidingTabLayout which takes
 * care of setting sane per-platform defaults
 */
public class SoundPlusPlusTabLayout extends SlidingTabLayout {

	public SoundPlusPlusTabLayout(Context context) {
		this(context, null);
	}

	public SoundPlusPlusTabLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SoundPlusPlusTabLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		//setSelectedIndicatorColors(context.getResources().getColor(ch.teamuit.android.soundplusplus.R.color.tabs_active_indicator));
		setDistributeEvenly(true);
	}

	/**
	 * Overrides the default text color
	 */
	@Override
	protected TextView createDefaultTabView(Context context) {
		TextView view = super.createDefaultTabView(context);
		//.setTextColor(getResources().getColorStateList(ch.teamuit.android.soundplusplus.R.color.tab_text_selector));
		view.setBackgroundResource(chongxuocmanhinh.sound_plusplus.R.drawable.unbound_ripple_light);
		view.setMaxLines(1);
		view.setEllipsize(TextUtils.TruncateAt.END);
		view.setTextSize(14);
		return view;
	}

}
