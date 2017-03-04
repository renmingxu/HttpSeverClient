import client.HttpClient;
import client.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by renmingxu on 17-3-3.
 */
public class a {
    static long totalReq = 0;
    static long totalOK = 0;
    static String lock = "";
    public static void main(String[] args) throws InterruptedException {
        Map<String, String> data = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            data.put("id" + i, "data" + i);
        }
        ArrayList<Thread> threads = new ArrayList<>();
        for (int j = 0; j < 1000; j++) {
            Thread t = new Thread(){
                @Override
                public void run() {
                    while (true) {
                        HttpClient httpClient = new HttpClient("http://localhost/post.php");
                        for (int i = 0; i < 99; i++) {
                            Response response = httpClient.post(data);
                            synchronized (lock) {
                                if (response.getStatus_code() == 200){
                                    totalOK ++;
                                    totalReq ++;
                                } else {
                                    totalReq ++;
                                }
                            }
                        }
                        httpClient.close();
                    }
                }
            };
            threads.add(t);
        }
        System.out.println("All Thread Created");
        for (Thread t :
                threads) {
            t.start();
        }
        System.out.println("All Thread Started");
        while (true) {
            Thread.sleep(1000);
            System.out.println("Total: " + totalReq + "  OK: " + totalOK);
        }
    }
}
