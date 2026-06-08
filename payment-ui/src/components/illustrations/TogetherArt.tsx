/** Overlapping team avatars — orange bento tile. */
export function TogetherArt() {
  const avatars = [
    { cx: 118, label: 'PG' },
    { cx: 150, label: 'AU' },
    { cx: 182, label: 'ST' },
  ];

  return (
    <svg className="bento-art__svg bento-art__svg--light" viewBox="0 0 300 120" fill="none" aria-hidden>
      {avatars.map((a, i) => (
        <g key={a.label}>
          <circle cx={a.cx} cy="58" r="28" fill="rgba(255,255,255,0.18)" stroke="rgba(255,255,255,0.55)" strokeWidth="1.2" />
          <text
            x={a.cx}
            y="62"
            textAnchor="middle"
            fill="#fff"
            fontSize="11"
            fontWeight="700"
            fontFamily="Inter, sans-serif"
            opacity={0.85 - i * 0.05}
          >
            {a.label}
          </text>
        </g>
      ))}
      <path
        d="M88 58h24M188 58h24"
        stroke="rgba(255,255,255,0.35)"
        strokeWidth="1.2"
        strokeDasharray="4 4"
      />
    </svg>
  );
}
