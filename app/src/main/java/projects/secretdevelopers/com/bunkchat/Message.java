package projects.secretdevelopers.com.bunkchat;

import android.graphics.Color;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Message implements Serializable{
    //Message type: 0 for text message, 1 for other byte formats
    private int messType;
    private String sender;
    private String textMessage;
    private byte[] otherMessage;
    private String formattedDate;
    private int color;


    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    Message(int mess, String sender, Date dateTime, int color, String text){

        messType = mess;
        this.sender = sender;
        String strDateFormat = "hh:mm:ss a";

        DateFormat dateFormat = new SimpleDateFormat(strDateFormat);

        formattedDate= dateFormat.format(dateTime);
        this.color = color;
        textMessage = text;
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
                                        