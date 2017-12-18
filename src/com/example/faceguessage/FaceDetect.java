package com.example.faceguessage;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;


//接收Face的DetectAPI处理的数据
public class FaceDetect 
{

	//返回处理信息的接口
    public interface Callback
    {
        void success(JSONObject result);
        void error(FaceppParseException exeption);
    }


    //根据官方要求，解析的图片要为二进制数组，所以这个方法用来把图片转换为二进制数组，并解析这个数组，返回json数据
    public static void detect(final Bitmap bm, final Callback callBack){
        new Thread(new Runnable() {
            public void run() {

                try {
                	//Face的网络请求
                    HttpRequests requests=new HttpRequests(Constant.key,Constant.secertKey,true,true);
                    //创建图片的副本
                    Bitmap bmSmall=Bitmap.createBitmap(bm,0,0,bm.getWidth(),bm.getHeight());
                    
                    //创建一个字节数组流对象，获取图片中的数据，转换成字节数组
                    ByteArrayOutputStream stream=new ByteArrayOutputStream();
                    
                    //使用compress方法把图片转换为字节
                    bmSmall.compress(Bitmap.CompressFormat.JPEG,100,stream);
                    
                    //把数据储存在字节数组中
                    byte[] array=stream.toByteArray();
                    
                    //把字节数组存入params
                    PostParameters params=new PostParameters();
                    params.setImg(array);
                    //解析这个数据，返回一个json对象
                    JSONObject jsonObject=requests.detectionDetect(params);


                    Log.e("TAG",jsonObject.toString());
                    //数据解析成功，调用返回处理信息的接口
                    if(callBack!=null)
                    {
                    	//传入json对象
                        callBack.success(jsonObject);
                    }
                } catch (FaceppParseException e) {
                    e.printStackTrace();

                  //数据解析失败，调用返回处理信息的接口
                    if(callBack!=null)
                    {
                    	//传入发生异常的信息
                        callBack.error(e);
                    }
                }

            }
        }).start();
    }
}
