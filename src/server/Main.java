package server;

/**
 * Created by renmingxu on 2017/3/6.
 */
public class Main {
    public static void main(String[] args) {
        HttpServer httpServer = new HttpServer("config.conf");
        httpServer.start();
    }
}
