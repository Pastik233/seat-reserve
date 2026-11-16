package com.seat.reserve.web.dto;

import com.seat.reserve.domain.LibSeat;

import java.util.List;

public record FreeSeatsResult(
        int totalSeats,
        int freeCount,
        List<LibSeat> freeSeats
) {
}