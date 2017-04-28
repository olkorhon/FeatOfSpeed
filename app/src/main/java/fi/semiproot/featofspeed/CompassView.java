package fi.semiproot.featofspeed;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: document your custom view class.
 */
public class CompassView extends View
{
    // Text
    private static String[] directions;

    // Padding
    int paddingLeft;
    int paddingTop;
    int paddingRight;
    int paddingBottom;

    // Content size derived from widgets actual size and padding
    int contentWidth;
    int contentHeight;

    // Used to accurately center text
    private final Rect textBounds = new Rect();

    // Text
    private int mTextColor = Color.RED;
    private TextPaint mTextPaint;
    private float mTextDimension = 0;

    // Bg panel
    private int mPanelColor = Color.BLACK;
    private Paint mPanelPaint;

    // Blob
    private int mBlobColor = Color.BLUE;
    private Paint mBlobPaint;
    private float blobMaxHorizontal = 20;
    private float blobMinHorizontal = 6;
    private float blobMaxVertical = 60;
    private float blobMinVertical = 30;

    // Data
    private float max_distance = 90;
    private float angleReal;
    private float angleVisible;
    private float[] target_angles;

    private LatLng pos;
    private List<Waypoint> waypoints;

    // Constructors
    public CompassView(Context context)
    {
        super(context);
        init(context, null, 0);
    }

    public CompassView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public CompassView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    // Initialize
    private void init(Context context, AttributeSet attrs, int defStyle)
    {
        // Padding
        paddingLeft = getPaddingLeft();
        paddingTop = getPaddingTop();
        paddingRight = getPaddingRight();
        paddingBottom = getPaddingBottom();

        // Test data
        angleVisible = 80;
        target_angles = new float[] {20f, 180f, 270f};

        directions = new String[] {
                getResources().getString(R.string.North),
                getResources().getString(R.string.East),
                getResources().getString(R.string.South),
                getResources().getString(R.string.West) };

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.CompassView, defStyle, 0);

        max_distance = a.getFloat(R.styleable.CompassView_MaxAngle, max_distance);
        blobMaxHorizontal = a.getFloat(R.styleable.CompassView_MaxBlobWidth, blobMaxHorizontal);
        blobMinHorizontal = a.getFloat(R.styleable.CompassView_MinBlobWidth, blobMinHorizontal);
        blobMaxVertical = a.getFloat(R.styleable.CompassView_MaxBlobHeight, blobMaxVertical);
        blobMinVertical = a.getFloat(R.styleable.CompassView_MinBlobHeight, blobMinVertical);

        // load color attributes
        mTextColor = a.getColor(
                R.styleable.CompassView_TextColor,
                mTextColor);

        mPanelColor = a.getColor(
                R.styleable.CompassView_PanelColor,
                mPanelColor);

        mBlobColor = a.getColor(
                R.styleable.CompassView_BlobColor,
                mBlobColor);

        // Use getDimensionPixelSize or getDimensionPixelOffset when dealing with
        // values that should fall on pixel boundaries.
        mTextDimension = a.getDimension(
                R.styleable.CompassView_exampleDimension,
                mTextDimension);

        a.recycle();

        // Set up a default paint objects
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.LEFT);

        mPanelPaint = new Paint();
        mPanelPaint.setColor(mPanelColor);

        mBlobPaint = new Paint();
        mBlobPaint.setColor(mBlobColor);

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();
    }

    public void setPos(LatLng pos) {
        this.pos = pos;

        if (waypoints != null)
            calculateAngles();
    }

    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints;

        if (pos != null)
            calculateAngles();
    }

    private void calculateAngles() {
        if (this.target_angles.length != this.waypoints.size())
            this.target_angles = new float[this.waypoints.size()];

        for (int i = 0; i < waypoints.size(); i++) {
            this.target_angles[i] = (float)angleFromCoordinate(this.pos.latitude, this.pos.longitude, this.waypoints.get(i).getLat(), this.waypoints.get(i).getLng());
        }
    }

    private double angleFromCoordinate(double lat1, double long1, double lat2, double long2) {
        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;
        brng = 360 - brng; // count degrees counter-clockwise - remove to make clockwise

        return brng;
    }

    public void refresh() {
        float difference = angularDistance(angleVisible, angleReal);
        angleVisible = angleReal;//0.7f * angleVisible + 0.3f * (angleVisible + difference);
        invalidate();
    }

    private void invalidateTextPaintAndMeasurements()
    {
        mTextPaint.setTextSize(mTextDimension);
        mTextPaint.setColor(mTextColor);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        contentWidth = getWidth() - paddingLeft - paddingRight;
        contentHeight = getHeight() - paddingTop - paddingBottom;

        // Draw background
        canvas.drawRect(paddingLeft, paddingTop, paddingLeft + contentWidth, paddingTop + contentHeight, mPanelPaint);

        // Draw direction texts
        drawDirections(canvas);

        // Draw target blobs
        drawBlobs(canvas);
    }

    private void drawDirections(Canvas canvas)
    {
        float middle_horizontal = paddingLeft + contentWidth / 2.0f;
        float middle_vertical = paddingTop + contentHeight / 2.0f;

        // Draw directions
        float angle_step = 360f / directions.length;
        for (int i = 0; i < directions.length; i++)
        {
            float dist = angularDistance(angleVisible, angle_step * i);

            if (Math.abs(dist) > max_distance)
                continue;

            drawTextCentred(
                    canvas,
                    mTextPaint,
                    directions[i],
                    middle_horizontal + (dist / max_distance) * (contentWidth / 2),
                    middle_vertical);
        }
    }

    private void drawTextCentred(Canvas canvas, Paint paint, String text, float cx, float cy)
    {
        paint.getTextBounds(text, 0, text.length(), textBounds);
        canvas.drawText(text, cx - textBounds.exactCenterX(), cy - textBounds.exactCenterY(), paint);
    }

    private void drawBlobs(Canvas canvas)
    {
        float middle_horizontal = paddingLeft + contentWidth / 2.0f;
        float middle_vertical = paddingTop + contentHeight / 2.0f;

        // Track drawn blobs
        float closest_dist = 181;
        int drawn = 0;

        // draw blobs
        for (int i = 0; i < target_angles.length; i++)
        {
            float dist = angularDistance(this.angleVisible, target_angles[i]);
            if (dist < closest_dist)
                closest_dist = dist;

            if (Math.abs(dist) > max_distance)
                continue;

            drawn += 1;

            float blobWidth = sinLerp(blobMaxHorizontal, blobMinHorizontal, Math.abs(dist / max_distance));
            float blobHeight = sinLerp(blobMaxVertical, blobMinVertical, Math.abs(dist / max_distance));

            canvas.drawRect(
                    middle_horizontal - blobWidth + (dist / max_distance) * (contentWidth / 2),
                    middle_vertical - blobHeight,
                    middle_horizontal + blobWidth + (dist / max_distance) * (contentWidth / 2),
                    middle_vertical + blobHeight,
                    mBlobPaint);
        }

        // Draw the nearest blob if no blobs drawn
        if (target_angles.length != 0 && drawn == 0)
        {
            if (closest_dist < 0)
            {
                canvas.drawRect(
                        paddingLeft - blobMinHorizontal,
                        middle_vertical - blobMinVertical,
                        paddingLeft + blobMinHorizontal,
                        middle_vertical + blobMinVertical,
                        mBlobPaint);
            }
            else
            {
                canvas.drawRect(
                        paddingLeft + contentWidth - blobMinHorizontal,
                        middle_vertical - blobMinVertical,
                        paddingLeft + contentWidth + blobMinHorizontal,
                        middle_vertical + blobMinVertical,
                        mBlobPaint);
            }
        }
    }

    // Returns a sinLerp of two floats
    private float sinLerp(float a, float b, float f)
    {
        f = (float)Math.sin(f * (Math.PI / 2.0) + Math.PI / 2.0) * -1.0f + 1.0f;
        return (a * (1.0f - f)) + (b * f);
    }

    // Returns the angular distance of seconds angleVisible from first angleVisible
    private float angularDistance(float a1, float a2)
    {
        float result = a2 - a1;
        return (result + 180) % 360 - 180;
    }

    // Setter for angleVisible
    public void setActualRotation(float angle)
    {
        this.angleReal = angle;
    }
}