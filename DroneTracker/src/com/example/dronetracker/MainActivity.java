package com.example.dronetracker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.example.Mail.Mail;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity  implements LocationListener{
    // Store the value of location globally so it can be sent via email
	public Location location;
	// Boolean to control sending of Emails before pictures are taken
	public boolean photoTaken = false;
	// Boolean to prevent sending a photo while the location is being found
	public boolean photoInProgress = false;
	// Storage location of image file
	final File file = new File(Environment.getExternalStorageDirectory()+"/DCIM/Camera", "IMG_Drone.jpg");
	// Initialize the camera preview
	private TextureView mTextureView = null;
	private Size mPreviewSize = null;
	private CameraDevice mCameraDevice = null;
	private CaptureRequest.Builder mPreviewBuilder = null;
	private CameraCaptureSession mPreviewSession = null;
	
	// Create the Surface Texture Listener
	private TextureView.SurfaceTextureListener mSurfaceTextureListner = new TextureView.SurfaceTextureListener() {
		
		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture surface) {
			// TODO Auto-generated method stub			
		}
		
		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
				int height) {
			// TODO Auto-generated method stub
		}
		
		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
			// TODO Auto-generated method stub
			return false;
		}
		
		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
				int height) {
			// TODO Auto-generated method stub			
			CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
			try{
				String cameraId = manager.getCameraIdList()[0];
				CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
				StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
				mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];
				manager.openCamera(cameraId, mStateCallback, null);
			}
			catch(CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
	};
	
	private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
		
		@Override
		public void onOpened(CameraDevice camera) {
			// TODO Auto-generated method stub
			mCameraDevice = camera;
			
			SurfaceTexture texture = mTextureView.getSurfaceTexture();
			if (texture == null) {
				return;
			}
			
			texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
			Surface surface = new Surface(texture);
			
			try {
				mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			} catch (CameraAccessException e){
				e.printStackTrace();
			}
			
			mPreviewBuilder.addTarget(surface);
			
			try {
				mCameraDevice.createCaptureSession(Arrays.asList(surface), mPreviewStateCallback, null);
			} catch (CameraAccessException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void onError(CameraDevice camera, int error) {
			// TODO Auto-generated method stub			
		}
		
		@Override
		public void onDisconnected(CameraDevice camera) {
			// TODO Auto-generated method stub			
		}
	};
	
	// Start the Camera Session
	private CameraCaptureSession.StateCallback mPreviewStateCallback = new CameraCaptureSession.StateCallback() {
		
		@Override
		public void onConfigured(CameraCaptureSession session) {
			// TODO Auto-generated method stub
			mPreviewSession = session;
			mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
			HandlerThread backgroundThread = new HandlerThread("CameraPreview");
			backgroundThread.start();
			Handler backgroundHandler = new Handler(backgroundThread.getLooper());
			
			try {
				mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
			} catch (CameraAccessException e) {
				e.printStackTrace();
			}
			
		}
		
		@Override
		public void onConfigureFailed(CameraCaptureSession session) {
			// TODO Auto-generated method stub
		}
	};
	
	// Runs when the app starts
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);
		mTextureView = (TextureView) findViewById(R.id.textureView1);
		mTextureView.setSurfaceTextureListener(mSurfaceTextureListner);
		
	}

	// Runs when the app is exited, but not closed
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (mCameraDevice != null)
		{
			mCameraDevice.close();
			mCameraDevice = null;
		}
	}
	
	// Send the email
	public void sendEmail(View view){
		// Prevents sending if the photo has not been taken
		if (photoTaken){
			// Set up the email to send from, and the email to mail to
			Mail email = new Mail("gdcerau@gmail.com","NinjaTurtleSwag");
			String[] toArr = {"croninstephen347@gmail.com"};
			email.setTo(toArr);
			email.setFrom("DroneyTracker@Droney.com");
			
			// If location was found, add it to the subject line
			if (location != null){
			email.setSubject("The following was sent from the DroneTracker Application. Lat: "+location.getLatitude()+", Long: "+location.getLongitude());
			}
			else{
				email.setSubject("The following was sent from the DroneTracker Application. Location Unavailable");
			}
			
			// Set body of email to description
			TextView desc = (EditText)findViewById(R.id.editText1);
			email.setBody("Description: "+desc.getText());
			
			// Send the email
			AsyncTaskRunner runner = new AsyncTaskRunner();
		    runner.execute(email);
		    
		    // Reset camera *** to be added later
		    photoTaken = false;
		    Toast.makeText(MainActivity.this, "Email Sent!",Toast.LENGTH_SHORT).show();
		    
		    // OLD Email chooser:
//		Intent email = new Intent(Intent.ACTION_SEND);
//    	email.putExtra(Intent.EXTRA_EMAIL, new String[]{"croninstephen347@gmail.com"});		  
//    	email.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
//    	email.putExtra(Intent.EXTRA_SUBJECT, "The following was sent from the DroneTracker Application.\n  Lat: "+location.getLatitude()+", Long: "+location.getLongitude());
//    	TextView desc = (EditText)findViewById(R.id.editText1);
//    	email.putExtra(Intent.EXTRA_TEXT,"Description: "+desc.getText());
//    	email.setType("*/*");
    	
    	//startActivity(Intent.createChooser(email, "Choose an Email client :"));
		}
		else{
			if (photoInProgress){
				Toast.makeText(MainActivity.this, "Please wait for location to be found...",Toast.LENGTH_SHORT).show();
			}
			else{
			Toast.makeText(MainActivity.this, "Take a photo first!",Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	// Capture button takes picture
	public void takePhoto(View view){
		// Prevent photo from being emailed until process is complete
		photoInProgress = true;
		
		// Check to see if camera is open
        if(null == mCameraDevice) {  
        	return;  
        }  
        
        // Get the GPS Location
        LocationManager locationMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);  
        
        // Capture the picture
        try {  
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());  
            Size[] jpegSizes = null; 
            
            // Set up camera type
            if (characteristics != null) {  
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);  
            }  
            
            // This does NOT set the image size, but it is a guess if all else fails
            int width = 480;  
            int height = 480;  
            
            // Choose the best quality image available from the camera
            if (jpegSizes != null && 0 < jpegSizes.length) {  
                width = jpegSizes[0].getWidth();  
                height = jpegSizes[0].getHeight();  
            }  
              
            // Read the image from the camera
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);  
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);  
            outputSurfaces.add(reader.getSurface());  
            outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));  
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);  
            captureBuilder.addTarget(reader.getSurface());  
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);  

            // Set up a image available listener
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {  
  
                @Override  
                public void onImageAvailable(ImageReader reader) {  
                    Image image = null;  
                    // Capture the image
                    try {  
                        image = reader.acquireLatestImage();  
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];                        
                        buffer.get(bytes);  
                        // Save the image using the save method
                        save(bytes);  
                    } catch (FileNotFoundException e) {  
                        e.printStackTrace();  
                    } catch (IOException e) {  
                        e.printStackTrace();  
                    } finally {  
                        if (image != null) {  
                            image.close();
                            // Wait 20 seconds to time out if GPS location could not be found
                            int count=0;
                            while (location == null &&  count < 20){
                            	SystemClock.sleep(1000);
                            	count++;
                            }
                            
                            // GEO-tag the image if location was found
                            if (location != null){
                            loc2Exif(file.getPath(), location);
                            }
                            else{
                            	Toast.makeText(MainActivity.this, "Failed to find location!", Toast.LENGTH_SHORT).show();
                            }
                            // Allow the picture to be sent
                            photoTaken = true;
                            photoInProgress = false;
                        }  
                    }  
                }  
                
                // Save the image
                private void save(byte[] bytes) throws IOException {  
                    OutputStream output = null;  
                    try {  
                        output = new FileOutputStream(file);  
                        // Crop the image
                        Bitmap bmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                        Bitmap cropped = Bitmap.createBitmap(bmap,0,0,bmap.getWidth(),bmap.getWidth());
                        cropped = Bitmap.createScaledBitmap(cropped, 480, 480, true);
                        cropped.compress(Bitmap.CompressFormat.JPEG, 85, output);
                        
                    } finally {  
                        if (null != output) {  
                            output.close();  
                        }  
                    }  
                }  
                  
            };  
              
            // Run the camera capture on a separate thread
            HandlerThread thread = new HandlerThread("CameraPicture");  
            thread.start();  
            final Handler backgroudHandler = new Handler(thread.getLooper());  
            reader.setOnImageAvailableListener(readerListener, backgroudHandler);  
              
            // Create a capture listener for when the image has been saved
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {  
            	  
                @Override  
                public void onCaptureCompleted(CameraCaptureSession session,  
                        CaptureRequest request, TotalCaptureResult result) {  
                    		super.onCaptureCompleted(session, request, result);  
                    		Toast.makeText(MainActivity.this, "Saved:"+file, Toast.LENGTH_SHORT).show();  
                }  
                  
            };  
              
            // start the capture session
            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {  
                  
                @Override  
                public void onConfigured(CameraCaptureSession session) {  
  
                    try {  
                        session.capture(captureBuilder.build(), captureListener, backgroudHandler);  
                    } catch (CameraAccessException e) {  
                        e.printStackTrace();  
                    }  
                }  
                  
                @Override  
                public void onConfigureFailed(CameraCaptureSession session) {  
                      
                }  
            }, backgroudHandler);  
              
        } catch (CameraAccessException e) {  
            e.printStackTrace();  
        }  
        
    }
	
	// Save the location
	@Override
	public void onLocationChanged(Location loc) {
		location = loc;
	}

	// Required (GPS)
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub	
	}

	// Required (GPS)
	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
	}

	// Required (GPS)
	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
	}  
	
	// GEO-Tag the image
	//http://stackoverflow.com/questions/10531544/write-geotag-jpegs-exif-data-in-android
	public void loc2Exif(String flNm, Location loc) {
		
		  try {
		    ExifInterface ef = new ExifInterface(flNm);
		    ef.setAttribute(ExifInterface.TAG_GPS_LATITUDE, dec2DMS(loc.getLatitude()));
		    ef.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,dec2DMS(loc.getLongitude()));
		    if (loc.getLatitude() > 0) 
		      ef.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N"); 
		    else              
		      ef.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
		    if (loc.getLongitude()>0) 
		      ef.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");    
		     else             
		       ef.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
		    ef.saveAttributes();
		    Toast.makeText(MainActivity.this, "Location Saved!", Toast.LENGTH_SHORT).show();
		    
		  } catch (IOException e) {}         
		}
	
	// Reformat the GPS Data
	String dec2DMS(double coord) {  
		  coord = coord > 0 ? coord : -coord;  // -105.9876543 -> 105.9876543
		  String sOut = Integer.toString((int)coord) + "/1,";   // 105/1,
		  coord = (coord % 1) * 60;         // .987654321 * 60 = 59.259258
		  sOut = sOut + Integer.toString((int)coord) + "/1,";   // 105/1,59/1,
		  coord = (coord % 1) * 60000;             // .259258 * 60000 = 15555
		  sOut = sOut + Integer.toString((int)coord) + "/1000";   // 105/1,59/1,15555/1000
		  return sOut;
		}
	
	// Send email on separate thread
	private class AsyncTaskRunner extends AsyncTask<Mail, Void, Void> {
		
		  @Override
		  protected Void doInBackground(Mail... params) {
			  Mail email = (Mail)params[0];
			  try {
					email.addAttachment(file.getPath());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					if(email.send()) { 
					      Toast.makeText(MainActivity.this, "Email was sent successfully.", Toast.LENGTH_LONG).show(); 
					    } else { 
					      Toast.makeText(MainActivity.this, "Email was not sent.", Toast.LENGTH_LONG).show(); 
					    }
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			  return null;
		   
		  }
		  // Required?? (EMAIL)
		  @Override
		  protected void onPreExecute() {
		   // Things to be done before execution of long running operation. For
		   // example showing ProgessDialog
		  }

		 }
	
}