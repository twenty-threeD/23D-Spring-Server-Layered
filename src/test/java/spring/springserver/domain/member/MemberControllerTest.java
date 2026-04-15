package spring.springserver.domain.member;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import spring.springserver.domain.member.controller.MemberController;
import spring.springserver.domain.member.data.response.DeleteAccountResponse;
import spring.springserver.domain.member.service.MemberService;
import spring.springserver.global.handler.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberController memberController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void deleteAccount() throws Exception {
        given(memberService.deleteAccount(any(HttpServletRequest.class), any(HttpServletResponse.class)))
                .willReturn(DeleteAccountResponse.of("회원 탈퇴가 완료되었습니다."));

        mockMvc.perform(delete("/api/delete/account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.message").value("회원 탈퇴가 완료되었습니다."));

        verify(memberService).deleteAccount(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }
}
