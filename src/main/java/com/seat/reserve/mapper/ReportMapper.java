package com.seat.reserve.mapper;

import com.seat.reserve.web.dto.OverviewReport;
import com.seat.reserve.web.dto.RankRow;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReportMapper {

    OverviewReport overview(@Param("from") LocalDate from,
                            @Param("to") LocalDate to);

    List<RankRow> seatUsageRank(@Param("from") LocalDate from,
                                @Param("to") LocalDate to,
                                @Param("limit") int limit);

    List<RankRow> violationRank(@Param("from") LocalDate from,
                                @Param("to") LocalDate to,
                                @Param("limit") int limit);

    List<RankRow> hourlyHeat(@Param("from") LocalDate from,
                             @Param("to") LocalDate to);
}
