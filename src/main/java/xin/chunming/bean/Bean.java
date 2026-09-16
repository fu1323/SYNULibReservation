package xin.chunming.bean;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
@Getter@Setter
public class Bean {
    private HashMap<String,String> seatId;
    private String unionId;
    private boolean renew;
    private boolean fallback;
    private int miniute;
    private int lastRenewHour;
    private int lastRenewMinute;

    public Bean() {
    }

    public Bean(HashMap<String, String> seatId, String unionId, boolean renew, boolean fallback, int miniute, int lastRenewHour, int lastRenewMinute, String token) {
        this.seatId = seatId;
        this.unionId = unionId;
        this.renew = renew;
        this.fallback = fallback;
        this.miniute = miniute;
        this.lastRenewHour = lastRenewHour;
        this.lastRenewMinute = lastRenewMinute;
        this.token = token;
    }

    private String token;
}
