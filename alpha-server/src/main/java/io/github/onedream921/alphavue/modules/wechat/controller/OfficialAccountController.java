package io.github.onedream921.alphavue.modules.wechat.controller;

import io.github.onedream921.alphavue.modules.wechat.service.OfficialAccountService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public WeChat server callback. The provider requires a plaintext challenge response. */
@RestController
@RequestMapping("/api/wechat/official-account")
public class OfficialAccountController {
    private final OfficialAccountService service;
    public OfficialAccountController(OfficialAccountService service) { this.service = service; }

    @GetMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public String verify(@RequestParam String signature, @RequestParam String timestamp, @RequestParam String nonce,
                         @RequestParam("echostr") String challenge) {
        return service.verifies(signature, timestamp, nonce) ? challenge : "";
    }

    @PostMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public String receive(@RequestParam String signature, @RequestParam String timestamp, @RequestParam String nonce) {
        return service.verifies(signature, timestamp, nonce) ? "success" : "";
    }
}
