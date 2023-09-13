package com.fce4.dtrtoolkit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.text.SimpleDateFormat;
import java.util.Date;

@SpringBootApplication
@ComponentScan("com.fce4.dtrtoolkit.Validators")
@ComponentScan("com.fce4.dtrtoolkit.Extractors")
@ComponentScan("com.fce4.dtrtoolkit")
@EnableScheduling

public class TypeApplication {
	public static void main(String[] args) {
		Date currentDate = new Date(System.currentTimeMillis());
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MMM");
		System.setProperty("timestamp", df.format(currentDate));
		SpringApplication.run(TypeApplication.class, args);
	}
}
