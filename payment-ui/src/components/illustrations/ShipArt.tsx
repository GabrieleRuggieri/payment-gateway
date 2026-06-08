/** Device wireframes — dark bento tile. */
export function ShipArt() {
  return (
    <svg className="bento-art__svg bento-art__svg--light" viewBox="0 0 300 140" fill="none" aria-hidden>
      <rect x="24" y="20" width="52" height="96" rx="10" stroke="currentColor" strokeWidth="1.2" opacity="0.5" />
      <rect x="32" y="28" width="36" height="56" rx="4" stroke="currentColor" strokeWidth="1" opacity="0.25" />
      <circle cx="50" cy="92" r="4" stroke="currentColor" strokeWidth="1" opacity="0.3" />

      <rect x="96" y="36" width="108" height="72" rx="8" stroke="currentColor" strokeWidth="1.2" opacity="0.5" />
      <rect x="104" y="44" width="92" height="48" rx="4" stroke="currentColor" strokeWidth="1" opacity="0.25" />
      <rect x="138" y="96" width="24" height="4" rx="2" fill="currentColor" opacity="0.2" />

      <rect x="224" y="28" width="56" height="84" rx="8" stroke="currentColor" strokeWidth="1.2" opacity="0.5" />
      <circle cx="236" cy="40" r="2" fill="currentColor" opacity="0.35" />
      <circle cx="244" cy="40" r="2" fill="currentColor" opacity="0.35" />
      <circle cx="252" cy="40" r="2" fill="currentColor" opacity="0.35" />
      <path d="M234 52h36M234 62h28M234 72h32" stroke="currentColor" strokeWidth="1" opacity="0.2" />
      <circle cx="252" cy="96" r="8" stroke="currentColor" strokeWidth="1" opacity="0.3" />
      <path d="M248 96h8M252 92v8" stroke="currentColor" strokeWidth="1" opacity="0.35" />
    </svg>
  );
}
