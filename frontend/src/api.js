const API_BASE = import.meta.env.VITE_API_BASE || '/api/v1'

export async function verifyClaim(claimText) {
  const res = await fetch(`${API_BASE}/claims/verify`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ claimText }),
  })

  if (!res.ok) {
    throw new Error(`Verification request failed (${res.status})`)
  }

  return res.json()
}
