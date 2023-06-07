package com.fce4.dtrtoolkit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.fce4.dtrtoolkit.validators")
@ComponentScan("com.fce4.dtrtoolkit")

public class TypeApplication {
	public static void main(String[] args) {
		SpringApplication.run(TypeApplication.class, args);
	}

}
