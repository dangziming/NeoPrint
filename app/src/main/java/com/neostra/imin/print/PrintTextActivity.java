package com.neostra.imin.print;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidParameterException;

public class PrintTextActivity extends Activity implements View.OnClickListener{
    private EditText printText;
    private TextView startPrint,defaultText;
    private Spinner textAlign,underline,fontBold,lineSpace,rotation,antiWhite,charEnlargement;
    public SerialControl mComPort;//串口

    public  String etstringzh = "热敏打印机的工作\n" +
            "原理是打印头上安装\n" +
            "有半导体加热元件，\n" +
            "打印头加热并接触热敏打\n" +
            "印纸后就可以打印出需要的图案\n";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_text);
        printText = findViewById(R.id.et_test_edit);
        startPrint = findViewById(R.id.tv_print);
        defaultText = findViewById(R.id.default_test_text);
        textAlign = findViewById(R.id.sp_text_align);
        underline = findViewById(R.id.sp_underline);
        fontBold = findViewById(R.id.sp_bold);
        lineSpace = findViewById(R.id.sp_line_space);
        rotation = findViewById(R.id.sp_rotation);
        antiWhite = findViewById(R.id.sp_anti_white);
        charEnlargement = findViewById(R.id.sp_char_enlargement);

        mComPort = new SerialControl();
        mComPort.setPort("/dev/ttyMT1");
        mComPort.setBaudRate("115200");
        OpenComPort(mComPort);
        mComPort.sendHex("1B23234344545902");//设置UTF-8编码
        startPrint.setOnClickListener(this);
        defaultText.setOnClickListener(this);

        ArrayAdapter<CharSequence> alignAdapter = ArrayAdapter.createFromResource(this,
                R.array.text_align,android.R.layout.simple_spinner_item);
        alignAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        textAlign.setAdapter(alignAdapter);
        textAlign.setSelection(0);
        textAlign.setOnItemSelectedListener(new ItemSelectedEvent());

        ArrayAdapter<CharSequence> underlineAdapter = ArrayAdapter.createFromResource(this,
                R.array.underline,android.R.layout.simple_spinner_item);
        underlineAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        underline.setAdapter(underlineAdapter);
        underline.setSelection(0);
        underline.setOnItemSelectedListener(new ItemSelectedEvent());

        ArrayAdapter<CharSequence> boldAdapter = ArrayAdapter.createFromResource(this,
                R.array.bold,android.R.layout.simple_spinner_item);
        boldAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fontBold.setAdapter(boldAdapter);
        fontBold.setSelection(0);
        fontBold.setOnItemSelectedListener(new ItemSelectedEvent());


        ArrayAdapter<CharSequence> spaceAdapter = ArrayAdapter.createFromResource(this,
                R.array.linespace,android.R.layout.simple_spinner_item);
        spaceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        lineSpace.setAdapter(spaceAdapter);
        lineSpace.setSelection(0);
        lineSpace.setOnItemSelectedListener(new ItemSelectedEvent());

        ArrayAdapter<CharSequence> rotationAdapter = ArrayAdapter.createFromResource(this,
                R.array.rotation,android.R.layout.simple_spinner_item);
        rotationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rotation.setAdapter(rotationAdapter);
        rotation.setSelection(0);
        rotation.setOnItemSelectedListener(new ItemSelectedEvent());

        ArrayAdapter<CharSequence> whiteAdapter = ArrayAdapter.createFromResource(this,
                R.array.antiwhite,android.R.layout.simple_spinner_item);
        whiteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        antiWhite.setAdapter(whiteAdapter);
        antiWhite.setSelection(0);
        antiWhite.setOnItemSelectedListener(new ItemSelectedEvent());

        ArrayAdapter<CharSequence> enlargementAdapter = ArrayAdapter.createFromResource(this,
                R.array.charenlargement,android.R.layout.simple_spinner_item);
        enlargementAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        charEnlargement.setAdapter(enlargementAdapter);
        charEnlargement.setSelection(0);
        charEnlargement.setOnItemSelectedListener(new ItemSelectedEvent());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_print:
                String testStr = printText.getText().toString();
                if (testStr != null && !"".equals(testStr)) {
                    if (mComPort!=null && mComPort.isOpen()){
                        mComPort.sendTxt(testStr);
                    }
                }
                break;
            case R.id.default_test_text:
                printText.setText(etstringzh);
                break;
        }
    }


    class ItemSelectedEvent implements Spinner.OnItemSelectedListener{
        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
        {
            if ((arg0 == textAlign) )//对齐方式
            {
                if(arg2 == 2){
                    mComPort.sendHex("1B6101");//居中
                }else if(arg2 == 3){
                    mComPort.sendHex("1B6102");//靠右
                }else {
                    mComPort.sendHex("1B6100");//居左 或 默认
                }
            }else if(arg0 == underline){//下划线
                if(arg2 == 1){
                    mComPort.sendHex("1B2D01");//下划线1
                }else if(arg2 == 2){
                    mComPort.sendHex("1B2D02");//下划线2
                }else {
                    mComPort.sendHex("1B2D00");// 取消
                }
            }else if(arg0 == fontBold){//字体加粗
                if(arg2 == 2){
                    mComPort.sendHex("1B4501");//加粗
                }else {
                    mComPort.sendHex("1B4500");//不加粗
                }
            }else if(arg0 == lineSpace){//行间距
                if(arg2 == 1){
                    mComPort.sendHex("1B330A");//10
                }else if(arg2 == 2){
                    mComPort.sendHex("1B3314");//20
                }else if(arg2 == 3){
                    mComPort.sendHex("1B331E");//30
                }else if(arg2 == 4){
                    mComPort.sendHex("1B3328");//40
                }else {
                    mComPort.sendHex("1B32");//默认
                }
            }else if(arg0 == rotation){//旋转
                if(arg2 == 1){
                    mComPort.sendHex("1B5601");//旋转90度
                }else {
                    mComPort.sendHex("1B5600");//正常
                }
            }else if(arg0 == antiWhite){//反白设置
                if(arg2 == 2){
                    mComPort.sendHex("1D4201");//设置反白
                }else {
                    mComPort.sendHex("1D4200");//默认 或 取消反白
                }
            }else if(arg0 == charEnlargement){//字体放大
                if(arg2 == 1){
                    mComPort.sendHex("1D2101");//倍高
                }else if(arg2 == 2){
                    mComPort.sendHex("1D2110");//倍宽
                }else {
                    mComPort.sendHex("1D2100");//默认
                }
            }
        }

        public void onNothingSelected(AdapterView<?> arg0)
        {}

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mComPort != null && mComPort.isOpen()){
            mComPort.sendHex("1B6100");//居左 或 默认
            mComPort.sendHex("1B2D00");// 取消下划线
            mComPort.sendHex("1B4500");//不加粗
            mComPort.sendHex("1B32");//默认
            mComPort.sendHex("1B5600");//旋转正常
            mComPort.sendHex("1D4200");//默认 或 取消反白
            mComPort.sendHex("1D2100");//字体默认
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

    //----------------------------------------------------串口控制类
    private class SerialControl extends SerialHelper{

        public SerialControl(){
        }

        @Override
        protected void onDataReceived(final ComBean ComRecData)
        {

        }
    }


    //----------------------------------------------------关闭串口
    private void CloseComPort(SerialHelper ComPort){
        if (ComPort!=null){
            ComPort.stopSend();
            ComPort.close();
        }
    }

    //------------------------------------------显示消息
    private void ShowMessage(String sMsg)
    {
        Toast.makeText(this, sMsg, Toast.LENGTH_SHORT).show();
    }
}
