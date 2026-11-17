package br.com.archbase.controlplane.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalLoginRoutesFilterTest {

    @Test
    void reconheceAsRotasDeLoginLocal() {
        assertThat(LocalLoginRoutesFilter.isLocalLoginRoute("/api/v1/auth/authenticate")).isTrue();
        assertThat(LocalLoginRoutesFilter.isLocalLoginRoute("/api/v1/auth/login-social")).isTrue();
        assertThat(LocalLoginRoutesFilter.isLocalLoginRoute("/api/v1/auth/sendResetPasswordEmail/a@b.c")).isTrue();
        assertThat(LocalLoginRoutesFilter.isLocalLoginRoute("/api/v1/auth/authenticate/")).isTrue();
    }

    @Test
    void naoMexeNasDemais() {
        assertThat(LocalLoginRoutesFilter.isLocalLoginRoute("/api/v1/auth/logout")).isFalse();
        assertThat(LocalLoginRoutesFilter.isLocalLoginRoute("/api/v1/auth/contexts")).isFalse();
        assertThat(LocalLoginRoutesFilter.isLocalLoginRoute("/api/v1/auth/authenticated")).isFalse();
        assertThat(LocalLoginRoutesFilter.isLocalLoginRoute("/api/v1/usuarios")).isFalse();
        assertThat(LocalLoginRoutesFilter.isLocalLoginRoute(null)).isFalse();
    }
}
