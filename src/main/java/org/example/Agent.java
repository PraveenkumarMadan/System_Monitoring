package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;


import java.net.UnknownHostException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

public class Agent{

    public static double formatDouble(double val){
        String formattedNumber=String.format("%.2f",val);
        return Double.parseDouble(formattedNumber); //change to double
    }
    public static double getMemoryUsage() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        GlobalMemory memory = hardware.getMemory();
        return formatDouble(((double) (memory.getTotal() - memory.getAvailable()) / memory.getTotal()) * 100.0);
    }

    public static Double getDiskUsage(){
        long totalSpace=0,usableSpace=0;
        try {
            for (FileStore store : FileSystems.getDefault().getFileStores()) {
                totalSpace += store.getTotalSpace();
                usableSpace += store.getUsableSpace();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        long usedSpace=totalSpace-usableSpace;
        return formatDouble((usedSpace*100.0)/totalSpace);

    }
    public static Double getCpuUsage(){
        SystemInfo systemInfo = new SystemInfo();
        CentralProcessor processor = systemInfo.getHardware().getProcessor();

        long[] prevTicks = processor.getSystemCpuLoadTicks();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long[] ticks = processor.getSystemCpuLoadTicks();

        long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long system = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];

        return formatDouble((user + nice + system) * 100.0 / (user + nice + system + idle));
    }

    public static String getOsName(){
        return  System.getProperty("os.name");
    }

    public  static  String getIpAddress() throws UnknownHostException {
        InetAddress localhost = InetAddress.getLocalHost();
        return localhost.getHostAddress();
    }
    public static  String getHostName() throws UnknownHostException {
        InetAddress localhost = InetAddress.getLocalHost();
        return localhost.getHostName();
    }
    public static String getMacAddress(){
        StringBuilder macAddress= new StringBuilder();

        try{
            InetAddress inetAddress=InetAddress.getLocalHost();
            NetworkInterface networkInterface=NetworkInterface.getByInetAddress(inetAddress);
            byte[] addressbytes =networkInterface.getHardwareAddress();
            for(int i=0;i<addressbytes.length;i++){
                macAddress.append(String.format("%02x", addressbytes[i]));
                if(i!=(addressbytes.length - 1)) macAddress.append(":");
            }
        }
        catch(UnknownHostException | SocketException e){
            System.out.println(e.getMessage());
        }
        return macAddress.toString();
    }

    public static String getdate(){
        LocalDate cuurentDate=LocalDate.now();
        DateTimeFormatter dateFormatter=DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return cuurentDate.format(dateFormatter);
    }

    public static String gettime(){
        LocalTime currentTime=LocalTime.now();
        DateTimeFormatter timeFormatter=DateTimeFormatter.ofPattern("HH:mm:ss");
        return currentTime.format(timeFormatter);
    }
    public static String getWindowsServices(){
        StringBuilder jsonString= new StringBuilder("[");
        try {
            String command = "powershell.exe Get-Service | Select-Object DisplayName, ServiceName, ServiceType, Status, StartType";
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean first=true;
            while ((line = reader.readLine()) != null) {
                if(!line.equals("")){
                    String[] parts=line.split(":");
                    for(int i=0;i<2;i++)parts[i]=parts[i].trim();
                    if(parts[0].equals("DisplayName")){
                        if(!first){
                            jsonString.append(",");
                        }
                        else{
                            first=false;
                        }
                        jsonString.append("{\"display_name\":\"");
                        jsonString.append(parts[1]);
                        jsonString.append("\",");
                    }
                    else if(parts[0].equals("ServiceName")){
                        jsonString.append("\"service_name\":\"");
                        jsonString.append(parts[1]);
                        jsonString.append("\",");
                    }
//                    else if(parts[0].equals("ServiceType")){
//                        jsonString.append("\"service_type\":\"");
//                        jsonString.append(parts[1]);
//                        jsonString.append("\",");
//                    }

                    else if(parts[0].equals("StartType")){
                        jsonString.append("\"start_type\":\"");
                        jsonString.append(parts[1]);
                        jsonString.append("\"}");
                    }
                    else if(parts[0].equals("Status")){
                        jsonString.append("\"status\":\"");
                        jsonString.append(parts[1]);
                        jsonString.append("\",");
                    }
                }

            }
            jsonString.append("]");



        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(jsonString);
        return jsonString.toString();
    }
    public static void main(String[] args) {
        try{
            String ipAddress=getIpAddress();
            System.out.println("IP Address: " + ipAddress);
            String deviceName =getHostName();
            System.out.println("Device Name: " + deviceName);
            String osName =getOsName();
            System.out.println("Operating System: " + osName);
            String macAddress=getMacAddress();
            System.out.println("Mac Address :"+macAddress);
            String jsonString="{\"ip_address\":\""+ipAddress+"\",\"device_name\":\""+deviceName+"\",\"operating_system\":\""+osName+"\",\"mac_address\":\""+macAddress+"\"}";
            try{
                String apiUrl="http://localhost:7070/Server/addsystem";
                HttpClient httpClient= HttpClients.createDefault();
                HttpPost httpPost=new HttpPost(apiUrl);
                httpPost.setHeader("Content-Type","application/json");
                httpPost.setEntity(new StringEntity(jsonString));
                HttpResponse httpResponse=httpClient.execute(httpPost);
                String key=httpResponse.getFirstHeader("unique_key").getValue();
                System.out.println("Unique key : "+key);

                String postUrl="http://localhost:7070/Server/addusagedata"; //system components for every 5  min
                HttpClient httpClient1=HttpClients.createDefault();
                HttpPost httpPost1=new HttpPost(postUrl);
                httpPost1.setHeader("Content-Type","application/json");


                String windowurl="http://localhost:7070/Server/addwindowservices"; //adding the windows services components
                HttpClient httpClient2=HttpClients.createDefault();
                HttpPost httpPost2=new HttpPost(windowurl);
                httpPost2.setHeader("Content-Type","application/json");
                httpPost2.setHeader("unique_key",key);
                int count=59;
                while(true){
                    Double diskUsage=getDiskUsage();
                    System.out.println("Disk Used % :"+diskUsage);
                    Double cpuUsage=getCpuUsage();
                    System.out.println("Cpu Used % :"+cpuUsage);
                    double memoryUsage=getMemoryUsage();
                    System.out.println("Memory Used % :"+memoryUsage);
                    String date=getdate();
                    String time=gettime();
                    String usagevalues="{\"unique_key\":\""+key+"\",\"cpu_usage\":\""+cpuUsage+"\",\"memory_usage\":\""+memoryUsage+"\",\"disk_usage\":\""+diskUsage+"\",\"event_time\":\""+time+"\",\"event_date\":\""+date+"\"}";
                    System.out.println(usagevalues);
                    httpPost1.setEntity(new StringEntity(usagevalues));
                    httpClient1.execute(httpPost1);
                    count++;
                    if (count==60) {
                        String jsondata=getWindowsServices();
                        httpPost2.setEntity(new StringEntity(jsondata));
                        httpClient2.execute(httpPost2);
                        count=0;
                    }
                    try{
                        Thread.sleep(60*1000);
                    }
                    catch (Exception e){
                        System.out.println(e.getMessage());
                    }
                }


            }
            catch(Exception e){
                System.out.println(e.getMessage());
            }
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }

    }
}
