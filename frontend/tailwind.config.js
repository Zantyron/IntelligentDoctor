/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#e6f7f5",
          100: "#b3e8e1",
          200: "#80d9cd",
          300: "#4dcab9",
          400: "#26bfa8",
          500: "#0d9488",
          600: "#0b7d73",
          700: "#09665e",
          800: "#064f49",
          900: "#043834",
        },
        surface: {
          DEFAULT: "#ffffff",
          muted: "#faf8f6",
          warm: "#fef9f5",
          border: "#e0dcd8",
        },
        accent: {
          amber: "#f59e0b",
          rose: "#ef4444",
          green: "#16a34a",
        },
      },
      fontSize: {
        "body": ["16px", { lineHeight: "1.65" }],
        "body-lg": ["18px", { lineHeight: "1.55" }],
        "heading": ["22px", { lineHeight: "1.35" }],
        "heading-lg": ["26px", { lineHeight: "1.3" }],
        "heading-xl": ["30px", { lineHeight: "1.2" }],
        "btn": ["17px", { lineHeight: "1.4" }],
        "caption": ["14px", { lineHeight: "1.55" }],
      },
      fontFamily: {
        sans: [
          "PingFang SC",
          "Noto Sans SC",
          "Microsoft YaHei",
          "SimHei",
          "system-ui",
          "sans-serif",
        ],
      },
      borderRadius: {
        card: "14px",
        btn: "12px",
        bubble: "14px",
      },
      boxShadow: {
        card: "0 1px 8px rgba(0,0,0,0.05), 0 1px 2px rgba(0,0,0,0.03)",
        "card-hover": "0 2px 14px rgba(0,0,0,0.06), 0 1px 4px rgba(0,0,0,0.03)",
      },
      animation: {
        "fade-up": "fadeUp 0.4s ease-out",
        "fade-in": "fadeIn 0.3s ease-out",
        "slide-up": "slideUp 0.35s cubic-bezier(0.16, 1, 0.3, 1)",
        "slide-down": "slideDown 0.35s cubic-bezier(0.16, 1, 0.3, 1)",
        "pulse-soft": "pulseSoft 2s ease-in-out infinite",
        "bounce-in": "bounceIn 0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55)",
      },
      keyframes: {
        fadeUp: {
          "0%": { opacity: "0", transform: "translateY(16px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
        fadeIn: {
          "0%": { opacity: "0" },
          "100%": { opacity: "1" },
        },
        slideUp: {
          "0%": { transform: "translateY(100%)" },
          "100%": { transform: "translateY(0)" },
        },
        slideDown: {
          "0%": { transform: "translateY(0)" },
          "100%": { transform: "translateY(100%)" },
        },
        pulseSoft: {
          "0%, 100%": { opacity: "1" },
          "50%": { opacity: "0.6" },
        },
        bounceIn: {
          "0%": { opacity: "0", transform: "scale(0.3)" },
          "50%": { transform: "scale(1.05)" },
          "70%": { transform: "scale(0.9)" },
          "100%": { opacity: "1", transform: "scale(1)" },
        },
      },
    },
  },
  plugins: [require("@tailwindcss/typography")],
};
