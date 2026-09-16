package xin.chunming.bean;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter@Setter@AllArgsConstructor@NoArgsConstructor
public class AllFail {
    String fallback;
    String delayminute;
    String maxtrycount;
}
