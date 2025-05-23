package com.rays;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties.Web;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.rays.common.FrontCtl;

/**
 * Dipanshi Mukati
 *
 */
@SpringBootApplication
public class ProjectOrsApplication extends SpringBootServletInitializer {
//	jo application ko servlet base deployment ke liye ready karta h

	@Autowired
	private Environment env;

	@Autowired
	FrontCtl frontCtl;

	public static void main(String[] args) {
		SpringApplication.run(ProjectOrsApplication.class, args);

	}

	/**
	 * Enables CORS to all urls (cross origin resource sharing)
	 * 
	 * @return
	 */
//	jo application ko servlet base deployment ke liye ready karta h

	@Bean
	public WebMvcConfigurer corsConfigurer() {

		WebMvcConfigurer w = new WebMvcConfigurer() {

			/**
			 * Add CORS
			 * 
			 */
			public void addCorsMappings(CorsRegistry registry) {
				CorsRegistration cors = registry.addMapping("/**");
				cors.allowedOrigins("http://localhost:4200");
				cors.allowedHeaders("*");
				cors.allowedMethods("GET", "POST", "PUT", "DELETE");

				cors.allowCredentials(true);
			}

			/**
			 * Add Interceptorst
			 */

			/*
			 * @Override public void addInterceptors(InterceptorRegistry registry) {
			 * registry.addInterceptor(frontCtl).addPathPatterns("/**").excludePathPatterns(
			 * "/Auth/**"); }
			 * 
			 * 
			 */

			/*
			 * @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
			 * registry.addResourceHandler("/**").addResourceLocations("classpath:/public/")
			 * ; }
			 * 
			 */

		};

		return w;
	}

}
