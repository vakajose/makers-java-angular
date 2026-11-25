package com.makers.loans.domain.model;

import com.makers.loans.domain.exception.InvalidUserDataException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void createNormalizesEmailAndTrimsName() {
        User user = User.create("  Ana Perez  ", "  ANA@Test.com ", "hashed", Role.USER);

        assertThat(user.getName()).isEqualTo("Ana Perez");
        assertThat(user.getEmail()).isEqualTo("ana@test.com");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void createRejectsInvalidFields() {
        assertThatThrownBy(() -> User.create(" ", "a@b.com", "hash", Role.USER))
                .isInstanceOf(InvalidUserDataException.class);
        assertThatThrownBy(() -> User.create("Ana", "invalid-email", "hash", Role.USER))
                .isInstanceOf(InvalidUserDataException.class);
        assertThatThrownBy(() -> User.create("Ana", "a@b.com", "", Role.USER))
                .isInstanceOf(InvalidUserDataException.class);
        assertThatThrownBy(() -> User.create("Ana", "a@b.com", "hash", null))
                .isInstanceOf(InvalidUserDataException.class);
    }

    @Test
    void updateProfilePreservesPasswordHashWhenBlankOrNull() {
        User user = User.create("Ana", "ana@test.com", "oldHash", Role.USER);

        user.updateProfile("Ana Maria", "ana.maria@test.com", Role.ADMIN, null);
        assertThat(user.getPasswordHash()).isEqualTo("oldHash");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(user.getEmail()).isEqualTo("ana.maria@test.com");

        user.updateProfile("Ana Maria", "ana.maria@test.com", Role.ADMIN, "newHash");
        assertThat(user.getPasswordHash()).isEqualTo("newHash");
    }
}
