package course.labs.graphicslab;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;

import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

public class BubbleActivity extends Activity {

	// These variables are for testing purposes, do not modify
	private final static int RANDOM = 0;
	private final static int SINGLE = 1;
	private final static int STILL = 2;
	private static int speedMode = RANDOM;

	private static final String TAG = "Lab-Graphics";

	// The Main view
	private RelativeLayout mFrame;

	// Bubble image's bitmap
	private Bitmap mBitmap;

	// Display dimensions
	private int mDisplayWidth, mDisplayHeight;

	// Sound variables

	// AudioManager
	private AudioManager mAudioManager;
	// SoundPool
	private SoundPool mSoundPool;
	// ID for the bubble popping sound
	private int mSoundID;
	// Audio volume
	private float mStreamVolume;

	// Gesture Detector
	private GestureDetector mGestureDetector;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		// Set up user interface
		mFrame = (RelativeLayout) findViewById(R.id.frame);

		// Load basic bubble Bitmap
		mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.b64);

	}

	@Override
	protected void onResume() {
		super.onResume();

		// Manage bubble popping sound
		// Use AudioManager.STREAM_MUSIC as stream type

		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

		mStreamVolume = (float) mAudioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC)
				/ mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

		// TODO - [done] make a new SoundPool, allowing up to 10 streams
		mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);


		// TODO - [done] set a SoundPool OnLoadCompletedListener that calls setupGestureDetector()
		mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				if(0 == status) {
					Log.d(TAG, "Sound loaded, sampleId: " + sampleId);
					setupGestureDetector();
				} else {
					Log.e(TAG, "Unable to load sound");
				}

			}
		});

		
		// TODO - [done] load the sound from res/raw/bubble_pop.wav
		mSoundID = mSoundPool.load(this, R.raw.bubble_pop, 1);

	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {

			// Get the size of the display so this View knows where borders are
			mDisplayWidth = mFrame.getWidth();
			mDisplayHeight = mFrame.getHeight();
			Log.d(TAG, "Frame width x height: " + mDisplayWidth + " x " + mDisplayHeight);

		}
	}

	// Set up GestureDetector
	private void setupGestureDetector() {
		Log.d(TAG, "Setting up GestureDetector");

		mGestureDetector = new GestureDetector(this,

		new GestureDetector.SimpleOnGestureListener() {

			// If a fling gesture starts on a BubbleView then change the
			// BubbleView's velocity

			@Override
			public boolean onFling(MotionEvent event1, MotionEvent event2,
					float velocityX, float velocityY) {

				// TODO - Implement onFling actions.
				// You can get all Views in mFrame using the
				// ViewGroup.getChildCount() method
				Log.d(TAG, "A Fling was detected");
				//Log.d(TAG, "The first motion event has");
				float startX = event1.getX();
				float startY = event1.getY();

				int childCount = mFrame.getChildCount();
				Log.d(TAG, "The number of children is " + childCount);

				for(int i = 0; i < childCount; i++) {
					View childView = mFrame.getChildAt(i);
					Rect bound = new Rect();
					childView.getHitRect(bound);
					if(bound.contains((int)startX, (int)startY)) { //No other way not to cast to Int?
						//This child is the touched down at first
						Log.d(TAG, "Found a buble to change its speed");

					}
				}

				return false;
			}

			// If a single tap intersects a BubbleView, then pop the BubbleView
			// Otherwise, create a new BubbleView at the tap's location and add
			// it to mFrame. You can get all views from mFrame with ViewGroup.getChildAt()

			@Override
			public boolean onSingleTapConfirmed(MotionEvent event) {

				// TODO - [working] Implement onSingleTapConfirmed actions.
				// You can get all Views in mFrame using the
				// ViewGroup.getChildCount() method

				int childCount = mFrame.getChildCount();
				Log.d(TAG, "Child count at start: " + childCount);

				float x = event.getX();
				float y = event.getY();

				boolean wasPopped = false;
				for(int i = 0; i < childCount; i++) {
					View childView = mFrame.getChildAt(i);
					BubbleView bubble = (BubbleView) childView;

					if(bubble.intersects(x, y)) {
						bubble.stop(true);
						wasPopped = true;
						break;
					}

				}
				//

				if(!wasPopped) {
					BubbleView bubbleView = new BubbleView(getApplicationContext(), x, y);

					mFrame.addView(bubbleView);

					childCount = mFrame.getChildCount();
					Log.d(TAG, "Child count at last: " + childCount);
				}

				

				return false;
			}
		});
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		// TODO - [done] Delegate the touch to the gestureDetector
		mGestureDetector.onTouchEvent(event);
		return false;
	
	}

	@Override
	protected void onPause() {
		
		// TODO - [done] Release all SoundPool resources
		mSoundPool.release();
		super.onPause();
	}

	// BubbleView is a View that displays a bubble.
	// This class handles animating, drawing, and popping amongst other actions.
	// A new BubbleView is created for each bubble on the display

	public class BubbleView extends View {

		private static final int BITMAP_SIZE = 64;
		private static final int REFRESH_RATE = 40;
		private final Paint mPainter = new Paint();
		private ScheduledFuture<?> mMoverFuture;
		private int mScaledBitmapWidth;
		private Bitmap mScaledBitmap;

		// location, speed and direction of the bubble
		private float mXPos, mYPos, mDx, mDy, mRadius, mRadiusSquared;
		private long mRotate, mDRotate;

		BubbleView(Context context, float x, float y) {
			super(context);

			Log.d(TAG, "Taped coord.: (" + x + ", " + y + ")");
			// Create a new random number generator to
			// randomize size, rotation, speed and direction
			Random r = new Random();

			// Creates the bubble bitmap for this BubbleView
			createScaledBitmap(r);

			// Radius of the Bitmap
			mRadius = mScaledBitmapWidth / 2;
			mRadiusSquared = mRadius * mRadius;

			Log.d(TAG, "Its radius is " + mRadius);

			// Adjust position to center the bubble under user's finger
			// The new mXPos, mYPos will be the top-left of the bitmap
			mXPos = x - mRadius;
			mYPos = y - mRadius;

			// Set the BubbleView's speed and direction
			setSpeedAndDirection(r);
			
			// Set the BubbleView's rotation
			setRotation(r);

			mPainter.setAntiAlias(true);

		}

		private void setRotation(Random r) {

			if (speedMode == RANDOM) {
				
				// TODO - [done] set rotation in range [1..3]

				mDRotate = r.nextInt(2) + 1;


			} else {
				mDRotate = 0;
			
			}
		}

		private void setSpeedAndDirection(Random r) {

			// Used by test cases
			switch (speedMode) {

			case SINGLE:

				mDx = 20;
				mDy = 20;
				break;

			case STILL:

				// No speed
				mDx = 0;
				mDy = 0;
				break;

			default:
				//RANDOM!
				// TODO - [done] Set movement direction and speed
				// Limit movement speed in the x and y
				// direction to [-3..3] pixels per movement.

				mDx = r.nextInt(6) - 3;
				mDy = r.nextInt(6) - 3;
			}
		}

		private void createScaledBitmap(Random r) {

			if (speedMode != RANDOM) {
				mScaledBitmapWidth = BITMAP_SIZE * 3;
			
			} else {
				//TODO - [done] set scaled bitmap size in range [1..3] * BITMAP_SIZE
				mScaledBitmapWidth = (r.nextInt(2) + 1) * BITMAP_SIZE;
			
			}

			// TODO - [done] create the scaled bitmap using size set above
			//Bitmap bubbleBitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.b64);
			mScaledBitmap = Bitmap.createScaledBitmap(mBitmap, mScaledBitmapWidth, mScaledBitmapWidth, true);
		}

		// Start moving the BubbleView & updating the display
		private void start() {

			// Creates a WorkerThread
			ScheduledExecutorService executor = Executors
					.newScheduledThreadPool(1);

			// Execute the run() in Worker Thread every REFRESH_RATE
			// milliseconds
			// Save reference to this job in mMoverFuture
			mMoverFuture = executor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {

					// TODO - implement movement logic.
					// Each time this method is run the BubbleView should
					// move one step. If the BubbleView exits the display, 
					// stop the BubbleView's Worker Thread. 
					// Otherwise, request that the BubbleView be redrawn. 

					//mXPos = mXPos + mDx;
					//mYPos = mYPos + mDy;


					
					
					
					

				}
			}, 0, REFRESH_RATE, TimeUnit.MILLISECONDS);
		}

		// Returns true if the BubbleView intersects position (x,y)
		private synchronized boolean intersects(float x, float y) {
			Log.d(TAG, "x:y = " + x + ":" + y);
			// TODO - Return true if the BubbleView intersects position (x,y)

			//This is based on rectangle not circle!
//			if((x >= mXPos && x <= mXPos + mScaledBitmapWidth) && (y >= mYPos && y <= mYPos + mScaledBitmapWidth)) {
//				return true;
//			}

			//This is more precise solution
			double area = (Math.pow(mXPos + mRadius - x, 2) + Math.pow(mYPos + mRadius - y, 2))*Math.PI;
			//Get the scaledBitMap's ...
			if( area < (Math.PI * mRadiusSquared)) {
				return true;
			}
			return false;
		}

		// Cancel the Bubble's movement
		// Remove Bubble from mFrame
		// Play pop sound if the BubbleView was popped

		private void stop(final boolean wasPopped) {

			if (null != mMoverFuture && !mMoverFuture.isDone()) {
				mMoverFuture.cancel(true);
			}

			// This work will be performed on the UI Thread
			mFrame.post(new Runnable() {
				@Override
				public void run() {

					// TODO - [done] Remove the BubbleView from mFrame
					mFrame.removeView(BubbleView.this);
					
					// TODO - [done] If the bubble was popped by user,
					// play the popping sound
					if (wasPopped) {
					
						mSoundPool.play(mSoundID, mStreamVolume, mStreamVolume, 1, 0, 1f);
						

					}
				}
			});
		}

		// Change the Bubble's speed and direction
		private synchronized void deflect(float velocityX, float velocityY) {

			//TODO - [working] set mDx and mDy to be the new velocities divided by the REFRESH_RATE
			mDx = velocityX/REFRESH_RATE;
			mDy = velocityY/REFRESH_RATE;
		}

		// Draw the Bubble at its current location
		@Override
		protected synchronized void onDraw(Canvas canvas) {

			// TODO - save the canvas
			canvas.save();

			
			// TODO - increase the rotation of the original image by mDRotate
//			Matrix matrix = new Matrix();
//			matrix.postRotate(mDRotate);
//			Bitmap rotatedBitmap = Bitmap.createBitmap(mScaledBitmap, 0, 0, mScaledBitmap.getWidth(), mScaledBitmap.getHeight(), matrix, true);
//			mScaledBitmap = rotatedBitmap;

			
			// TODO Rotate the canvas by current rotation
			// Hint - Rotate around the bubble's center, not its position
			//canvas.translate(mScaledBitmap.getWidth()/2, mScaledBitmap.getHeight()/2);
			canvas.rotate(mDRotate);



			
			// TODO - draw the bitmap at its new location
			canvas.drawBitmap(mScaledBitmap, mXPos, mYPos, mPainter);


			
			// TODO - restore the canvas
			canvas.restore();

			
		}

		// Returns true if the BubbleView is still on the screen after the move
		// operation
		private synchronized boolean moveWhileOnScreen() {

			// TODO - Move the BubbleView


			return false;
		}

		// Return true if the BubbleView is off the screen after the move
		// operation
		private boolean isOutOfView() {

			// TODO - Return true if the BubbleView is off the screen after
			// the move operation


			return false;
		}
	}

	// Do not modify below here

	@Override
	public void onBackPressed() {
		openOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		getMenuInflater().inflate(R.menu.menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_still_mode:
			speedMode = STILL;
			return true;
		case R.id.menu_single_speed:
			speedMode = SINGLE;
			return true;
		case R.id.menu_random_mode:
			speedMode = RANDOM;
			return true;
		case R.id.quit:
			exitRequested();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void exitRequested() {
		super.onBackPressed();
	}
}