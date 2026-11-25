package siteguard.model;

import java.text.SimpleDateFormat;
import java.util.Date;

/** Small helper that turns milliseconds into readable text. */
public class TimeText {

    public static String format(long timeMs) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return f.format(new Date(timeMs));
    }

    public static String clock(long timeMs) {
        SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss");
        return f.format(new Date(timeMs));
    }
}
