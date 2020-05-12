package com.smtp.server;

import java.io.*;
import java.util.concurrent.ThreadLocalRandom;

class Mail{
    private static final String path = "SMTPServer//emails//";
    private static String mailFrom;
    private static String RCPTTo;
    private static String data;

    public static void setMailFrom(String mail){
        String []data = mail.split("\r\n");
        mailFrom = data[0];
    }
    public static void setRCPTTo(String RCPT){
        String []data = RCPT.split("\r\n");
        RCPTTo = data[0];
    }
    public static void setData(String msg){
        data = msg;
    }

    public static String getData() {
        return data;
    }

    public static String getMailFrom() {
        return mailFrom;
    }

    public static String getRCPTTo() {
        return RCPTTo;
    }

    public static void handleWriteFile(){

        byte[] msgBuffer = data.trim().getBytes();
        int randomNum = ThreadLocalRandom.current().nextInt(0, 150000);
        String directoryName = path+RCPTTo;
        String fileName = mailFrom+"_"+randomNum+".txt";

        File directory = new File(directoryName);

        if (! directory.exists()){
            System.out.println("123" );
            directory.mkdirs();
        }

        File file = new File(directoryName + "/" + fileName);


        try{
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(data);
            bw.close();
        }
        catch (IOException e){
            e.printStackTrace();
            System.exit(-1);
        }


//            RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
//            raf.write(msgBuffer);
    }
}