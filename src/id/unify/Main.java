/**
 * @author: Muhammad Hadir
 * @date: 11/16/2017
 */

package id.unify;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Main {

    private static final int MIN_INT_VALUE = 0;
    private static final int MAX_INT_VALUE = 255;
    private static final int PICTURE_HEIGHT = 50;
    private static final int PICTURE_WIDTH = 50;
    private static final int TIMEOUT_VALUE_SEC = 5; // wait 5 seconds before timing out the request

    private static final String REQUEST_URL = "https://www.random" +
            ".org/integers/?num="+ (PICTURE_HEIGHT * PICTURE_WIDTH) +"&min="+ MIN_INT_VALUE +"&max="+ MAX_INT_VALUE +"&col=1&base=10&format=plain&rnd=new";

    private static final String QUOTA_CHECKER_URL = "https://www.random.org/quota/?format=plain";

    public static void main(String[] args) {
        URL url = null;
        HttpURLConnection connection;
        BufferedReader in;
        StringBuilder sb;
        int httpCode;

        try {
            url = new URL(QUOTA_CHECKER_URL);
            connection = (HttpURLConnection) url.openConnection();
            httpCode = connection.getResponseCode();

            if(HttpURLConnection.HTTP_OK == httpCode) {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                int quotaValue = Integer.valueOf(formatResponseOutput(in)).intValue();
                if(quotaValue >= 0) {
                    /* make the call to get random integers */

                    url = new URL(REQUEST_URL);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(1000 * TIMEOUT_VALUE_SEC); // timeout of 0 means infinite timeout
                    httpCode = connection.getResponseCode();
                    if(HttpURLConnection.HTTP_OK == httpCode) {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        generateBitmapImage(generatePixelValueArray(in));
                    } else {
                        in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                        printErrorFromAPI(httpCode, formatResponseOutput(in));
                        return;
                    }
                } else {
                    /* back off from making the call as per the RANDOM.ORG guidelines */
                    System.err.println("API quota expired, please try to run the program after 10 mins.");
                    return;
                }
            } else {
                in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                printErrorFromAPI(httpCode, formatResponseOutput(in));
                return;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateBitmapImage(int[] pixelValues) throws IOException {
        BufferedImage bufferedImage = new BufferedImage(PICTURE_WIDTH, PICTURE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        int pixelIndex = 0;
        for(int x = 0; x < PICTURE_HEIGHT; x++){
            pixelIndex = x * (PICTURE_WIDTH - 1);
            for (int y = 0; y < PICTURE_WIDTH; y++) {
                bufferedImage.setRGB(x, y, pixelValues[pixelIndex + x + y]);
            }
        }
        ImageIO.write(bufferedImage, "bmp", new File("generatedBMPImage.bmp"));
    }

    private static String formatResponseOutput(BufferedReader in) throws IOException {
        StringBuffer sb = new StringBuffer();
        String line;
        while((line = in.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private static int[] generatePixelValueArray(BufferedReader in) throws IOException {
        int[] pixels = new int[PICTURE_HEIGHT * PICTURE_WIDTH];
        String pixelValStr;
        int i = 0;
        while((pixelValStr = in.readLine()) != null) {
            pixels[i++] = Integer.valueOf(pixelValStr).intValue();
        }
        return pixels;
    }

    private static void printErrorFromAPI(int httpCode, String errMsgFromAPI) {
        String errMsg = null;
        switch (httpCode) {
            case HttpURLConnection.HTTP_UNAVAILABLE:
                errMsg = "503: " + errMsgFromAPI;
                break;
            case HttpURLConnection.HTTP_BAD_REQUEST:
                errMsg = "400: " + errMsgFromAPI;
                break;
            case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
                errMsg = "408" + errMsgFromAPI;
                break;
            case HttpURLConnection.HTTP_MOVED_PERM:
                errMsg = "301" + errMsgFromAPI;
                break;
            default:
                errMsg = "";
                break;
        }
        System.err.println("HTTP Error - " + errMsg);
    }
}
