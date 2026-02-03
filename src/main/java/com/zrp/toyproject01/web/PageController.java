package com.zrp.toyproject01.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @GetMapping("/")
    public String main() {
        return "main"; // main.html
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // login.html
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup"; // signup.html
    }

    @GetMapping("/post/write")
    public String postWrite() {
        return "post/write";
    }

    @GetMapping("/post/edit/{id}")
    public String postEdit(
        @PathVariable Long id
    ) {
        return "post/edit";
    }

    @GetMapping("/post/{id}")
    public String postDetail(
        @PathVariable Long id
    ) {
        return "post/detail";
    }
}