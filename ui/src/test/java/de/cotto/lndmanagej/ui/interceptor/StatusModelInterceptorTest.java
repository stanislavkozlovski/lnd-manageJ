package de.cotto.lndmanagej.ui.interceptor;

import de.cotto.lndmanagej.ui.StatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static de.cotto.lndmanagej.controller.dto.StatusModelFixture.STATUS_MODEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusModelInterceptorTest {

    @InjectMocks
    private StatusModelInterceptor statusModelInterceptor;

    @Mock
    private StatusService statusService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Object handler;

    @Test
    void postHandle_modelNotNull_addStatusModel() {
        when(statusService.getStatus()).thenReturn(STATUS_MODEL);
        ModelAndView modelAndView = new ModelAndView();
        statusModelInterceptor.postHandle(request, response, handler, modelAndView);
        verify(statusService).getStatus();
        assertThat(modelAndView.getModel().containsKey("status")).isTrue();
        assertThat(modelAndView.getModel().get("status")).isEqualTo(STATUS_MODEL);
    }

    @Test
    void postHandle_noModel_addStatusModel() {
        ModelAndView modelAndView = null;
        statusModelInterceptor.postHandle(request, response, handler, modelAndView);
        verifyNoInteractions(statusService);
    }

}
