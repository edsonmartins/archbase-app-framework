package br.com.archbase.security.access;

/**
 * Escape para quem não modela senioridade no perfil.
 *
 * <p>Por padrão o nível de uma pessoa vem do seu perfil, que é um por usuário. Isso cobre a maioria
 * dos casos e mantém o valor univalorado, que é o que um piso ordinal exige. Mas nem toda operação
 * modela assim: há quem derive senioridade de um grupo, de um campo do domínio, ou de um sistema de
 * RH externo.
 *
 * <p>Registre um bean desta interface e ele passa a responder. Mesmo padrão de
 * {@code ArchbaseRoleResolver}: o framework não conhece o vocabulário da aplicação, então pergunta a
 * quem conhece.
 *
 * <p>Devolver {@code null} significa "não sei" — a resolução volta ao perfil, e daí à política de
 * {@code archbase.security.access-level.default}. Não é o mesmo que devolver {@link AccessLevel#READER}.
 */
@FunctionalInterface
public interface ArchbaseAccessLevelResolver {

    /**
     * O nível deste sujeito, ou {@code null} para deixar a resolução seguir seu curso normal.
     */
    AccessLevel resolveLevel(AccessSubject subject);
}
