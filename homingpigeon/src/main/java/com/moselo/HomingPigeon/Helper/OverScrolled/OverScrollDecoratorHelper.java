package com.moselo.HomingPigeon.Helper.OverScrolled;

import android.support.v7.widget.RecyclerView;

public class OverScrollDecoratorHelper {

    public static final int ORIENTATION_VERTICAL = 0;
//    public static final int ORIENTATION_HORIZONTAL = 1;

    public static IOverScrollDecor setUpOverScroll(RecyclerView recyclerView, int orientation) {
        switch (orientation) {
//            case ORIENTATION_HORIZONTAL:
//                return new HorizontalOverScrollBounceEffectDecorator(new RecyclerViewOverScrollDecorAdapter(recyclerView));
            case ORIENTATION_VERTICAL:
                return new VerticalOverScrollBounceEffectDecorator(new RecyclerViewOverScrollDecorAdapter(recyclerView));
            default:
                throw new IllegalArgumentException("orientation");
        }
    }

}