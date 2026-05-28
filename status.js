export const ORDER_STATUS_TEXT = {
  PENDING: "待配送",
  DISPATCHED: "配送中",
  DELIVERED: "已送达",
  CANCELLED: "已取消"
};

export const VEHICLE_STATUS_TEXT = {
  IDLE: "空闲中",
  BUSY: "任务中",
  OFFLINE: "离线"
};

export function orderStatusText(code) {
  return ORDER_STATUS_TEXT[code] || String(code || "");
}

export function vehicleStatusText(code) {
  return VEHICLE_STATUS_TEXT[code] || String(code || "");
}

export function orderStatusTagType(code) {
  if (code === "PENDING") return "warning";
  if (code === "DISPATCHED") return "primary";
  if (code === "DELIVERED") return "success";
  if (code === "CANCELLED") return "info";
  return "info";
}

export function vehicleStatusTagType(code) {
  if (code === "IDLE") return "success";
  if (code === "BUSY") return "warning";
  if (code === "OFFLINE") return "info";
  return "info";
}

