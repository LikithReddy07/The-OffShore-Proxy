package offshore.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TheOffShoreProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TheOffShoreProxyApplication.class, args);
        System.out.println("The OffShore Proxy Application...");

        new Thread(() -> {
            try {
                new OffShoreServer().start(9090); // listen on port 9090
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

}
