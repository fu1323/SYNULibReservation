package xin.chunming;

import java.util.ArrayList;

public class Bean {
    private ArrayList<String> seatId;
    private String unionId;
    private boolean renew;
    private int miniute;

    public int getMiniute() {
        return miniute;
    }

    public void setMiniute(int miniute) {
        this.miniute = miniute;
    }

    public ArrayList<String> getSeatId() {
        return seatId;
    }

    public void setSeatId(ArrayList<String> seatId) {
        this.seatId = seatId;
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

    public void setRenew(boolean renew) {
        this.renew = renew;
    }

    public Bean(ArrayList<String> seatId, String unionId, boolean renew, int miniute, String token) {
        this.seatId = seatId;
        this.unionId = unionId;
        this.renew = renew;
        this.miniute = miniute;
        this.token = token;
    }

    private String token;
}
