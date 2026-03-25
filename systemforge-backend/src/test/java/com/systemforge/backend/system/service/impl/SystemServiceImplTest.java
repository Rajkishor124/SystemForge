package com.systemforge.backend.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemforge.backend.common.dto.PagedResponse;
import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import com.systemforge.backend.common.enums.SystemType;
import com.systemforge.backend.common.exception.BusinessException;
import com.systemforge.backend.common.exception.ResourceNotFoundException;
import com.systemforge.backend.recommendation.dto.RecommendationRequest;
import com.systemforge.backend.recommendation.dto.RecommendationResult;
import com.systemforge.backend.recommendation.service.RecommendationService;
import com.systemforge.backend.system.dto.SystemDefinitionDto;
import com.systemforge.backend.system.dto.UserSystemConfigDto;
import com.systemforge.backend.system.dto.request.CreateSystemConfigRequest;
import com.systemforge.backend.system.entity.SystemDefinition;
import com.systemforge.backend.system.entity.UserSystemConfig;
import com.systemforge.backend.system.mapper.SystemMapper;
import com.systemforge.backend.system.repository.SystemDefinitionRepository;
import com.systemforge.backend.system.repository.UserSystemConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemServiceImplTest {

    @Mock
    private SystemDefinitionRepository systemDefinitionRepository;

    @Mock
    private UserSystemConfigRepository configRepository;

    @Mock
    private SystemMapper systemMapper;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SystemServiceImpl systemService;

    private UUID userId;
    private UUID configId;
    private UserSystemConfig config;
    private UserSystemConfigDto configDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        configId = UUID.randomUUID();
        
        config = UserSystemConfig.builder()
                .userId(userId)
                .configName("My Test App")
                .appType(AppType.RIDE_HAILING)
                .appScale(AppScale.MEDIUM)
                .isGenerated(false)
                .build();
        config.setId(configId);
        
        configDto = UserSystemConfigDto.builder()
                .id(configId)
                .userId(userId)
                .configName("My Test App")
                .generated(false)
                .build();
    }

    @Test
    void getAllSystems_returnsList() {
        SystemDefinition def = new SystemDefinition();
        SystemDefinitionDto dto = SystemDefinitionDto.builder().build();
        
        when(systemDefinitionRepository.findByIsActiveTrue()).thenReturn(List.of(def));
        when(systemMapper.toDto(def)).thenReturn(dto);
        
        List<SystemDefinitionDto> result = systemService.getAllSystems();
        
        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
    }

    @Test
    void getSystemsByType_returnsFilteredList() {
        SystemDefinition def = new SystemDefinition();
        SystemDefinitionDto dto = SystemDefinitionDto.builder().build();
        
        when(systemDefinitionRepository.findBySystemTypeAndIsActiveTrue(SystemType.POSTGRESQL)).thenReturn(List.of(def));
        when(systemMapper.toDto(def)).thenReturn(dto);
        
        List<SystemDefinitionDto> result = systemService.getSystemsByType(SystemType.POSTGRESQL);
        
        assertEquals(1, result.size());
        assertEquals(dto, result.get(0));
    }

    @Test
    void createConfig_savesAndReturnsDto() {
        // We mock field injection by setting up a request manually via reflection/mock or constructor if possible
        CreateSystemConfigRequest request = mock(CreateSystemConfigRequest.class);
        when(request.getConfigName()).thenReturn("My Test App");
        when(request.getAppType()).thenReturn(AppType.RIDE_HAILING);
        when(request.getAppScale()).thenReturn(AppScale.MEDIUM);
        when(request.getSelectedSystemsJson()).thenReturn("[]");

        when(configRepository.save(any(UserSystemConfig.class))).thenReturn(config);
        when(systemMapper.toDto(config)).thenReturn(configDto);

        UserSystemConfigDto result = systemService.createConfig(userId, request);

        assertNotNull(result);
        assertEquals(configDto.getId(), result.getId());
        
        ArgumentCaptor<UserSystemConfig> captor = ArgumentCaptor.forClass(UserSystemConfig.class);
        verify(configRepository).save(captor.capture());
        
        UserSystemConfig captured = captor.getValue();
        assertEquals(userId, captured.getUserId());
        assertEquals("My Test App", captured.getConfigName());
        assertFalse(captured.isGenerated());
    }

    @Test
    void getConfigById_found_returnsDto() {
        when(configRepository.findByIdAndUserId(configId, userId)).thenReturn(Optional.of(config));
        when(systemMapper.toDto(config)).thenReturn(configDto);
        
        UserSystemConfigDto result = systemService.getConfigById(userId, configId);
        
        assertEquals(configDto, result);
    }

    @Test
    void getConfigById_notFound_throwsException() {
        when(configRepository.findByIdAndUserId(configId, userId)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> systemService.getConfigById(userId, configId));
    }

    @Test
    void getUserConfigs_returnsPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserSystemConfig> page = new PageImpl<>(List.of(config), pageable, 1);
        
        when(configRepository.findByUserId(userId, pageable)).thenReturn(page);
        when(systemMapper.toDto(config)).thenReturn(configDto);
        
        PagedResponse<UserSystemConfigDto> res = systemService.getUserConfigs(userId, pageable);
        
        assertEquals(1, res.getContent().size());
        assertEquals(configDto, res.getContent().get(0));
        assertEquals(1, res.getTotalElements());
    }

    @Test
    void generateArchitecture_notGenerated_triggersAiAndSaves() throws Exception {
        when(configRepository.findByIdAndUserId(configId, userId)).thenReturn(Optional.of(config));
        
        RecommendationResult recommendationResult = RecommendationResult.builder().build();
        when(recommendationService.recommend(any(RecommendationRequest.class))).thenReturn(recommendationResult);
        when(objectMapper.writeValueAsString(recommendationResult)).thenReturn("{\"status\":\"ok\"}");
        when(configRepository.save(any(UserSystemConfig.class))).thenReturn(config);
        when(systemMapper.toDto(config)).thenReturn(configDto);
        
        UserSystemConfigDto result = systemService.generateArchitecture(userId, configId);
        
        assertNotNull(result);
        
        ArgumentCaptor<RecommendationRequest> reqCaptor = ArgumentCaptor.forClass(RecommendationRequest.class);
        verify(recommendationService).recommend(reqCaptor.capture());
        assertEquals(AppType.RIDE_HAILING, reqCaptor.getValue().getAppType());
        assertEquals(AppScale.MEDIUM, reqCaptor.getValue().getScale());
        
        verify(configRepository).save(config);
        assertTrue(config.isGenerated());
        assertEquals("{\"status\":\"ok\"}", config.getGeneratedOutputJson());
    }

    @Test
    void generateArchitecture_alreadyGenerated_throwsException() {
        config.setGenerated(true);
        when(configRepository.findByIdAndUserId(configId, userId)).thenReturn(Optional.of(config));
        
        assertThrows(BusinessException.class, () -> systemService.generateArchitecture(userId, configId));
        verifyNoInteractions(recommendationService);
    }

    @Test
    void generateArchitecture_jsonFailure_throwsException() throws Exception {
        when(configRepository.findByIdAndUserId(configId, userId)).thenReturn(Optional.of(config));
        
        RecommendationResult recommendationResult = RecommendationResult.builder().build();
        when(recommendationService.recommend(any(RecommendationRequest.class))).thenReturn(recommendationResult);
        
        // Mock Jackson failure
        when(objectMapper.writeValueAsString(recommendationResult))
            .thenThrow(new JsonProcessingException("Simulation failure") {});
            
        assertThrows(BusinessException.class, () -> systemService.generateArchitecture(userId, configId));
    }

    @Test
    void deleteConfig_setsDeletedAndSaves() {
        when(configRepository.findByIdAndUserId(configId, userId)).thenReturn(Optional.of(config));
        
        systemService.deleteConfig(userId, configId);
        
        verify(configRepository).save(config);
        assertTrue(config.isDeleted());
    }
}
