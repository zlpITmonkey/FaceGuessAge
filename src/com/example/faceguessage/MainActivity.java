package com.example.faceguessage;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.faceguessage.FaceDetect.Callback;
import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class MainActivity extends Activity implements View.OnClickListener {

    private static final int PICK_CODE=0x110;
    private static final int CAMERA_PHOTO = 0x113;
    private ImageView mPhoto;
    private Button mCamera;
    private Button mGetImage;
    private Button mDetect;
    private TextView mTip;
    private File phoneFile;
    private View mWaiting;
    private Boolean isCamera=false;
    private Boolean isAlbum=false;

    private Bitmap mPhotoImg;
    private String facephoto;

    private String mCurrentPhotoStr;

    private Paint mPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        initViews();
        initEvents();
        mPaint=new Paint();

    }

    public void initEvents(){
        mGetImage.setOnClickListener(this);
        mDetect.setOnClickListener(this);
        mCamera.setOnClickListener(this);
    }

    public void initViews()
    {
        mPhoto=(ImageView)findViewById(R.id.id_image);
        mGetImage=(Button)findViewById(R.id.id_getImage);
        mDetect=(Button)findViewById(R.id.id_detect);
        mCamera=(Button)findViewById(R.id.id_camera);
        mTip=(TextView)findViewById(R.id.id_tip);
        mWaiting=findViewById(R.id.id_waiting);
        
    }

    protected void onActivityResult(int requestCode, int resultCode,Intent intent){
        if(requestCode==PICK_CODE)
        {
            if(intent!=null){
                Uri uri=intent.getData();
                Cursor cursor=getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();
                //获取图片索引
                int idx=cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                mCurrentPhotoStr=cursor.getString(idx);

                cursor.close();
                //压缩照片
                resizePhoto(mCurrentPhotoStr);
                mPhoto.setImageBitmap(mPhotoImg);
                mTip.setText("敢接招？↗↗");
            }
        }
        else if (requestCode==CAMERA_PHOTO) 
        {
        	Bitmap faceFile = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory()
					+ "/Facephoto.jpg");
        	facephoto=Environment.getExternalStorageDirectory()
					+ "/Facephoto.jpg";
        	if(faceFile!=null){
        		resizePhoto(facephoto);
        		mPhoto.setImageBitmap(mPhotoImg);
        		mTip.setText("无美颜的你↗↗");
        	}
		}

        super.onActivityResult(requestCode,resultCode,intent);
    }

    //采样率压缩图片(相册)
    private void resizePhoto(String pathName) {

        BitmapFactory.Options options=new BitmapFactory.Options();
        //true为解码器不会去解析图片，但还是可以获取图片的宽高信息（读边）
        options.inJustDecodeBounds=true;
        //把要处理的图片放入
        BitmapFactory.decodeFile(pathName,options);

        //计算缩放比
        double ratio=Math.max(options.outWidth*1.0d/1024f,options.outHeight*1.0d/1024f);

        //设置采样率
        options.inSampleSize=(int) Math.ceil(ratio);
        //解析图片
        options.inJustDecodeBounds=false;
        //获取处理好的图片
        Bitmap decodeFile = BitmapFactory.decodeFile(pathName,options);
        //获取角度
        int degree=readPictureDegree(pathName);
        //把重新调整好角度的图片设置（角度为正数，则为顺时针旋转，相反，逆时针）
        mPhotoImg = rotaingImageView(degree, decodeFile);
    }
    
    // 读取图片属性：旋转的角度 
	public static int readPictureDegree(String path) {
		int degree = 0;
		try {
			ExifInterface exifInterface = new ExifInterface(path);
	 //两个参数：1.tag属性（这里获取的是方向），2.设置默认值（这里默认为正常角度，即竖屏）
	 //返回值为具体的方向（值为整数，可以点击看源码）
			int orientation = exifInterface.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
			switch (orientation) 
			{
			//如果orientation获取的为旋转90度（代码6），设置角度为90
			case ExifInterface.ORIENTATION_ROTATE_90:
				degree = 90;
				break;
			//逆时针旋转180度（代号3）
			case ExifInterface.ORIENTATION_ROTATE_180:
				degree = 180;
				break;
			 //向右旋转270度（代号8）
			case ExifInterface.ORIENTATION_ROTATE_270:
				degree = 270;
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return degree;
	}

	//把图片进行旋转（正数为顺时针，负数为逆时针，例如移动，缩放之类的，都是如此）
	public static Bitmap rotaingImageView(int angle , Bitmap bitmap) {
        //旋转图片 动作
		Matrix matrix = new Matrix();;
        matrix.postRotate(angle);
        // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
        		bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		return resizedBitmap;
	}
    
    private static final int MSG_SUCCESS=0x111;
    private static final int MSG_ERROR=0x112;


    private android.os.Handler mHandler=new android.os.Handler(){
        public void handleMessage(Message msg){
            switch (msg.what){
                case MSG_SUCCESS:
                    mWaiting.setVisibility(View.GONE);
                    JSONObject rs=(JSONObject) msg.obj;

                    prePareRsBitmap(rs);

                    mPhoto.setImageBitmap(mPhotoImg);
                    break;
                case MSG_ERROR:
                    mWaiting.setVisibility(View.GONE);

                    String errorMsg=(String) msg.obj;
                    if(TextUtils.isEmpty(errorMsg)){
                        mTip.setText("请检查网络！");
                    }else{
                        mTip.setText(errorMsg);
                    }
                    break;
            }

            super.handleMessage(msg);
        }
    };
    
    //解析数据，并绘制到显示框
    private void prePareRsBitmap(JSONObject rs) 
    {
        Bitmap bitmap=Bitmap.createBitmap(mPhotoImg.getWidth(),mPhotoImg.getHeight(),mPhotoImg.getConfig());

        Canvas canvas=new Canvas(bitmap);
        //把新的图绘制
        canvas.drawBitmap(mPhotoImg,0,0,null);

        try {
            JSONArray faces=rs.getJSONArray("face");
            
            //获取人脸的数量
            int faceCount=faces.length();

            mTip.setText("脸群:"+faceCount);

            //循环绘制
            for(int i=0;i<faceCount;i++)
            {
                JSONObject face= faces.getJSONObject(i);
                //取出位置信息
                JSONObject posObj=face.getJSONObject("position");

                //人脸中心点的坐标（返回的值为百分比，x就为占x轴的百分比，y就为占y轴的百分比）
                float x=(float )posObj.getJSONObject("center").getDouble("x");
                float  y=(float )posObj.getJSONObject("center").getDouble("y");
                
                //人脸的宽度和高度（返回的值为百分比，w就为占图片宽的百分比，h就为占图片高的百分比）
                float  w=(float )posObj.getDouble("width");
                float  h=(float )posObj.getDouble("height");

                //将百分比转换为具体的数值
                x=x/100*bitmap.getWidth();
                y=y/100*bitmap.getHeight();

                w=w/100*bitmap.getWidth();
                h=h/100*bitmap.getHeight();

                //设置画笔颜色和粗细
                mPaint.setColor(0xffffffff);
                mPaint.setStrokeWidth(2);

                //画框住人脸的四条线，分别是左竖，上横，右竖，下横
                canvas.drawLine(x-w/2,y-h/2,x-w/2,y+h/2,mPaint);
                canvas.drawLine(x-w/2,y-h/2,x+w/2,y-h/2,mPaint);
                canvas.drawLine(x+w/2,y-h/2,x+w/2,y+h/2,mPaint);
                canvas.drawLine(x-w/2,y+h/2,x+w/2,y+h/2,mPaint);

                //获取年龄和性别信息
                int age=face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                String gender=face.getJSONObject("attribute").getJSONObject("gender").getString("value");

                
                //取出TextView上的图片
                Bitmap ageBitmap=bulidAgeBitmap(age,"Male".equals(gender));

                int ageWidth=ageBitmap.getWidth();
                int ageHeight=ageBitmap.getHeight();

               /**根据图片与ImageView的大小判断，宽高都小于ImageView，则要把显示框进行一定比例的缩小
                *（当图片很小时，显示框就会显得很大，如果这是把图片放大显示在ImageView上，那么显示框也会被放大）*/
                if(bitmap.getWidth()<mPhoto.getWidth()&&bitmap.getHeight()<mPhoto.getHeight())
                {
                	//缩放比例（图片的宽度除以imageView的宽度）
                    float ratio=Math.max(bitmap.getWidth()*1.0f/mPhoto.getWidth(),bitmap.getHeight()*1.0f/mPhoto.getHeight());
                    //绘制缩放图片
                    ageBitmap=Bitmap.createScaledBitmap(ageBitmap,(int)(ageWidth*ratio),(int)(ageHeight*ratio),false);
                }
                
                //绘制显现框
                canvas.drawBitmap(ageBitmap,x-ageBitmap.getWidth()/2,y-h/2-ageBitmap.getHeight(),null);

                mPhotoImg=bitmap;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

   /**获取TextView中的图片
    * （View 组件中的内容可以通过Cache机制保存为bitmap，首先要通过setDrawingCacheEnabled(true)开启Cache
    * 然后再通过 getDrawingCache() 获取bitmap资源，这里第二句就是将获取到bitmap资源后再创建一个bitmap
    * setDrawingCacheEnabled(false)是销毁Cache
    * 若想更新Cache 重新以此方式获取新的bitmap就要调用这个方法   不然你得到的始终是同一bitmap）*/
    private Bitmap bulidAgeBitmap(int age, boolean isMale) 
    {
        TextView tv=(TextView)mWaiting.findViewById( R.id.id_age_and_gender);
        tv.setText(age+"");
        if(isMale)
        {
        	//设置TextView左边的图片为女性
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male),null,null,null);

        }
        else{
        	//设置TextView左边的图片为男性
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female),null,null,null);
        }

        //同意从Textview中获取图片（开启cache）
        tv.setDrawingCacheEnabled(true);
        //从TextView中获取图片
        Bitmap bitmap=Bitmap.createBitmap(tv.getDrawingCache());
        //更新Cache，显示新的cache（这样，每次都能获取新生成在TextView的图片）
        tv.destroyDrawingCache();
        //设置默认显示文字
        tv.setText("11");
        return bitmap;
    }

    //点击事件
    public void onClick(View view) {
        switch (view.getId()){
                case R.id.id_getImage:
                	isCamera=false;
                	isAlbum=true;
                    Intent intent=new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    startActivityForResult(intent,PICK_CODE);
                    break;
                case R.id.id_camera:
                	isCamera=true;	
                	isAlbum=false;
                	Intent intent1 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        			phoneFile = new File(Environment.getExternalStorageDirectory()
        					 + "/Facephoto.jpg");
        			intent1.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(phoneFile));
        			startActivityForResult(intent1,CAMERA_PHOTO);
        			break;
                case R.id.id_detect:
                	 //set frame layout as visible
                	if (mPhotoImg!= null) 
                	{
                		 mWaiting.setVisibility(View.VISIBLE);                   
                         if (facephoto!=null&&isCamera) 
                         {
     						resizePhoto(Environment.getExternalStorageDirectory()
     	                			+ "/Facephoto.jpg");
     					}
                         else  if(mCurrentPhotoStr!=null&&!mCurrentPhotoStr.trim().equals("")&&isAlbum)
                         {
                             resizePhoto(mCurrentPhotoStr);
                         }
                         FaceDetect.detect(mPhotoImg,new FaceDetect.Callback(){

                             public void success(JSONObject result) {
                                 Message msg=Message.obtain();
                                 msg.what=MSG_SUCCESS;
                                 msg.obj=result;
                                 mHandler.sendMessage(msg);
                             }

                             public void error(FaceppParseException exeption) {
                                 Message msg=Message.obtain();
                                 msg.what=MSG_ERROR;
                                 msg.obj=exeption.getErrorMessage();
                                 mHandler.sendMessage(msg);
                             }
                         });
					}
                	else 
                	{                     	
  						Toast.makeText(getApplicationContext(), "请选择或拍摄照片", -1).show();
  					}
                    break;
            }

    }
}