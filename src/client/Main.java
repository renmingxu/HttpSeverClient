package client;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by renmingxu on 2017/3/8.
 */
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("HttpClient");
        while (true) {
            System.out.println("get or post ?");
            String cmd = scanner.next();
            System.out.print("Url:");
            String url = scanner.next();
            System.out.println("Url:" + url);
            HttpClient httpClient = new HttpClient(url);
            Response response = null;
            switch (cmd) {
                case "get":
                    response = httpClient.get();
                    break;
                case "post":
                    System.out.print("data type: string(str) or key_value(kv)");
                    String type = scanner.next();
                    if ("str".equals(type)) {
                        System.out.print("Data:");
                        String data = scanner.next();
                        response = httpClient.post(data);
                    } else if ("kv".equals(type)) {
                        Map<String, String> d = new HashMap<>();
                        while (true) {
                            System.out.print("Data: key:");
                            String k = scanner.next();
                            System.out.print("Data: value:");
                            String v = scanner.next();
                            d.put(k,v);
                            System.out.print("Exit?:(y)");
                            if ("y".equals(scanner.next())) {
                                break;
                            }
                        }
                        response = httpClient.post(d);
                    }
                    break;
                    default:
                        continue;
            }
            if (response == null) {
                continue;
            }
            System.out.println("print content(print) or save to file(save)");
            String c = scanner.next();
            if ("print".equals(c)) {
                System.out.println(response.getText());
            }else if ("save".equals(c)) {
                System.out.print("filename");
                String filename = scanner.next();
                File file = new File(filename);
                FileOutputStream o = null;
                try {
                    o = new FileOutputStream(file);
                    o.write(response.getContent());
                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                }
                try {
                    o.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
