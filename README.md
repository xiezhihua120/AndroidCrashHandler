我们写程序的时候都希望能写出一个没有任何Bug的程序，期望在任何情况下都不会发生程序崩溃。但没有一个程序员能保证自己写的程序绝对不会出现异常崩溃。特别是当你用户数达到一定数量级后，你也更容易发现应用不同情况下的崩溃。

　　对于还没发布的应用程序，我们可以通过测试、分析Log的方法来收集崩溃信息。但对已经发布的程序，我们不可能让用户去查看崩溃信息然后再反馈给开发者。所以，设计一个对于小白用户都可以轻松实现反馈的应用就显得很重要了。我这里结合我自己写的一个Demo，来分析从崩溃开始到崩溃信息反馈到我们服务器，我们程序都需要做什么。


  当我们的程序因未捕获的异常而突然终止时，系统会调用处理程序的接口**UncaughtExceptionHandler**。如果我们想处理未被程序正常捕获的异常，只需实现这个接口里的uncaughtException方法，uncaughtException方法回传了Thread 和 Throwable两个参数。通过这两个参数，我们来对异常进行我们需要的处理。


综上，我对异常处理方式的思路是这样的:
> 
> 1.我们需要首先收集产生崩溃的手机信息，因为Android的样机种类繁多，很可能某些特定机型下会产生莫名的bug。
> 
> 2.将手机的信息和崩溃信息写入文件系统中。这样方便后续处理。
> 
> 3.崩溃的应用需要可以自动重启。重启的页面设置成反馈页面，询问           用户是否需要上传崩溃报告。
> 
> 4.用户同意后，即将2中写入的崩溃信息文件发送到自己的服务器。

通过上面的步骤，我们就可以写出大概的伪代码:

	handleException() {
	  collectDeviceInfo(context); //手机手机信息
	  writeCrashInfoToFile(ex); //写入崩溃文件
	  restart(); //应用重启
	 }

最后，在重启页面通过AsyncTask将崩溃信息上传服务器。

有了以上思路，我们一步一步的写出每个伪函数的具体代码。

#### 1.收集手机的信息: ####

	/**
	  *
	  * @param ctx
	  * 手机设备相关信息
	  */
	 public void collectDeviceInfo(Context ctx) {
	  try {
	   PackageManager pm = ctx.getPackageManager();
	   PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(),
	     PackageManager.GET_ACTIVITIES);
	   if (pi != null) {
	    String versionName = pi.versionName == null ? "null"
	      : pi.versionName;
	    String versionCode = pi.versionCode + "";
	    infos.put("versionName", versionName);
	    infos.put("versionCode", versionCode);
	    infos.put("crashTime", formatter.format(new Date()));
	   }
	  } catch (NameNotFoundException e) {
	   Log.e(TAG, "an error occured when collect package info", e);
	  }
	  Field[] fields = Build.class.getDeclaredFields();
	  for (Field field: fields) {
	   try {
	    field.setAccessible(true);
	    infos.put(field.getName(), field.get(null).toString());
	    Log.d(TAG, field.getName() + " : " + field.get(null));
	   } catch (Exception e) {
	    Log.e(TAG, "an error occured when collect crash info", e);
	   }
	  }
	 }

#### 2.崩溃和手机信息写入文件: ####

	/**
	  *
	  * @param ex
	  * 将崩溃写入文件系统
	  */
	 private void writeCrashInfoToFile(Throwable ex) {
	  StringBuffer sb = new StringBuffer();
	  for (Map.Entry<String, String> entry: infos.entrySet()) {
	   String key = entry.getKey();
	   String value = entry.getValue();
	   sb.append(key + "=" + value + "\n");
	  }
	  Writer writer = new StringWriter();
	  PrintWriter printWriter = new PrintWriter(writer);
	  ex.printStackTrace(printWriter);
	  Throwable cause = ex.getCause();
	  while (cause != null) {
	   cause.printStackTrace(printWriter);
	   cause = cause.getCause();
	  }
	  printWriter.close();
	  String result = writer.toString();
	  sb.append(result);
	  //这里把刚才异常堆栈信息写入SD卡的Log日志里面
	  if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
	  {
	   String sdcardPath = Environment.getExternalStorageDirectory().getPath();
	   String filePath = sdcardPath + "/cym/crash/";
	   localFileUrl = writeLog(sb.toString(), filePath);
	  }
	 }
	 /**
	  *
	  * @param log
	  * @param name
	  * @return 返回写入的文件路径
	  * 写入Log信息的方法，写入到SD卡里面
	  */
	 private String writeLog(String log, String name)
	 {
	  CharSequence timestamp = new Date().toString().replace(" ", "");
	  timestamp = "crash";
	  String filename = name + timestamp + ".log";
	  File file = new File(filename);
	  if(!file.getParentFile().exists()){
	   file.getParentFile().mkdirs();
	  }
	  try
	  {
	   Log.d("TAG", "写入到SD卡里面");
	   //	 FileOutputStream stream = new FileOutputStream(new File(filename));
	   //	 OutputStreamWriter output = new OutputStreamWriter(stream);
	   file.createNewFile();
	   FileWriter fw=new FileWriter(file,true);
	   BufferedWriter bw = new BufferedWriter(fw);
	   //写入相关Log到文件
	   bw.write(log);
	   bw.newLine();
	   bw.close();
	   fw.close();
	   return filename;
	  }
	  catch (IOException e)
	  {
	   Log.e(TAG, "an error occured while writing file...", e);
	   e.printStackTrace();
	   return null;
	  }
	 }

#### 3.重启应用: ####

	注：我尝试过好多种应用重启的方法，最终选择采用PendingIntent的方式。
	private void restart(){
	   try{ 
	             Thread.sleep(2000); 
	         }catch (InterruptedException e){ 
	             Log.e(TAG, "error : ", e); 
	         } 
	         Intent intent = new Intent(context.getApplicationContext(), SendCrashActivity.class); 
	         PendingIntent restartIntent = PendingIntent.getActivity( 
	           context.getApplicationContext(), 0, intent, 
	                 Intent.FLAG_ACTIVITY_NEW_TASK); 
	         //退出程序 
	         AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE); 
	         mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, 
	                 restartIntent); // 1秒钟后重启应用 
	 }

#### 4.上传崩溃 ####

应用重启后来到的是SendCrashActivity界面，在这里我设置了一个简单的按钮，点击后即可上传崩溃信息。代码比较多，这里列一个比较有用的上传方法吧:

	public static String uploadFile(File file,String requestUrl){
	  String result = null; 
	  String BOUNDARY = UUID.randomUUID().toString(); //边界标识 随机生成 
	  String PREFIX = "--" ;
	  String LINE_END = "\r\n"; 
	  String CONTENT_TYPE = "multipart/form-data"; //内容类型 
	  try{
	   URL url = new URL(requestUrl); 
	   HttpURLConnection conn = (HttpURLConnection) url.openConnection(); 
	   conn.setReadTimeout(TIME_OUT); 
	   conn.setConnectTimeout(TIME_OUT); 
	   conn.setDoInput(true); //允许输入流 
	   conn.setDoOutput(true); //允许输出流 
	   conn.setUseCaches(false); //不允许使用缓存 
	   conn.setRequestMethod("POST"); //请求方式 
	   conn.setRequestProperty("Charset", CHARSET); //设置编码 
	   conn.setRequestProperty("connection", "keep-alive"); 
	   conn.setRequestProperty("Content-Type", CONTENT_TYPE + ";boundary=" + BOUNDARY);
	
	
	   if(file!=null) 
	   { 
	    /**
	     * 当文件不为空，把文件包装并且上传
	     */ 
	    DataOutputStream dos = new DataOutputStream(conn.getOutputStream()); 
	    StringBuffer sb = new StringBuffer(); 
	    sb.append(PREFIX); 
	    sb.append(BOUNDARY); 
	    sb.append(LINE_END); 
	    /**
	     * 这里重点注意：
	     * name里面的值为服务器端需要key 只有这个key 才可以得到对应的文件
	     * filename是文件的名字，包含后缀名的 比如:abc.png 
	     */ 
	
	    sb.append("Content-Disposition: form-data; name=\"uploadcrash\"; filename=\""+file.getName()+"\""+LINE_END); 
	    sb.append("Content-Type: application/octet-stream; charset="+CHARSET+LINE_END); 
	    sb.append(LINE_END); 
	    dos.write(sb.toString().getBytes()); 
	    InputStream is = new FileInputStream(file); 
	    byte[] bytes = new byte[1024]; 
	    int len = 0; 
	    while((len=is.read(bytes))!=-1) 
	    { 
	     dos.write(bytes, 0, len); 
	    } 
	    is.close(); 
	    dos.write(LINE_END.getBytes()); 
	    byte[] end_data = (PREFIX+BOUNDARY+PREFIX+LINE_END).getBytes(); 
	    dos.write(end_data); 
	    dos.flush(); 
	    /**
	     * 获取响应码 200=成功
	     * 当响应成功，获取响应的流 
	     */ 
	     int res = conn.getResponseCode(); 
	    Log.e(TAG, "response code:"+res); 
	    // if(res==200) 
	    // { 
	    Log.e(TAG, "request success"); 
	    InputStream input = conn.getInputStream(); 
	    StringBuffer sb1= new StringBuffer(); 
	    int ss ; 
	    while((ss=input.read())!=-1) 
	    { 
	     sb1.append((char)ss); 
	    } 
	    result = sb1.toString(); 
	    Log.e(TAG, "result : "+ result); 
	    // } 
	   // else{ 
	    // Log.e(TAG, "request error"); 
	    // } 
	   } 
	  }catch (MalformedURLException e) { 
	   e.printStackTrace(); 
	  } catch (IOException e) { 
	   e.printStackTrace(); 
	  }
	
	  return result;
	 }

整个流程基本走完，我们来看一下最终效果。（MainActivity点击按钮后执行了一个2/0的操作，所以崩溃）

![](http://saymagic-blog.stor.sinaapp.com/140925141910.gif)

我将崩溃上传到了我的sae服务器的storage里。下图中红色圈起来的文件即是我们上传的崩溃文件。

![](http://saymagic-blog.stor.sinaapp.com/140925142145.png)

我把这个文件下载下来，内容如下:

	TIME=1383016889000
	FINGERPRINT=generic/sdk/generic:4.4/KRT16L/892118:eng/test-keys
	HARDWARE=goldfish
	UNKNOWN=unknown
	RADIO=unknown
	BOARD=unknown
	versionCode=1
	PRODUCT=sdk
	versionName=1.0
	DISPLAY=sdk-eng 4.4 KRT16L 892118 test-keys
	USER=android-build
	HOST=vpak27.mtv.corp.google.com
	DEVICE=generic
	TAGS=test-keys
	MODEL=sdk
	BOOTLOADER=unknown
	crashTime=2014-09-24 05:39:21
	CPU_ABI=armeabi-v7a
	CPU_ABI2=armeabi
	IS_DEBUGGABLE=true
	ID=KRT16L
	SERIAL=unknown
	MANUFACTURER=unknown
	BRAND=generic
	TYPE=eng
	java.lang.IllegalStateException: Could not execute method of the activity
	 at android.view.View$1.onClick(View.java:3814)
	 at android.view.View.performClick(View.java:4424)
	 at android.view.View$PerformClick.run(View.java:18383)
	 at android.os.Handler.handleCallback(Handler.java:733)
	 at android.os.Handler.dispatchMessage(Handler.java:95)
	 at android.os.Looper.loop(Looper.java:137)
	 at android.app.ActivityThread.main(ActivityThread.java:4998)
	 at java.lang.reflect.Method.invokeNative(Native Method)
	 at java.lang.reflect.Method.invoke(Method.java:515)
	 at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:777)
	 at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:593)
	 at dalvik.system.NativeStart.main(Native Method)
	Caused by: java.lang.reflect.InvocationTargetException
	 at java.lang.reflect.Method.invokeNative(Native Method)
	 at java.lang.reflect.Method.invoke(Method.java:515)
	 at android.view.View$1.onClick(View.java:3809)
	 ... 11 more
	Caused by: java.lang.ArithmeticException: divide by zero
	 at so.cym.crashhandlerdemo.MainActivity.generateAnr(MainActivity.java:20)
	 ... 14 more
	java.lang.reflect.InvocationTargetException
	 at java.lang.reflect.Method.invokeNative(Native Method)
	 at java.lang.reflect.Method.invoke(Method.java:515)
	 at android.view.View$1.onClick(View.java:3809)
	 at android.view.View.performClick(View.java:4424)
	 at android.view.View$PerformClick.run(View.java:18383)
	 at android.os.Handler.handleCallback(Handler.java:733)
	 at android.os.Handler.dispatchMessage(Handler.java:95)
	 at android.os.Looper.loop(Looper.java:137)
	 at android.app.ActivityThread.main(ActivityThread.java:4998)
	 at java.lang.reflect.Method.invokeNative(Native Method)
	 at java.lang.reflect.Method.invoke(Method.java:515)
	 at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:777)
	 at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:593)
	 at dalvik.system.NativeStart.main(Native Method)
	Caused by: java.lang.ArithmeticException: divide by zero
	 at so.cym.crashhandlerdemo.MainActivity.generateAnr(MainActivity.java:20)
	 ... 14 more
	java.lang.ArithmeticException: divide by zero
	 at so.cym.crashhandlerdemo.MainActivity.generateAnr(MainActivity.java:20)
	 at java.lang.reflect.Method.invokeNative(Native Method)
	 at java.lang.reflect.Method.invoke(Method.java:515)
	 at android.view.View$1.onClick(View.java:3809)
	 at android.view.View.performClick(View.java:4424)
	 at android.view.View$PerformClick.run(View.java:18383)
	 at android.os.Handler.handleCallback(Handler.java:733)
	 at android.os.Handler.dispatchMessage(Handler.java:95)
	 at android.os.Looper.loop(Looper.java:137)
	 at android.app.ActivityThread.main(ActivityThread.java:4998)
	 at java.lang.reflect.Method.invokeNative(Native Method)
	 at java.lang.reflect.Method.invoke(Method.java:515)
	 at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:777)
	 at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:593)
	 at dalvik.system.NativeStart.main(Native Method)
	


#### 总结 ####

通过上面的文件，我们就可以分析什么时候产生崩溃，什么机型下会产生崩溃。

Android里有一种崩溃(严格意义将不叫崩溃)是捕获不到的，那就是ANR，关于ANR的相关知识可以阅读我的另一篇博文[http://cym.so/2014/09/25/ANR%E5%AE%8C%E5%85%A8%E8%A7%A3%E6%9E%90.html](http://cym.so/2014/09/25/ANR%E5%AE%8C%E5%85%A8%E8%A7%A3%E6%9E%90.html "http://cym.so/2014/09/25/ANR%E5%AE%8C%E5%85%A8%E8%A7%A3%E6%9E%90.html")

原文链接:[http://cym.so/2014/09/25/Android%E5%B4%A9%E6%BA%83%E5%AE%8C%E5%85%A8%E8%A7%A3%E6%9E%90.html](http://cym.so/2014/09/25/Android%E5%B4%A9%E6%BA%83%E5%AE%8C%E5%85%A8%E8%A7%A3%E6%9E%90.html)