<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import request from '../api/request.js'

const STATUS_META = {
  BOOKED: { text: '待签到', type: 'primary' },
  CHECKED_IN: { text: '在座', type: 'success' },
  LEAVING: { text: '暂离中', type: 'warning' },
  FINISHED: { text: '已完成', type: 'info' },
  CANCELLED: { text: '已取消', type: 'info' },
  EXPIRED: { text: '已失效', type: 'danger' },
}

const date = ref(dayjs())
const rows = ref([])
const loading = ref(false)
const seatNoMap = ref({})

async function load() {
  loading.value = true
  try {
    rows.value = await request.get('/bookings/my', { params: { date: date.value.format('YYYY-MM-DD') } })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  request.get('/rooms').then(async (rooms) => {
    const seatLists = await Promise.all(rooms.map((r) => request.get(`/rooms/${r.id}/seats`)))
    const map = {}
    seatLists.forEach((seats) => seats.forEach((s) => { map[s.id] = s.seatNo }))
    seatNoMap.value = map
  })
})

watch(date, () => load(), { immediate: true })

async function doAction(id, action, tips) {
  await request.post(`/bookings/${id}/${action}`)
  ElMessage.success(tips)
  load()
}

async function doCancel(id) {
  try {
    await ElMessageBox.confirm('确定取消这次预约？', '提示', { type: 'warning' })
    await request.delete(`/bookings/${id}`)
    ElMessage.success('已取消')
    load()
  } catch {
    // 用户点了取消，什么都不做
  }
}
</script>

<template>
  <el-card size="small">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span>我的预约</span>
        <div style="display: flex; gap: 12px; align-items: center">
          <el-date-picker
            v-model="date"
            type="date"
            :clearable="false"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            :disabled-date="(d) => d && d.isBefore(dayjs().startOf('day'))"
          />
          <el-button @click="load">
            刷新
          </el-button>
        </div>
      </div>
    </template>

    <el-table
      v-loading="loading"
      :data="rows"
      size="small"
      style="width: 100%"
      empty-text="这一天还没有预约"
    >
      <el-table-column
        label="座位"
        width="100"
      >
        <template #default="{ row }">
          <strong>{{ seatNoMap[row.seatId] || `#${row.seatId}` }}</strong>
        </template>
      </el-table-column>
      <el-table-column
        label="时段"
        width="160"
      >
        <template #default="{ row }">
          {{ row.startTime.slice(0, 5) }} - {{ row.endTime.slice(0, 5) }}
        </template>
      </el-table-column>
      <el-table-column
        label="状态"
        width="110"
      >
        <template #default="{ row }">
          <el-tag
            :type="(STATUS_META[row.status] || {}).type || 'info'"
            size="small"
          >
            {{ (STATUS_META[row.status] || {}).text || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="签到时间"
        width="140"
      >
        <template #default="{ row }">
          {{ row.checkinAt ? dayjs(row.checkinAt).format('HH:mm:ss') : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <div style="display: flex; gap: 8px">
            <template v-if="row.status === 'BOOKED'">
              <el-button
                size="small"
                type="primary"
                @click="doAction(row.id, 'checkin', '签到成功，开始计时')"
              >
                签到
              </el-button>
              <el-button
                size="small"
                type="danger"
                plain
                @click="doCancel(row.id)"
              >
                取消
              </el-button>
            </template>
            <el-button
              v-else-if="row.status === 'CHECKED_IN'"
              size="small"
              @click="doAction(row.id, 'leave', '已暂离，30 分钟内记得回来')"
            >
              暂离
            </el-button>
            <el-button
              v-else-if="row.status === 'LEAVING'"
              size="small"
              type="primary"
              @click="doAction(row.id, 'back', '欢迎回来')"
            >
              回来
            </el-button>
            <span
              v-else
              style="color: #bbb"
            >—</span>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
