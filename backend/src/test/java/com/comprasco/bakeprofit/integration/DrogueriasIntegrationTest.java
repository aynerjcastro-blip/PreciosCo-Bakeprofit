package com.comprasco.bakeprofit.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Tag("integration")
class Hu28DrogueriasIntegrationTest {

    private static final long DROGUERIAS_SECTION_ID = 16L;
    private static final long NON_EXISTENT_SECTION_ID = 999999L;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("HU-28: Caso de éxito al consultar productos/sección de droguerías (HTTP 200)")
    void buscarDroguerias_casoExito_retorna200() throws Exception {
        mockMvc.perform(get("/api/products/search")
                        .param("sectionId", String.valueOf(DROGUERIAS_SECTION_ID))
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("HU-28: Caso de error al consultar sección de droguerías inexistente (HTTP 404)")
    void buscarDroguerias_seccionInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/api/products/search")
                        .param("sectionId", String.valueOf(NON_EXISTENT_SECTION_ID))
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}