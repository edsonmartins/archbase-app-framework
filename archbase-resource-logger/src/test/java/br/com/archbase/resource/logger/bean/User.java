package br.com.archbase.resource.logger.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;


@Data
@AllArgsConstructor
public class User {
    @Nullable
    private Integer id;

    @Nonnull
    private String email;

    @Nonnull
    private String password;

    public User() {
    }
}
