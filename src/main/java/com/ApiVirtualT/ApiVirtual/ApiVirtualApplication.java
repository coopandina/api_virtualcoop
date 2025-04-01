package com.ApiVirtualT.ApiVirtual;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;


@SpringBootApplication
public class ApiVirtualApplication extends  SpringBootServletInitializer {
	public static void main(String[] args) {
		SpringApplication.run(ApiVirtualApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.sources(ApiVirtualApplication.class);
	}

}
