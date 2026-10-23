package Prescription_Management.Service;

import Prescription_Management.DTO.RequestDto.UserRequest;
import Prescription_Management.DTO.ResponseDto.UserResponse;
import Prescription_Management.Entity.Role;
import Prescription_Management.Entity.User;
import Prescription_Management.Exception.ResourceNotFoundException;
import Prescription_Management.Repository.UserRepository;
import Prescription_Management.Service.ServiceImpl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserServiceImpl userService;

    private User user;
    private UserRequest userRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Dr. Smith");
        user.setRegistrationNumber("DOC001");
        user.setPhoneNumber("+1234567890");
        user.setPassword("encoded_password");
        user.setRole(Role.DOCTOR);

        userRequest = new UserRequest("Dr. Smith", "DOC001", "+1234567890", "Password@123", Role.DOCTOR);
        userResponse = new UserResponse(1L, "Dr. Smith", "DOC001", "+1234567890", Role.DOCTOR);
    }

    @Nested
    @DisplayName("createUser()")
    class CreateUser {

        @Test
        @DisplayName("should create user successfully when registration number is unique")
        void shouldCreateUserSuccessfully() {
            given(userRepository.existsByRegistrationNumber("DOC001")).willReturn(false);
            given(passwordEncoder.encode("Password@123")).willReturn("encoded_password");
            given(userRepository.save(any(User.class))).willReturn(user);
            given(modelMapper.map(user, UserResponse.class)).willReturn(userResponse);

            UserResponse result = userService.createUser(userRequest);

            assertThat(result).isNotNull();
            assertThat(result.getRegistrationNumber()).isEqualTo("DOC001");
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when registration number already exists")
        void shouldThrowWhenRegistrationNumberDuplicate() {
            given(userRepository.existsByRegistrationNumber("DOC001")).willReturn(true);

            assertThatThrownBy(() -> userService.createUser(userRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Registration number already exists");

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when password is missing on creation")
        void shouldThrowWhenPasswordMissingOnCreate() {
            userRequest.setPassword(null);

            assertThatThrownBy(() -> userService.createUser(userRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Password is required");
        }

        @Test
        @DisplayName("should default role to DOCTOR when role is null")
        void shouldDefaultRoleToDoctorWhenNull() {
            userRequest.setRole(null);
            given(userRepository.existsByRegistrationNumber("DOC001")).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn("encoded");
            given(userRepository.save(any(User.class))).willAnswer(inv -> {
                User saved = inv.getArgument(0);
                assertThat(saved.getRole()).isEqualTo(Role.DOCTOR);
                return saved;
            });
            given(modelMapper.map(any(), eq(UserResponse.class))).willReturn(userResponse);

            userService.createUser(userRequest);
        }
    }

    @Nested
    @DisplayName("getUserById()")
    class GetUserById {

        @Test
        @DisplayName("should return user response when found")
        void shouldReturnUserWhenFound() {
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(modelMapper.map(user, UserResponse.class)).willReturn(userResponse);

            UserResponse result = userService.getUserById(1L);

            assertThat(result).isEqualTo(userResponse);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsers {

        @Test
        @DisplayName("should return paginated user responses")
        void shouldReturnPagedResults() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(user));
            given(userRepository.findAll(pageable)).willReturn(userPage);
            given(modelMapper.map(user, UserResponse.class)).willReturn(userResponse);

            Page<UserResponse> result = userService.getAllUsers(pageable);

            assertThat(result).hasSize(1);
            assertThat(result.getContent().get(0).getRegistrationNumber()).isEqualTo("DOC001");
        }

        @Test
        @DisplayName("should return empty page when no users exist")
        void shouldReturnEmptyPageWhenNoUsers() {
            Pageable pageable = PageRequest.of(0, 10);
            given(userRepository.findAll(pageable)).willReturn(Page.empty());

            Page<UserResponse> result = userService.getAllUsers(pageable);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateUser()")
    class UpdateUser {

        @Test
        @DisplayName("should update user successfully")
        void shouldUpdateUserSuccessfully() {
            UserRequest updatedData = new UserRequest("Dr. Updated", "DOC001", "+9876543210", null, Role.ADMIN);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.findByRegistrationNumber("DOC001")).willReturn(Optional.of(user));
            given(userRepository.save(any(User.class))).willReturn(user);
            given(modelMapper.map(any(User.class), eq(UserResponse.class))).willReturn(userResponse);

            userService.updateUser(1L, updatedData);

            then(userRepository).should().save(any(User.class));
            assertThat(user.getName()).isEqualTo("Dr. Updated");
            assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when registration number belongs to another user")
        void shouldThrowWhenRegistrationNumberTakenByAnotherUser() {
            User anotherUser = new User();
            anotherUser.setId(2L);
            anotherUser.setRegistrationNumber("DOC001");

            UserRequest updatedData = new UserRequest("New", "DOC001", "+1234567890", null, null);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(userRepository.findByRegistrationNumber("DOC001")).willReturn(Optional.of(anotherUser));

            assertThatThrownBy(() -> userService.updateUser(1L, updatedData))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Registration number already exists");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user to update does not exist")
        void shouldThrowWhenUserToUpdateNotFound() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(99L, userRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("should delete user when they have no records")
        void shouldDeleteUserWhenNoRecords() {
            user.getRecords().clear();
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            userService.deleteUser(1L);

            then(userRepository).should().delete(user);
        }

        @Test
        @DisplayName("should throw IllegalStateException when user has associated records")
        void shouldThrowWhenUserHasRecords() {
            Prescription_Management.Entity.Record record = new Prescription_Management.Entity.Record();
            user.getRecords().add(record);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.deleteUser(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot delete a user associated with prescription records");

            then(userRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user to delete does not exist")
        void shouldThrowWhenUserToDeleteNotFound() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
