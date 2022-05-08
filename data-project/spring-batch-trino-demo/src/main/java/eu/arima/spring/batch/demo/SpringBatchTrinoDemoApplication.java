package eu.arima.spring.batch.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication()
public class SpringBatchTrinoDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBatchTrinoDemoApplication.class, args);
	}

}
