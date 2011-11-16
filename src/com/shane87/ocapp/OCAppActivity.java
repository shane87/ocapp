package com.shane87.ocapp;

import java.io.OutputStreamWriter;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.shane87.ocapp.ShellInterface;

public class OCAppActivity extends Activity {
    int selection;
    ArrayAdapter<String> adapterForOcSpinner;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if(checkKern())
        {
        	setContentView(R.layout.main);
        	setupControls();
        }
    }
    
    private boolean checkKern()
    {
    	if(!checkSu())
    		return false;
    	String temp;
    	temp = ShellInterface.getProcessOutput("cat /proc/version");
    	if(temp.contains("talon"))
    		return true;
    	else
    		return false;
    }
    
    private boolean checkSu()
    {
    	if(ShellInterface.isSuAvailable())
    		return true;
    	else
    		return false;
    }
    
    private void setupControls()
    {
    	loadCurOc();
    	Button applyBtnPtr = (Button)findViewById(R.id.applyBtn);
    	Button saveBtnPtr = (Button)findViewById(R.id.saveBtn);
    	Spinner ocSpinnerPtr = (Spinner)findViewById(R.id.ocSpinner);
    	
    	adapterForOcSpinner = new ArrayAdapter<String>(this,
    			android.R.layout.simple_spinner_item);
    	
    	adapterForOcSpinner.setDropDownViewResource(
    			android.R.layout.simple_spinner_dropdown_item);
    	adapterForOcSpinner.add("1000 MHz");
    	adapterForOcSpinner.add("1120 MHz");
    	adapterForOcSpinner.add("1200 MHz");
    	adapterForOcSpinner.add("1300 MHz");
    	
    	ocSpinnerPtr.setAdapter(adapterForOcSpinner);
    	ocSpinnerPtr.setSelection(selection);
    	
    	OnItemSelectedListener ocListener = new Spinner.OnItemSelectedListener()
    	{

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				selection = (int)arg0.getSelectedItemId();
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {				
			}
    		
    	};
    	
    	ocSpinnerPtr.setOnItemSelectedListener(ocListener);
    	
    	applyBtnPtr.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				apply();
			}
		});
    	
    	saveBtnPtr.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				save();				
			}
		});
    }
    
    private void loadCurOc()
    {
    	String temp;
    	temp = ShellInterface.getProcessOutput("cat /sys/devices/virtual/misc/semaphore_cpufreq/oc");
    	
    	selection = Integer.parseInt(temp.trim());
    }
    
    private void apply()
    {
    	String temp, temp2;
    	int maxFreq = 0;
    	OutputStreamWriter writer;
    	
    	clearUV();
    	
    	switch(selection)
    	{
    	case 0:
    		maxFreq = 1000;
    		break;
    	case 1:
    		maxFreq = 1120;
    		break;
    	case 2:
    		maxFreq = 1200;
    		break;
    	case 3:
    		maxFreq = 1300;
    		break;
    	}
    	
    	temp = "echo ";
    	temp = temp.concat(Integer.toString(selection));
    	temp = temp.concat(" > /sys/devices/virtual/misc/semaphore_cpufreq/oc\n");
    	
    	temp2 = "echo ";
    	temp2 = temp2.concat(Integer.toString(maxFreq * 1000));
    	temp2 = temp2.concat(" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq\n");
    	
    	try
    	{
    		writer = new OutputStreamWriter(openFileOutput("temp.sh", 0));
    		writer.write("rmmod cpufreq_stats\n");
    		writer.write(temp);
    		writer.write("sleep 2\n");
    		writer.write("insmod /lib/modules/cpufreq_stats.ko\n");
    		writer.write(temp2);
    		writer.close();
    	}catch(Exception Ignored){}
    	
    	ShellInterface.runCommand("chmod 777 /data/data/com.shane87.ocapp/files/temp.sh");
    	ShellInterface.runCommand("/data/data/com.shane87.ocapp/files/temp.sh");
    	ShellInterface.runCommand("rm -f /data/data/com.shane87.ocapp/files/temp.sh");
    	
    	Toast.makeText(this, "Settings Applied!!", Toast.LENGTH_LONG).show();
    	
    }
    
    private void save()
    {
    	String temp;
    	OutputStreamWriter writer;
    	int maxFreq = 0;
    	
    	switch(selection)
    	{
    	case 0:
    		maxFreq = 1000;
    		break;
    	case 1:
    		maxFreq = 1120;
    		break;
    	case 2:
    		maxFreq = 1200;
    		break;
    	case 3:
    		maxFreq = 1300;
    		break;
    	}
    	
    	try
    	{
    		writer = new OutputStreamWriter(openFileOutput("S50_ocmod", 0));
    		
    		temp = "#!/system/bin/sh\n" +
    			   "#S50_ocmod\n" +
    			   "#This file will apply the selected oc level at boot\n\n" +
    			   "rmmod cpufreq_stats\n" +
    			   "echo " + Integer.toString(selection) + " > /sys/devices/virtual/misc/semaphore_cpufreq/oc\n" +
    			   "sleep 2\n\n" +
    			   "insmod /lib/modules/cpufreq_stats.ko\n" +
    			   "echo " + Integer.toString(maxFreq * 1000) + " > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
    		
    		writer.write(temp);
    		writer.close();
    	}catch(Exception Ignored){}
    	
    	ShellInterface.runCommand("busybox mount -o remount,rw /system");
    	ShellInterface.runCommand("busybox rm -f /system/etc/init.d/S_volt_scheduler");
    	ShellInterface.runCommand("busybox rm -f /system/etc/init.d/S50_ocmod");
    	ShellInterface.runCommand("busybox cp /data/data/com.shane87.ocapp/files/S50_ocmod /etc/init.d/S50_ocmod");
    	ShellInterface.runCommand("chmod 777 /etc/init.d/S50_ocmod");
    	ShellInterface.runCommand("busybox rm /data/data/com.shane87.ocapp/files/S50_ocmod");
    	ShellInterface.runCommand("busybox mount -o remount,ro /system");
    	
    	Toast.makeText(this, "Settings saved to /etc/init.d/S50_ocmod!!", Toast.LENGTH_LONG).show();
    }
    
    private void clearUV()
    {
    	String temp;
    	String tempAr[];
    	temp = ShellInterface.getProcessOutput("cat /sys/devices/system/cpu/cpu0/cpufreq/UV_mV_table");
    	
    	temp = temp.trim();
    	tempAr = temp.split(" ");
    	
    	tempAr[0] = "0";
    	temp = "\"";
    	for(int i = 0; i < tempAr.length; i++)
    	{
    		temp = temp.concat(tempAr[i] + " ");
    	}
    	
    	temp = temp.concat("\"");
    	
    	ShellInterface.runCommand("echo " + temp + " > /sys/devices/system/cpu/cpu0/cpufreq/UV_mV_table");
    }
}