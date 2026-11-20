package com.seat.reserve.service;

import com.seat.reserve.common.BizException;
import com.seat.reserve.domain.LibRoom;
import com.seat.reserve.domain.LibSeat;
import com.seat.reserve.mapper.RoomMapper;
import com.seat.reserve.mapper.SeatMapper;
import com.seat.reserve.web.dto.SeatUpsertRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 管理端座位管理：新增 / 修改 / 置维修 / 删除。
 *
 * <p>两个贯彻始终的设计点（面试可讲）：
 * <ol>
 *   <li><b>缓存一致性</b>：座位列表被 {@code RoomService} 缓存了 10 分钟，任何写操作
 *       成功后必须主动失效 {@code cache:seat:room:{roomId}} 与 {@code cache:room:all}，
 *       否则用户端会继续看到已下架的座位。</li>
 *   <li><b>引用完整性</b>：座位可能被预约表引用。置维修要检查"当前进行中的预约"，
 *       删除要检查"历史全部预约"——不能让一个有预约的座位凭空消失。</li>
 * </ol>
 */
@Slf4j
@Service
public class SeatAdminService {

    private final SeatMapper seatMapper;
    private final RoomMapper roomMapper;
    private final RoomService roomService;

    public SeatAdminService(SeatMapper seatMapper, RoomMapper roomMapper, RoomService roomService) {
        this.seatMapper = seatMapper;
        this.roomMapper = roomMapper;
        this.roomService = roomService;
    }

    /** 新增座位：房间须存在；座位号唯一由 DB 唯一键兜底，把异常翻译成友好提示 */
    @Transactional
    public LibSeat create(SeatUpsertRequest req) {
        LibRoom room = roomMapper.findById(req.roomId());
        if (room == null) {
            throw BizException.notFound("阅览室不存在");
        }
        LibSeat seat = new LibSeat();
        seat.setRoomId(req.roomId());
        seat.setSeatNo(req.seatNo().trim());
        seat.setZone(req.zone() == null ? "" : req.zone().trim());
        seat.setHasPower(req.hasPower());
        seat.setStatus(req.status());
        try {
            seatMapper.insert(seat);
        } catch (DuplicateKeyException e) {
            // uk_room_seat(room_id, seat_no) 冲突
            throw BizException.conflict("该阅览室已存在座位号 " + seat.getSeatNo());
        }
        roomService.evictRoomCache(req.roomId());
        log.info("[管理端] 新增座位 {} = {} {}", seat.getId(), seat.getSeatNo(), req.roomId());
        return seat;
    }

    /**
     * 修改座位：roomId / seatNo 不可变。
     * 存在性判断用 update 的影响行数，天然防并发删除后的空更新。
     */
    @Transactional
    public LibSeat update(Long id, SeatUpsertRequest req) {
        LibSeat old = seatMapper.findById(id);
        if (old == null) {
            throw BizException.notFound("座位不存在");
        }
        // 置维修前的一致性校验：有进行中的预约就不允许下架
        if (req.status() == 0 && seatMapper.countActiveBookingsBySeat(id) > 0) {
            throw BizException.conflict("该座位当前有进行中的预约，无法置为维修");
        }
        LibSeat seat = new LibSeat();
        seat.setId(id);
        seat.setZone(req.zone() == null ? "" : req.zone().trim());
        seat.setHasPower(req.hasPower());
        seat.setStatus(req.status());
        if (seatMapper.update(seat) == 0) {
            throw BizException.notFound("座位不存在");
        }
        roomService.evictRoomCache(old.getRoomId());
        log.info("[管理端] 修改座位 {} status={} hasPower={}", id, req.status(), req.hasPower());
        return seatMapper.findById(id);
    }

    /** 删除座位：仅允许删除从未被预约过的座位，否则留下指向不存在座位的孤儿记录 */
    @Transactional
    public void delete(Long id) {
        LibSeat seat = seatMapper.findById(id);
        if (seat == null) {
            throw BizException.notFound("座位不存在");
        }
        if (seatMapper.countBookingsBySeat(id) > 0) {
            throw BizException.conflict("该座位已有预约记录，不能删除；如需停用请改为「维修」状态");
        }
        if (seatMapper.deleteById(id) == 0) {
            throw BizException.notFound("座位不存在");
        }
        roomService.evictRoomCache(seat.getRoomId());
        log.info("[管理端] 删除座位 {} = {}", id, seat.getSeatNo());
    }

    /** 管理端列表：直接读库不走缓存，确保看到的是最新状态（含维修中的） */
    public List<LibSeat> listByRoom(Long roomId) {
        if (roomMapper.findById(roomId) == null) {
            throw BizException.notFound("阅览室不存在");
        }
        return seatMapper.listByRoom(roomId);
    }
}
