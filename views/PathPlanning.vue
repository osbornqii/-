<template>
  <div class="page">
    <el-card>
      <div style="display:flex; align-items:center; gap:8px; margin-bottom:12px">
        <el-icon color="#36a3e0"><MapLocation /></el-icon>
        <span style="font-weight:700">路径规划</span>
        <el-tag v-if="selectedSlot" style="margin-left:auto" type="info" effect="light">时段：{{ selectedSlot }}</el-tag>
      </div>

      <div class="controls">
        <div style="display:flex; align-items:center; gap:8px">
          <span style="font-size:14px; color:#606266">预计配送日期:</span>
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            size="default"
            style="width:160px"
          />
        </div>
        <el-select v-model="selectedSlot" placeholder="请选择配送时段" style="width:180px">
          <el-option v-for="t in timeSlots" :key="t" :label="t" :value="t" />
        </el-select>
        <el-button type="success" :loading="calculating" @click="calculate">计算路径</el-button>
        <el-button type="primary" :loading="loading" @click="loadLatest">查询路径</el-button>
      </div>
    </el-card>

    <div class="cards">
      <el-row :gutter="16">
        <el-col v-for="c in vehicleCards" :key="c.vehicleNo" :xs="24" :sm="12" :md="8">
          <el-card :style="cardStyle(c.color)">
            <div class="card-head">
              <div class="card-title">
                <span class="dot" :style="{ background: c.color }"></span>
                <span>车辆 {{ c.vehicleNo }}（{{ c.vehicleNumber }}）</span>
              </div>
              <div class="card-actions">
                <el-tag :type="c.load > vehicleCapacity ? 'danger' : 'success'" effect="light">
                  载重 {{ c.load.toFixed(2) }}/{{ vehicleCapacity }}kg
                </el-tag>
                <el-button size="small" :disabled="!c.hasTask" @click="toggleRoute(c.vehicleNo - 1)">
                  {{ c.showRoute ? "关闭路径" : "查看路径" }}
                </el-button>
              </div>
            </div>

            <div class="card-line">
              <span class="label">车辆编号</span>
              <span class="value">{{ c.vehicleNumber }}</span>
            </div>
            <div class="card-line">
              <span class="label">距离</span>
              <span class="value">{{ c.distanceKm.toFixed(2) }} km</span>
            </div>
            <div class="card-line">
              <span class="label">路线顺序</span>
            </div>
            <div class="route">
              <div v-if="!c.stops || c.stops.length === 0" style="color:#99a; font-size:12px">无任务</div>
              <el-space v-else wrap size="small">
                <el-tag v-for="(name, idx) in c.stops" :key="idx" size="small" effect="light">
                  {{ name }}
                </el-tag>
              </el-space>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div class="map-area">
      <div v-if="!browserAk" class="ak-warning">未配置 VITE_BAIDU_MAP_AK_BROWSER，无法加载地图</div>
      <div class="map-wrapper">
        <div class="map-controls">
          <el-button-group>
            <el-button size="small" @click="zoomIn"><el-icon><Plus /></el-icon></el-button>
            <el-button size="small" @click="zoomOut"><el-icon><Minus /></el-icon></el-button>
            <el-button size="small" @click="resetView"><el-icon><Refresh /></el-icon></el-button>
          </el-button-group>
        </div>
        <div id="map" class="map"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { MapLocation, Minus, Plus, Refresh } from "@element-plus/icons-vue";
import http from "../api/http";

const timeSlots = [
  "09:00~10:00", "10:00~11:00", "11:00~12:00", "12:00~13:00",
  "13:00~14:00", "14:00~15:00", "15:00~16:00", "16:00~17:00",
  "17:00~18:00", "18:00~19:00", "19:00~20:00", "20:00~21:00"
];
const selectedDate = ref(new Date());
const selectedSlot = ref("09:00~10:00");
const calculating = ref(false);
const loading = ref(false);

// 监听日期和时段变化，自动加载
watch([selectedDate, selectedSlot], () => {
  loadLatest();
}, { immediate: false });

const browserAk = import.meta.env.VITE_BAIDU_MAP_AK_BROWSER;
const serverAk = import.meta.env.VITE_BAIDU_MAP_AK_SERVER;

// 坐标转换函数集合

// WGS84转GCJ02（火星坐标）
function wgs84ToGcj02(lng, lat) {
  const pi = 3.14159265358979324;
  const a = 6378245.0;
  const ee = 0.00669342162296594323;
  
  if (outOfChina(lng, lat)) {
    return { lng, lat };
  }
  
  let dLat = transformLat(lng - 105.0, lat - 35.0);
  let dLng = transformLng(lng - 105.0, lat - 35.0);
  const radLat = lat / 180.0 * pi;
  let magic = Math.sin(radLat);
  magic = 1 - ee * magic * magic;
  const sqrtMagic = Math.sqrt(magic);
  dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
  dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
  
  return {
    lng: lng + dLng,
    lat: lat + dLat
  };
}

// GCJ02转BD09（百度坐标）
function gcj02ToBd09(lng, lat) {
  const pi = 3.14159265358979324;
  const x = lng;
  const y = lat;
  const z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * pi / 180);
  const theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * pi / 180);
  
  return {
    lng: z * Math.cos(theta) + 0.0065,
    lat: z * Math.sin(theta) + 0.006
  };
}

// WGS84转BD09（百度坐标）
function wgs84ToBd09(lng, lat) {
  const gcj02 = wgs84ToGcj02(lng, lat);
  return gcj02ToBd09(gcj02.lng, gcj02.lat);
}

// 辅助函数
function transformLat(x, y) {
  const pi = 3.14159265358979324;
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
  ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
  ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
  ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
  return ret;
}

function transformLng(x, y) {
  const pi = 3.14159265358979324;
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
  ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
  ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
  ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x * pi / 30.0)) * 2.0 / 3.0;
  return ret;
}

function outOfChina(lng, lat) {
  return !(lng > 73.66 && lng < 135.05 && lat > 3.86 && lat < 53.55);
}

const deliveryPoints = [
  { id: 0, name: "快递站", lng: 110.300710, lat: 25.106313 },
  { id: 1, name: "图书馆", lng: 110.298548, lat: 25.101785 },
  { id: 2, name: "研究生公寓", lng: 110.290096, lat: 25.097971 },
  { id: 3, name: "新5期大门", lng: 110.293150, lat: 25.097369 },
  { id: 4, name: "容园", lng: 110.289969, lat: 25.098731 },
  { id: 5, name: "5期宿舍", lng: 110.292174, lat: 25.097044 },
  { id: 6, name: "雅园", lng: 110.300053, lat: 25.105328 },
  { id: 7, name: "恬园", lng: 110.294803, lat: 25.098692 },
  { id: 8, name: "一期学生公寓", lng: 110.297748, lat: 25.105215 },
  { id: 9, name: "生命科学学院", lng: 110.291542, lat: 25.101342 },
  { id: 10, name: "环境与资源学院", lng: 110.292259, lat: 25.101475 },
  { id: 11, name: "法学院", lng: 110.293623, lat: 25.104240 },
  { id: 12, name: "马克思主义学院", lng: 110.293902, lat: 25.104161 },
  { id: 13, name: "政治与行政学院", lng: 110.294101, lat: 25.104385 },
  { id: 14, name: "经济管理学院", lng: 110.294863, lat: 25.103937 },
  { id: 15, name: "厚藩楼", lng: 110.297023, lat: 25.099948 },
  { id: 16, name: "公共体育教研部", lng: 110.293449, lat: 25.104865 },
  { id: 17, name: "综合体育馆", lng: 110.289793, lat: 25.102530 },
  { id: 18, name: "文学院", lng: 110.293882, lat: 25.103393 },
  { id: 19, name: "美术学院", lng: 110.299679, lat: 25.103821 },
  { id: 20, name: "后勤接待培训中心静园宾馆", lng: 110.300410, lat: 25.104019 }
];

const vehicleCapacity = 15;
const vehicleBindings = ref([]);
const vehicleColors = ["#d81e06", "#1d953f", "#1296db", "#f4ea2a", "#f20c00", "#1296db", "#ffce54", "#34c38f"];

let mapInstance = null;
let baseOverlays = [];
let routeOverlays = [];

const currentPaths = ref([]);
const routeCache = new Map();

const vehicleCards = ref([]);

async function loadVehicles() {
  try {
    const res = await http.get("/api/vehicles");
    const list = Array.isArray(res.data) ? res.data : [];
    vehicleBindings.value = list.map(v => v.vehicleNumber);
    vehicleCards.value = list.map((v, i) => ({
      vehicleNo: i + 1,
      vehicleNumber: v.vehicleNumber,
      color: vehicleColors[i % vehicleColors.length],
      stops: [],
      load: 0,
      distanceKm: 0,
      hasTask: false,
      showRoute: true
    }));
    currentPaths.value = list.map(() => "");
  } catch (e) {
    console.error("loadVehicles failed", e);
    ElMessage.error("获取车辆列表失败");
  }
}

function cardStyle(color) {
  return {
    borderTop: `4px solid ${color}`,
    background: "#fff"
  };
}

//加载百度地图
function loadBaiduMap() {
  if (!browserAk) return Promise.resolve(false);
  if (window.BMapGL) return Promise.resolve(true);
  if (window.__baiduMapLoadingPromise) return window.__baiduMapLoadingPromise;

  window.__baiduMapLoadingPromise = new Promise(resolve => {
    window.BMAP_PROTOCOL = "https";

    const existingCss = document.getElementById("baidu-map-css");
    if (!existingCss) {
      const link = document.createElement("link");
      link.id = "baidu-map-css";
      link.rel = "stylesheet";
      link.href = "https://api.map.baidu.com/res/webgl/10/bmap.css";
      document.head.appendChild(link);
    }

    const script = document.createElement("script");
    script.id = "baidu-map-script";
    script.src = `https://api.map.baidu.com/getscript?type=webgl&v=1.0&ak=${browserAk}`;
    script.onload = () => resolve(Boolean(window.BMapGL));
    script.onerror = () => resolve(false);
    document.head.appendChild(script);
  });

  return window.__baiduMapLoadingPromise;
}

function initMap() {
  if (!window.BMapGL) return;
  mapInstance = new window.BMapGL.Map("map");
  const point = deliveryPoints[0];
  const center = new window.BMapGL.Point(point.lng, point.lat);
  mapInstance.centerAndZoom(center, 16);
  mapInstance.enableScrollWheelZoom(true);
  renderBaseMarkers();
}

function renderBaseMarkers() {
  if (!mapInstance || !window.BMapGL) return;
  baseOverlays = [];
  const bounds = new window.BMapGL.Bounds();
  for (const p of deliveryPoints) {
    const point = new window.BMapGL.Point(p.lng, p.lat);
    bounds.extend(point);
    const marker = new window.BMapGL.Marker(point);
    const label = new window.BMapGL.Label(`${p.id} ${p.name}`, { position: point, offset: new window.BMapGL.Size(10, -20) });
    label.setStyle({
      color: "#222",
      border: "1px solid #999",
      backgroundColor: "#fff",
      padding: "2px 6px",
      borderRadius: "4px",
      fontSize: "12px"
    });
    mapInstance.addOverlay(marker);
    mapInstance.addOverlay(label);
    baseOverlays.push(marker, label);
  }
  mapInstance.setViewport(bounds);
}

function clearRoutes() {
  if (!mapInstance) return;
  for (const o of routeOverlays) {
    mapInstance.removeOverlay(o);
  }
  routeOverlays = [];
}

function resetMap() {
  if (!mapInstance) return;
  for (const o of baseOverlays) {
    mapInstance.removeOverlay(o);
  }
  baseOverlays = [];
  clearRoutes();
  renderBaseMarkers();
}

function zoomIn() {
  if (!mapInstance) return;
  mapInstance.zoomIn();
}

function zoomOut() {
  if (!mapInstance) return;
  mapInstance.zoomOut();
}

function resetView() {
  resetMap();
}

function toLocalDateTimeForSlot(slot, date) {
  if (!slot) return null;
  const startPart = String(slot).split("~")[0];
  const [h, m] = startPart.split(":").map(v => Number(v));
  
  // 确保使用 local date 组件构建，避免 UTC 转换偏差
  const d = date ? new Date(date) : new Date();
  const year = d.getFullYear();
  const month = d.getMonth();
  const day = d.getDate();
  
  const target = new Date(year, month, day, Number.isFinite(h) ? h : 0, Number.isFinite(m) ? m : 0, 0, 0);
  
  const pad = n => String(n).padStart(2, "0");
  const yStr = target.getFullYear();
  const mStr = pad(target.getMonth() + 1);
  const dStr = pad(target.getDate());
  const hhStr = pad(target.getHours());
  const mmStr = pad(target.getMinutes());
  const ssStr = pad(target.getSeconds());
  
  return `${yStr}-${mStr}-${dStr}T${hhStr}:${mmStr}:${ssStr}`;
}

function isSameSlot(localDateTimeText, slot) {
  if (!localDateTimeText || !slot) return false;
  const d = new Date(localDateTimeText);
  if (Number.isNaN(d.getTime())) return false;
  const now = new Date();
  const startPart = String(slot).split("~")[0];
  const [h] = startPart.split(":").map(v => Number(v));
  return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() && d.getDate() === now.getDate() && d.getHours() === h;
}

function parsePathStrings(payload) {
  if (!payload) return { totalDistance: null, paths: [] };

  if (Array.isArray(payload.paths)) {
    return { totalDistance: payload.totalDistance ?? null, paths: payload.paths.map(String) };
  }

  const text = typeof payload === "string" ? payload : String(payload.planningResult ?? "");
  if (!text) return { totalDistance: null, paths: [] };

  const trimmed = text.trim();
  if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
    try {
      const obj = JSON.parse(trimmed);
      if (Array.isArray(obj?.paths)) {
        return { totalDistance: obj.totalDistance ?? null, paths: obj.paths.map(String) };
      }
    } catch (e) {
    }
  }

  const matches = trimmed.match(/0(?:-\d+)+-0/g) || [];
  return { totalDistance: null, paths: matches };
}

function pathToIds(pathStr) {
  if (!pathStr) return [];
  return pathStr
    .split("-")
    .map(s => s.trim())
    .filter(Boolean)
    .map(s => Number(s))
    .filter(n => Number.isFinite(n));
}

function normalizeRoundTripIds(ids) {
  if (!Array.isArray(ids)) return [];
  const normalized = ids.filter(n => Number.isFinite(n));
  if (normalized.length === 0) return [];
  if (normalized[0] !== 0) normalized.unshift(0);
  if (normalized[normalized.length - 1] !== 0) normalized.push(0);
  return normalized;
}

function planningHasAnyRoute(planningText) {
  const t = String(planningText ?? "").trim();
  if (!t) return false;
  try {
    const obj = JSON.parse(t);
    const vehicles = Array.isArray(obj?.vehicles) ? obj.vehicles : [];
    if (vehicles.some(v => v?.hasTask === true)) return true;
    if (vehicles.some(v => String(v?.path || "").trim())) return true;
    const paths = Array.isArray(obj?.paths) ? obj.paths : [];
    if (paths.some(p => String(p || "").trim())) return true;
  } catch {
    const matches = t.match(/0(?:-\d+)+-0/g) || [];
    if (matches.length > 0) return true;
  }
  return false;
}

function getPointById(id) {
  return deliveryPoints.find(p => p.id === id) || null;
}

function getBdPointById(id) {
  const point = getPointById(id);
  if (!point) return null;
  return wgs84ToBd09(point.lng, point.lat);
}

function haversineMeters(a, b) {
  const lat1 = (a.lat * Math.PI) / 180;
  const lon1 = (a.lng * Math.PI) / 180;
  const lat2 = (b.lat * Math.PI) / 180;
  const lon2 = (b.lng * Math.PI) / 180;
  const dlat = lat2 - lat1;
  const dlon = lon2 - lon1;
  const sin1 = Math.sin(dlat / 2);
  const sin2 = Math.sin(dlon / 2);
  const aa = sin1 * sin1 + Math.cos(lat1) * Math.cos(lat2) * sin2 * sin2;
  const c = 2 * Math.asin(Math.sqrt(aa));
  return 6371000 * c;
}

function calcDistanceKm(ids) {
  if (!Array.isArray(ids) || ids.length < 2) return 0;
  let total = 0;
  for (let i = 0; i < ids.length - 1; i++) {
    const p1 = getPointById(ids[i]);
    const p2 = getPointById(ids[i + 1]);
    if (!p1 || !p2) continue;
    total += haversineMeters(p1, p2);
  }
  return total / 1000;
}

async function applyPlanning(planningResultText) {
  if (!planningResultText) return;
  
  let obj = null;
  try {
    const t = String(planningResultText).trim();
    if (t.startsWith("{") || t.startsWith("[")) {
      obj = JSON.parse(t);
    }
  } catch (e) {
    console.error("Parse planningResult failed", e);
  }

  if (!obj) {
    // 尝试正则提取路径串作为兜底
    const matches = String(planningResultText).match(/0(?:-\d+)+-0/g) || [];
    if (matches.length > 0) {
      obj = { paths: matches };
    } else {
      ElMessage.error("无法解析路径规划数据");
      return;
    }
  }

  // 确保车辆信息已加载，因为后面需要根据车辆编号匹配数据
  if (!vehicleCards.value || vehicleCards.value.length === 0) {
    await loadVehicles();
  }

  const fromVehicles = Array.isArray(obj?.vehicles) ? obj.vehicles : null;
  const depotName = deliveryPoints[0]?.name || "快递站";
  
  // 建立车辆编号到路径的映射
  const paths = [];
  vehicleCards.value.forEach((card, idx) => {
    let pStr = "";
    let normalizedIds = [];
    if (fromVehicles) {
      const v = fromVehicles.find(x => x?.vehicleNumber === card.vehicleNumber);
      pStr = v ? String(v.path || "") : "";
      if (!pStr && Array.isArray(obj.paths)) {
        pStr = String(obj.paths[idx] || "");
      }
    } else if (Array.isArray(obj.paths)) {
      pStr = String(obj.paths[idx] || "");
    }
    if (pStr) {
      normalizedIds = normalizeRoundTripIds(pathToIds(pStr));
      pStr = normalizedIds.length > 0 ? normalizedIds.join("-") : "";
    }

    paths[idx] = pStr;
    
    // 更新卡片上的统计信息
    if (!pStr) {
      card.stops = [];
      card.load = 0;
      card.distanceKm = 0;
      card.hasTask = false;
    } else {
      const v = fromVehicles ? fromVehicles.find(x => x?.vehicleNumber === card.vehicleNumber) : null;
      if (v) {
        const rawStops = Array.isArray(v.stops) ? v.stops : [];
        const stops = rawStops.map(String).filter(Boolean).map(stop => {
          // 统一快递站名称
          if (stop === "快递站(仓库)") return depotName;
          return stop;
        });
        if (stops.length > 0) {
          if (stops[0] !== depotName) stops.unshift(depotName);
          if (stops[stops.length - 1] !== depotName) stops.push(depotName);
        } else if (normalizedIds.length > 0) {
          stops.push(...normalizedIds.map(id => deliveryPoints.find(p => p.id === id)?.name || "未知"));
        }
        card.stops = stops;
        card.load = v.loadKg || 0;
        card.distanceKm = v.distanceKm || 0;
        card.hasTask = Boolean(v?.hasTask) || String(v?.path || "").trim().length > 0 || Number(v?.orderCount || 0) > 0;
      } else {
        card.stops = normalizedIds.map(id => deliveryPoints.find(p => p.id === id)?.name || "未知");
        card.load = 0;
        card.distanceKm = 0;
        card.hasTask = normalizedIds.length > 0;
      }
    }
  });

  currentPaths.value = paths;
  
  // 确保地图已初始化后再绘图
  if (mapInstance && window.BMapGL) {
    await drawRoutes(paths);
  } else {
    // 如果地图还没准备好，等待一小会儿再试一次
    setTimeout(() => {
      if (mapInstance && window.BMapGL) drawRoutes(paths);
    }, 1000);
  }
}

function toggleRoute(idx) {
  const card = vehicleCards.value[idx];
  if (!card || !card.hasTask) return;
  card.showRoute = !card.showRoute;
  drawRoutes(currentPaths.value);
}

//server端获取路网路径
async function getSegmentPoints(a, b) {
  if (!serverAk || !window.fetch) {
    // 回退到直线时，返回原始坐标
    return straightSegment(a, b);
  }
  const key = `${a.id}-${b.id}-walking`;
  if (routeCache.has(key)) return routeCache.get(key);
  try {
    const origin = `${a.lat},${a.lng}`;
    const destination = `${b.lat},${b.lng}`;
    // 使用步行规划 API 获取路网路径
    const url = `/baidu-map-api/directionlite/v1/walking?origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(destination)}&ak=${encodeURIComponent(serverAk)}&output=json`;
    const res = await fetch(url);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    
    // 检查百度地图 API 自身的业务状态码 (0 表示成功)
    if (data.status !== 0) {
      console.warn(`Baidu Map API error: ${data.message} (status: ${data.status})`);
      throw new Error(data.message || "API Error");
    }

    const routes = Array.isArray(data?.result?.routes) ? data.result.routes : [];
    if (routes.length === 0) throw new Error("No routes found");

    const steps = Array.isArray(routes[0]?.steps) ? routes[0].steps : [];
    const pts = [];
    for (const s of steps) {
      const path = String(s?.path || "");
      if (!path) continue;
      const pairs = path.split(";").map(x => x.trim()).filter(Boolean);
      for (const pair of pairs) {
        const [lngStr, latStr] = pair.split(",").map(x => x.trim());
        const lng = Number(lngStr);
        const lat = Number(latStr);
        if (Number.isFinite(lng) && Number.isFinite(lat)) pts.push({ lng, lat });
      }
    }
    
    if (pts.length < 2) throw new Error("Path too short");
    
    routeCache.set(key, pts);
    return pts;
  } catch (e) {
    console.error(`Route fetch failed (${a.name} -> ${b.name}):`, e.message);
    // 失败时回退到直线，但会在控制台打印警告
    const pts = straightSegment(a, b);
    routeCache.set(key, pts);
    return pts;
  }
}

function straightSegment(a, b) {
  return [
    { lng: a.lng, lat: a.lat },
    { lng: b.lng, lat: b.lat }
  ];
}
async function drawRoutes(pathStrings) {
  if (!mapInstance || !window.BMapGL) return;
  clearRoutes();

  const offsets = [
    [0, 0],
    [0.00003, 0.00003],
    [-0.00003, 0.00003],
    [0.00003, -0.00003],
    [-0.00003, -0.00003],
    [0.00006, 0.00006],
    [-0.00006, 0.00006],
    [0.00006, -0.00006]
  ];
  
  for (let idx = 0; idx < pathStrings.length; idx++) {
    const pathStr = pathStrings[idx];
    const card = vehicleCards.value[idx];
    if (!pathStr || !card || !card.showRoute) continue;

    const ids = pathToIds(pathStr);
    if (ids.length < 2) continue;

    const segPoints = [];
    for (let i = 0; i < ids.length - 1; i++) {
      const a = deliveryPoints.find(p => p.id === ids[i]);
      const b = deliveryPoints.find(p => p.id === ids[i + 1]);
      if (!a || !b) continue;
      
      const routePts = await getSegmentPoints(a, b);
      for (const pt of routePts) segPoints.push(pt);
    }

    const [dlng, dlat] = offsets[idx % offsets.length] || [0, 0];
    const points = segPoints.map(pt => {
      return new window.BMapGL.Point(pt.lng + dlng, pt.lat + dlat);
    });

    if (points.length < 2) continue;

    const polyline = new window.BMapGL.Polyline(points, {
      strokeColor: card.color || "#333",
      strokeWeight: 6,
      strokeOpacity: 0.85
    });
    mapInstance.addOverlay(polyline);
    routeOverlays.push(polyline);
    
    // 添加路线序号标注
    const midIdx = Math.floor(points.length / 2);
    const pos = points[midIdx];
    if (pos) {
      const label = new window.BMapGL.Label(`${card.vehicleNumber || (idx + 1)}`, { 
        position: pos, 
        offset: new window.BMapGL.Size(10, -20) 
      });
      label.setStyle({
        color: "#fff",
        backgroundColor: card.color || "#333",
        border: "none",
        padding: "2px 6px",
        borderRadius: "4px",
        fontSize: "12px"
      });
      mapInstance.addOverlay(label);
      routeOverlays.push(label);
    }
  }
}

async function calculate() {
  if (calculating.value) return;
  if (!selectedDate.value || !selectedSlot.value) {
    ElMessage.warning("请先选择日期和时段");
    return;
  }
  calculating.value = true;
  ElMessage.info("正在计算最优路径，请稍候...");
  
  try {
    const payload = {
      start: "快递站(仓库)",
      waypoints: [],
      destination: "快递站(仓库)",
      expectedDeliveryTime: toLocalDateTimeForSlot(selectedSlot.value, selectedDate.value),
      force: true
    };
    
    console.log("Requesting calculate with payload:", payload);
    const res = await http.post("/api/path/assigned-calculate", payload);
    console.log("Calculate response:", res.data);
    
    const planning = res?.data?.planningResult || "";
    if (!planning) {
      ElMessage.error("后端未返回路径规划结果，请检查后端日志");
      return;
    }
    
    let obj = null;
    try { 
      obj = JSON.parse(planning); 
    } catch (e) { 
      console.error("Parse planning result failed:", e);
      obj = null; 
    }
    
    if (obj && obj.totalOrdersFound === 0) {
      let debugMsg = "";
      if (obj.debug_totalOrdersInDb !== undefined) {
        debugMsg = ` (库总数: ${obj.debug_totalOrdersInDb}, 该日订单数: ${obj.debug_countWithExpectedDate || 0}, 该时段数: ${obj.debug_countInSlotBeforeFilter || 0})`;
      }
      ElMessage.warning(`该日期时段未找到匹配订单${debugMsg}。请确保订单已分配车辆且日期时段准确匹配。`);
    } else {
      const count = Number(obj?.totalOrdersFound ?? 0);
      if (count > 0) {
        ElMessage.success(`计算成功，有 ${count} 个待配送订单`);
      }
    }

    await applyPlanning(planning);
    const afterHasRoute = currentPaths.value.some(p => String(p || "").trim());
    if (!afterHasRoute) {
      const planner = obj?.debug_planner ?? "missing";
      const slotText = obj?.debug_slotStart && obj?.debug_slotEnd ? `${obj.debug_slotStart}~${obj.debug_slotEnd}` : "missing";
      const slotOrders = obj?.debug_countInSlotBeforeFilter ?? "missing";
      const candidateOrders = obj?.totalOrdersFound ?? "missing";
      const mappedNullCount = obj?.debug_mappedNullCount ?? "missing";
      const hint = planner === "missing" ? "（后端未更新/未重启或返回非JSON）" : "";
      ElMessage.warning(`本次计算未生成任何路线${hint}，planner=${planner}，slot=${slotText}，slotOrders=${slotOrders}，candidateOrders=${candidateOrders}，unmapped=${mappedNullCount}`);
    }
  } catch (e) {
    console.error("Calculate error:", e);
    const errMsg = e?.response?.data?.message || e?.message || "网络请求失败";
    ElMessage.error(`计算失败: ${errMsg}`);
  } finally {
    calculating.value = false;
  }
}

async function loadLatest() {
  if (loading.value) return;
  if (!selectedDate.value || !selectedSlot.value) {
    return;
  }
  loading.value = true;
  try {
    const expectedTime = toLocalDateTimeForSlot(selectedSlot.value, selectedDate.value);
    
    // 安全地解析 ISO 字符串为本地时间，避免时区偏差
    const parts = expectedTime.split(/[-T:]/);
    const start = new Date(parts[0], parts[1] - 1, parts[2], parts[3], parts[4], parts[5]);
    const end = new Date(start.getTime() + 60 * 60 * 1000 - 1000);
    
    const pad = n => String(n).padStart(2, "0");
    const formatIso = date => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
    
    const params = {
      start: formatIso(start),
      end: formatIso(end)
    };

    const res = await http.get("/api/paths", { params });
    const list = Array.isArray(res.data) ? res.data : [];
    
    const sorted = [...list].sort((a, b) => String(b?.computedAt || "").localeCompare(String(a?.computedAt || "")));
    const latestWithVehiclesAndRoute = sorted.find(p => String(p?.planningResult || "").includes('"vehicles"') && planningHasAnyRoute(p.planningResult));
    const latestWithRoute = latestWithVehiclesAndRoute || sorted.find(p => planningHasAnyRoute(p?.planningResult));
    
    if (!latestWithRoute?.planningResult) {
      const payload = {
        start: "快递站(仓库)",
        waypoints: [],
        destination: "快递站(仓库)",
        expectedDeliveryTime: expectedTime
      };
      const calcRes = await http.post("/api/path/assigned-calculate", payload);
      const planning = calcRes?.data?.planningResult || "";
      if (!planning) {
        clearRoutes();
        vehicleCards.value.forEach(c => {
          c.stops = [];
          c.load = 0;
          c.distanceKm = 0;
          c.hasTask = false;
        });
        return;
      }
      await applyPlanning(String(planning));
      return;
    }
    await applyPlanning(String(latestWithRoute.planningResult));
  } catch (e) {
    console.error("loadLatest failed", e);
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await loadVehicles();
  const ok = await loadBaiduMap();
  if (ok) {
    initMap();
    // 延迟一小段时间确保地图完全准备好后再加载数据
    setTimeout(() => {
      loadLatest();
    }, 500);
  }
});
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.controls {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.cards {
  width: 100%;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.card-actions {
  display: flex;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.card-line {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  margin-bottom: 4px;
}

.card-line .label {
  color: #666;
}

.route {
  margin-top: 8px;
}

.map-area {
  width: 100%;
}

.ak-warning {
  color: #b00;
  margin-bottom: 8px;
}

.map-wrapper {
  position: relative;
  width: 100%;
  height: 600px;
  border: 1px solid #ddd;
}

.map-controls {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 999;
}

.map {
  width: 100%;
  height: 100%;
}
</style>
