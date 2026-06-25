import { marked } from "marked";
import DOMPurify from "dompurify";

marked.setOptions({
  breaks: true,
  gfm: true,
});

export function renderMarkdown(text: string): string {
  if (!text?.trim()) return "";
  const raw = marked.parse(text, { async: false }) as string;
  return DOMPurify.sanitize(raw, {
    USE_PROFILES: { html: true },
  });
}

export function plainTextFromMarkdown(text: string): string {
  return text.replace(/[#*`_\[\]]/g, "").trim();
}
