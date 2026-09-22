// 【核心修复】：确保这一行完整存在，且 'react' 是全小写！
import { useState, useEffect, useMemo } from 'react';
import ReactECharts from 'echarts-for-react';
import 'echarts-liquidfill';
import './InventoryDashboard.css';

// 模拟后端返回的仓储数据
const mockData = {
    kpi: [
        { name: '原材料总库存', value: 12580, unit: '吨', trend: '+2.5%', isUp: true },
        { name: '电阻片总库存', value: 84320, unit: '片', trend: '-1.2%', isUp: false },
        { name: '避雷器总库存', value: 3250, unit: '台', trend: '+5.8%', isUp: true },
        { name: '芯组总库存', value: 15600, unit: '组', trend: '+0.5%', isUp: true }
    ],
    material: [
        { name: '氧化锌', value: 4500 }, { name: '铜材', value: 3200 },
        { name: '铝材', value: 2800 }, { name: '硅橡胶', value: 2080 }
    ],
    resistorBatch: [
        { name: '批次 A23', value: 12000 }, { name: '批次 B19', value: 9500 },
        { name: '批次 C04', value: 8200 }, { name: '批次 D11', value: 6500 }, { name: '批次 E02', value: 4300 }
    ],
    arresterHealth: 0.78, // 避雷器库存健康度 (当前库存/安全库存)
    coreGroup: [
        { name: '10kV', max: 10000, value: 8500 },
        { name: '35kV', max: 10000, value: 6200 },
        { name: '110kV', max: 10000, value: 4800 },
        { name: '220kV', max: 10000, value: 3100 },
        { name: '500kV', max: 10000, value: 1200 }
    ]
};

export default function InventoryDashboard() {
    const [time, setTime] = useState(new Date());

    // useEffect(() => {
    //     // 每隔 2 小时自动刷新一次页面，释放内存并获取最新数据
    //     const refreshTimer = setTimeout(() => {
    //         window.location.reload();
    //     }, 2 * 60 * 60 * 1000);
    //
    //     return () => clearTimeout(refreshTimer);
    // }, []);

    // 更新时间
    useEffect(() => {
        const timer = setInterval(() => setTime(new Date()), 1000);
        return () => clearInterval(timer);
    }, []);

    // 1. 原材料分布 (南丁格尔玫瑰图)
    const materialOption = useMemo(() => ({
        tooltip: { trigger: 'item', formatter: '{b}: {c}吨 ({d}%)' },
        series: [{
            type: 'pie',
            radius: ['20%', '70%'],
            roseType: 'area',
            itemStyle: { borderRadius: 5, borderColor: '#030b1a', borderWidth: 2 },
            label: { color: '#8cd5ff', fontSize: 12 },
            data: mockData.material.map((item, i) => ({
                ...item,
                itemStyle: { color: ['#00f0ff', '#00aaff', '#0055ff', '#00ffaa'][i % 4] }
            }))
        }]
    }), []);

    // 2. 电阻片批次 TOP5 (横向渐变柱状图)
    const resistorOption = useMemo(() => ({
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: '15%', right: '10%', top: '10%', bottom: '10%' },
        xAxis: { type: 'value', axisLabel: { color: '#8cd5ff' }, splitLine: { lineStyle: { color: 'rgba(25, 186, 239, 0.1)' } } },
        yAxis: { type: 'category', data: mockData.resistorBatch.map(i => i.name).reverse(), axisLabel: { color: '#8cd5ff' }, axisTick: { show: false }, axisLine: { show: false } },
        series: [{
            type: 'bar',
            barWidth: 15,
            data: mockData.resistorBatch.map(i => i.value).reverse(),
            itemStyle: {
                borderRadius: [0, 10, 10, 0],
                color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#0055ff' }, { offset: 1, color: '#00f0ff' }] }
            }
        }]
    }), []);

    // 3. 避雷器库存健康度 (水波球 LiquidFill)
    const arresterOption = useMemo(() => ({
        series: [{
            type: 'liquidFill',
            data: [mockData.arresterHealth, mockData.arresterHealth - 0.05, mockData.arresterHealth - 0.1],
            color: ['#00f0ff', '#00aaff', '#0055ff'],
            backgroundStyle: { color: 'rgba(6, 25, 55, 0.8)', borderColor: '#00f0ff', borderWidth: 2 },
            outline: { show: false },
            label: {
                formatter: () => `${(mockData.arresterHealth * 100).toFixed(0)}%`,
                fontSize: 32,
                fontWeight: 'bold',
                color: '#fff'
            }
        }]
    }), []);

    // 4. 芯组电压等级分布 (雷达图)
    const coreGroupOption = useMemo(() => ({
        radar: {
            indicator: mockData.coreGroup.map(i => ({ name: i.name, max: i.max })),
            shape: 'polygon',
            splitNumber: 4,
            axisName: { color: '#8cd5ff', fontSize: 14 },
            splitLine: { lineStyle: { color: 'rgba(25, 186, 239, 0.2)' } },
            splitArea: { areaStyle: { color: ['rgba(25, 186, 239, 0.05)', 'rgba(25, 186, 239, 0.1)'] } },
            axisLine: { lineStyle: { color: 'rgba(25, 186, 239, 0.3)' } }
        },
        series: [{
            type: 'radar',
            data: [{
                value: mockData.coreGroup.map(i => i.value),
                name: '当前库存',
                areaStyle: { color: 'rgba(0, 240, 255, 0.3)' },
                lineStyle: { color: '#00f0ff', width: 2 },
                itemStyle: { color: '#00f0ff' }
            }]
        }]
    }), []);

    return (
        <div className="dashboard-container">
            {/* 顶部标题 */}
            <header className="dashboard-header">
                <h1 className="dashboard-title">智慧仓储库存驾驶舱</h1>
                <div className="header-time">
                    {time.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })}
                    {' '}
                    {time.toLocaleTimeString('zh-CN', { hour12: false })}
                </div>
            </header>

            {/* 主体内容 */}
            <main className="dashboard-body">
                {/* 第一行：核心 KPI 指标 */}
                <div className="kpi-row">
                    {mockData.kpi.map((item, index) => (
                        <div className="kpi-card" key={index}>
                            <div className="kpi-label">{item.name}</div>
                            <div className="kpi-value">
                                {item.value.toLocaleString()}
                                <span className="kpi-unit">{item.unit}</span>
                            </div>
                            <div className={`kpi-trend ${item.isUp ? 'trend-up' : 'trend-down'}`}>
                                较昨日 {item.trend}
                            </div>
                        </div>
                    ))}
                </div>

                {/* 第二行 左侧：原材料分布 */}
                <div className="chart-panel">
                    <div className="panel-title">原材料库存分布 (吨)</div>
                    <div className="chart-container">
                        <ReactECharts option={materialOption} style={{ height: '100%' }} />
                    </div>
                </div>

                {/* 第二行 中间：芯组雷达图 */}
                <div className="chart-panel">
                    <div className="panel-title">芯组各电压等级库存 (组)</div>
                    <div className="chart-container">
                        <ReactECharts option={coreGroupOption} style={{ height: '100%' }} />
                    </div>
                </div>

                {/* 第二行 右侧：避雷器健康度 */}
                <div className="chart-panel">
                    <div className="panel-title">避雷器库存健康度</div>
                    <div className="chart-container">
                        <ReactECharts option={arresterOption} style={{ height: '100%' }} />
                    </div>
                </div>

                {/* 第三行 左侧：电阻片批次 */}
                <div className="chart-panel" style={{ gridColumn: '1 / 3' }}>
                    <div className="panel-title">电阻片批次库存 TOP 5 (片)</div>
                    <div className="chart-container">
                        <ReactECharts option={resistorOption} style={{ height: '100%' }} />
                    </div>
                </div>

                {/* 第三行 右侧：预留或滚动列表 */}
                <div className="chart-panel">
                    <div className="panel-title">实时出入库动态</div>
                    <div className="chart-container" style={{ overflow: 'hidden', position: 'relative' }}>
                        <div style={{
                            position: 'absolute', width: '100%',
                            animation: 'scrollUp 20s linear infinite',
                            color: '#8cd5ff', fontSize: 14, lineHeight: '30px'
                        }}>
                            {[...Array(15)].map((_, i) => (
                                <div key={i} style={{ borderBottom: '1px dashed rgba(25,186,239,0.2)', padding: '5px 0' }}>
                  <span style={{ color: i % 2 === 0 ? '#52c41a' : '#f5222d' }}>
                    [{i % 2 === 0 ? '入库' : '出库'}]
                  </span>
                                    {' '}批次 B19 电阻片 {Math.floor(Math.random() * 500 + 100)} 片 - 10:2{i % 10}
                                </div>
                            ))}
                        </div>
                        <style>{`
              @keyframes scrollUp {
                0% { transform: translateY(0); }
                100% { transform: translateY(-50%); }
              }
            `}</style>
                    </div>
                </div>
            </main>
        </div>
    );
}
