import { useCallback, useEffect, useState } from 'react';
import { Button, Card, DatePicker, Popconfirm, Space, Table, Tag, message } from 'antd';
import dayjs from 'dayjs';
import request from '../api/request.js';

// 状态 → 中文 + 标签颜色
const STATUS_META = {
  BOOKED:     { text: '待签到', color: 'blue' },
  CHECKED_IN: { text: '在座',   color: 'green' },
  LEAVING:    { text: '暂离中', color: 'orange' },
  FINISHED:   { text: '已完成', color: 'default' },
  CANCELLED:  { text: '已取消', color: 'default' },
  EXPIRED:    { text: '已失效', color: 'red' },
};

export default function MyBookings() {
  const [date, setDate] = useState(dayjs());
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [seatNoMap, setSeatNoMap] = useState({});   // { 座位id: 'A-02' }

  // 拉当天的预约列表；date 变了自动重拉
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const list = await request.get('/bookings/my', {
        params: { date: date.format('YYYY-MM-DD') },
      });
      setRows(list);
    } finally {
      setLoading(false);
    }
  }, [date]);

  // load() 内部会先 setLoading(true)，React 19 的新规则会对这次同步 setState 报警；
  // 但「发请求前先亮 loading」本身就是和外部系统同步，接受这一次同步渲染。
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { load(); }, [load]);

  // 一次性把座位号字典建好（/bookings/my 里只有 seatId）
  useEffect(() => {
    request.get('/rooms').then(async (rooms) => {
      // Promise.all：几个请求同时发，等都回来
      const seatLists = await Promise.all(rooms.map((r) => request.get(`/rooms/${r.id}/seats`)));
      const map = {};
      seatLists.forEach((seats) => {
        seats.forEach((s) => { map[s.id] = s.seatNo; });
      });
      setSeatNoMap(map);
    });
  }, []);

  // 统一的"调用状态接口 + 刷新列表"
  async function doAction(id, action, tips) {
    await request.post(`/bookings/${id}/${action}`);
    message.success(tips);
    load();
  }

  async function doCancel(id) {
    await request.delete(`/bookings/${id}`);
    message.success('已取消');
    load();
  }

  const columns = [
    {
      title: '座位',
      dataIndex: 'seatId',
      width: 100,
      render: (seatId) => <strong>{seatNoMap[seatId] || `#${seatId}`}</strong>,
    },
    {
      title: '时段',
      width: 160,
      // 后端 LocalTime 序列化成 "13:00:00"，slice(0,5) 取成 "13:00"
      render: (_, row) => `${row.startTime.slice(0, 5)} - ${row.endTime.slice(0, 5)}`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (status) => {
        const meta = STATUS_META[status] || { text: status, color: 'default' };
        return <Tag color={meta.color}>{meta.text}</Tag>;
      },
    },
    {
      title: '签到时间',
      dataIndex: 'checkinAt',
      width: 140,
      render: (v) => (v ? dayjs(v).format('HH:mm:ss') : '—'),
    },
    {
      title: '操作',
      render: (_, row) => {
        const buttons = [];

        if (row.status === 'BOOKED') {
          buttons.push(
            <Button key="checkin" size="small" type="primary"
                    onClick={() => doAction(row.id, 'checkin', '签到成功，开始计时')}>
              签到
            </Button>,
            <Popconfirm key="cancel" title="确定取消这次预约？" okText="取消预约" cancelText="算了"
                        onConfirm={() => doCancel(row.id)}>
              <Button size="small" danger>取消</Button>
            </Popconfirm>,
          );
        }
        if (row.status === 'CHECKED_IN') {
          buttons.push(
            <Button key="leave" size="small"
                    onClick={() => doAction(row.id, 'leave', '已暂离，30 分钟内记得回来')}>
              暂离
            </Button>,
          );
        }
        if (row.status === 'LEAVING') {
          buttons.push(
            <Button key="back" size="small" type="primary"
                    onClick={() => doAction(row.id, 'back', '欢迎回来')}>
              回来
            </Button>,
          );
        }

        return <Space size={4}>{buttons.length > 0 ? buttons : <span style={{ color: '#bbb' }}>—</span>}</Space>;
      },
    },
  ];

  return (
    <Card
      size="small"
      title="我的预约"
      extra={
        <Space>
          <DatePicker
            value={date}
            allowClear={false}
            onChange={(d) => d && setDate(d)}
            disabledDate={(d) => d && d.isBefore(dayjs().startOf('day'))}
          />
          <Button onClick={load}>刷新</Button>
        </Space>
      }
    >
      <Table
        rowKey="id"
        size="small"
        loading={loading}
        columns={columns}
        dataSource={rows}
        pagination={false}
        locale={{ emptyText: '这一天还没有预约' }}
      />
    </Card>
  );
}