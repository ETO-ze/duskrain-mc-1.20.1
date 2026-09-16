import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.concurrent.Executors;

/** Single public asset endpoint. No filesystem paths are accepted from requests. */
public class ResourcePackServer {
    public static void main(String[] args) throws Exception {
        Path pack=Path.of(args[0]).toRealPath();byte[] data=Files.readAllBytes(pack);
        HttpServer server=HttpServer.create(new InetSocketAddress(args[1],Integer.parseInt(args[2])),16);
        server.createContext("/",e->{try {
            if(!e.getRequestURI().getPath().equals("/DuskRain-Jade-City.zip")){e.sendResponseHeaders(404,-1);return;}
            if(!e.getRequestMethod().equals("GET")&&!e.getRequestMethod().equals("HEAD")){e.getResponseHeaders().set("Allow","GET, HEAD");e.sendResponseHeaders(405,-1);return;}
            e.getResponseHeaders().set("Content-Type","application/zip");
            e.getResponseHeaders().set("Cache-Control","no-cache");
            e.getResponseHeaders().set("X-Content-Type-Options","nosniff");
            e.getResponseHeaders().set("Content-Length",Integer.toString(data.length));
            if(e.getRequestMethod().equals("HEAD")){e.sendResponseHeaders(200,-1);return;}
            e.sendResponseHeaders(200,data.length);e.getResponseBody().write(data);
        }finally{e.close();}});
        var pool=Executors.newFixedThreadPool(4);server.setExecutor(pool);
        Runtime.getRuntime().addShutdownHook(new Thread(()->{server.stop(0);pool.shutdownNow();}));
        server.start();System.out.println("DuskRain resource pack ready: "+server.getAddress()+" ("+data.length+" bytes)");
    }
}
