package com.neostra.imin.print;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android_serialport_api.SerialPortFinder;

import static java.lang.Thread.sleep;

public class MainActivity extends Activity {
    private final String TAG = "NeoPrint";
    Spinner mSerialPort,mBaudRate;
    ToggleButton mToggleButton,mPrintToggle;
    Button mSelfCheck,mPrintInfo,mPrintText,mContinuePrint,mBarCode,mQRCode,mTicket,mHotTest,mUpdate;
    SerialPortFinder mSerialPortFinder;//串口设备搜索
    public SerialControl mComPort;//串口
    private String updateVersion = "PT95-SGE-9004.bin";
    private int hotTestTime = 60000;
    private ProgressDialog updateDialog = null;

    long intervalTime = 3000;   // 间隔时间:MS
    int executionTimes = 1;     // 执行次数
    private final int closePrint = 0x001;
    private final int cancelProgress = 0x002;
    int whichTicket = -1;
    public StringBuilder sMsg ;
    private PrintThread printThread;
    private boolean isPrintStart = false;
    private MyHandler mHandler;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_print);

        mHandler = new MyHandler(MainActivity.this);

        //默认打开/dev/ttyMT1 端口 波特率115200
        mComPort =  new SerialControl();
        mComPort.setPort("/dev/ttyMT1");
        mComPort.setBaudRate("115200");
        OpenComPort(mComPort);

        initView();//初始化主界面
    }

    //----------------------------------------------------初始化主界面
    private void initView(){
        mSerialPort = findViewById(R.id.serialPort);
        mBaudRate = findViewById(R.id.baudRate);
        mToggleButton = findViewById(R.id.toggleButton);
        mPrintToggle = findViewById(R.id.printToggle);
        mSelfCheck = findViewById(R.id.selfCheck);
        mPrintInfo = findViewById(R.id.printInfo);
        mPrintText = findViewById(R.id.printText);
        mContinuePrint = findViewById(R.id.continuePrint);
        mBarCode = findViewById(R.id.barcode);
        mQRCode = findViewById(R.id.qrcode);
        mTicket = findViewById(R.id.ticket);
        mHotTest = findViewById(R.id.hottest);
        mUpdate = findViewById(R.id.update);


        if (mComPort!=null && mComPort.isOpen()){
            mToggleButton.setChecked(true);
            OpenPrint();
            mPrintToggle.setChecked(true);
        }

        //显示所支持的串口
        mSerialPortFinder= new SerialPortFinder();
        String[] entryValues = mSerialPortFinder.getAllDevicesPath();
        List<String> allDevices = new ArrayList<String>();
        for (int i = 0; i < entryValues.length; i++) {
            allDevices.add(entryValues[i]);
        }
        ArrayAdapter<String> aspnDevices = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, allDevices);
        mSerialPort.setAdapter(aspnDevices);
        mSerialPort.setSelection(4);
        mSerialPort.setOnItemSelectedListener(new ItemSelectedEvent());

        //显示支持的波特率
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.baudrates_value,android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBaudRate.setAdapter(adapter);
        mBaudRate.setSelection(16);
        mBaudRate.setOnItemSelectedListener(new ItemSelectedEvent());

        //串口状态
        mToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    mComPort.setPort(mSerialPort.getSelectedItem().toString());
                    mComPort.setBaudRate(mBaudRate.getSelectedItem().toString());
                    OpenComPort(mComPort);
                }else {
                    CloseComPort(mComPort);
                }
            }
        });

        //打印机上电状态
        mPrintToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    OpenPrint();
                }else {
                    ClosePrint();
                }
            }
        });

        mSelfCheck.setOnClickListener(onClickListener);
        mPrintInfo.setOnClickListener(onClickListener);
        mPrintText.setOnClickListener(onClickListener);
        mTicket.setOnClickListener(onClickListener);
        mBarCode.setOnClickListener(onClickListener);
        mQRCode.setOnClickListener(onClickListener);
        mContinuePrint.setOnClickListener(onClickListener);
        mHotTest.setOnClickListener(onClickListener);
        mUpdate.setOnClickListener(onClickListener);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mSelfCheck){        //自检页
                if (mComPort!=null && mComPort.isOpen()){
                    mComPort.sendHex("1B23234344545903");//设置GBK编码
                    mComPort.sendHex("1B232353454C46");//打印自检信息
                }

            }else if(v == mPrintInfo){       //打印机信息
                sMsg = new StringBuilder();
                if (mComPort!=null && mComPort.isOpen()){
                    mComPort.sendHex("1D6739");//主板序列号
                    mComPort.sendHex("1D6761");//打印机ID号
                    mComPort.sendHex("1D6762");//出厂日期
                    mComPort.sendHex("1D6766");//打印机软件版本号
                    mComPort.sendHex("1D6767");//厂商名称
                    mComPort.sendHex("1D6768");//机器名称
                }
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(MainActivity.this, PrintInfoActivity.class);
                intent.putExtra("printinfo",sMsg.toString());
                startActivity(intent);

            }else if(v == mPrintText){      //打印文本
                Intent intent = new Intent(MainActivity.this, PrintTextActivity.class);
                startActivity(intent);

            }else if(v == mTicket){         //小票打印
                selectPrintCase();

            }else if(v == mBarCode){         //一维码
                if (mComPort!=null && mComPort.isOpen()){
                    mComPort.sendHex("1D6B00303132333435363738393900");//UPC-A 012345678998
                    mComPort.sendHex("1B6402");//走纸2行
                    mComPort.sendHex("1D6B044E454F5354524131323300");//CODE39 NEOSTRA123
                    mComPort.sendHex("1B6402");//走纸2行
                    mComPort.sendHex("1D6B044E454F38383838383800");//CODE39 NEO888888
                    mComPort.sendHex("1B6402");//走纸2行
                }

            }else if(v == mQRCode){         //二维码
                if (mComPort!=null && mComPort.isOpen()){
                    mComPort.sendHex("1B2323515049580B");//设置QR像素点大小为11
                    mComPort.sendHex("1D286B0300314308");//设置QR单元大小为8
                    mComPort.sendHex("1D286B0D0031503030313233343536373839");//打印内容为“0123456789”的二维码
                    mComPort.sendHex("1D286B0D00315130");
                    mComPort.sendHex("1B6402");//走纸2行
                    mComPort.sendHex("1D286B14003150304E454F5354524130313233343536373839");//打印内容为“NEOSTTA0123456789”的二维码
                    mComPort.sendHex("1D286B0D00315130");
                    mComPort.sendHex("1B6402");//走纸2行
                    mComPort.sendHex("1D286B12003150307777772E6E656F737472612E636F6D");//打印内容为“www.neostra.com”的二维码
                    mComPort.sendHex("1D286B0D00315130");
                    mComPort.sendHex("1B6402");//走纸2行
                }

            }else if(v == mContinuePrint){         //连续打印
                showCustomViewDialog();

            }else if(v == mHotTest){         //过热验证
                final String [] selectTime = {"30s","60s","90s","120s"};
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("过热测试打印时间");
                builder.setSingleChoiceItems(selectTime, 1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 0){
                            hotTestTime = 30000;
                        }else if(which == 1){
                            hotTestTime = 60000;
                        }
                        else if(which == 2){
                            hotTestTime = 90000;
                        }else if(which == 3){
                            hotTestTime = 120000;
                        }
                    }
                });
                builder.setPositiveButton("开始测试", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HotTestThread thread = new HotTestThread();
                        thread.start();
                    }
                });
                final AlertDialog dialog = builder.create();
                dialog.show();


            }else if(v == mUpdate){         //软件升级
                final String [] selectVersion = {"PT95-SGE-9004.bin","PT95-SGE-9003.bin"};
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("请选择升级固件");
                builder.setSingleChoiceItems(selectVersion, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 1){
                            updateVersion = "PT95-SGE-9003.bin";
                        }else {
                            updateVersion = "PT95-SGE-9004.bin";
                        }
                    }
                });
                builder.setPositiveButton("开始升级", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UpdateThread thread = new UpdateThread();
                        thread.start();
                        updateProgress();
                    }
                });
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    };

    //----------------------------------------------------串口号或波特率变化时，关闭打开的串口
    class ItemSelectedEvent implements Spinner.OnItemSelectedListener{
        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
        {
            String mPort = mSerialPort.getSelectedItem().toString();
            String mRate = mBaudRate.getSelectedItem().toString();
            if(!mPort.equals("/dev/ttyMT1") || !mRate.equals("115200"))
            {
                CloseComPort(mComPort);
                mToggleButton.setChecked(false);
            }
        }
        public void onNothingSelected(AdapterView<?> arg0) {}

    }

    //-------------小票打印------start-----------------------------------
    private void selectPrintCase() {
        whichTicket = -1;
        LinearLayout choose = (LinearLayout) getLayoutInflater().inflate(R.layout.select_print_case,null);
        final String [] selectContent={"排队小票","交易凭条","共享小票","电影票","美团外卖"};
        AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("票样选择打印");
        builder.setView(choose);
        builder.setSingleChoiceItems(selectContent, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                whichTicket = i;
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();

        Button mPositiveButton = choose.findViewById(R.id.positive);
        mPositiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("dzm","whichTicket =  " + whichTicket);
                switch (whichTicket) {
                    case 0:
                        printSmallTicket();// 排队票据
                        break;
                    case 1:
                        printBankBill();//交易凭条
                        break;
                    case 2:
                        printShareTicket();//共享小票
                        break;
                    case 3:
                        printCinemaTicket();//电影票
                        break;
                    case 4:
                        meiTuanTakeawayTicket();//美团外卖
                        break;
                    default:
                        break;
                }
            }
        });

        Button mNegativeButton = choose.findViewById(R.id.negative);
        mNegativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

    }

    //排队小票
    private void printSmallTicket(){
        if(mComPort != null && mComPort.isOpen()){
            mComPort.sendHex("1B23234344545902");//设置UTF-8编码
            mComPort.sendHex("1B6101");//居中
            mComPort.sendHex("1B4501");//加粗
            mComPort.sendTxt("中国农业银行\n\n" + "办理业务(一)\n\n");
            mComPort.sendTxt("1000\n\n");
            mComPort.sendHex("1B4500");//不加粗
            mComPort.sendHex("1B6100");//居左
            mComPort.sendTxt("您前面有 10 人等候，请注意叫号! \n" + "欢迎光临！我们将竭诚为你服务。\n\n");
            mComPort.sendHex("1B6101");//居中
            mComPort.sendHex("1B23235150495808");//设置QR像素点大小为11
            mComPort.sendHex("1D286B030031430A");//设置QR单元大小为16
            mComPort.sendHex("1D286B0B0031503057656C6F636F6D65");//打印内容为“Welocome”的二维码
            mComPort.sendHex("1D286B0D00315130");
            mComPort.sendHex("1B6102");//居右
            mComPort.sendTxt("\n"+ sdf.format(new Date()).toString() + "\n\n");
            mComPort.sendHex("1B6101");//居中
            mComPort.sendHex("1D6B044131323334353637385A00");//CODE39 A12345678Z
        }
    }

    //交易凭条
    private void printBankBill(){
        if(mComPort != null && mComPort.isOpen()) {
            mComPort.sendHex("1B23234344545902");//设置UTF-8编码
            mComPort.sendHex("1B6101");//居中
            mComPort.sendTxt("交通银行自助设备交易凭条\n\n");
            mComPort.sendHex("1B6100");//居左
            mComPort.sendTxt("  交易类型：交行借记卡发卡确认单\n" + "  电子柜员：ABHB347\n\n");
            mComPort.sendTxt("借记卡号：62226206100212****4"
                    + "\n" + "流水号码：102200ABHB347642959"
                    + "\n" + "开卡日期：20171022           "
                    + "\n" + "开卡时间：16:16:12"
                    + "\n" + "客户姓名：*成       性别：男"
                    + "\n" + "身份证号：4210831993060****0"
                    + "\n" + "手机号码：186821****1"
                    + "\n" + "机构号码：01421091999        "
                    + "\n" + "终端号码：8ABHB347"
                    + "\n" + "签约信息：----------------------"
                    + "\n" + "电话银行：未开通"
                    + "\n" + "自助银行：签约成功"
                    + "\n" + "网上银行：未开通"
                    + "\n" + "手机银行：未开通"
                    + "\n" + "  银信通：未开通"
                    + "\n" + "贷记卡自助还款：未开通"
                    + "\n\n" + "感谢您使用" + "“" + "交通银行自助发卡机" + "”"
                    + "\n" + "请妥善保管交易凭条；"
                    + "\n" + "交通银行客户热线：95559\n\n");
        }
    }

    //共享小票
    private void printShareTicket(){
        if(mComPort != null && mComPort.isOpen()) {
            mComPort.sendHex("1B23234344545902");//设置UTF-8编码
            mComPort.sendHex("1B6100");//居左
            mComPort.sendHex("1B4501");//加粗
            mComPort.sendTxt("酷玩共享玩具\n\n");
            mComPort.sendHex("1B6101");//居中
            mComPort.sendHex("1B23235150495808");//设置QR像素点大小为11
            mComPort.sendHex("1D286B0300314308");//设置QR单元大小为16
            mComPort.sendHex("1D286B0A0031503053686172696E67");//打印内容为“Sharing”的二维码
            mComPort.sendHex("1D286B0D00315130");
            mComPort.sendHex("1B4500");//不加粗
            mComPort.sendHex("1B6100");//居左
            mComPort.sendTxt("\n");
            mComPort.sendTxt("订单号：1234567890\n");
            mComPort.sendTxt("用户手机：138*****88\n");
            mComPort.sendTxt("归还时间："+ sdf.format(new Date()).toString() + "\n\n");
        }
    }

    //电影票
    private void printCinemaTicket(){
        if(mComPort != null && mComPort.isOpen()) {
            mComPort.sendHex("1B23234344545902");//设置UTF-8编码
            mComPort.sendHex("1B6101");//居中
            mComPort.sendHex("1B4501");//加粗
            mComPort.sendTxt("猫眼电影\n\n");
            mComPort.sendHex("1B6100");//居左
            mComPort.sendTxt("新华银兴国际影城（客运站对面）\n");
            mComPort.sendTxt("影片：流浪地球（原版3D）\n\n");
            mComPort.sendHex("1D2101");//倍高
            mComPort.sendHex("1B6101");//居中
            mComPort.sendTxt("影厅：1号厅\n");
            mComPort.sendTxt("座位：05排09号\n\n");
            mComPort.sendHex("1D2100");//取消倍高
            mComPort.sendHex("1B4500");//不加粗
            mComPort.sendHex("1B6100");//居左
            mComPort.sendTxt("日期：2019-02-19   时间：10:50\n");
            mComPort.sendTxt("座类：普通座       票类：网络票\n");
            mComPort.sendTxt("票价：43.0元       服务费：0.0元\n\n");
            mComPort.sendHex("1B6101");//居中
            mComPort.sendHex("1B23235150495808");//设置QR像素点大小为11
            mComPort.sendHex("1D286B0300314308");//设置QR单元大小为16
            mComPort.sendHex("1D286B12003150307777772E6E656F737472612E636F6D");//打印内容为“www.neostra.com”的二维码
            mComPort.sendHex("1D286B0D00315130");
            mComPort.sendTxt("\n\n");
        }
    }

    //美团外卖
    private void meiTuanTakeawayTicket(){
        if(mComPort != null && mComPort.isOpen()) {
            mComPort.sendHex("1B23234344545902");//设置UTF-8编码
            mComPort.sendHex("1B6100");//居左
            mComPort.sendTxt("划菜单旭生大厦 (21楼桑格尔)@#\n广东省深圳市宝安区宝安大道4004号\n中天美景大酒店XXX(女士)" + "\n"
                    + "美团外卖9(1位) 账单:0009\n\n");
            mComPort.sendHex("1B4501");//加粗
            mComPort.sendHex("1D2101");//倍高
            mComPort.sendTxt("美团外卖#9\n\n");
            mComPort.sendTxt("收餐人隐私号 13190909090" + "\n" + " 4806,"
                    + "手机号 17718181818" + "\n");
            mComPort.sendTxt("----------------------------\n");
            mComPort.sendTxt("辣椒小炒肉+米饭1盒  1份\n");
            mComPort.sendTxt("----------------------------\n");
            mComPort.sendHex("1D2100");//取消倍高
            mComPort.sendHex("1B4500");//不加粗
            mComPort.sendTxt("胡青 点菜:11:14 打印:12-14 11:14\n\n");
        }
    }
    //-------------小票打印----------end-------------------------------

    //-------------连续打印----------start-----------------------------
    //声明一个AlertDialog构造器
    private AlertDialog.Builder builder;
    private AlertDialog dialog;
    EditText interval_et, execution_et;
    private void showCustomViewDialog() {
        builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.ContinuePrint);
        /**
         * 设置内容区域为自定义View
         */
        LinearLayout printDialog = (LinearLayout) getLayoutInflater().inflate(
                R.layout.pop_continue_print, null);
        builder.setView(printDialog);
        interval_et = printDialog.findViewById(R.id.interval_time_et);
        execution_et = printDialog.findViewById(R.id.execution_times_et);
        Button print = printDialog.findViewById(R.id.start_print_Btn);
        print.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mComPort!=null && mComPort.isOpen()) {
                    // 执行连续打印操作
                    dialog.dismiss();
                    intervalTime = Long.valueOf(interval_et.getText().toString().trim());
                    executionTimes = Integer.valueOf(execution_et.getText().toString().trim());
                    if (printThread == null) {
                        printThread = new PrintThread();
                    }
                    try {
                        printThread.start();
                    } catch (Exception e) {
                        printThread.run();
                    }
                }else {
                    return;
                }
            }
        });
        builder.setCancelable(true);
        dialog = builder.create();
        dialog.show();
    }

    public class PrintThread extends Thread {
        @Override
        public void run() {
            while (true) {
                synchronized (PrintThread.this) {
                    getPrintNum();
                    printTest();
                    if (executionTimes == Integer.valueOf(Num) - 1) {
                        Num = 1;
                        intervalTime = 0;
                        executionTimes = 0;
                        break;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ShowMessage( "第" + String.valueOf(Num) + "张");
                        }
                    });

                    try {
                        PrintThread.this.wait(intervalTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // 小票连续打印变量
    static int Num = 1;
    private String nums = "";

    private void getPrintNum() {
        nums = String.valueOf(Num);
        Num++;
    }

    private void printTest(){
        if (mComPort!=null && mComPort.isOpen()) {
            mComPort.sendHex("1B6101");//居中
            mComPort.sendHex("1D6B00303132333435363738393900");//UPC-A 012345678998
            mComPort.sendTxt("\n\n");
            mComPort.sendHex("1D286B12003150307777772E6E656F737472612E636F6D");//打印内容为“www.neostra.com”的二维码
            mComPort.sendHex("1D286B0D00315130");
            mComPort.sendTxt("\n\n");
        }else {
            ShowMessage("请先检查串口是否打开");
        }
    }
    //-------------连续打印----------end-----------------------------


//-------------过热验证----------start-----------------------------
    public class HotTestThread extends Thread {
        @Override
        public void run() {
            super.run();
            printBlackBlock();
        }
    }

    private void printBlackBlock() {
        isPrintStart = true;
        Message message = mHandler.obtainMessage();
        message.what = closePrint;
        mHandler.sendMessageDelayed(message, hotTestTime);
        String hex = "1D76303030003000";
        for(int i = 1; i <= 288;i++ ){
            hex = hex + "FFFFFFFFFFFFFFFF";
        }
        synchronized (MainActivity.this) {
            while (isPrintStart) {
                if (mComPort!=null && mComPort.isOpen()){
                    mComPort.sendHex(hex);
                    mComPort.sendHex("1D6100");
                }
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class MyHandler extends Handler {
        private WeakReference<MainActivity> reference;

        public MyHandler(MainActivity activity) {
            this.reference = new WeakReference<>(activity);
        }

        @Override
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case closePrint:
                    Log.d(TAG,"hottest end closePrint===");
                    ShowMessage("打印机过热测试结束！");
                    isPrintStart = false;
                    break;
                case cancelProgress:
                    updateDialog.cancel();
                    break;
            }
            super.dispatchMessage(msg);
        }
    }
//-------------过热验证----------end-----------------------------


//-------------软件升级----------start-----------------------------
    private void updateProgress(){
        Message message = mHandler.obtainMessage();
        message.what = cancelProgress;
        mHandler.sendMessageDelayed(message, 120000);

        updateDialog = new ProgressDialog(this);
        updateDialog.setTitle("打印机升级");
        updateDialog.setMessage("正在升级中……");
        updateDialog.setCancelable(false);
        updateDialog.show();
    }
    public class UpdateThread extends Thread {
        @Override
        public void run() {
            super.run();
            printUpdate();
        }
    }

    private void printUpdate(){
        String  commandHex = "1B232355505047";
        try {
            InputStream is = getAssets().open(updateVersion);
            int length = is.available();
            String lenghexlittle = MyFunc.NeoByteArrToHex(MyFunc.intToBytesLittle(length));

            Log.d(TAG,"prnt update leng1 = " + length);
            Log.d(TAG,"prnt update lenghexlittle = " + lenghexlittle);


            byte[] fileByte = new byte[length];
            is.read(fileByte);

            String xLen = MyFunc.NeoByteArrToHex(fileByte);
            int hexSum = MyFunc.makeChecksum(xLen);
            String hexSumlittle = MyFunc.NeoByteArrToHex(MyFunc.intToBytesLittle(hexSum));

            Log.d(TAG,"prnt update Xlen  = " + xLen);
            Log.d(TAG,"prnt update hexSum  = " + hexSum);
            Log.d(TAG,"prnt update hexSumlittle  = " + hexSumlittle);

            commandHex = commandHex + hexSumlittle + lenghexlittle + xLen;

            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mComPort!=null && mComPort.isOpen()){
            Log.d(TAG,"send update command");
            Log.d(TAG,"commandHex = " + commandHex);
            mComPort.sendHex(commandHex);
        }
    }
// -------------软件升级----------end-----------------------------

    //----------------------------------------------------串口控制类
    private class SerialControl extends SerialHelper{
        public SerialControl(){
        }

        @Override
        protected void onDataReceived(final ComBean ComRecData)
        {
            sMsg.append(new String(ComRecData.bRec));
            Log.d(TAG,"onDataReceived == " + new String(ComRecData.bRec));
            Log.d(TAG,"onDataReceived hex==========" + MyFunc.ByteArrToHex(ComRecData.bRec));
        }
    }

    //----------------------------------------------------打开串口
    private void OpenComPort(SerialHelper ComPort){
        try
        {
            ComPort.open();
        } catch (SecurityException e) {
            ShowMessage("打开串口失败:没有串口读/写权限!");
        } catch (IOException e) {
            ShowMessage("打开串口失败:未知错误!");
        } catch (InvalidParameterException e) {
            ShowMessage("打开串口失败:参数错误!");
        }
    }

    //----------------------------------------------------关闭串口
    private void CloseComPort(SerialHelper ComPort){
        if (ComPort!=null){
            ComPort.stopSend();
            ComPort.close();
        }
    }

    //----------------------------------------------------打印机上电
    private void OpenPrint(){
        try {
            //上电
            Log.d(TAG,"OpenPrint() printer power on");
            BufferedWriter bw = new BufferedWriter(new FileWriter("/sys/devices/platform/ns_power/ns_power"));
            bw.write("0x100");
            bw.close();
        } catch (IOException e) {
            Log.d(TAG, "Unable to write result file " + e.getMessage());
        }
    }

    //----------------------------------------------------打印机下电
    private void ClosePrint(){
        //下电
        Log.d(TAG,"OpenPrint() printer power off");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("/sys/devices/platform/ns_power/ns_power"));
            bw.write("0x101");
            bw.close();
        } catch (IOException e) {
            Log.d(TAG, "Unable to write result file " + e.getMessage());
        }
    }
    //------------------------------------------显示消息
    private void ShowMessage(String sMsg)
    {
        Toast.makeText(this, sMsg, Toast.LENGTH_SHORT).show();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        ClosePrint();
        CloseComPort(mComPort);
    }
}
