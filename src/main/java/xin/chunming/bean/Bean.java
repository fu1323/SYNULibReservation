package xin.chunming.bean;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
@Getter@Setter@NoArgsConstructor@AllArgsConstructor
public class Bean {
    private HashMap<String,String> seatId;
    private String unionId;
    private boolean renew;
    private boolean fallback;
    private int miniute;
    private int lastRenewHour;
    private int lastRenewMinute;
    private int renew_gap_minute;


    public Bean(HashMap<String, String> seatId, String unionId, boolean renew, boolean fallback, int miniute, int lastRenewHour, int lastRenewMinute, String token,int renew_gap_minute) {
        this.seatId = seatId;
        this.unionId = unionId;
        this.renew = renew;
        this.fallback = fallback;
        this.miniute = miniute;
        this.lastRenewHour = lastRenewHour;
        this.lastRenewMinute = lastRenewMinute;
        this.token = token;
        this.renew_gap_minute=renew_gap_minute;
    }

    private String token;
}
