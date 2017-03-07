package server;

/**
 * Created by renmingxu on 2017/3/6.
 */
public class Main {
    public static void main(String[] args) {
        HttpServer httpServer = new HttpServer(8080, "d:\\html\\");
        httpServer.start();
    }
}
