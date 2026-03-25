package com.systemforge.backend.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemforge.backend.auth.service.JwtService;
import com.systemforge.backend.auth.service.SecurityService;
import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import com.systemforge.backend.common.enums.SystemType;
import com.systemforge.backend.system.dto.SystemDefinitionDto;
import com.systemforge.backend.system.dto.UserSystemConfigDto;
import com.systemforge.backend.system.dto.request.CreateSystemConfigRequest;
import com.systemforge.backend.system.service.SystemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SystemController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SystemService systemService;

    @MockBean
    private SecurityService securityService;

    @MockBean
    private JwtService jwtService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        // Since security is disabled via excludeAutoConfiguration, we mock our central SecurityService
        when(securityService.getAuthenticatedUserId()).thenReturn(userId);
    }

    @Test
    void getAllSystems_returnsOk() throws Exception {
        SystemDefinitionDto sys = new SystemDefinitionDto();
        sys.setSystemType(SystemType.STRIPE);

        when(systemService.getAllSystems()).thenReturn(List.of(sys));

        mockMvc.perform(get("/api/v1/systems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].systemType").value("STRIPE"));
    }

    @Test
    void createConfig_success_returnsCreated() throws Exception {
        CreateSystemConfigRequest req = new CreateSystemConfigRequest();
        req.setConfigName("My Test Config");
        req.setAppType(AppType.SAAS);
        req.setAppScale(AppScale.MEDIUM);
        req.setSelectedSystemsJson("[]");

        UserSystemConfigDto dto = new UserSystemConfigDto();
        dto.setId(UUID.randomUUID());

        when(systemService.createConfig(eq(userId), any(CreateSystemConfigRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v1/systems/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(dto.getId().toString()));
    }

    @Test
    void generateArchitecture_success_returnsPayload() throws Exception {
        UUID configId = UUID.randomUUID();
        UserSystemConfigDto dto = new UserSystemConfigDto();
        dto.setId(configId);

        when(systemService.generateArchitecture(userId, configId)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/systems/configs/{id}/generate", configId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
