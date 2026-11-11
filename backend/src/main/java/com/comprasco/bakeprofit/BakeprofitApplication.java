package com.comprasco.bakeprofit;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BakeprofitApplication {

	/**
	 * Zona de la aplicación: las fechas se guardan como LocalDateTime en columnas TIMESTAMP sin zona,
	 * así que la hora que se guarda es la del reloj de la JVM. Fijarla hace que el backend guarde la
	 * hora de Colombia en cualquier servidor, igual que el scraper y la base (DECISIONS.md, "Zona horaria").
	 * El driver de PostgreSQL usa esta zona en su sesión, así que también aplica a los DEFAULT now().
	 */
	public static final String ZONA_HORARIA = "America/Bogota";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA));
		SpringApplication.run(BakeprofitApplication.class, args);
	}

}