import { useEffect, useState } from 'react'

/** Animated count-up (ease-out cubic) for dashboard stat cards. */
export default function useCountUp(target, duration = 1100) {
  const [value, setValue] = useState(0)

  useEffect(() => {
    if (target == null) return
    let start
    let raf
    const step = (t) => {
      start ??= t
      const p = Math.min((t - start) / duration, 1)
      setValue(Math.round(target * (1 - Math.pow(1 - p, 3))))
      if (p < 1) raf = requestAnimationFrame(step)
    }
    raf = requestAnimationFrame(step)
    return () => cancelAnimationFrame(raf)
  }, [target, duration])

  return value
}
