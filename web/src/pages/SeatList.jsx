import { useEffect, useMemo, useState } from 'react';
import {
  Button, Card, Col, DatePicker, Empty, Row, Select, Space, Spin, Tag, message,
} from 'antd';
import { useNavigate } from 'react-router-dom';
import dayjs from 'dayjs';
import request from '../api/request.js';

// ---------- 时间工具 ----------
const SLOT_MINUTES = 30;   // 与后端 BookingValidator.SLOT_MINUTES 一致
const MAX_SLOTS = 8;       // 单次最长 4 小时 = 8 格，与后端 MAX_MINUTES 一致
const DEFAULT_SLOTS = 4;   // 默认帮用户填 2 小时

const toMinutes = (hhmm) => {
  if (!hhmm) return NaN;
  const [h, m] = hhmm.split(':').map(Number);
  return h * 60 + m;
};
const toHHmm = (minutes) => {
  const h = String(Math.floor(minutes / 60)).padStart(2, '0');
  const m = String(minutes % 60).padStart(2, '0');
  return `${h}:${m}`;
};
// 后端返回的 LocalTime 形如 "08:00:00"，统一裁成 "08:00"
const cutSeconds = (t) => (t ? t.slice(0, 5) : '');

/** 按阅览室的开放时间生成刻度；房间还没加载时返回空数组 */
function buildSlots(room) {
  if (!room) return [];
  const open = toMinutes(cutSeconds(room.openTime));
  const close = toMinutes(cutSeconds(room.closeTime));
  if (Number.isNaN(open) || Number.isNaN(close) || close - open < SLOT_MINUTES) return [];
  const slots = [];
  for (let m = open; m <= close; m += SLOT_MINUTES) slots.push(toHHmm(m));
  return slots;
}

/**
 * 今天最早还能选的刻度下标：「现在」向上取整到 30 分钟，再多留一格。
 * 以前这里死顶在 20:00，晚上打开页面时默认值本身就是个已经错过的时段，
 * 提交后会被后端判成「无法签到」，接着记一次违约。末尾留两格，保证挑得到结束时间。
 */
function earliestStartIndex(slots) {
  if (slots.length === 0) return 0;
  const now = dayjs();
  const next = Math.ceil((now.hour() * 60 + now.minute()) / SLOT_MINUTES) * SLOT_MINUTES
    + SLOT_MINUTES;
  const idx = slots.findIndex((t) => toMinutes(t) >= next);
  const lastStartable = Math.max(0, slots.length - 2);
  return idx < 0 ? lastStartable : Math.min(idx, lastStartable);
}

export default function SeatList() {
  const navigate = useNavigate();

  const [rooms, setRooms] = useState([]);              // 阅览室列表
  const [roomId, setRoomId] = useState(null);          // 当前选的阅览室
  const [date, setDate] = useState(dayjs());           // 当前选的日期（dayjs 对象）
  const [start, setStart] = useState('');              // 用户选的开始时间，空串表示还没选
  const [end, setEnd] = useState('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);          // { totalSeats, freeCount, freeSeats }
  const [selectedSeatId, setSelectedSeatId] = useState(null);

  const room = useMemo(() => rooms.find((r) => r.id === roomId) || null, [rooms, roomId]);
  const slots = useMemo(() => buildSlots(room), [room]);

  // 今天已经错过的刻度不能再当开始时间（后端会以「无法签到」拒绝）
  const earliestIdx = date.isSame(dayjs(), 'day') ? earliestStartIndex(slots) : 0;

  /*
   * 以下是「派生值」而不是 effect 回写 state：
   * 房间切换（08:00-22:00 ⇄ 08:30-21:30）或把日期切回今天时，
   * 用户之前选的时间可能已经不合法，这里在渲染时直接兜底成默认值，
   * 既不会出现「先渲染一帧非法值再纠正」的闪烁，也不用担心级联渲染。
   */
  const startIdx = slots.indexOf(start);
  const effectiveStart = startIdx >= earliestIdx ? start : (slots[earliestIdx] ?? '');
  const fallbackEnd = slots[Math.min(slots.indexOf(effectiveStart) + DEFAULT_SLOTS,
    slots.length - 1)] ?? '';
  const effectiveEnd = slots.includes(end) && toMinutes(end) > toMinutes(effectiveStart)
    ? end
    : fallbackEnd;

  // 座位也要校验：换时间后原来点的座位可能已经不在空闲列表里了
  const selectedSeat = useMemo(
    () => result?.freeSeats?.find((s) => s.id === selectedSeatId) ?? null,
    [result, selectedSeatId],
  );

  // ① 只在页面首次打开时跑一次：拉阅览室
  useEffect(() => {
    request.get('/rooms').then((list) => {
      setRooms(list);
      if (list.length > 0) setRoomId(list[0].id);
    });
  }, []);

  // ② 房间 / 日期 / 时间任一变化，就重新查空闲座位
  useEffect(() => {
    if (!roomId || !effectiveStart || !effectiveEnd) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoading(true);
    request
      .get(`/rooms/${roomId}/seats/free`, {
        params: { date: date.format('YYYY-MM-DD'), start: effectiveStart, end: effectiveEnd },
      })
      .then((data) => setResult(data))
      .catch(() => setResult(null))     // 400 的提示由拦截器弹，这里只清空结果
      .finally(() => setLoading(false));
  }, [roomId, date, effectiveStart, effectiveEnd]);

  // 选开始时间时，顺手把结束时间顶到 +2 小时，减少非法组合
  function handleStartChange(value) {
    setStart(value);
    const i = slots.indexOf(value);
    const nextEnd = slots[Math.min(i + DEFAULT_SLOTS, slots.length - 1)];
    setEnd(nextEnd && toMinutes(nextEnd) > toMinutes(value) ? nextEnd : '');
  }

  // 最后一格只能当结束时间，不能当开始时间
  const startOptions = useMemo(
    () => slots.slice(earliestIdx, -1).map((t) => ({ label: t, value: t })),
    [slots, earliestIdx],
  );
  // 结束时间只给"比开始晚 30 分钟 ~ 4 小时"的选项
  const endOptions = useMemo(() => slots
    .filter((t) => {
      const diff = toMinutes(t) - toMinutes(effectiveStart);
      return diff >= SLOT_MINUTES && diff <= SLOT_MINUTES * MAX_SLOTS;
    })
    .map((t) => ({ label: t, value: t })), [slots, effectiveStart]);

  async function handleBook() {
    if (!selectedSeat) {
      message.warning('请先点一个座位');
      return;
    }
    // 这里的 body 对应后端 CreateBookingRequest{seatId, bookDate, startTime, endTime, remark}
    await request.post('/bookings', {
      seatId: selectedSeat.id,
      bookDate: date.format('YYYY-MM-DD'),
      startTime: effectiveStart,
      endTime: effectiveEnd,
      remark: '',
    });
    message.success('预约成功');
    navigate('/my');          // 下单成功直接去"我的预约"看结果
  }

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      {/* ---------- 筛选区 ---------- */}
      <Card size="small">
        <Space wrap>
          <Select
            style={{ width: 180 }}
            value={roomId}
            onChange={setRoomId}
            options={rooms.map((r) => ({
              label: `${r.name}（${r.floor}）`,
              value: r.id,
            }))}
            placeholder="选阅览室"
          />

          <DatePicker
            value={date}
            onChange={(d) => d && setDate(d)}
            allowClear={false}
            // 过去的日子不让选
            disabledDate={(d) => d && d.isBefore(dayjs().startOf('day'))}
          />

          <Select style={{ width: 110 }} value={effectiveStart || undefined}
                  onChange={handleStartChange} options={startOptions} placeholder="开始" />
          <span style={{ color: '#888' }}>至</span>
          <Select style={{ width: 110 }} value={effectiveEnd || undefined}
                  onChange={setEnd} options={endOptions} placeholder="结束" />

          <Button type="primary" onClick={handleBook}
                  disabled={!selectedSeat || !effectiveStart || !effectiveEnd}>
            预约这个座位
          </Button>
        </Space>
      </Card>

      {/* ---------- 结果区 ---------- */}
      <Card
        size="small"
        title={result ? `空闲 ${result.freeCount} / ${result.totalSeats} 个可用座位` : '座位'}
        extra={<span style={{ color: '#999' }}>已选的座位会高亮，点"预约这个座位"提交</span>}
      >
        <Spin spinning={loading}>
          {result && result.freeSeats.length > 0 ? (
            <Row gutter={[12, 12]}>
              {result.freeSeats.map((seat) => {
                const active = selectedSeatId === seat.id;
                return (
                  <Col key={seat.id} xs={12} sm={8} md={6} lg={4}>
                    <Card
                      size="small"
                      hoverable
                      onClick={() => setSelectedSeatId(seat.id)}
                      style={{
                        textAlign: 'center',
                        borderColor: active ? '#1677ff' : undefined,
                        background: active ? '#e6f4ff' : undefined,
                      }}
                    >
                      <div style={{ fontWeight: 600 }}>{seat.seatNo}</div>
                      <div style={{ fontSize: 12, color: '#888' }}>{seat.zone || '—'}</div>
                      {seat.hasPower === 1 && (
                        <Tag color="green" style={{ marginTop: 6, marginRight: 0 }}>插座</Tag>
                      )}
                    </Card>
                  </Col>
                );
              })}
            </Row>
          ) : (
            <Empty description={result ? '这个时段没有空闲座位了' : '暂无数据'} />
          )}
        </Spin>
      </Card>
    </Space>
  );
}
