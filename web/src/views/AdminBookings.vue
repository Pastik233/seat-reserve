<script setup>
import { ref, onMounted } from 'vue'
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
const status = ref(undefined)   // undefined = 全部
const rows = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    rows.value = await request.get('/admin/bookings', {
      params: { date: date.value.format('YYYY-MM-DD'), ...(status.value ? { status: status.value } : {}) },
    })
  } finally {
    loading.value = false
  }
}

onMounted(() => load())

async function doCancel(row) {
  try {
    await ElMessageBox.confirm(`强制取消 ${row.nickname || row.username} 的预约？`, '提示', { type: 'warning' })
    await request.delete(`/admin/bookings/${row.id}`)
    ElMessage.success('已强制取消')
    load()
  } catch {
    // 用户取消
  }
}

const statusOptions = Object.entries(STATUS_META).map(([value, m]) => ({ value, label: m.text }))
</script>

<template>
  <el-card size="small">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span>预约管理</span>
        <div style="display: flex; gap: 12px; align-items: center">
          <el-date-picker
            v-model="date"
            type="date"
            :clearable="false"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
          />
          <el-select
            v-model="status"
            style="width: 120px"
            clearable
            placeholder="全部状态"
          >
            <el-option
              v-for="o in statusOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
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
      empty-text="这一天没有预约"
    >
      <el-table-column
        label="座位"
        width="90"
      >
        <template #default="{ row }">
          <strong>{{ row.seatNo || `#${row.seatId}` }}</strong>
        </template>
      </el-table-column>
      <el-table-column
        label="阅览室"
        width="120"
      >
        <template #default="{ row }">
          {{ row.roomName || '—' }}
        </template>
      </el-table-column>
      <el-table-column
        label="预约人"
        width="120"
      >
        <template #default="{ row }">
          {{ row.nickname || row.username || `#${row.userId}` }}
        </template>
      </el-table-column>
      <el-table-column
        label="时段"
        width="130"
      >
        <template #default="{ row }">
          {{ row.startTime.slice(0, 5) }} - {{ row.endTime.slice(0, 5) }}
        </template>
      </el-table-column>
      <el-table-column
        label="状态"
        width="100"
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
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-popconfirm
            v-if="row.status === 'BOOKED'"
            title="强制取消该预约？"
            @confirm="doCancel(row)"
          >
            <template #reference>
              <el-button
                link
                type="danger"
                size="small"
              >
                强制取消
              </el-button>
            </template>
          </el-popconfirm>
          <span
            v-else
            style="color: #bbb"
          >—</span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
