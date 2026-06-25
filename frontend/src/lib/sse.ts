import type { ChatResultMetadata } from "@/types";
import { apiUrl } from "./api";

export interface SseHandlers {
  onMeta?: (content: string) => void;
  onChunk?: (content: string, assembled: string) => void;
  onResult?: (metadata: ChatResultMetadata) => void;
  onError?: (message: string) => void;
}

function parseSseEvent(rawEvent: string) {
  const lines = rawEvent.split("\n").filter(Boolean);
  let eventName = "message";
  let data = "";
  for (const line of lines) {
    if (line.startsWith("event:")) eventName = line.slice(6).trim();
    if (line.startsWith("data:")) data += line.slice(5).trim();
  }
  return { eventName, data };
}

export async function consumeEventStream(
  response: Response,
  handlers: SseHandlers
): Promise<string> {
  if (!response.body) {
    throw new Error("后端服务未返回流式响应");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";
  let assembled = "";

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split("\n\n");
    buffer = events.pop() || "";

    for (const rawEvent of events) {
      const parsed = parseSseEvent(rawEvent);
      if (!parsed.data) continue;

      const payload = JSON.parse(parsed.data);

      if (parsed.eventName === "meta") {
        handlers.onMeta?.(payload.content || "模型开始生成回复...");
      }

      if (parsed.eventName === "chunk") {
        assembled += payload.content;
        handlers.onChunk?.(payload.content, assembled);
      }

      if (parsed.eventName === "result") {
        handlers.onResult?.(payload.metadata);
      }

      if (parsed.eventName === "error") {
        handlers.onError?.(payload.content || "未知错误");
      }
    }
  }

  return assembled;
}

export async function streamChat(
  endpoint: string,
  body: object,
  handlers: SseHandlers
): Promise<string> {
  const response = await fetch(apiUrl(endpoint), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream",
    },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    throw new Error("后端服务未返回流式响应");
  }

  return consumeEventStream(response, handlers);
}
