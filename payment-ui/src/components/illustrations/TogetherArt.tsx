/** Overlapping avatar wireframes — orange bento tile, Ship / Configure style. */
export function TogetherArt() {
  const avatars = [
    { cx: 108, cy: 78 },
    { cx: 140, cy: 68 },
    { cx: 172, cy: 78 },
    { cx: 204, cy: 72 },
  ];

  return (
    <svg className="bento-art__svg bento-art__svg--light" viewBox="0 0 280 140" fill="none" aria-hidden>
      <ellipse cx="140" cy="76" rx="118" ry="48" stroke="currentColor" strokeWidth="1.2" opacity="0.28" />

      {avatars.map((a, i) => (
        <g key={a.cx} opacity={0.55 + i * 0.1}>
          <circle cx={a.cx} cy={a.cy} r="26" stroke="currentColor" strokeWidth="1.2" />
          <circle cx={a.cx} cy={a.cy - 7} r="7" stroke="currentColor" strokeWidth="1" opacity="0.85" />
          <path
            d={`M${a.cx - 12} ${a.cy + 14} Q${a.cx} ${a.cy + 2} ${a.cx + 12} ${a.cy + 14}`}
            stroke="currentColor"
            strokeWidth="1"
            opacity="0.85"
          />
        </g>
      ))}

      {/* Chat bubble accent */}
      <rect x="36" y="28" width="64" height="28" rx="10" stroke="currentColor" strokeWidth="1.2" opacity="0.4" />
      <path d="M48 56 L54 62 L58 56" stroke="currentColor" strokeWidth="1" opacity="0.35" />
      <path d="M48 40h40M48 48h28" stroke="currentColor" strokeWidth="1" opacity="0.3" />
    </svg>
  );
}
