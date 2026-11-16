package com.seat.reserve.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.seat.reserve.common.BizException;
import com.seat.reserve.common.JsonCache;
import com.seat.reserve.domain.LibRoom;
import com.seat.reserve.domain.LibSeat;
import com.seat.reserve.mapper.RoomMapper;
import com.seat.reserve.mapper.SeatMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private static final String KEY_ROOM_ALL = "cache:room:all";
    private static final String KEY_ROOM_SEATS = "cache:seat:room:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final RoomMapper roomMapper;
    private final SeatMapper seatMapper;
    private final JsonCache cache;

    public RoomService(RoomMapper roomMapper, SeatMapper seatMapper, JsonCache cache) {
        this.roomMapper = roomMapper;
        this.seatMapper = seatMapper;
        this.cache = cache;
    }

    public List<LibRoom> listRooms() {
        Optional<List<LibRoom>> cached = cache.get(KEY_ROOM_ALL, new TypeReference<List<LibRoom>>() { });
        if (cached.isPresent()) {
            log.debug("cache HIT {}", KEY_ROOM_ALL);
            return cached.get();
        }
        log.debug("cache MISS {}", KEY_ROOM_ALL);
        List<LibRoom> rooms = roomMapper.findAllOpen();
        cache.put(KEY_ROOM_ALL, rooms, TTL);
        return rooms;
    }

    public List<LibSeat> listSeats(Long roomId) {
        String key = KEY_ROOM_SEATS + roomId;
        Optional<List<LibSeat>> cached = cache.get(key, new TypeReference<List<LibSeat>>() { });
        if (cached.isPresent()) {
            log.debug("cache HIT {}", key);
            return cached.get();
        }
        log.debug("cache MISS {}", key);
        if (roomMapper.findById(roomId) == null) {
            throw BizException.notFound("阅览室不存在");
        }
        List<LibSeat> seats = seatMapper.listByRoom(roomId);
        cache.put(key, seats, TTL);
        return seats;
    }

    /** 管理员改房间/座位时调用（第 8 步做后台时用） */
    public void evictRoomCache(Long roomId) {
        cache.evict(KEY_ROOM_ALL, KEY_ROOM_SEATS + roomId);
    }
}