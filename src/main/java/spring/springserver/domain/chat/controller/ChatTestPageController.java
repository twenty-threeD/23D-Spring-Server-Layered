package spring.springserver.domain.chat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatTestPageController {

    @GetMapping("/chat-test")
    public String chatTestPage() {

        return "chat-test";
    }
}
