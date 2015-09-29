package com.example.SquareView;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Display;
import android.view.SurfaceView;
import android.view.WindowManager;

public class SquareView extends SurfaceView {
	
	public Point size2 = new Point();
	
  public SquareView(Context context) {
    super(context);
  }

  public SquareView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SquareView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  // The only real thing that matters, cover up the background with a square
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
    setY(size);
    WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
    Display display = wm.getDefaultDisplay();
    display.getSize(size2);
    int height = size2.y;
    setMeasuredDimension(size,Math.abs(height-size));
    this.setBackgroundColor(0Xffeeeeee);
  }
}