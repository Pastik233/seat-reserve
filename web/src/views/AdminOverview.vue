<script setup>
import { ref, watch } from 'vue'
import dayjs from 'dayjs'
import request from '../api/request.js'

// el-date-picker daterange 需要绑定一个数组
const range = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const overview = ref(null)
const seatRank = ref([])
const violationRank = ref([])

function load() {
  const q = { from: range.value[0], to: range.value[1] }
  request.get('/admin/overview', { params: q }).then((d) => { overview.value = d })
  request.get('/admin/seats/rank', { params: q }).then((d) => { seatRank.value = d })
  request.get('/admin/violations/rank', { params: q }).then((d) => { violationRank.value = d })
}

watch(range, () => load(), { immediate: true })

const columns = [
  { prop: 'name', label: '名称' },
  { prop: 'extra', label: '所属/账号' },
  { prop: 'times', label: '次数' },
  { prop: 'hours', label: '使用时长(h)' },
]
</script>

<template>
  <div style="display: flex; flex-direction: column; gap: 16px">
    <h3 style="margin: 0">
      运营概览
    </h3>
    <el-date-picker
      v-model="range"
      type="daterange"
      :clearable="false"
      value-format="YYYY-MM-DD"
      format="YYYY-MM-DD"
      start-placeholder="开始日期"
      end-placeholder="结束日期"
      style="width: 260px"
    />

    <el-row :gutter="16">
      <el-col :span="6">
        <el-card>
          <el-statistic
            title="预约总数"
            :value="overview?.total ?? '-'"
          />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <el-statistic
            title="履约完成"
            :value="overview?.finished ?? '-'"
          />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <el-statistic
            title="违约作废"
            :value="overview?.expired ?? '-'"
          />
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <el-statistic
            title="履约率"
            :value="overview?.fulfillRate ?? '-'"
            :precision="3"
            suffix="%"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card header="座位使用排行">
          <el-table
            :data="seatRank"
            size="small"
            style="width: 100%"
          >
            <el-table-column
              v-for="c in columns"
              :key="c.prop"
              :prop="c.prop"
              :label="c.label"
            />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="违约排行">
          <el-table
            :data="violationRank"
            size="small"
            style="width: 100%"
          >
            <el-table-column
              v-for="c in columns"
              :key="c.prop"
              :prop="c.prop"
              :label="c.label"
            />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
