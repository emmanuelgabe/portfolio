package com.emmanuelgabe.portfolio.chat.tools;

import com.emmanuelgabe.portfolio.chat.ToolCallTracker;
import com.emmanuelgabe.portfolio.dto.CvResponse;
import com.emmanuelgabe.portfolio.dto.SiteConfigurationResponse;
import com.emmanuelgabe.portfolio.service.CvService;
import com.emmanuelgabe.portfolio.service.SiteConfigurationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteInfoToolTest {

    @Mock
    private SiteConfigurationService siteConfigService;

    @Mock
    private CvService cvService;

    @Mock
    private ToolCallTracker tracker;

    @InjectMocks
    private SiteInfoTool siteInfoTool;

    // ========== getSiteConfig ==========

    @Test
    void should_returnSiteConfig_when_getSiteConfigCalled() {
        // Arrange
        SiteConfigurationResponse config = new SiteConfigurationResponse();
        config.setFullName("Emmanuel Gabe");
        config.setGithubUrl("https://github.com/emmanuelgabe");
        when(siteConfigService.getSiteConfiguration()).thenReturn(config);

        // Act
        SiteConfigurationResponse result = siteInfoTool.getSiteConfig();

        // Assert
        assertThat(result.getFullName()).isEqualTo("Emmanuel Gabe");
        assertThat(result.getGithubUrl()).isEqualTo("https://github.com/emmanuelgabe");
        verify(siteConfigService).getSiteConfiguration();
    }

    // ========== getCurrentCV ==========

    @Test
    void should_returnCv_when_getCurrentCVCalledAndCvExists() {
        // Arrange
        CvResponse cv = new CvResponse();
        cv.setFileUrl("/api/cv/download");
        cv.setCurrent(true);
        when(cvService.getCurrentCv()).thenReturn(Optional.of(cv));

        // Act
        CvResponse result = siteInfoTool.getCurrentCV();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFileUrl()).isEqualTo("/api/cv/download");
        verify(cvService).getCurrentCv();
    }

    @Test
    void should_returnNull_when_getCurrentCVCalledAndNoCvAvailable() {
        // Arrange
        when(cvService.getCurrentCv()).thenReturn(Optional.empty());

        // Act
        CvResponse result = siteInfoTool.getCurrentCV();

        // Assert
        assertThat(result).isNull();
    }
}
