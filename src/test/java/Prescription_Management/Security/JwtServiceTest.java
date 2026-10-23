package Prescription_Management.Security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    // A valid 256-bit (64 hex chars) secret key
    private static final String SECRET = "2b016e78b2ac280d4661469307439555474408400dcf1bef82ad002c071513d9";
    private static final long EXPIRATION_MS = 3600000L; // 1 hour

    private UserDetails userDetails;
    private String token;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_MS);

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("DOC001")
                .password("irrelevant")
                .authorities("ROLE_DOCTOR")
                .build();

        token = jwtService.generateToken(userDetails, 1L, "DOCTOR", "Dr. Smith");
    }

    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {

        @Test
        @DisplayName("should produce a non-null token string")
        void shouldProduceNonNullToken() {
            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("should embed subject (registration number) in the token")
        void shouldEmbedSubject() {
            String extracted = jwtService.extractRegistrationNumber(token);
            assertThat(extracted).isEqualTo("DOC001");
        }

        @Test
        @DisplayName("should embed userId, role, and name as extra claims")
        void shouldEmbedExtraClaims() {
            Claims claims = jwtService.extractAllClaims(token);
            assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
            assertThat(claims.get("role", String.class)).isEqualTo("DOCTOR");
            assertThat(claims.get("name", String.class)).isEqualTo("Dr. Smith");
        }
    }

    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValid {

        @Test
        @DisplayName("should return true for a freshly generated token")
        void shouldReturnTrueForFreshToken() {
            assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
        }

        @Test
        @DisplayName("should return false when username does not match")
        void shouldReturnFalseWhenSubjectMismatch() {
            UserDetails otherUser = org.springframework.security.core.userdetails.User.builder()
                    .username("OTHER001")
                    .password("irrelevant")
                    .authorities("ROLE_DOCTOR")
                    .build();

            assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
        }

        @Test
        @DisplayName("should return false for an already-expired token")
        void shouldReturnFalseForExpiredToken() {
            // Generate a token with -1ms expiration (already expired)
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1L);
            String expiredToken = jwtService.generateToken(userDetails, 1L, "DOCTOR", "Dr. Smith");
            ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_MS);

            assertThat(jwtService.isTokenValid(expiredToken, userDetails)).isFalse();
        }
    }

    @Nested
    @DisplayName("extractRegistrationNumber()")
    class ExtractRegistrationNumber {

        @Test
        @DisplayName("should extract the correct registration number from a valid token")
        void shouldExtractRegistrationNumber() {
            assertThat(jwtService.extractRegistrationNumber(token)).isEqualTo("DOC001");
        }
    }

    @Nested
    @DisplayName("getExpirationTime()")
    class GetExpirationTime {

        @Test
        @DisplayName("should return configured expiration time in milliseconds")
        void shouldReturnConfiguredExpiration() {
            assertThat(jwtService.getExpirationTime()).isEqualTo(EXPIRATION_MS);
        }
    }
}
