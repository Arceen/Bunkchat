package projects.secretdevelopers.com.bunkchat;

import android.graphics.Color;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Message implements Serializable{
    //Message type: 0 for text message, 1 for hello, 2 for acknowledgement, 3 for other byte formats
    private int messType;
    private String sender;
    private String textMessage;
    private byte[] otherMessage;
    private String formattedDate;
    private int color;

    public ArrayList<ClientScanResult> getReceiverList() {
        return receiverList;
    }

    public void setReceiverList(ArrayList<ClientScanResult> receiverList) {
        this.receiverList = receiverList;
    }

    private ArrayList<ClientScanResult> receiverList;

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    //request(hello) message

    Message(int messType){
        this.messType = messType;

    }


    //acknowledgement message
    Message(int mess,ArrayList<ClientScanResult> receiverList ){
        this.messType = mess;
        this.receiverList = receiverList;
    }

    Message(int mess, String sender, Date dateTime, int color){

        messType = mess;
        this.sender = sender;
        String strDateFormat = "hh:mm:ss a";

        DateFormat dateFormat = new SimpleDateFormat(strDateFormat);

        formattedDate= dateFormat.format(dateTime);
        this.color = color;

    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public int getMessType() {

        return messType;
    }

    public void setMessType(int messType) {
        this.messType = messType;
    }

    public String getTextMessage() {
        return textMessage;
    }

    public void setTextMessage(String textMessage) {
        this.textMessage = textMessage;
    }

    public byte[] getOtherMessage() {
        return otherMessage;
    }

    public void setOtherMessage(byte[] otherMessage) {
        this.otherMessage = otherMessage;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public void setFormattedDate(String formattedDate) {
        this.formattedDate = formattedDate;
    }

}
                                        