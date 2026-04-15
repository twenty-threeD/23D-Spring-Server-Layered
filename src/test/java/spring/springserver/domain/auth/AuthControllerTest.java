package spring.springserver.domain.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import spring.springserver.domain.auth.controller.AuthController;
import spring.springserver.domain.auth.data.request.ChangeUsernameRequest;
import spring.springserver.domain.auth.data.request.FindUsernameRequest;
import spring.springserver.domain.auth.data.request.PasswordResetRequest;
import spring.springserver.domain.auth.data.request.SignInRequest;
import spring.springserver.domain.auth.data.request.SignUpRequest;
import spring.springserver.domain.auth.data.response.ChangeUsernameResponse;
import spring.springserver.domain.auth.data.response.FindUsernameResponse;
import spring.springserver.domain.auth.data.response.PasswordResetResponse;
import spring.springserver.domain.auth.data.response.SignInResponse;
import spring.springserver.domain.auth.data.response.SignOutResponse;
import spring.springserver.domain.auth.data.response.SignUpResponse;
import spring.springserver.domain.auth.service.AuthService;
import spring.springserver.domain.member.entity.Role;
import spring.springserver.global.handler.GlobalExceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("회원가입 성공")
    void signUp() throws Exception {
        SignUpRequest request = new SignUpRequest(
                "Kim",
                "junhyeon",
                "kim@example.com",
                "01012345678",
                "Password1!",
                Role.USER
        );

        given(authService.signUp(request))
                .willReturn(SignUpResponse.of("회원가입 완료"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.message").value("회원가입 완료"));

        verify(authService).signUp(request);
    }

    @Test
    @DisplayName("로그인 성공")
    void signIn() throws Exception {
        SignInRequest request = new SignInRequest("junhyeon", "Password1!");

        given(authService.signIn(eq(request), any(HttpServletResponse.class)))
                .willReturn(SignInResponse.of("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));

        verify(authService).signIn(eq(request), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("로그아웃 성공")
    void signOut() throws Exception {
        given(authService.signOut(any(HttpServletRequest.class), any(HttpServletResponse.class)))
                .willReturn(SignOutResponse.of("로그아웃 완료"));

        mockMvc.perform(post("/api/auth/signout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.message").value("로그아웃 완료"));

        verify(authService).signOut(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("비인증 비밀번호 재설정 성공")
    void resetPasswordWithoutAuth() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest("junhyeon", "NewPassword1!");

        given(authService.resetPasswordWithoutAuth(request))
                .willReturn(PasswordResetResponse.of("비밀번호 재설정 완료"));

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.message").value("비밀번호 재설정 완료"));

        verify(authService).resetPasswordWithoutAuth(request);
    }

    @Test
    @DisplayName("인증 비밀번호 재설정 성공")
    void resetPasswordWithAuth() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest("junhyeon", "NewPassword1!");

        given(authService.resetPasswordWithAuth(any(HttpServletRequest.class), any(HttpServletResponse.class), eq(request)))
                .willReturn(PasswordResetResponse.of("비밀번호 변경 완료"));

        mockMvc.perform(post("/api/auth/password/reset/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.message").value("비밀번호 변경 완료"));

        verify(authService).resetPasswordWithAuth(any(HttpServletRequest.class), any(HttpServletResponse.class), eq(request));
    }

    @Test
    @DisplayName("아이디 찾기 성공")
    void findUsername() throws Exception {
        FindUsernameRequest request = new FindUsernameRequest("kim@example.com");

        given(authService.findUsername(request))
                .willReturn(FindUsernameResponse.of("junhyeon"));

        mockMvc.perform(get("/api/auth/username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.username").value("junhyeon"));

        verify(authService).findUsername(request);
    }

    @Test
    @DisplayName("아이디 변경 성공")
    void resetUsernameWithAuth() throws Exception {
        String requestBody = """
                {
                  "email": "kim@example.com",
                  "new_username": "newjunhyeon"
                }
                """;

        given(authService.resetUsernameWithAuth(any(HttpServletRequest.class), any(HttpServletResponse.class), any(ChangeUsernameRequest.class)))
                .willReturn(ChangeUsernameResponse.of("아이디 변경 완료"));

        mockMvc.perform(post("/api/auth/username/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.message").value("아이디 변경 완료"));

        ArgumentCaptor<ChangeUsernameRequest> captor = ArgumentCaptor.forClass(ChangeUsernameRequest.class);
        verify(authService).resetUsernameWithAuth(any(HttpServletRequest.class), any(HttpServletResponse.class), captor.capture());

        assertThat(captor.getValue().email()).isEqualTo("kim@example.com");
        assertThat(captor.getValue().newUsername()).isEqualTo("newjunhyeon");
    }

    @Test
    @DisplayName("회원가입 검증 실패")
    void signUp_validationFail() throws Exception {
        String invalidRequest = """
                {
                  "name": "",
                  "username": "",
                  "email": "invalid-email",
                  "phone": "",
                  "password": "short",
                  "role": null
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"));

        verify(authService, never()).signUp(any(SignUpRequest.class));
    }
}

