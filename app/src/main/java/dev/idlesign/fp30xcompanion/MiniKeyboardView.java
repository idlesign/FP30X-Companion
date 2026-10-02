package dev.idlesign.fp30xcompanion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public final class MiniKeyboardView extends View {
    public interface Listener {
        void onTone(ToneRole role);
        void onRemove(ToneRole role);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Listener listener;
    private PerformanceState state;
    private boolean primaryRemovable;

    public MiniKeyboardView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setBackgroundColor(Color.TRANSPARENT);
        setFocusable(true);
    }

    public void bind(PerformanceState state, boolean primaryRemovable) {
        this.state = state;
        this.primaryRemovable = primaryRemovable;
        setContentDescription(modeLabel(state.mode()));
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desired = dp(state != null && state.mode() != PerformanceMode.SINGLE ? 156 : 120);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                resolveSize(desired, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (state == null) return;
        float width = getWidth();
        float margin = dp(2);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(margin, margin, width - margin, getHeight() - margin),
                dp(12), dp(12), paint);

        if (state.mode() == PerformanceMode.SINGLE) {
            drawRole(canvas, new RectF(dp(10), dp(10), width - dp(10), dp(82)),
                    getContext().getString(R.string.role_single), state.primaryTone.name,
                    primaryRemovable, Color.rgb(255, 240, 199));
            drawKeys(canvas, dp(10), dp(88), width - dp(10), getHeight() - dp(10), -1);
        } else if (state.mode() == PerformanceMode.DUAL) {
            drawRole(canvas, new RectF(dp(10), dp(10), width - dp(10), dp(64)),
                    getContext().getString(R.string.role_main), state.primaryTone.name,
                    primaryRemovable, Color.rgb(255, 240, 199));
            drawRole(canvas, new RectF(dp(10), dp(69), width - dp(10), dp(123)),
                    getContext().getString(R.string.role_layer), state.layerTone.name,
                    true, Color.rgb(223, 243, 220));
            drawKeys(canvas, dp(10), dp(130), width - dp(10), getHeight() - dp(10), -1);
        } else {
            float middle = width * 0.42f;
            drawRole(canvas, new RectF(dp(10), dp(10), middle, dp(123)),
                    getContext().getString(R.string.role_left), state.leftTone.name,
                    true, Color.rgb(215, 232, 255));
            drawRole(canvas, new RectF(middle, dp(10), width - dp(10), dp(123)),
                    getContext().getString(R.string.role_right), state.primaryTone.name,
                    primaryRemovable, Color.rgb(255, 228, 214));
            paint.setColor(Color.rgb(230, 106, 44));
            canvas.drawRect(middle - dp(2), dp(7), middle + dp(2), dp(126), paint);
            drawKeys(canvas, dp(10), dp(130), width - dp(10), getHeight() - dp(10), middle);
        }
    }

    private void drawRole(Canvas canvas, RectF area, String role, String tone,
                          boolean removable, int color) {
        paint.setColor(color);
        canvas.drawRoundRect(area, dp(8), dp(8), paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(11));
        paint.setColor(Color.rgb(230, 106, 44));
        canvas.drawText(role.toUpperCase(), area.left + dp(12), area.top + dp(19), paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(dp(15));
        paint.setColor(Color.rgb(24, 28, 35));
        float maxWidth = area.width() - dp(removable ? 60 : 24);
        canvas.drawText(ellipsize(tone, maxWidth), area.left + dp(12), area.top + dp(44), paint);
        if (removable) drawRemove(canvas, area.right - dp(25), area.centerY());
    }

    private void drawRemove(Canvas canvas, float centerX, float centerY) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.8f));
        paint.setColor(Color.rgb(80, 87, 98));
        canvas.drawCircle(centerX, centerY, dp(10), paint);
        canvas.drawLine(centerX - dp(5), centerY, centerX + dp(5), centerY, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawKeys(Canvas canvas, float left, float top, float right, float bottom,
                          float splitX) {
        int whiteKeys = 14;
        float keyWidth = (right - left) / whiteKeys;
        paint.setStyle(Paint.Style.FILL);
        for (int index = 0; index < whiteKeys; index++) {
            float x = left + index * keyWidth;
            paint.setColor(Color.WHITE);
            canvas.drawRect(x, top, x + keyWidth, bottom, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(0.7f));
            paint.setColor(Color.rgb(120, 126, 136));
            canvas.drawRect(x, top, x + keyWidth, bottom, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        int[] blackAfter = {0, 1, 3, 4, 5, 7, 8, 10, 11, 12};
        paint.setColor(Color.rgb(32, 36, 43));
        for (int index : blackAfter) {
            float x = left + (index + 0.68f) * keyWidth;
            canvas.drawRoundRect(new RectF(x, top, x + keyWidth * 0.64f,
                    top + (bottom - top) * 0.62f), 0, 0, paint);
        }
        if (splitX > 0) {
            paint.setColor(Color.rgb(230, 106, 44));
            canvas.drawRect(splitX - dp(1.5f), top, splitX + dp(1.5f), bottom, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (state == null || event.getAction() != MotionEvent.ACTION_UP) return true;
        performClick();
        ToneRole role = hitRole(event.getX(), event.getY());
        if (role == null) return true;
        if (hitRemove(role, event.getX(), event.getY())) {
            listener.onRemove(role);
        } else {
            listener.onTone(role);
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private ToneRole hitRole(float x, float y) {
        if (state.mode() == PerformanceMode.SINGLE) return ToneRole.PRIMARY;
        if (state.mode() == PerformanceMode.DUAL) {
            return y < dp(67) ? ToneRole.PRIMARY : ToneRole.LAYER;
        }
        return x < getWidth() * 0.42f ? ToneRole.LEFT : ToneRole.PRIMARY;
    }

    private boolean hitRemove(ToneRole role, float x, float y) {
        boolean removable = role != ToneRole.PRIMARY || primaryRemovable;
        if (!removable) return false;
        float right;
        if (state.mode() == PerformanceMode.SPLIT && role == ToneRole.LEFT) {
            right = getWidth() * 0.42f;
        } else {
            right = getWidth() - dp(10);
        }
        return x >= right - dp(50);
    }

    private String ellipsize(String value, float width) {
        if (paint.measureText(value) <= width) return value;
        String result = value;
        while (result.length() > 1 && paint.measureText(result + "…") > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "…";
    }

    private String modeLabel(PerformanceMode mode) {
        return switch (mode) {
            case SINGLE -> getContext().getString(R.string.mode_single);
            case DUAL -> getContext().getString(R.string.mode_dual);
            case SPLIT -> getContext().getString(R.string.mode_split);
        };
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private int dp(int value) {
        return Math.round(dp((float) value));
    }
}
