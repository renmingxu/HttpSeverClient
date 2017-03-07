package server;

/**
 * Created by renmingxu on 2017/3/6.
 */
public class Main {
    public static void main(String[] args) {
        HttpServer httpServer;
        if (args.length == 1) {
            httpServer = new HttpServer(args[0]);
        } else {
            httpServer = new HttpServer("config.conf");
        }
        httpServer.start();
    }
}
