package xin.chunming.bean;

import java.util.HashMap;

public class Bean {
    private HashMap<String,String> seatId;
    private String unionId;
    private boolean renew;
    private int miniute;
    private int lastRenewHour;
    private int lastRenewMinute;


    public int getMiniute() {
        return miniute;
    }

    public void setMiniute(int miniute) {
        this.miniute = miniute;
    }



    public String getUnionId() {
        return unionId;
    }

    public void setUnionId(String unionId) {
        this.unionId = unionId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isRenew() {
        return renew;
    }

    public int getLastRenewHour() {
        return lastRenewHour;
    }

    public void setLastRenewHour(int lastRenewHour) {
        this.lastRenewHour = lastRenewHour;
    }

    public int getLastRenewMinute() {
        return lastRenewMinute;
    }

    public void setLastRenewMinute(int lastRenewMinute) {
        this.lastRenewMinute = lastRenewMinute;
    }

    public void setRenew(boolean renew) {
        this.renew = renew;
    }

    public HashMap<String, String> getSeatId() {
        return seatId;
    }

    public void setSeatId(HashMap<String, String> seatId) {
        this.seatId = seatId;
    }

    public Bean() {
    }

    public Bean(HashMap<String, String> seatId, String unionId, boolean renew, int miniute, int lastRenewHour, int lastRenewMinute, String token) {
        this.seatId = seatId;
        this.unionId = unionId;
        this.renew = renew;
        this.miniute = miniute;
        this.lastRenewHour = lastRenewHour;
        this.lastRenewMinute = lastRenewMinute;
        this.token = token;
    }

    private String token;
}
