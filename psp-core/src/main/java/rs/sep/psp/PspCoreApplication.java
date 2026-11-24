package rs.sep.psp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PspCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(PspCoreApplication.class, args);
    }
}
