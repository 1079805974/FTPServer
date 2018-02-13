package utils;

import com.sun.deploy.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class Utils {
    public static final long YEAR = 525600000;

    public static String getFileInfo(File file) throws IOException {
        String infoArray[] = new String[9];
        long modifiedTime = file.lastModified();
        Date modifiedDate = new Date(modifiedTime);
        Calendar c = Calendar.getInstance();
        c.setTime(modifiedDate);
        int modifiedYear = c.get(Calendar.YEAR);
        int modifiedMonth = c.get(Calendar.MONTH) + 1;
        int modifiedDay = c.get(Calendar.DAY_OF_MONTH) + 1;
        int modifiedHour = c.get(Calendar.HOUR);
        int modifiedMinute = c.get(Calendar.MINUTE);
        Date now = new Date();
        if (now.getTime() - modifiedDate.getTime() > YEAR) {
            infoArray[7] = modifiedYear + "";
        } else {
            infoArray[7] = modifiedHour + ":" + modifiedMinute;
        }
        infoArray[6] = modifiedDay + "";
        infoArray[5] = numberToMonth(modifiedMonth);
        if (file.isDirectory()) {
            infoArray[0] = "drwxr-xr-x";
            infoArray[4] = "4096";
        } else {
            infoArray[0] = "-rw-r--r--";
            infoArray[4] = file.length() + "";
        }
        infoArray[1] = "1";
        //Path path = Paths.get(file.getAbsolutePath());
        //UserPrincipal owner = Files.getOwner(path);
        //infoArray[3]= owner.getName();
        infoArray[3] = "ftp";
        infoArray[2] = "ftp";
        infoArray[8] = file.getName();
        List<String> list = Arrays.asList(infoArray);
        return StringUtils.join(list, "      ");
    }

    public static String numberToMonth(int month) {
        switch (month) {
            case 1:
                return "Jan";
            case 2:
                return "Feb";
            case 3:
                return "Mar";
            case 4:
                return "Apr";
            case 5:
                return "May";
            case 6:
                return "Jun";
            case 7:
                return "Jul";
            case 8:
                return "Aug";
            case 9:
                return "Sep";
            case 10:
                return "Oct";
            case 11:
                return "Nov";
            case 12:
                return "Dec";
            default:
                return "";
        }
    }
    public static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }
}
