<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import request from '../api/request.js'

const SLOT_MINUTES = 30   // 与后端 BookingValidator.SLOT_MINUTES 一致
const MAX_SLOTS = 8       // 单次最长 4 小时
const DEFAULT_SLOTS = 4   // 默认帮用户填 2 小时

const toMinutes = (hhmm) => {
  if (!hhmm) return NaN
  const [h, m] = hhmm.split(':').map(Number)
  return h * 60 + m
}
const toHHmm = (minutes) => {
  const h = String(Math.floor(minutes / 60)).padStart(2, '0')
  const m = String(minutes % 60).padStart(2, '0')
  return `${h}:${m}`
}
const cutSeconds = (t) => (t ? t.slice(0, 5) : '')

function buildSlots(room) {
  if (!room) return []
  const open = toMinutes(cutSeconds(room.openTime))
  const close = toMinutes(cutSeconds(room.closeTime))
  if (Number.isNaN(open) || Number.isNaN(close) || close - open < SLOT_MINUTES) return []
  const slots = []
  for (let m = open; m <= close; m += SLOT_MINUTES) slots.push(toHHmm(m))
  return slots
}

function earliestStartIndex(slots) {
  if (slots.length === 0) return 0
  const now = dayjs()
  const next = Math.ceil((now.hour() * 60 + now.minute()) / SLOT_MINUTES) * SLOT_MINUTES + SLOT_MINUTES
  const idx = slots.findIndex((t) => toMinutes(t) >= next)
  const lastStartable = Math.max(0, slots.length - 2)
  return idx < 0 ? lastStartable : Math.min(idx, lastStartable)
}

const router = useRouter()

const rooms = ref([])
const roomId = ref(null)
const date = ref(dayjs())
const start = ref('')
const end = ref('')
const loading = ref(false)
const result = ref(null)
const selectedSeatId = ref(null)

const room = computed(() => rooms.value.find((r) => r.id === roomId.value) || null)
const slots = computed(() => buildSlots(room.value))
const isToday = computed(() => date.value.isSame(dayjs(), 'day'))
const earliestIdx = computed(() => (isToday.value ? earliestStartIndex(slots.value) : 0))

// 派生值：房间/日期/时间变化时，非法选项在渲染时直接兜底，不回写 state
const startIdx = computed(() => slots.value.indexOf(start.value))
const effectiveStart = computed(() => (startIdx.value >= earliestIdx.value ? start.value : (slots.value[earliestIdx.value] ?? '')))
const fallbackEnd = computed(() => (slots.value[Math.min(slots.value.indexOf(effectiveStart.value) + DEFAULT_SLOTS, slots.value.length - 1)] ?? ''))
const effectiveEnd = computed(() => (
  slots.value.includes(end.value) && toMinutes(end.value) > toMinutes(effectiveStart.value)
    ? end.value : fallbackEnd.value
))

const selectedSeat = computed(() => result.value?.freeSeats?.find((s) => s.id === selectedSeatId.value) ?? null)
const startOptions = computed(() => slots.value.slice(earliestIdx.value, -1).map((t) => ({ label: t, value: t })))
const endOptions = computed(() => slots.value.filter((t) => {
  const diff = toMinutes(t) - toMinutes(effectiveStart.value)
  return diff >= SLOT_MINUTES && diff <= SLOT_MINUTES * MAX_SLOTS
}).map((t) => ({ label: t, value: t })))

onMounted(() => {
  request.get('/rooms').then((list) => {
    rooms.value = list
    if (list.length > 0) roomId.value = list[0].id
  })
})

// 房间/日期/时间变化 → 重新查空闲座位（watch 对应 React 的 effect 依赖数组）
watch([roomId, date, effectiveStart, effectiveEnd], () => {
  if (!roomId.value || !effectiveStart.value || !effectiveEnd.value) return
  loading.value = true
  request.get(`/rooms/${roomId.value}/seats/free`, {
    params: { date: date.value.format('YYYY-MM-DD'), start: effectiveStart.value, end: effectiveEnd.value },
  }).then((data) => { result.value = data })
    .catch(() => { result.value = null })
    .finally(() => { loading.value = false })
}, { immediate: true })

function handleStartChange(value) {
  start.value = value
  const i = slots.value.indexOf(value)
  const nextEnd = slots.value[Math.min(i + DEFAULT_SLOTS, slots.value.length - 1)]
  end.value = nextEnd && toMinutes(nextEnd) > toMinutes(value) ? nextEnd : ''
}

async function handleBook() {
  if (!selectedSeat.value) {
    ElMessage.warning('请先点一个座位')
    return
  }
  await request.post('/bookings', {
    seatId: selectedSeat.value.id,
    bookDate: date.value.format('YYYY-MM-DD'),
    startTime: effectiveStart.value,
    endTime: effectiveEnd.value,
    remark: '',
  })
  ElMessage.success('预约成功')
  router.push('/my')
}
</script>

<template>
  <div style="display: flex; flex-direction: column; gap: 16px">
    <el-card size="small">
      <div style="display: flex; flex-wrap: wrap; gap: 12px; align-items: center">
        <el-select
          v-model="roomId"
          style="width: 180px"
          placeholder="选阅览室"
        >
          <el-option
            v-for="r in rooms"
            :key="r.id"
            :label="`${r.name}（${r.floor}）`"
            :value="r.id"
          />
        </el-select>
        <el-date-picker
          v-model="date"
          type="date"
          :clearable="false"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
          :disabled-date="(d) => d && d.isBefore(dayjs().startOf('day'))"
        />
        <el-select
          :model-value="effectiveStart || undefined"
          style="width: 110px"
          placeholder="开始"
          @update:model-value="handleStartChange"
        >
          <el-option
            v-for="t in startOptions"
            :key="t.value"
            :label="t.label"
            :value="t.value"
          />
        </el-select>
        <span style="color: #888">至</span>
        <el-select
          v-model="end"
          style="width: 110px"
          placeholder="结束"
        >
          <el-option
            v-for="t in endOptions"
            :key="t.value"
            :label="t.label"
            :value="t.value"
          />
        </el-select>
        <el-button
          type="primary"
          :disabled="!selectedSeat || !effectiveStart || !effectiveEnd"
          @click="handleBook"
        >
          预约这个座位
        </el-button>
      </div>
    </el-card>

    <el-card
      v-loading="loading"
      size="small"
    >
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>{{ result ? `空闲 ${result.freeCount} / ${result.totalSeats} 个可用座位` : '座位' }}</span>
          <span style="color: #999; font-size: 13px">点座位高亮，再点"预约这个座位"提交</span>
        </div>
      </template>
      <el-empty
        v-if="!result || result.freeSeats.length === 0"
        :description="result ? '这个时段没有空闲座位了' : '暂无数据'"
      />
      <div
        v-else
        style="display: grid; grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); gap: 12px"
      >
        <el-card
          v-for="seat in result.freeSeats"
          :key="seat.id"
          shadow="hover"
          :style="{ textAlign: 'center', cursor: 'pointer',
                    borderColor: selectedSeatId === seat.id ? '#409eff' : undefined,
                    background: selectedSeatId === seat.id ? '#ecf5ff' : undefined }"
          @click="selectedSeatId = seat.id"
        >
          <div style="font-weight: 600">
            {{ seat.seatNo }}
          </div>
          <div style="font-size: 12px; color: #888">
            {{ seat.zone || '—' }}
          </div>
          <el-tag
            v-if="seat.hasPower === 1"
            type="success"
            size="small"
            style="margin-top: 6px"
          >
            插座
          </el-tag>
        </el-card>
      </div>
    </el-card>
  </div>
</template>
