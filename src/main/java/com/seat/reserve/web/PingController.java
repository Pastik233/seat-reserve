package com.seat.reserve.web;

import com.seat.reserve.common.R;
import com.seat.reserve.mapper.SeatMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PingController {

    private final SeatMapper seatMapper;

    /** 构造器注入：单构造器时 Spring 自动装配，不需要 @Autowired */
    public PingController(SeatMapper seatMapper) {
        this.seatMapper = seatMapper;
    }

    @GetMapping("/ping")
    public R<Map<String, Object>> ping() {
        return R.ok(Map.of(
                "seatCount", seatMapper.countAll(),
                "db", "mysql"
        ));
    }
}