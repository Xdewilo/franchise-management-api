package com.jeremyposada.franchise.domain.model;

import com.jeremyposada.franchise.domain.exception.DomainErrorCode;
import com.jeremyposada.franchise.domain.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Name — value object de nombre comercial")
class NameTest {

    @Nested
    @DisplayName("Construcción")
    class Construction {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "\t", "\n"})
        @DisplayName("rechaza nombres nulos o en blanco")
        void rejectsBlankNames(String candidate) {
            assertThatThrownBy(() -> new Name(candidate))
                    .isInstanceOf(ValidationException.class)
                    .extracting("code")
                    .isEqualTo(DomainErrorCode.INVALID_NAME);
        }

        @Test
        @DisplayName("rechaza nombres que exceden el máximo")
        void rejectsTooLongNames() {
            String tooLong = "a".repeat(Name.MAX_LENGTH + 1);

            assertThatThrownBy(() -> new Name(tooLong))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining(String.valueOf(Name.MAX_LENGTH));
        }

        @Test
        @DisplayName("admite un nombre justo en el límite")
        void acceptsNameAtTheLimit() {
            String atLimit = "a".repeat(Name.MAX_LENGTH);

            assertThat(new Name(atLimit).value()).hasSize(Name.MAX_LENGTH);
        }

        @Test
        @DisplayName("normaliza los espacios de los extremos")
        void trimsSurroundingWhitespace() {
            assertThat(new Name("  Vive Fresh  ").value()).isEqualTo("Vive Fresh");
        }

        @Test
        @DisplayName("mide la longitud sobre el texto ya normalizado")
        void measuresLengthAfterTrimming() {
            String padded = "  " + "a".repeat(Name.MAX_LENGTH) + "  ";

            assertThat(new Name(padded).value()).hasSize(Name.MAX_LENGTH);
        }
    }

    @Nested
    @DisplayName("Igualdad de negocio")
    class BusinessEquality {

        @Test
        @DisplayName("ignora mayúsculas y espacios al comparar")
        void ignoresCaseAndWhitespace() {
            assertThat(new Name("Vive Fresh").matches(new Name("  vive fresh "))).isTrue();
        }

        @Test
        @DisplayName("distingue nombres diferentes")
        void distinguishesDifferentNames() {
            assertThat(new Name("Vive Fresh").matches(new Name("Vive Salud"))).isFalse();
        }

        @Test
        @DisplayName("no coincide con un nombre nulo")
        void doesNotMatchNull() {
            assertThat(new Name("Vive Fresh").matches(null)).isFalse();
        }
    }
}
