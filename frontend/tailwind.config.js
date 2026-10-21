/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        surface: {
          0: '#06070b',
          1: '#0c0d14',
          2: '#12141d',
          3: '#191c27',
          4: '#212535',
        },
        border: {
          subtle: '#1c2030',
          DEFAULT: '#262b3d',
          strong: '#343a52',
        },
        text: {
          primary: '#e4e6ef',
          secondary: '#8b90a8',
          muted: '#505571',
        },
        accent: {
          DEFAULT: '#818cf8',
          dim: '#6366f1',
          bright: '#a5b4fc',
          glow: 'rgba(99, 102, 241, 0.15)',
        },
        mint: {
          DEFAULT: '#34d399',
          dim: '#059669',
          glow: 'rgba(52, 211, 153, 0.12)',
        },
        status: {
          success: '#34d399',
          warning: '#fbbf24',
          error: '#f87171',
          pending: '#fb923c',
        },
      },
      fontFamily: {
        sans: ['"IBM Plex Sans"', 'system-ui', 'sans-serif'],
        mono: ['"IBM Plex Mono"', 'ui-monospace', 'monospace'],
      },
      fontSize: {
        '2xs': ['0.6875rem', { lineHeight: '1rem' }],
      },
      backgroundImage: {
        'grid-pattern': `url("data:image/svg+xml,%3Csvg width='40' height='40' viewBox='0 0 40 40' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='%23ffffff' fill-opacity='0.02'%3E%3Cpath d='M0 0h1v40H0zM39 0h1v40h-1z'/%3E%3Cpath d='M0 0h40v1H0zM0 39h40v1H0z'/%3E%3C/g%3E%3C/svg%3E")`,
      },
    },
  },
  plugins: [],
}
