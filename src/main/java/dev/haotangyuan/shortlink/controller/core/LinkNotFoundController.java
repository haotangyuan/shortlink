package dev.haotangyuan.shortlink.controller.core;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 短链接未找到控制层
 * @author: haotangyuan
 */
@Controller
public class LinkNotFoundController {

    /**
     * 短链接未找到页面
     * @return notfound.html
     */
    @RequestMapping("/page/notfound")
    public String notfound() {
        return "notfound";
    }
}
