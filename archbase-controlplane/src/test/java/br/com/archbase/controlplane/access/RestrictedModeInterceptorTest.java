package br.com.archbase.controlplane.access;

import br.com.archbase.controlplane.config.ControlPlaneProperties;
import br.com.archbase.controlplane.license.LicenseStatus;
import br.com.archbase.controlplane.state.ControlPlaneState;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RestrictedModeInterceptorTest {

    private ControlPlaneState expired() {
        ControlPlaneState state = Mockito.mock(ControlPlaneState.class);
        Mockito.when(state.licenseStatus()).thenReturn(LicenseStatus.EXPIRED);
        return state;
    }

    @Test
    void reportSoRegistraEDeixaPassar() throws Exception {
        ControlPlaneProperties.RestrictedMode config = new ControlPlaneProperties.RestrictedMode();
        config.setEnforcement(ControlPlaneProperties.Enforcement.REPORT);
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean segue = new RestrictedModeInterceptor(expired(), config)
                .preHandle(new MockHttpServletRequest("POST", "/api/pedidos"), response, new Object());
        assertThat(segue).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void enforceRecusaEscritaELiberaLeituraECaminhosPermitidos() throws Exception {
        ControlPlaneProperties.RestrictedMode config = new ControlPlaneProperties.RestrictedMode();
        RestrictedModeInterceptor interceptor = new RestrictedModeInterceptor(expired(), config);

        MockHttpServletResponse escrita = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(new MockHttpServletRequest("PUT", "/api/pedidos/1"), escrita, new Object()))
                .isFalse();
        assertThat(escrita.getStatus()).isEqualTo(403);
        assertThat(escrita.getContentAsString()).contains("LICENSE_EXPIRED");

        assertThat(interceptor.preHandle(new MockHttpServletRequest("GET", "/api/pedidos"),
                new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(interceptor.preHandle(new MockHttpServletRequest("POST", "/api/v1/auth/authenticate"),
                new MockHttpServletResponse(), new Object())).isTrue();
    }
}
