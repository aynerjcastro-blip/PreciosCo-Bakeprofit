package com.comprasco.bakeprofit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Nunca procesa el staging aunque STAGING_ENABLED esté definida en el equipo
@org.junit.jupiter.api.Tag("integration")
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_URL", matches = ".+")
@SpringBootTest(properties = "preciosco.staging.enabled=false")
class BakeprofitApplicationTests {

	@Test
	void contextLoads() {
	}

}
