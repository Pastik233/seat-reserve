<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/request.js'

const rooms = ref([])
const roomId = ref(null)
const seats = ref([])
const loading = ref(false)

const dialogOpen = ref(false)
const editing = ref(null)   // null=新增，否则是在改的座位
const form = ref({ roomId: null, seatNo: '', zone: '', hasPower: false, status: true })

onMounted(() => {
  request.get('/rooms').then((list) => {
    rooms.value = list
    if (list.length > 0) roomId.value = list[0].id
  })
})

watch(roomId, () => loadSeats(), { immediate: true })

function loadSeats() {
  if (!roomId.value) return
  loading.value = true
  request.get('/admin/seats', { params: { roomId: roomId.value } })
    .then((d) => { seats.value = d })
    .catch(() => { seats.value = [] })
    .finally(() => { loading.value = false })
}

function openCreate() {
  editing.value = null
  form.value = { roomId: roomId.value, seatNo: '', zone: '', hasPower: false, status: true }
  dialogOpen.value = true
}

function openEdit(seat) {
  editing.value = seat
  form.value = {
    roomId: seat.roomId,
    seatNo: seat.seatNo,
    zone: seat.zone,
    hasPower: seat.hasPower === 1,
    status: seat.status === 1,
  }
  dialogOpen.value = true
}

async function handleSubmit() {
  const payload = {
    roomId: form.value.roomId,
    seatNo: form.value.seatNo,
    zone: form.value.zone || '',
    hasPower: form.value.hasPower ? 1 : 0,
    status: form.value.status ? 1 : 0,
  }
  if (editing.value) {
    await request.put(`/admin/seats/${editing.value.id}`, payload)
    ElMessage.success('座位已更新')
  } else {
    await request.post('/admin/seats', payload)
    ElMessage.success('座位已新增')
  }
  dialogOpen.value = false
  loadSeats()
}

async function handleDelete(seat) {
  try {
    await ElMessageBox.confirm('已被预约过的座位无法删除，只能置为维修。确认删除？', '提示', { type: 'warning' })
    await request.delete(`/admin/seats/${seat.id}`)
    ElMessage.success('座位已删除')
    loadSeats()
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <div>
    <h3 style="margin: 0 0 16px">
      座位管理
    </h3>
    <div style="display: flex; gap: 12px; margin-bottom: 16px; align-items: center">
      <el-select
        v-model="roomId"
        style="width: 200px"
        placeholder="选阅览室"
      >
        <el-option
          v-for="r in rooms"
          :key="r.id"
          :label="`${r.name}（${r.floor}）`"
          :value="r.id"
        />
      </el-select>
      <el-button
        type="primary"
        :disabled="!roomId"
        @click="openCreate"
      >
        新增座位
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="seats"
      size="small"
      style="width: 100%"
    >
      <el-table-column
        prop="seatNo"
        label="座位号"
        width="100"
      />
      <el-table-column
        label="分区"
        width="100"
      >
        <template #default="{ row }">
          {{ row.zone || '—' }}
        </template>
      </el-table-column>
      <el-table-column
        label="插座"
        width="80"
      >
        <template #default="{ row }">
          <el-tag
            :type="row.hasPower === 1 ? 'success' : 'info'"
            size="small"
          >
            {{ row.hasPower === 1 ? '有' : '无' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="状态"
        width="100"
      >
        <template #default="{ row }">
          <el-tag
            :type="row.status === 1 ? 'primary' : 'danger'"
            size="small"
          >
            {{ row.status === 1 ? '可用' : '维修' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            size="small"
            @click="openEdit(row)"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            size="small"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogOpen"
      :title="editing ? `编辑座位 ${editing.seatNo}` : '新增座位'"
      width="440px"
    >
      <el-form
        :model="form"
        label-position="top"
      >
        <el-form-item label="所属阅览室">
          <el-select
            v-model="form.roomId"
            :disabled="!!editing"
            style="width: 100%"
          >
            <el-option
              v-for="r in rooms"
              :key="r.id"
              :label="r.name"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="座位号">
          <el-input
            v-model="form.seatNo"
            :disabled="!!editing"
            maxlength="16"
            placeholder="如 A-07，同一阅览室内不可重复"
          />
        </el-form-item>
        <el-form-item label="分区">
          <el-input
            v-model="form.zone"
            maxlength="32"
            placeholder="如 A区 / 靠窗"
          />
        </el-form-item>
        <el-form-item label="是否有插座">
          <el-switch
            v-model="form.hasPower"
            active-text="有"
            inactive-text="无"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            active-text="可用"
            inactive-text="维修"
          />
          <div style="font-size: 12px; color: #999">
            关闭即置为维修，有进行中预约的座位不允许下架
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
