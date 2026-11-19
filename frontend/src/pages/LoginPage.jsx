import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { landingPathFor, useAuth } from '../context/AuthContext.jsx'
import logo from '../assets/pcc-logo.png'
import propertyImg from '../assets/property.jpg'

const features = [
  {
    title: 'Property Management',
    text: 'Manage buildings, apartments, tenants and rental agreements.',
    icon: (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8">
        <path strokeLinecap="round" strokeLinejoin="round" d="M3 21h18M5 21V7l7-4 7 4v14M9 9h1.5M9 13h1.5M13.5 9H15M13.5 13H15M9 17h6v4H9z" />
      </svg>
    ),
  },
  {
    title: 'Land Management',
    text: 'Manage land parcels, ownership records and transactions.',
    icon: (
      <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8">
        <path strokeLinecap="round" strokeLinejoin="round" d="M9 20l-5.5-2.5v-13L9 7l6-2.5L20.5 7v13L15 17.5 9 20zm0-13v13m6-15.5v13" />
      </svg>
    ),
  },
]

export default function LoginPage() {
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    const form = new FormData(e.target)
    try {
      const user = await login(form.get('username'), form.get('password'))
      navigate(landingPathFor(user), { replace: true })
    } catch (err) {
      setError(err.message || 'Unable to sign in. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="grid min-h-screen w-full bg-white lg:grid-cols-[1.15fr_1fr]">
      {/* ── Left branding panel ─────────────────────────────── */}
      <section className="relative hidden flex-col justify-between overflow-hidden bg-gradient-to-br from-pcc-950 via-pcc-800 to-pcc-600 p-12 text-white lg:flex xl:p-16">
        {/* decorative blobs */}
        <div className="pointer-events-none absolute -top-32 -left-32 h-96 w-96 rounded-full bg-pcc-500/30 blur-3xl animate-blob" />
        <div className="pointer-events-none absolute -bottom-40 -right-24 h-[28rem] w-[28rem] rounded-full bg-fuchsia-500/25 blur-3xl animate-blob [animation-delay:-6s]" />
        <div className="pointer-events-none absolute top-1/3 right-1/4 h-64 w-64 rounded-full bg-pcc-300/20 blur-3xl animate-blob [animation-delay:-10s]" />
        {/* subtle grid overlay */}
        <div className="pointer-events-none absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.05)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.05)_1px,transparent_1px)] bg-[size:56px_56px]" />

        {/* header */}
        <div className="relative flex items-center gap-3 animate-fade-in">
          <img
            src={logo}
            alt="PCC Cameroon logo"
            className="h-12 w-12 rounded-full bg-white object-contain p-1 ring-2 ring-white/40 animate-pulse-glow"
          />
          <span className="text-lg font-semibold tracking-wide">PCC Cameroon</span>
        </div>

        {/* hero copy */}
        <div className="relative mt-10 max-w-xl">
          <h1 className="text-4xl font-bold leading-tight tracking-tight animate-slide-right xl:text-5xl">
            Property &amp; <span className="text-pcc-300">Land Management</span>
          </h1>
          <p className="mt-4 text-base leading-relaxed text-pcc-100/90 animate-slide-right [animation-delay:120ms] xl:text-lg">
            Manage your properties, buildings, apartments and land assets from
            one centralized platform.
          </p>
        </div>

        {/* image + features */}
        <div className="relative mt-10 flex items-center gap-8">
          <ul className="flex-1 space-y-5">
            {features.map((f, i) => (
              <li
                key={f.title}
                className="flex items-start gap-4 animate-slide-right"
                style={{ animationDelay: `${240 + i * 140}ms` }}
              >
                <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-white/10 text-pcc-200 ring-1 ring-white/20 backdrop-blur transition-transform duration-300 hover:scale-110">
                  {f.icon}
                </span>
                <div>
                  <h3 className="font-semibold">{f.title}</h3>
                  <p className="mt-1 text-sm leading-relaxed text-pcc-100/80">{f.text}</p>
                </div>
              </li>
            ))}
          </ul>

          <div className="relative hidden w-64 shrink-0 animate-float xl:block 2xl:w-80">
            <img
              src={propertyImg}
              alt="Property management"
              className="w-full rounded-2xl object-cover shadow-2xl shadow-pcc-950/50 ring-1 ring-white/30"
            />
            <div className="absolute -bottom-4 -left-6 rounded-xl bg-white/95 px-4 py-2 text-xs font-semibold text-pcc-800 shadow-lg backdrop-blur animate-slide-up [animation-delay:500ms]">
              Centralized Assets Platform
            </div>
          </div>
        </div>

        {/* footer */}
        <p className="relative mt-10 text-sm text-pcc-200/80 animate-fade-in [animation-delay:600ms]">
          © PCC Cameroon · {new Date().getFullYear()}
        </p>
      </section>

      {/* ── Right login panel ───────────────────────────────── */}
      <section className="relative flex items-center justify-center overflow-hidden bg-slate-50 p-6 sm:p-10">
        {/* soft decorative gradients */}
        <div className="pointer-events-none absolute -top-24 -right-24 h-72 w-72 rounded-full bg-pcc-200/50 blur-3xl" />
        <div className="pointer-events-none absolute -bottom-24 -left-24 h-72 w-72 rounded-full bg-pcc-300/40 blur-3xl" />

        <div className="relative w-full max-w-md animate-slide-left">
          {/* mobile-only brand header */}
          <div className="mb-8 flex items-center justify-center gap-3 lg:hidden">
            <img src={logo} alt="PCC Cameroon logo" className="h-12 w-12 rounded-full ring-2 ring-pcc-200" />
            <div>
              <p className="font-semibold text-slate-900">PCC Cameroon</p>
              <p className="text-xs text-slate-500">Property &amp; Land Management</p>
            </div>
          </div>

          {/* card */}
          <div className="rounded-3xl border border-slate-100 bg-white p-8 shadow-2xl shadow-pcc-500/10 sm:p-10">
            <div className="mb-8 flex flex-col items-center text-center">
              <img
                src={logo}
                alt="PCC logo"
                className="mb-5 h-20 w-20 rounded-full bg-white object-contain p-1.5 shadow-lg shadow-pcc-500/20 ring-4 ring-pcc-100 animate-pulse-glow"
              />
              <h2 className="text-2xl font-bold tracking-tight text-slate-900">Welcome Back</h2>
              <p className="mt-1.5 text-sm text-slate-500">
                Sign in to PCC Property Management System
              </p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-5">
              {error && (
                <div className="flex items-center gap-2.5 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600 ring-1 ring-red-200 animate-slide-up" role="alert">
                  <svg className="h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v4m0 4h.01M12 3a9 9 0 100 18 9 9 0 000-18z" />
                  </svg>
                  {error}
                </div>
              )}
              <div className="animate-slide-up [animation-delay:150ms]">
                <label htmlFor="username" className="mb-1.5 block text-sm font-medium text-slate-700">
                  Username
                </label>
                <div className="group relative">
                  <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-400 transition-colors group-focus-within:text-pcc-600">
                    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM5 21a7 7 0 0114 0" />
                    </svg>
                  </span>
                  <input
                    id="username"
                    name="username"
                    type="text"
                    required
                    autoComplete="username"
                    placeholder="Enter your username"
                    className="w-full rounded-xl border border-slate-200 bg-slate-50 py-3 pl-11 pr-4 text-sm text-slate-900 outline-none transition-all duration-300 placeholder:text-slate-400 focus:border-pcc-500 focus:bg-white focus:ring-4 focus:ring-pcc-500/15"
                  />
                </div>
              </div>

              <div className="animate-slide-up [animation-delay:250ms]">
                <label htmlFor="password" className="mb-1.5 block text-sm font-medium text-slate-700">
                  Password
                </label>
                <div className="group relative">
                  <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3.5 text-slate-400 transition-colors group-focus-within:text-pcc-600">
                    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                    </svg>
                  </span>
                  <input
                    id="password"
                    name="password"
                    type={showPassword ? 'text' : 'password'}
                    required
                    autoComplete="current-password"
                    placeholder="Enter your password"
                    className="w-full rounded-xl border border-slate-200 bg-slate-50 py-3 pl-11 pr-12 text-sm text-slate-900 outline-none transition-all duration-300 placeholder:text-slate-400 focus:border-pcc-500 focus:bg-white focus:ring-4 focus:ring-pcc-500/15"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((v) => !v)}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                    className="absolute inset-y-0 right-0 flex items-center pr-3.5 text-slate-400 transition-colors hover:text-pcc-600"
                  >
                    {showPassword ? (
                      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M3 3l18 18M10.6 10.7a2 2 0 002.7 2.7M7.4 7.5A9.8 9.8 0 003 12a9.9 9.9 0 0015.6 4.6M12 5c5 0 9 7 9 7a17 17 0 01-2.2 2.9" />
                      </svg>
                    ) : (
                      <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.8">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>
                    )}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="group relative mt-2 w-full overflow-hidden rounded-xl bg-gradient-to-r from-pcc-700 via-pcc-600 to-pcc-500 py-3.5 text-sm font-semibold text-white shadow-lg shadow-pcc-500/30 transition-all duration-300 animate-slide-up [animation-delay:350ms] hover:-translate-y-0.5 hover:shadow-xl hover:shadow-pcc-500/40 focus:outline-none focus:ring-4 focus:ring-pcc-500/40 active:translate-y-0 disabled:cursor-not-allowed disabled:opacity-80"
              >
                <span className="absolute inset-0 -translate-x-full bg-gradient-to-r from-transparent via-white/25 to-transparent transition-transform duration-700 group-hover:translate-x-full" />
                <span className="relative flex items-center justify-center gap-2">
                  {loading ? (
                    <>
                      <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-90" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z" />
                      </svg>
                      Signing in…
                    </>
                  ) : (
                    'Login'
                  )}
                </span>
              </button>
            </form>
          </div>

          <p className="mt-6 text-center text-xs text-slate-400 animate-fade-in [animation-delay:500ms] lg:hidden">
            © PCC Cameroon · {new Date().getFullYear()}
          </p>
        </div>
      </section>
    </div>
  )
}
