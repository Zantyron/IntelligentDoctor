declare global {
  interface Window {
    __APP_CONFIG__?: {
      API_BASE_URL?: string;
    };
  }
}

export function apiUrl(path: string): string {
  const base =
    localStorage.getItem("API_BASE_URL") ||
    window.__APP_CONFIG__?.API_BASE_URL ||
    "";
  return `${base.replace(/\/$/, "")}${path}`;
}

export function createSessionId(): string {
  return `session-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
}

export async function copyText(text: string): Promise<void> {
  const value = String(text || "").trim();
  if (!value) return;
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(value);
    return;
  }
  const area = document.createElement("textarea");
  area.value = value;
  area.style.position = "fixed";
  area.style.opacity = "0";
  document.body.appendChild(area);
  area.select();
  document.execCommand("copy");
  area.remove();
}

export async function fetchJson<T>(
  path: string,
  options?: RequestInit
): Promise<T> {
  const response = await fetch(apiUrl(path), options);
  const payload = await response.json();
  if (!response.ok || !payload.success) {
    throw new Error(payload.message || "请求失败");
  }
  return payload.data as T;
}
