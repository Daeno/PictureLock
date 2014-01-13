

package com.example.vvdpicturelock;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String  TAG = "VVDPictureLock::Main::Activity";
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int MEDIA_TYPE_IMAGE = 1;
	private Uri fileUri;
	
	private boolean isSetting = false;
	
	// matching parameters
	private float GOOD_MATCH_RATIO = 0.7f;
	private float FINAL_MATCH_RATIO = 0.45f;
	private int   KNN_K = 2;
	
	private MenuItem mItemSetting;
	private MenuItem mItemTesting;
	private MenuItem mItemShow;
	private int		 currentLayout;
	private boolean  isPass = false;
	
	double psnrV;
	Scalar mssimV;
	String path = Environment.getExternalStorageDirectory().getPath() + "/Android/data/com.example.vvdpicturelock/files/Pictures/VVDPictureLock/";
	String password = path + "password.jpg";
	
	 private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

       @Override
       public void onManagerConnected(int status) {
           switch (status) {
               case LoaderCallbackInterface.SUCCESS:
               {
                   Log.i(TAG, "OpenCV loaded successfully");

                   /* Now enable camera view to start receiving frames */
                   //mOpenCvCameraView.setOnTouchListener(Puzzle15Activity.this);
                   //mOpenCvCameraView.enableView();
               } break;
               default:
               {
                   super.onManagerConnected(status);
               } break;
           }
       }
   };
   
   Handler handler = new Handler(){
	   @Override
	   public void handleMessage(Message msg){
		   TextView pas = (TextView) findViewById( R.id.psnrbox);
		   switch(msg.what){
		   	case -1:
		   		pas.setText("Pending!!!");
			   	break;
		   	case 0:
		   		if(isPass)
					pas.setText("You Pass!!!");
				else
					pas.setText("You SHALL NOT PASSSSSSSSSSS!!!");
		   		break;
		   }
	   }
   };
	private void settingBound(){
		TextView box = (TextView) findViewById( R.id.box );
		
		fileUri = getOutputMediaUri( MEDIA_TYPE_IMAGE );
		box.setText( fileUri.getPath() );
		Button btnShot = (Button) findViewById( R.id.btnShot);
		btnShot.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				fileUri = getOutputMediaUri( MEDIA_TYPE_IMAGE );
				TextView box = (TextView) findViewById( R.id.box );
				box.setText( fileUri.getPath() );
				Intent pictureIntent = new Intent( MediaStore.ACTION_IMAGE_CAPTURE );
				pictureIntent.putExtra( MediaStore.EXTRA_OUTPUT, fileUri );
				startActivityForResult( pictureIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE );
			}
		});
		Button btnRun = (Button) findViewById(R.id.runTest);
		btnRun.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				isPass = false;
				Message msg = Message.obtain();
				msg.what = -1;
				handler.sendMessage(msg);
				new Thread(){
					public void run(){
						imageDetection(path + "testing.jpg");
						Message msg = Message.obtain();
						msg.what = 0;
						handler.sendMessage(msg);
					}
				}.start();
			}
		});
		
		final TextView thredTxt = (TextView) findViewById(R.id.textFinalThreshold);
		SeekBar thred = (SeekBar) findViewById(R.id.finalThreshold);
		thred.setProgress((int)(100*FINAL_MATCH_RATIO));
		thredTxt.setText("final match ratio: " + Float.toString(FINAL_MATCH_RATIO));
		thred.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				float f = (float)progress / 100.0f;
				thredTxt.setText("final match ratio: " + Float.toString(f));
				FINAL_MATCH_RATIO = f;
			}
		});
		
		
		final TextView thredTxt2 = (TextView) findViewById(R.id.textGoodThreshold);
		SeekBar thred2 = (SeekBar) findViewById(R.id.goodThreshold);
		thredTxt2.setText("good match ratio: " + Float.toString(GOOD_MATCH_RATIO));
		thred2.setProgress((int)(100*GOOD_MATCH_RATIO));
		thred2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				float f = (float)progress / 100.0f;
				thredTxt2.setText("good match ratio: " + Float.toString(f));
				GOOD_MATCH_RATIO = f;
			}
		});
		
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		currentLayout = R.layout.activity_main;
		settingBound();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.main, menu);
		mItemSetting = menu.add("Setting Password");
		mItemTesting = menu.add("Testing");
		mItemShow = menu.add("Show Result");
		return true;
	}
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		if (item == mItemSetting){
			currentLayout = R.layout.activity_main;
			setContentView(R.layout.activity_main);
			isPass = false;
			isSetting = true;
			Log.i("Menu","setting = true");
		}
		else if(item == mItemTesting){
			currentLayout = R.layout.activity_main;
			setContentView(R.layout.activity_main);
			isPass = false;
			isSetting = false;
			Log.i("Menu","setting = false");
		}
		else if(item == mItemShow){
			currentLayout = R.layout.view_result;
			setContentView(R.layout.view_result);
			
			ImageView s = (ImageView) findViewById(R.id.resultGoodView);
			File resultFile = new File(path + "result_good.jpg");
			s.setImageURI(Uri.fromFile(resultFile));
			s = (ImageView) findViewById(R.id.resultFinalView);
			resultFile = new File(path + "result_final.jpg");
			s.setImageURI(Uri.fromFile(resultFile));
		}
		return true;
	 }
	protected void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		if ( requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE )
		{
		   if ( resultCode == RESULT_OK )
		   {
		   	try
		   	{		   
		   	   if(!isSetting){
			   	   //imageQualityEstimaton( fileUri.getPath() );
			   	   //TextView pbox = (TextView) findViewById( R.id.psnrbox );
			   	   //TextView mbox = (TextView) findViewById( R.id.mssimbox );
			   	   /*
			   	   if ( psnrV == 0 )
			   	      pbox.setText( "The Same!" );
			   	   else
			   	   	pbox.setText( Double.toString( psnrV ) );
			   	   
			   	   mbox.setText( "R: " + mssimV.val[ 2 ] * 100 + "%\n" + "G: " + mssimV.val[ 1 ] * 100 + "%\n"
			   	   		+ "B: " + mssimV.val[ 0 ] * 100 + "%" );
			   	   */
			   	   
			   	   //imageDetection( fileUri.getPath() );
			   	   
		   	   }
		   	}
		   	catch ( Exception e)
		   	{
		   		
		   	}
		   }
		}
	}
	
	private Uri getOutputMediaUri( int type )
	{
		return Uri.fromFile( getOutputMediaFile( type ) );
	}
	
	private File getOutputMediaFile( int type )
	{		
		File mediaStorageDirectory = new File( getApplicationContext().getExternalFilesDir( Environment.DIRECTORY_PICTURES ), "VVDPictureLock" );
		
		if ( !mediaStorageDirectory.exists() )
		{
			if ( !mediaStorageDirectory.mkdirs() )
			{
				Log.d( "VVDPictureLock", "failed to create directory" );
				return null;
			}
		}
		
		//String timeStamp = new SimpleDateFormat( "yyMMdd_HHmmss").format( new Date() );
		File mediaFile = null;
		
		if ( type == MEDIA_TYPE_IMAGE ){
			if(isSetting)
				mediaFile = new File( mediaStorageDirectory.getPath() + File.separator + "password.jpg" );
			else
				mediaFile = new File( mediaStorageDirectory.getPath() + File.separator + "testing.jpg" );
		}
			
		
		return mediaFile;
	}
	
	private void imageQualityEstimaton( String proposal )
	{
		final int PSNRTriggerValue = 40;
		Log.i(TAG,"inside image Quality");

		Mat sample = Highgui.imread( proposal );//, Highgui.CV_LOAD_IMAGE_COLOR );
		Mat reference = Highgui.imread( password ); //, Highgui.CV_LOAD_IMAGE_COLOR );
		Log.i("imageUri", proposal);
		Log.i("imageUri", password);
		Log.i(TAG,"image load succeed");
		psnrV = getPSNR( reference, sample );
		
		if ( psnrV < PSNRTriggerValue && psnrV > 0 )
		{
			mssimV = getMSSIM( reference, sample );
		}
		Log.i(TAG,"Algo finished");
		ImageView s = (ImageView) findViewById( R.id.sample );
		ImageView r = (ImageView) findViewById( R.id.reference );
		s.setImageURI( fileUri );
		r.setImageURI( Uri.parse(password) );
	}
	
	private double getPSNR( final Mat R, final Mat S )
	{
		Mat difference = new Mat();
		Log.i("Matrix Size",R.size().toString());
		Log.i("Matrix Size",S.size().toString());
		Core.absdiff( R, S, difference );
		difference.convertTo( difference, CvType.CV_32F );
		difference = difference.mul( difference );
		
		Scalar sum = Core.sumElems( difference );
		double sse = sum.val[ 0 ] + sum.val[ 1 ] + sum.val[ 2 ];
		
		if ( sse <= 1e-10 )
			return 0;
		else
		{
			double mse = sse / ( double ) ( R.channels() * R.total() );
			double psnr = 10.0 * Math.log10( ( 255 * 255 ) / mse );
			return psnr;
		}
	}
	
	private Scalar getMSSIM( final Mat R, final Mat S )
	{
		int d = CvType.CV_32F;		
				
		Mat convertedR = new Mat();
		Mat convertedS = new Mat();
		
		R.convertTo( convertedR, d );
		S.convertTo( convertedS, d );
		
		Mat squaredR = convertedR.mul( convertedR );
		Mat squaredS = convertedS.mul( convertedS );
		Mat RSProduct = convertedR.mul( convertedS );
		
		
		Mat blurredR = new Mat();
		Mat blurredS = new Mat();
		
		Imgproc.GaussianBlur( convertedR, blurredR, new Size( 11, 11 ), 1.5 );
		Imgproc.GaussianBlur( convertedS, blurredS, new Size( 11, 11 ), 1.5 );
		
		Mat blurredSquaredR = convertedR.mul( convertedR );
		Mat blurredSquaredS = convertedS.mul( convertedS );
		Mat RSBlurredProduct = convertedR.mul( convertedS );
		
		Mat sigmaRSquared = new Mat();
		Mat sigmaSSquared = new Mat();
		Mat sigmaRSProduct = new Mat();
		
		Imgproc.GaussianBlur( squaredR, sigmaRSquared, new Size( 11, 11 ), 1.5 );
		Core.subtract( sigmaRSquared, blurredSquaredR, sigmaRSquared );
		Imgproc.GaussianBlur( squaredS, sigmaSSquared, new Size( 11, 11 ), 1.5 );
		Core.subtract( sigmaSSquared, blurredSquaredS, sigmaSSquared );
		Imgproc.GaussianBlur(RSProduct, sigmaRSProduct, new Size( 11, 11 ), 1.5 );
		Core.subtract( sigmaRSProduct, RSBlurredProduct, sigmaRSProduct );
		
		
		
		Mat m1 = new Mat();
		Mat m2 = new Mat();
		Mat m3 = new Mat();
		Mat C1 = new Mat( R.rows(), R.cols(), RSBlurredProduct.type(), new Scalar( 6.5025 ) );
		Mat C2 = new Mat( R.rows(), R.cols(), sigmaRSProduct.type(), new Scalar( 58.5225 ) );	
		
		Core.scaleAdd( RSBlurredProduct, 2.0, C1, m1 );
		Core.scaleAdd( sigmaRSProduct, 2.0, C2, m2 );
		m3 = m1.mul( m2 );
		
		Core.add( blurredSquaredR, blurredSquaredS, m1 );
		Core.add( m1, C1, m1 );
		Core.add( sigmaRSquared, sigmaSSquared, m2 );
		Core.add( m2, C2, m2 );
		m1 = m1.mul( m2 ); //
		
		Mat ssim_map = new Mat();
		Core.divide( m3, m1, ssim_map );
		
		Scalar mssim = Core.mean( ssim_map );
		return mssim;
	}
	
	private void imageDetection( String proposal )
	{
		isPass = false;
		Log.i("abcdef", "imagedetect" );
		long StartTime = System.currentTimeMillis();
		MatOfKeyPoint kpR = new MatOfKeyPoint();
		MatOfKeyPoint kpS = new MatOfKeyPoint();
		ArrayList<MatOfDMatch> matches = new ArrayList<MatOfDMatch>();
		
		//MatOfDMatch matches = new MatOfDMatch();
		MatOfDMatch goodMatches = new MatOfDMatch();
		Mat descR = new Mat();
		Mat descS = new Mat();
		Size dst_size = new Size();
		float scale = 0.3f;
		Mat reference = Highgui.imread( password, Highgui.IMREAD_GRAYSCALE );
		Mat sample = Highgui.imread( proposal, Highgui.IMREAD_GRAYSCALE );
		dst_size.width = reference.size().width*scale;
		dst_size.height = reference.size().height*scale;
		Imgproc.resize(reference, reference, dst_size);
		
		dst_size.width = sample.size().width*scale;
		dst_size.height = sample.size().height*scale;
		
		Imgproc.resize(sample, sample, dst_size);
		
		FeatureDetector detector = FeatureDetector.create( FeatureDetector.FAST );
		DescriptorExtractor descriptor = DescriptorExtractor.create( DescriptorExtractor.FREAK );
		DescriptorMatcher matcher = DescriptorMatcher.create( DescriptorMatcher.BRUTEFORCE_HAMMING );
		
		Imgproc.equalizeHist(reference, reference);
		Imgproc.equalizeHist(sample, sample);
		
		Rect objectBoundary = new Rect(reference.width()/4, reference.height()/4, reference.width()/2, reference.height()/2);
		detector.detect( reference, kpR );
		{
			ArrayList<KeyPoint> temp = new ArrayList<KeyPoint>();
			KeyPoint[] kk = kpR.toArray(); 
			for(int i=0;i<kk.length;i++){
				if(kk[i].pt.inside(objectBoundary)){
					temp.add(kk[i]);
				}
			}
			kpR.fromList(temp);
		}
		descriptor.compute( reference, kpR, descR );
		detector.detect( sample, kpS );
		descriptor.compute( sample, kpS, descS );
	//	matcher.match( descS, descR, matches );	
		matcher.knnMatch( descS, descR, matches, KNN_K );
		Log.i("metches", Integer.toString(matches.size()));
		
	//=========CrossCheck==========	
/*		ArrayList<DMatch> crossCheckList = new ArrayList<DMatch>();
		MatOfDMatch invMatches = new MatOfDMatch();		
		MatOfDMatch crossCheckMatches = new MatOfDMatch();
		
		matcher.match( descR, descS, invMatches );
		getCrossCheckedMatches( matches, invMatches, crossCheckList );
		crossCheckMatches.fromList( crossCheckList );*/
		
		
		/*
	 //========CrossCheck K=========
		Log.i("abcdef", "in K");
		ArrayList<MatOfDMatch> crossCheckList = new ArrayList<MatOfDMatch>();
		ArrayList<MatOfDMatch> invMatches = new ArrayList<MatOfDMatch>();
		ArrayList<DMatch> temp = new ArrayList<DMatch>();
		MatOfDMatch crossCheckMatches = new MatOfDMatch();
		
		matcher.knnMatch( descR, descS, invMatches, KNN_K );
		Log.i("abcdef", "123");
		getCrossCheckedMatchesK( matches, invMatches, crossCheckList );
		Log.i("abcdef", "456");
		
		for ( int i = 0; i < crossCheckList.size(); i++ )
		{
			DMatch[] mArray =  crossCheckList.get( i ).toArray();
			temp.add( mArray[ 0 ] );
		}
		
		crossCheckMatches.fromList( temp );
		Log.i("abcdef", "out K");*/
	// ========CrossCheck K End=========
		
		
	   ArrayList<DMatch> gmList = new ArrayList<DMatch>(); 
			 
//	 getGoodMatches( matches, descS.rows(), gmList );
	   getGoodMatchesK( matches, descS.rows(), gmList );
		goodMatches.fromList( gmList );
		
	//========CrossCheck G=========
		/*ArrayList<DMatch> crossCheckListG = new ArrayList<DMatch>();
		MatOfDMatch	ccMatches = new MatOfDMatch();		
		MatOfDMatch ccGoodMatches = new MatOfDMatch();	
	   
		ccMatches.fromList(crossCheckList);
		Log.i("abcdef", "123123123");
		getGoodMatches( ccMatches, ccMatches.rows(), crossCheckListG );
		
		ccGoodMatches.fromList( crossCheckListG );
		
		Log.i("abcdef", "out G");*/
		//========CrossCheck G End=========	
		
		/*
	 //========CrossCheck KG=========
		
		Log.i("abcdef", "in KG");
	   ArrayList<DMatch> crossCheckListG = new ArrayList<DMatch>();
		MatOfDMatch ccGoodMatches = new MatOfDMatch();	
	   
		getGoodMatchesK( crossCheckList, crossCheckList.size(), crossCheckListG );
		ccGoodMatches.fromList( crossCheckListG );
		Log.i("abcdef", "out KG");
		// ========CrossCheck KG End========= 
		*/
		
		
		
		ArrayList<Point> objectList = new ArrayList<Point>();
		ArrayList<Point> sceneList = new ArrayList<Point>();
		KeyPoint[] kpRArray = kpR.toArray();
		KeyPoint[] kpSArray = kpS.toArray();
		
		for ( int i = 0; i < gmList.size(); i++ )
		{
			//if(kpRArray[ gmList.get( i ).trainIdx].pt.inside(objectBoundary)){
				sceneList.add( kpSArray[ gmList.get( i ).queryIdx ].pt );
				objectList.add( kpRArray[ gmList.get( i ).trainIdx ].pt );
			/*}else{
				Log.i("HOMO","outside");
			}*/
			
		}
		
		MatOfPoint2f object = new MatOfPoint2f();
		MatOfPoint2f scene = new MatOfPoint2f();
		MatOfDMatch finalMatches = new MatOfDMatch();
		object.fromList( objectList );
		scene.fromList( sceneList );
		if(objectList.size() >= 4){
			Mat status = new Mat();
			Mat H = Calib3d.findHomography( object, scene, Calib3d.RANSAC, 3, status );
			
			
			ArrayList<DMatch> finalList = new ArrayList<DMatch>();		
			
			for ( int i = 0; i < objectList.size(); i++ )
			{
				if ( status.get( i, 0 )[ 0 ] != 0)
					finalList.add( gmList.get( i ) );
			}
			if(finalList.size() >= FINAL_MATCH_RATIO * gmList.size()){
				isPass = true;
			}else{
				isPass = false;
			}
			finalMatches.fromList( finalList );
			Log.i("final","size: " + Integer.toString(finalMatches.rows()));
		}
		else{
			Log.i("final","not pass");
		}
		Mat imageMatches = new Mat();
		File mediaStorageDirectory = new File( getApplicationContext().getExternalFilesDir( Environment.DIRECTORY_PICTURES ), "VVDPictureLock" );
		String timeStamp = new SimpleDateFormat( "yyMMdd_HHmmss").format( new Date() );
			
		//Features2d.drawMatches( sample, kpS, reference, kpR, crossCheckMatches, imageMatches, Scalar.all( -1 ), Scalar.all( -1 ), new MatOfByte(), Features2d.NOT_DRAW_SINGLE_POINTS );
		//Highgui.imwrite(  mediaStorageDirectory.getPath() + File.separator + "OUT_CC_" + timeStamp + ".jpg", imageMatches );
		Features2d.drawMatches( sample, kpS, reference, kpR, goodMatches, imageMatches, Scalar.all( -1 ), Scalar.all( -1 ), new MatOfByte(), Features2d.NOT_DRAW_SINGLE_POINTS );
		Highgui.imwrite(  mediaStorageDirectory.getPath() + File.separator + "OUT_GOOD_" + timeStamp + ".jpg", imageMatches );
		//Features2d.drawMatches( sample, kpS, reference, kpR, ccGoodMatches, imageMatches, Scalar.all( -1 ), Scalar.all( -1 ), new MatOfByte(), Features2d.NOT_DRAW_SINGLE_POINTS );
		//Highgui.imwrite(  mediaStorageDirectory.getPath() + File.separator + "OUT_CCG_" + timeStamp + ".jpg", imageMatches );
		Core.rectangle(reference,objectBoundary.tl(),objectBoundary.br(),Scalar.all(-1));
		Features2d.drawMatches( sample, kpS, reference, kpR, finalMatches, imageMatches, Scalar.all( -1 ), Scalar.all( -1 ), new MatOfByte(), Features2d.NOT_DRAW_SINGLE_POINTS );
		Highgui.imwrite(  mediaStorageDirectory.getPath() + File.separator + "OUT_FINAL_" + timeStamp + ".jpg", imageMatches );		
		
		
		
		long EndTime = System.currentTimeMillis();
	   	long ExecutionTime = EndTime - StartTime;
	   	Log.i("abcdef", "Time: " + ExecutionTime );
	   	System.gc();
	}

	private void getGoodMatches( MatOfDMatch matches, int rows, ArrayList<DMatch> gmList )
	{
		double distance;
		double minDistance = 100000;
		
		DMatch[] mArray =	matches.toArray();
		
		for ( int i = 0; i < rows; i++ )
		{
			distance = mArray[ i ].distance;
						
			if ( distance < minDistance )
				minDistance = distance;
		}
		
		for ( int i = 0; i < rows; i++ )
		{
			if ( mArray[ i ].distance <= Math.max( 1.23 * minDistance, 0.02 ) )
				gmList.add( mArray[ i ] );			
		}
		
		Log.i("abcdef", "out goodMatches");
	}
	
	private void getGoodMatchesK( ArrayList<MatOfDMatch> matches, int rows, ArrayList<DMatch> gmList )
	{
      DMatch[] mArray;
		
		for ( int i = 0; i < rows; i++ )
		{
			if ( matches.get( i ).rows() >= 2 )
			{
				mArray = matches.get( i ).toArray();
				DMatch m1 = mArray[ 0 ];
				DMatch m2 = mArray[ 1 ];
				
				if ( m1.distance < GOOD_MATCH_RATIO * m2.distance )
				   gmList.add( m1 );
				/*else
					Log.i("GMK","fail");*/
			}
		}
		Log.i("GMK","Size: " + Integer.toString(gmList.size()));
	}
	
	private void getCrossCheckedMatches( MatOfDMatch matches, MatOfDMatch invMatches , ArrayList<DMatch> ccList )
	{
		DMatch[] mArray = matches.toArray();
		DMatch[] invMArray = invMatches.toArray();
		DMatch forward;
		DMatch backward;
		
		for ( int i = 0; i < mArray.length; i++ )
		{
			forward = mArray[ i ];			
			backward = invMArray[ forward.trainIdx ];
			
			if ( backward.trainIdx == forward.queryIdx )
				ccList.add( forward );			
		}
	}
	
	private void getCrossCheckedMatchesK( ArrayList<MatOfDMatch> matches, ArrayList<MatOfDMatch> invMatches , ArrayList<MatOfDMatch> ccList )
	{
		DMatch[] mArray;
		DMatch[] invMArray;
		DMatch forward;
		DMatch backward;
		
		Log.i("abcdef", "aasdf");
		for ( int i = 0; i < matches.size(); i++ )
		{
			mArray = matches.get( i ).toArray();
			
		//	if ( matches.get( i ).rows() >= 2 )
			//{
				boolean flag = true;
				forward = mArray[ 0 ];
				invMArray = invMatches.get( forward.trainIdx ).toArray();
				for(int j = 0;j<invMArray.length && flag;j++){
					backward = invMArray[ j ];
					
					if ( backward.trainIdx == forward.queryIdx )
					{
						MatOfDMatch f = new MatOfDMatch();
						f.fromArray( mArray );
						ccList.add( f );
						flag = false;
					}
				}
		//	}
		}
		
		Log.i("CCMK", "size: " + Integer.toString(ccList.size()));
	}
	
	public void onResume()
   {
       super.onResume();
       OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
   }
	@Override
	public void onContentChanged(){
		 	if(currentLayout == R.layout.activity_main){
				settingBound();
		 	}
	}
}



